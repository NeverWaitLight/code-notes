import logging
import textwrap
from collections import defaultdict
from http import HTTPStatus

import dashscope
from dashscope import Generation
from datasets import load_dataset
from sklearn.metrics import accuracy_score, classification_report
from tqdm import tqdm

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler("evaluation.log", encoding="utf-8"),
        logging.StreamHandler(),
    ],
)
logger = logging.getLogger(__name__)

API_KEY = "sk-a4e10b09167d48f686eca678dedd4dd6"

logger.info("正在加载数据集...")
dataset = load_dataset("SetFit/amazon_massive_intent_zh-CN", split="validation")
logger.info(f"数据集加载完成，共 {len(dataset)} 条样本")

INTENTS = [
    "calendar_remove",
    "transport_ticket",
    "qa_factoid",
    "iot_hue_lightdim",
    "datetime_convert",
    "iot_hue_lightup",
    "music_dislikeness",
    "iot_hue_lightoff",
    "alarm_query",
    "play_music",
    "music_likeness",
    "lists_query",
    "transport_taxi",
    "qa_stock",
    "iot_wemo_on",
    "email_query",
    "general_quirky",
    "general_joke",
    "music_query",
    "play_podcasts",
    "recommendation_movies",
    "social_post",
    "play_game",
    "transport_traffic",
    "email_querycontact",
    "recommendation_locations",
    "calendar_set",
    "play_audiobook",
    "calendar_query",
    "audio_volume_mute",
    "email_sendemail",
    "transport_query",
    "audio_volume_up",
    "music_settings",
    "lists_createoradd",
    "qa_currency",
    "email_addcontact",
    "play_radio",
    "audio_volume_down",
    "takeaway_order",
    "iot_wemo_off",
    "cooking_recipe",
    "general_greet",
    "weather_query",
    "alarm_set",
    "iot_hue_lighton",
    "iot_cleaning",
    "datetime_query",
    "qa_maths",
    "qa_definition",
    "cooking_query",
    "alarm_remove",
    "iot_coffee",
    "iot_hue_lightchange",
    "recommendation_events",
    "takeaway_query",
    "lists_remove",
    "social_query",
    "news_query",
]

SYSTEM_PROMPT_TEMPLATE = textwrap.dedent("""
    # Role
    你是一个资深的自然语言理解（NLU）专家，擅长从用户输入的文本中精准提取用户的真实意图。将意图归一化到给定的意图列表，注意中英文翻译时的准确性。
    
    # Task
    请分析用户输入的文本，并从给定的意图列表 {INTENTS} 中选择最匹配的一个。如果用户的输入不属于列表中的任何意图，请将其归类为 "unknown"。
    
    # Few-Shot Examples
    {FEW_SHOT_EXAMPLES}
    
    # Constraints
    1. **唯一性**：只返回一个最相关的意图。
    2. **客观性**：仅基于文本内容判断，不要过度解读。
    3. **格式化输出**：只输出 intent（意图名）
    4. **语言一致性**：推理理由请使用中文。
    
    # Execution Process
    1. 仔细阅读用户输入的文本。
    2. 参考上面的示例，理解不同意图的表达方式。
    3. 对比 {INTENTS} 中定义的各个意图及其潜在含义。
    4. 进行逻辑推理，排除干扰项。
    """).strip()


def extract_few_shot_examples(dataset, intents, max_total_examples=20):
    """
    从数据集中提取示例用于few-shot learning，总数限制为max_total_examples
    确保每个意图至少有一个示例，且所有示例文本都不重复
    """
    intent_examples = defaultdict(list)
    used_texts = set()
    total_count = 0
    max_total = max_total_examples

    # 第一阶段：为每个意图收集至少1个唯一示例
    for item in dataset:
        intent = item["label_text"]
        text = item["text"]

        if intent in intents and len(intent_examples[intent]) == 0:
            if text not in used_texts:
                intent_examples[intent].append(text)
                used_texts.add(text)
                total_count += 1

                if total_count >= max_total:
                    break

    # 第二阶段：为已有示例的意图继续添加更多唯一示例，直到达到总数限制
    if total_count < max_total:
        for item in dataset:
            if total_count >= max_total:
                break

            intent = item["label_text"]
            text = item["text"]

            if intent in intents and text not in used_texts:
                intent_examples[intent].append(text)
                used_texts.add(text)
                total_count += 1

    few_shot_text = "以下是各意图的示例：\n\n"
    for intent in intents:
        if intent in intent_examples and intent_examples[intent]:
            few_shot_text += f"意图: {intent}\n"
            for example in intent_examples[intent]:
                few_shot_text += f"  示例: {example}\n"
            few_shot_text += "\n"

    actual_count = sum(len(v) for v in intent_examples.values())
    covered_intents = len(
        [i for i in intents if i in intent_examples and intent_examples[i]]
    )
    logger.info(
        f"已提取 {actual_count} 个唯一few-shot示例（限制: {max_total}），覆盖 {covered_intents}/{len(intents)} 个意图"
    )
    return few_shot_text


