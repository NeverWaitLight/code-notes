import textwrap
from http import HTTPStatus

import dashscope
from dashscope import Generation
from datasets import load_dataset
from sklearn.metrics import accuracy_score, classification_report
from tqdm import tqdm

API_KEY = "sk-a4e10b09167d48f686eca678dedd4dd6"

# 1. 加载数据集 (使用验证集 validation)
print("正在加载数据集...")
dataset = load_dataset("SetFit/amazon_massive_intent_zh-CN", split="validation")

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
    
    # Constraints
    1. **唯一性**：只返回一个最相关的意图。
    2. **客观性**：仅基于文本内容判断，不要过度解读。
    3. **格式化输出**：只输出 intent（意图名）
    4. **语言一致性**：推理理由请使用中文。
    
    # Execution Process
    1. 仔细阅读用户输入的文本。
    2. 对比 {INTENTS} 中定义的各个意图及其潜在含义。
    3. 进行逻辑推理，排除干扰项。
    """).strip()


def call_llm(text, temperature=0.0):
    system_prompt = SYSTEM_PROMPT_TEMPLATE.format(INTENTS=str(INTENTS))

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": text},
    ]

    dashscope.api_key = API_KEY

    response = Generation.call(
        model="qwen-flash",
        messages=messages,
        result_format="message",
        parameters={"temperature": temperature},
    )

    if response.status_code == HTTPStatus.OK:
        return response.output.choices[0].message["content"]
    else:
        print(f"请求失败: {response.message}")
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
def run_evaluation(num_samples=None):
    # 获取所有唯一的意图标签列表
    all_intents = list(set(dataset["label_text"]))

    y_true = []
    y_pred = []

    if num_samples is None:
        test_subset = dataset
        print(f"开始测试完整数据集，共 {len(dataset)} 条样本...")
    else:
        test_subset = dataset.select(range(min(num_samples, len(dataset))))
        print(f"开始测试 {len(test_subset)} 条样本...")

    for item in tqdm(test_subset):
        input_text = item["text"]
        true_label = item["label_text"]

        # 获取 Agent 输出
        raw_output = call_llm(input_text)

        # 清理输出
        predicted_label = clean_output(raw_output, all_intents)

        y_true.append(true_label)
        y_pred.append(predicted_label)

    # 5. 输出评估报告
    print("\n" + "=" * 30)
    print("评估完成！报告如下：")
    print(f"准确率 (Accuracy): {accuracy_score(y_true, y_pred):.2%}")
    print("\n详细分类指标:")
    # zero_division=0 处理 Agent 预测出数据集以外标签的情况
    print(classification_report(y_true, y_pred, zero_division=0))


if __name__ == "__main__":
    run_evaluation(num_samples=50)
