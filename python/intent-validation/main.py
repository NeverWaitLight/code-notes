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
    {"intent": "calendar_remove", "intent_ch": "删除日历"},
    {"intent": "transport_ticket", "intent_ch": "交通票务"},
    {"intent": "qa_factoid", "intent_ch": "事实问答"},
    {"intent": "iot_hue_lightdim", "intent_ch": "调暗智能灯光"},
    {"intent": "datetime_convert", "intent_ch": "时间转换"},
    {"intent": "iot_hue_lightup", "intent_ch": "调亮智能灯光"},
    {"intent": "music_dislikeness", "intent_ch": "音乐不喜欢"},
    {"intent": "iot_hue_lightoff", "intent_ch": "关闭智能灯光"},
    {"intent": "alarm_query", "intent_ch": "查询闹钟"},
    {"intent": "play_music", "intent_ch": "播放音乐"},
    {"intent": "music_likeness", "intent_ch": "音乐喜欢"},
    {"intent": "lists_query", "intent_ch": "查询清单"},
    {"intent": "transport_taxi", "intent_ch": "打车服务"},
    {"intent": "qa_stock", "intent_ch": "股票查询"},
    {"intent": "iot_wemo_on", "intent_ch": "开启智能开关"},
    {"intent": "email_query", "intent_ch": "查询邮件"},
    {"intent": "general_quirky", "intent_ch": "一般性古怪问题"},
    {"intent": "general_joke", "intent_ch": "讲笑话"},
    {"intent": "music_query", "intent_ch": "查询音乐"},
    {"intent": "play_podcasts", "intent_ch": "播放播客"},
    {"intent": "recommendation_movies", "intent_ch": "推荐电影"},
    {"intent": "social_post", "intent_ch": "社交发布"},
    {"intent": "play_game", "intent_ch": "玩游戏"},
    {"intent": "transport_traffic", "intent_ch": "交通路况"},
    {"intent": "email_querycontact", "intent_ch": "查询联系人邮箱"},
    {"intent": "recommendation_locations", "intent_ch": "推荐地点"},
    {"intent": "calendar_set", "intent_ch": "设置日历"},
    {"intent": "play_audiobook", "intent_ch": "播放有声书"},
    {"intent": "calendar_query", "intent_ch": "查询日历"},
    {"intent": "audio_volume_mute", "intent_ch": "静音"},
    {"intent": "email_sendemail", "intent_ch": "发送邮件"},
    {"intent": "transport_query", "intent_ch": "交通查询"},
    {"intent": "audio_volume_up", "intent_ch": "增大音量"},
    {"intent": "music_settings", "intent_ch": "音乐设置"},
    {"intent": "lists_createoradd", "intent_ch": "创建或添加清单"},
    {"intent": "qa_currency", "intent_ch": "货币汇率查询"},
    {"intent": "email_addcontact", "intent_ch": "添加联系人"},
    {"intent": "play_radio", "intent_ch": "播放电台"},
    {"intent": "audio_volume_down", "intent_ch": "降低音量"},
    {"intent": "takeaway_order", "intent_ch": "外卖订购"},
    {"intent": "iot_wemo_off", "intent_ch": "关闭智能开关"},
    {"intent": "cooking_recipe", "intent_ch": "烹饪菜谱"},
    {"intent": "general_greet", "intent_ch": "一般性问候"},
    {"intent": "weather_query", "intent_ch": "天气查询"},
    {"intent": "alarm_set", "intent_ch": "设置闹钟"},
    {"intent": "iot_hue_lighton", "intent_ch": "开启智能灯光"},
    {"intent": "iot_cleaning", "intent_ch": "智能清洁"},
    {"intent": "datetime_query", "intent_ch": "日期时间查询"},
    {"intent": "qa_maths", "intent_ch": "数学计算"},
    {"intent": "qa_definition", "intent_ch": "定义查询"},
    {"intent": "cooking_query", "intent_ch": "烹饪查询"},
    {"intent": "alarm_remove", "intent_ch": "删除闹钟"},
    {"intent": "iot_coffee", "intent_ch": "智能咖啡机"},
    {"intent": "iot_hue_lightchange", "intent_ch": "改变智能灯光颜色"},
    {"intent": "recommendation_events", "intent_ch": "推荐活动"},
    {"intent": "takeaway_query", "intent_ch": "外卖查询"},
    {"intent": "lists_remove", "intent_ch": "删除清单项"},
    {"intent": "social_query", "intent_ch": "社交查询"},
    {"intent": "news_query", "intent_ch": "新闻查询"},
]

SYSTEM_PROMPT_TEMPLATE = textwrap.dedent("""
    # Role
    你是一个资深的自然语言理解（NLU）专家，擅长从用户输入的文本中精准提取用户的真实意图。将意图归一化到给定的意图列表，注意中英文翻译时的准确性。
    
    # Task
    请分析用户输入的文本，并从给定的意图列表 {INTENTS} 中选择最匹配的一个。如果用户的输入不属于列表中的任何意图，请将其归类为 "unknown"。
    先判断请求的领域（Domain，如：音乐、家电），再从该领域下选择具体意图。
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


def extract_few_shot_examples(dataset, intents_list, max_total_examples=20):
    """
    从数据集中提取示例用于few-shot learning，总数限制为max_total_examples
    确保每个意图至少有一个示例，且所有示例文本都不重复
    intents_list: 意图列表，每个元素为包含 intent 和 intent_ch 的字典
    """
    intent_names = [item["intent"] for item in intents_list]
    intent_examples = defaultdict(list)
    used_texts = set()
    total_count = 0
    max_total = max_total_examples

    # 第一阶段：为每个意图收集至少1个唯一示例
    for item in dataset:
        intent = item["label_text"]
        text = item["text"]

        if intent in intent_names and len(intent_examples[intent]) == 0:
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

            if intent in intent_names and text not in used_texts:
                intent_examples[intent].append(text)
                used_texts.add(text)
                total_count += 1

    few_shot_text = "以下是各意图的示例：\n\n"
    for intent_item in intents_list:
        intent = intent_item["intent"]
        intent_ch = intent_item["intent_ch"]
        if intent in intent_examples and intent_examples[intent]:
            few_shot_text += f"意图: {intent} ({intent_ch})\n"
            for example in intent_examples[intent]:
                few_shot_text += f"  示例: {example}\n"
            few_shot_text += "\n"

    actual_count = sum(len(v) for v in intent_examples.values())
    covered_intents = len(
        [i for i in intent_names if i in intent_examples and intent_examples[i]]
    )
    logger.info(
        f"已提取 {actual_count} 个唯一few-shot示例（限制: {max_total}），覆盖 {covered_intents}/{len(intent_names)} 个意图"
    )
    return few_shot_text


def call_llm(text, few_shot_examples="", temperature=0.0):
    intents_str = ", ".join(
        [f"{item['intent']} ({item['intent_ch']})" for item in INTENTS]
    )
    system_prompt = SYSTEM_PROMPT_TEMPLATE.format(
        INTENTS=intents_str, FEW_SHOT_EXAMPLES=few_shot_examples
    )

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": text},
    ]

    dashscope.api_key = API_KEY

    logger.debug(f"调用LLM，输入文本: {text[:50]}...")
    response = Generation.call(
        model="deepseek-v3.2",
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