def call_llm(text, few_shot_examples="", temperature=0.0):
    system_prompt = SYSTEM_PROMPT_TEMPLATE.format(
        INTENTS=str(INTENTS), FEW_SHOT_EXAMPLES=few_shot_examples
    )

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": text},
    ]

    dashscope.api_key = API_KEY

    logger.debug(f"调用LLM，输入文本: {text[:50]}...")
    response = Generation.call(
        model="qwen-flash",
        messages=messages,
        result_format="message",
        parameters={"temperature": temperature},
    )

    if response.status_code == HTTPStatus.OK:
        result = response.output.choices[0].message["content"]
        logger.debug(f"LLM返回结果: {result}")
        return result
    else:
        logger.error(f"请求失败: {response.message}")
        return "unknown"


# 3. 结果清理函数
def clean_output(output, intent_list):
    """
    因为 Agent 可能会输出多余的文字（如 "意图是：alarm_set"），
    我们需要通过关键词匹配提取出标准的意图标签。
    """
    output = output.lower().strip()
    for intent in intent_list:
        if intent in output:
            return intent
    return "unknown"  # 如果没匹配上，归类为未知


# 4. 执行验证逻辑
def run_evaluation(num_samples=None, max_few_shot_examples=20):
    # 获取所有唯一的意图标签列表
    all_intents = list(set(dataset["label_text"]))
    logger.info(f"数据集中共有 {len(all_intents)} 个不同的意图")

    # 提取few-shot示例
    logger.info("正在从数据集中提取few-shot示例...")
    few_shot_examples = extract_few_shot_examples(
        dataset, INTENTS, max_few_shot_examples
    )
    logger.info("Few-shot示例提取完成")

    y_true = []
    y_pred = []

    if num_samples is None:
        test_subset = dataset
        logger.info(f"开始测试完整数据集，共 {len(dataset)} 条样本...")
    else:
        test_subset = dataset.select(range(min(num_samples, len(dataset))))
        logger.info(f"开始测试 {len(test_subset)} 条样本...")

    correct_count = 0
    for idx, item in enumerate(tqdm(test_subset, desc="评估进度")):
        input_text = item["text"]
        true_label = item["label_text"]

        # 获取 Agent 输出
        raw_output = call_llm(input_text, few_shot_examples)

        # 清理输出
        predicted_label = clean_output(raw_output, all_intents)

        is_correct = predicted_label == true_label
        if is_correct:
            correct_count += 1

        logger.debug(
            f"样本 {idx + 1}: 真实={true_label}, 预测={predicted_label}, "
            f"正确={'✓' if is_correct else '✗'}"
        )

        y_true.append(true_label)
        y_pred.append(predicted_label)

    # 5. 输出评估报告
    accuracy = accuracy_score(y_true, y_pred)
    logger.info("\n" + "=" * 30)
    logger.info("评估完成！报告如下：")
    logger.info(f"准确率 (Accuracy): {accuracy:.2%}")
    logger.info(f"正确样本数: {correct_count}/{len(y_true)}")
    logger.info("\n详细分类指标:")
    # zero_division=0 处理 Agent 预测出数据集以外标签的情况
    report = classification_report(y_true, y_pred, zero_division=0)
    logger.info(f"\n{report}")

    print("\n" + "=" * 30)
    print("评估完成！报告如下：")
    print(f"准确率 (Accuracy): {accuracy:.2%}")
    print(f"正确样本数: {correct_count}/{len(y_true)}")
    print("\n详细分类指标:")
    print(report)


if __name__ == "__main__":
    run_evaluation(max_few_shot_examples=60)
