import os

from llama_index.core import Document, PropertyGraphIndex, Settings
from llama_index.embeddings.huggingface import HuggingFaceEmbedding
from llama_index.llms.deepseek import DeepSeek
from llama_index.graph_stores.nebula import NebulaPropertyGraphStore

# ==========================================
# 1. 配置 DeepSeek 和 嵌入模型
# ==========================================

# 【核心配置】使用 OpenAI 类，但指向 DeepSeek 的接口地址
# DeepSeek 官方兼容 OpenAI SDK
llm = DeepSeek(
    model="deepseek-chat",  # DeepSeek V3 模型名称
    temperature=0.0,  # 抽取实体时温度设为0，保持稳定
    max_tokens=4096
)

# 【核心配置】使用本地嵌入模型 (免费、快、中文效果好)
# 第一次运行会自动下载模型 (约100MB)，之后就是离线的
print("正在加载 Embedding 模型...")
embed_model = HuggingFaceEmbedding(
    model_name="BAAI/bge-small-zh-v1.5"
)

# 将它们设为全局默认，这样 PropertyGraphIndex 就会自动使用它们
Settings.llm = llm
Settings.embed_model = embed_model


def main():
    # ==========================================
    # 2. 配置 NebulaGraph 图数据库存储
    # ==========================================
    graph_store = NebulaPropertyGraphStore(
        url=os.getenv("NEBULA_ADDRESS", "nebula://127.0.0.1:9669"),
        space="llamaindex_nebula_property_graph",
        overwrite=True
    )

    print("已连接到 NebulaGraph 图数据库")

    # ==========================================
    # 3. 准备测试数据 (一段充满隐含关系的文本)
    # ==========================================
    text = """
    项目代号：'天网计划'（Skynet）。
    该项目的核心负责人是李雷，但他对外声称自己只是个普通的程序员。
    韩梅梅是李雷的大学同学，她负责为一家名为'深海科技'的公司采购高性能显卡。
    实际上，'深海科技'是'天网计划'的硬件供应商。
    昨天，李雷在某二手交易平台上秘密出售了一批来自'深海科技'的报废硬盘。
    """

    documents = [Document(text=text)]

    print("\n--------------------------------------------------")
    print("开始构建图谱并存储到 NebulaGraph")
    print("--------------------------------------------------")

    # ==========================================
    # 4. 构建图谱索引并存储到 NebulaGraph
    # ==========================================
    index = PropertyGraphIndex.from_documents(
        documents, property_graph_store=graph_store, show_progress=True
    )

    print("\n图谱已成功存储到 NebulaGraph 数据库")

    # ==========================================
    # 5. 创建查询引擎
    # ==========================================
    query_engine = index.as_query_engine(include_text=True)

    # ==========================================
    # 6. 提问测试
    # ==========================================
    question = "韩梅梅和李雷之间有什么潜在的商业利益关联？"

    print(f"\n问题: {question}")
    print("DeepSeek 正在思考...")

    response = query_engine.query(question)

    print(f"\n回答:\n{response}")

    # ==========================================
    # 7. 查看提取的三元组
    # ==========================================
    print("\n提取的部分关系图谱:")
    retriever = index.as_retriever(choice_batch_size=10)
    nodes = retriever.retrieve(question)
    for node in nodes:
        if "triplet" in node.metadata:
            print(node.metadata["triplet"])

    print("\n可以通过 NebulaGraph Studio 或客户端工具查看完整图谱")


if __name__ == "__main__":
    main()
