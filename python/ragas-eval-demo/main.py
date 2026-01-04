model = "deepseek-v3.2"
embedding_model = "text-embedding-v4"
api_key = "sk-a4e10b09167d48f686eca678dedd4dd6"
base_url = "https://dashscope.aliyuncs.com/compatible-mode/v1"

data = {
    "user_input": ["什么是 Ragas?", "Ragas 的最新版本是多少？"],
    "retrieved_contexts": [
        [
            "Ragas 是一个用于评估 RAG 系统的开源框架。",
            "它提供了一系列基于 LLM 的指标。",
        ],
        ["Ragas 正在快速迭代。", "当前最新版本在 2025 年末发布。"],
    ],
    "response": ["Ragas 是评估 RAG 管道的工具。 ", "目前的版本是 0.4.2。"],
    "reference": [
        "Ragas 是一个帮助评估集成检索增强生成 (RAG) 管道的框架。",
        "Ragas 的最新版本是 0.4.2。",
    ],
}
