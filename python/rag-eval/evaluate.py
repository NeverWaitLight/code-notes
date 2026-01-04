"""
RAG评估脚本
使用Ragas v0.4的experiment装饰器评估自定义RAG系统
测试指标: ContextRelevance
"""

import asyncio
from typing import Any, Dict

from openai import AsyncOpenAI
from pydantic import BaseModel
from ragas import Dataset, experiment
from ragas.embeddings.base import embedding_factory
from ragas.llms import llm_factory
from ragas.metrics.collections import ContextRelevance

from config import Config
from rag_client import RAGClient


class ExperimentResult(BaseModel):
    """实验结果模型，包含ContextRelevance评估指标和检索到的上下文"""

    context_relevance: float
    retrieved_contexts: list[str]


def init_llm() -> Any:
    """
    初始化LLM客户端
    配置为使用OpenAI协议请求阿里百炼平台
    """
    api_key = Config.get_api_key()
    client = AsyncOpenAI(api_key=api_key, base_url=Config.get_base_url())

    llm = llm_factory(Config.get_model_name(), client=client)
    return llm


def init_embedding() -> Any:
    """
    初始化Embedding客户端
    配置为使用OpenAI协议请求阿里百炼平台
    """
    api_key = Config.get_api_key()
    client = AsyncOpenAI(api_key=api_key, base_url=Config.get_base_url())

    embedding = embedding_factory(
        provider="openai",
        model=Config.get_embedding_model_name(),
        client=client,
    )
    return embedding


def init_metrics(llm: Any) -> Dict[str, Any]:
    """
    初始化评估指标
    返回包含ContextRelevance metric实例的字典
    """
    return {
        "context_relevance": ContextRelevance(llm=llm),
    }


def init_dataset() -> Dataset:
    """
    初始化数据集
    创建Dataset对象，添加测试数据并保存

    参数:
        name: 数据集名称
        root_dir: 数据集保存的根目录，默认为当前目录

    返回:
        创建并保存后的Dataset对象
    """
    test_data = [
        {
            "user_input": "叶凡当前的身体状态和修为境界是什么",
        },
        {
            "user_input": "仙火在叶凡体内产生了什么样的饥饿感和躁动",
        },
        {
            "user_input": "叶凡从丹房执事那里获得了什么丹药，丹药存放在哪里",
        },
        {
            "user_input": "叶凡在家族中的地位和族人对他的态度如何",
        },
        {
            "user_input": "叶凡的偏院和硬板床的环境描述",
        },
        {
            "user_input": "家族废弃的杂物柴房在哪里，环境如何",
        },
        {
            "user_input": "基础炼气诀对直接吞服未炼化丹药有什么警告",
        },
        {
            "user_input": "叶凡的经脉因为仙火产生了什么症状",
        },
        {
            "user_input": "劣质养气丹的外观和杂质特征",
        },
        {
            "user_input": "叶凡昨夜对抗仙火反噬和吸收月华后的身体状态",
        },
        {
            "user_input": "仙火因为品尝月华被唤醒活性后的表现",
        },
        {
            "user_input": "叶凡被当众退婚羞辱的具体情节",
        },
        {
            "user_input": "忠仆老周与叶凡的关系",
        },
        {
            "user_input": "叶凡掌心的伤口和暗紫色结痂的描述",
        },
        {
            "user_input": "叶凡决定喂食仙火时的心理状态和决心",
        },
        {
            "user_input": "家族内部对叶凡的议论和冷眼",
        },
        {
            "user_input": "叶凡如何避开耳目前往废弃柴房",
        },
        {
            "user_input": "叶凡体内仙火对特定能量的需求",
        },
        {
            "user_input": "叶凡修为从炼气四层跌落到炼气三层的过程",
        },
        {
            "user_input": "叶凡在退婚后的精神状态和眼神变化",
        },
    ]

    dataset = Dataset(name="test_dataset", backend="local/csv", root_dir=".")
    for sample in test_data:
        dataset.append(sample)
    dataset.save()
    return dataset


# 全局变量用于存储rag_client和metrics（用于experiment函数）
_global_rag_client = RAGClient()
_global_metrics: Dict[str, Any] = {}


@experiment(ExperimentResult)
async def run_evaluation(row) -> ExperimentResult:
    """
    experiment装饰器的评估函数
    使用全局变量访问rag_client和metrics
    row参数由experiment装饰器传入，是Dataset中的样本字典
    """
    global _global_rag_client, _global_metrics

    # 调用RAG系统获取上下文
    user_input = row["user_input"]
    print(f"正在处理查询: {user_input}")

    try:
        rag_result = await _global_rag_client.query(user_input)
        retrieved_contexts = rag_result.get("retrieved_contexts", [])
        print(f"RAG请求成功，contexts数量: {len(retrieved_contexts)}")
    except Exception as e:
        print(f"RAG请求失败: {e}")
        raise

    if not retrieved_contexts:
        raise ValueError("RAG系统返回的retrieved_contexts不能为空")

    # 计算ContextRelevance指标
    context_relevance_result = await _global_metrics["context_relevance"].ascore(
        user_input=user_input, retrieved_contexts=retrieved_contexts
    )

    return ExperimentResult(
        context_relevance=context_relevance_result.value,
        retrieved_contexts=retrieved_contexts,
    )


def setup_evaluation(rag_client: RAGClient) -> None:
    """
    设置评估环境
    初始化LLM和metrics，并设置全局变量
    """
    global _global_rag_client, _global_metrics

    llm = init_llm()
    _global_metrics = init_metrics(llm)
    _global_rag_client = rag_client


async def main():
    """
    主函数
    示例用法
    """
    # 创建RAG客户端
    rag_client = RAGClient()

    # 设置评估环境
    setup_evaluation(rag_client)

    # 初始化数据集
    dataset = init_dataset()

    # 运行评估
    print("开始评估...")
    results = await run_evaluation.arun(dataset)

    # 输出结果
    print("\n评估结果:")
    print("=" * 80)
    dataset_items = list(dataset)
    for i, result in enumerate(results):
        item = dataset_items[i]
        if isinstance(item, dict):
            user_input = item.get("user_input", f"样本 {i + 1}")
        else:
            user_input = getattr(item, "user_input", f"样本 {i + 1}")
        print(f"\n样本 {i + 1}:")
        print(f"  查询: {user_input}")
        print(f"  Context Relevance: {result.context_relevance:.4f}")
        print("  引用原文:")
        for j, context in enumerate(result.retrieved_contexts, 1):
            print(f"    [{j}] {context}")

    # 计算平均分数
    if results:
        avg_context_relevance = sum(r.context_relevance for r in results) / len(results)

        print("\n" + "=" * 80)
        print("平均分数:")
        print(f"  Context Relevance: {avg_context_relevance:.4f}")


if __name__ == "__main__":
    asyncio.run(main())
