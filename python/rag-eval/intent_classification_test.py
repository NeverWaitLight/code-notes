"""
意图判断测试脚本
使用 clinc150 数据集测试阿里百炼平台的意图判断能力
"""

import asyncio
import csv
import os
from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Dict, List

os.environ["HTTP_PROXY"] = "http://127.0.0.1:7890"
os.environ["HTTPS_PROXY"] = "http://127.0.0.1:7890"
os.environ["HUGGINGFACE_HUB_CACHE"] = os.path.join(
    os.path.expanduser("~"), ".cache", "huggingface"
)
os.environ["HF_HUB_DISABLE_SYMLINKS_WARNING"] = "1"

from openai import AsyncOpenAI
from datasets import load_dataset

DASHSCOPE_API_KEY = "sk-a4e10b09167d48f686eca678dedd4dd6"
DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
MODEL_NAME = "qwen3-max"
MAX_SAMPLES = None
OUTPUT_DIR = "experiments"


class IntentClassifier:
    def __init__(self, api_key: str, base_url: str, model_name: str):
        self.client = AsyncOpenAI(api_key=api_key, base_url=base_url)
        self.model_name = model_name
        self.intent_labels = None

    async def get_intent_labels(self, dataset) -> List[str]:
        if self.intent_labels is None:
            self.intent_labels = sorted(set(item["intent"] for item in dataset))
        return self.intent_labels

    async def classify(self, text: str, intent_labels: List[str]) -> str:
        labels_str = "\n".join([f"- {label}" for label in intent_labels])
        prompt = f"""你是一个意图分类专家。请分析用户输入的文本，判断其意图属于以下150个类别中的哪一个。

可选的意图类别：
{labels_str}

用户输入：{text}

请只返回意图类别的名称，不要返回其他内容。如果无法确定，请选择最接近的类别。"""

        try:
            response = await self.client.chat.completions.create(
                model=self.model_name,
                messages=[
                    {"role": "system", "content": "你是一个专业的意图分类助手。"},
                    {"role": "user", "content": prompt},
                ],
                temperature=0.1,
            )
            predicted_intent = (
                response.choices[0]
                .message.content.strip()
                .strip('"')
                .strip("'")
                .strip()
            )

            if predicted_intent not in intent_labels:
                for label in intent_labels:
                    if (
                        predicted_intent.lower() in label.lower()
                        or label.lower() in predicted_intent.lower()
                    ):
                        return label
                return intent_labels[0] if intent_labels else "unknown"
            return predicted_intent
        except Exception as e:
            print(f"分类请求失败: {e}")
            return "unknown"


class EvaluationMetrics:
    def __init__(self):
        self.predictions: List[str] = []
        self.true_labels: List[str] = []

    def add_result(self, true_label: str, predicted_label: str):
        self.true_labels.append(true_label)
        self.predictions.append(predicted_label)

    def calculate_accuracy(self) -> float:
        if not self.true_labels:
            return 0.0
        return sum(
            1 for t, p in zip(self.true_labels, self.predictions) if t == p
        ) / len(self.true_labels)

    def calculate_per_class_metrics(self) -> Dict[str, Dict[str, float]]:
        all_labels = set(self.true_labels + self.predictions)
        metrics = {}
        for label in all_labels:
            tp = sum(
                1
                for t, p in zip(self.true_labels, self.predictions)
                if t == label and p == label
            )
            fp = sum(
                1
                for t, p in zip(self.true_labels, self.predictions)
                if t != label and p == label
            )
            fn = sum(
                1
                for t, p in zip(self.true_labels, self.predictions)
                if t == label and p != label
            )
            precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
            recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
            f1 = (
                2 * precision * recall / (precision + recall)
                if (precision + recall) > 0
                else 0.0
            )
            metrics[label] = {
                "precision": precision,
                "recall": recall,
                "f1": f1,
                "support": tp + fn,
            }
        return metrics

    def get_confusion_matrix_summary(self, top_n: int = 20) -> Dict[str, int]:
        error_pairs = [
            (t, p) for t, p in zip(self.true_labels, self.predictions) if t != p
        ]
        return dict(Counter(error_pairs).most_common(top_n))


