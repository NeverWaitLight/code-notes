import os

from llama_index.core import Document, PropertyGraphIndex, Settings
from llama_index.embeddings.huggingface import HuggingFaceEmbedding
from llama_index.llms.deepseek import DeepSeek
from llama_index.graph_stores.neo4j import Neo4jPropertyGraphStore

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
    # 2. 配置 Neo4j 图数据库存储
    # ==========================================
    graph_store = Neo4jPropertyGraphStore(
        username=os.getenv("NEO4J_USERNAME", "neo4j"),
        password=os.getenv("NEO4J_PASSWORD", "password"),
        url=os.getenv("NEO4J_URL", "bolt://localhost:7687"),
        database=os.getenv("NEO4J_DATABASE", "neo4j"),
    )

    print("已连接到 Neo4j 图数据库 (数据库: xiyouji)")

    # ==========================================
    # 3. 读取西游记文本文件
    # ==========================================
    file_path = r"C:\Users\admin\Downloads\西游记.txt"

    if not os.path.exists(file_path):
        print(f"错误: 文件不存在: {file_path}")
        return

    print(f"正在读取文件: {file_path}")
    with open(file_path, "r", encoding="utf-8") as f:
        text = f.read()

    print(f"文件读取完成，文本长度: {len(text)} 字符")

    # PropertyGraphIndex 会自动将大文本分块处理
    # 相同的实体名称会自动合并到同一个节点
    documents = [Document(text=text)]

    print("\n--------------------------------------------------")
    print("开始构建图谱并存储到 Neo4j")
    print("注意: 相同的实体名称会自动合并到同一个节点")
    print("--------------------------------------------------")

    # ==========================================
    # 4. 构建图谱索引并存储到 Neo4j
    # PropertyGraphIndex 会自动:
    # - 将大文本分块处理
    # - 提取实体和关系
    # - 相同名称的实体会自动合并到同一个节点
    # ==========================================
    index = PropertyGraphIndex.from_documents(
        documents, property_graph_store=graph_store, show_progress=True
    )

    print("\n图谱已成功存储到 Neo4j 数据库 (xiyouji)")

    # ==========================================
    # 5. 统计信息
    # ==========================================
    print("\n可以通过 Neo4j Browser 访问 http://localhost:7474 查看完整图谱")
    print("数据库名称: xiyouji")


if __name__ == "__main__":
    main()