async def load_clinc150_dataset(max_samples: int = None):
    print("正在加载 clinc150 数据集...")
    full_dataset = load_dataset("DeepPavlov/clinc150", "default")
    dataset = full_dataset["test"]

    if len(dataset) > 0:
        sample = dataset[0]
        if "data" in sample:
            dataset = dataset.rename_column("data", "text")
        if "labels" in sample:
            dataset = dataset.rename_column("labels", "intent")

    if max_samples:
        dataset = dataset.select(range(min(max_samples, len(dataset))))

    print(f"数据集加载完成，共 {len(dataset)} 个样本")
    return dataset


async def run_evaluation(classifier: IntentClassifier, dataset):
    metrics = EvaluationMetrics()
    intent_labels = await classifier.get_intent_labels(dataset)
    print(f"共有 {len(intent_labels)} 个意图类别，开始评估 {len(dataset)} 个样本...")

    results = []
    for idx, item in enumerate(dataset, 1):
        text = item["text"]
        true_intent = item["intent"]
        predicted_intent = await classifier.classify(text, intent_labels)
        is_correct = true_intent == predicted_intent

        metrics.add_result(true_intent, predicted_intent)
        results.append(
            {
                "text": text,
                "true_intent": true_intent,
                "predicted_intent": predicted_intent,
                "is_correct": is_correct,
            }
        )

        if idx % 10 == 0 or idx == len(dataset):
            accuracy = metrics.calculate_accuracy()
            print(f"[{idx}/{len(dataset)}] 当前准确率: {accuracy:.4f}")

        await asyncio.sleep(0.1)

    return metrics, results


def save_results(
    metrics: EvaluationMetrics, results: List[Dict], output_dir: str = OUTPUT_DIR
):
    output_path = Path(output_dir)
    output_path.mkdir(exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")

    csv_path = output_path / f"{timestamp}-intent_classification_results.csv"
    with open(csv_path, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(
            f, fieldnames=["text", "true_intent", "predicted_intent", "is_correct"]
        )
        writer.writeheader()
        writer.writerows(results)

    report_path = output_path / f"{timestamp}-evaluation_report.txt"
    accuracy = metrics.calculate_accuracy()
    correct_count = sum(1 for r in results if r["is_correct"])

    with open(report_path, "w", encoding="utf-8") as f:
        f.write("意图判断评估报告\n")
        f.write("=" * 80 + "\n")
        f.write(f"生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write(f"总体准确率: {accuracy:.4f} ({accuracy * 100:.2f}%)\n")
        f.write(f"总样本数: {len(results)}\n")
        f.write(f"正确预测: {correct_count}\n")
        f.write(f"错误预测: {len(results) - correct_count}\n\n")

        per_class_metrics = metrics.calculate_per_class_metrics()
        f.write("各类别性能指标:\n")
        f.write("-" * 80 + "\n")
        f.write(
            f"{'意图类别':<30} {'精确率':<10} {'召回率':<10} {'F1分数':<10} {'支持数':<10}\n"
        )
        f.write("-" * 80 + "\n")
        for label, metric in sorted(per_class_metrics.items()):
            f.write(
                f"{label:<30} {metric['precision']:<10.4f} {metric['recall']:<10.4f} "
                f"{metric['f1']:<10.4f} {metric['support']:<10}\n"
            )

        f.write("\n最常见的错误分类 (Top 20):\n")
        f.write("-" * 80 + "\n")
        for (true_label, pred_label), count in metrics.get_confusion_matrix_summary(
            20
        ).items():
            f.write(
                f"真实: {true_label:<30} -> 预测: {pred_label:<30} (出现 {count} 次)\n"
            )

    print(f"结果已保存: {csv_path}, {report_path}")


async def main():
    print("=" * 80)
    print("意图判断测试 - 使用 clinc150 数据集")
    print("=" * 80)

    dataset = await load_clinc150_dataset(MAX_SAMPLES)
    classifier = IntentClassifier(
        api_key=DASHSCOPE_API_KEY, base_url=DASHSCOPE_BASE_URL, model_name=MODEL_NAME
    )
    metrics, results = await run_evaluation(classifier, dataset)

    accuracy = metrics.calculate_accuracy()
    correct_count = sum(1 for r in results if r["is_correct"])
    print("\n" + "=" * 80)
    print("评估完成")
    print("=" * 80)
    print(f"总体准确率: {accuracy:.4f} ({accuracy * 100:.2f}%)")
    print(f"总样本数: {len(results)}")
    print(f"正确预测: {correct_count}")
    print(f"错误预测: {len(results) - correct_count}")

    save_results(metrics, results)
    print("\n测试完成！")


if __name__ == "__main__":
    asyncio.run(main())
