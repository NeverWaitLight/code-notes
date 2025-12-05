import os
from typing import Literal

from llama_index.core import Document, PropertyGraphIndex, Settings
from llama_index.embeddings.huggingface import HuggingFaceEmbedding
from llama_index.llms.deepseek import DeepSeek
from llama_index.graph_stores.neo4j import Neo4jPropertyGraphStore
from llama_index.core.indices.property_graph import SchemaLLMPathExtractor

# ==========================================
# 1. 配置 DeepSeek 和 嵌入模型
# ==========================================

# 【核心配置】使用 OpenAI 类，但指向 DeepSeek 的接口地址
# DeepSeek 官方兼容 OpenAI SDK
llm = DeepSeek(
    model="deepseek-chat",  # DeepSeek V3 模型名称
    temperature=0.0,  # 抽取实体时温度设为0，保持稳定
    max_tokens=4096,
)

# 【核心配置】使用本地嵌入模型 (免费、快、中文效果好)
# 第一次运行会自动下载模型 (约100MB)，之后就是离线的
print("正在加载 Embedding 模型...")
embed_model = HuggingFaceEmbedding(model_name="BAAI/bge-small-zh-v1.5")

# 将它们设为全局默认，这样 PropertyGraphIndex 就会自动使用它们
Settings.llm = llm
Settings.embed_model = embed_model

# ==========================================
# 定义自定义实体和关系模式
# ==========================================
# 定义可能的实体类型
entities = Literal["PERSON", "PLACE", "ORGANIZATION", "OBJECT", "EVENT", "CONCEPT"]

# 定义可能的关系类型
relations = Literal[
    "HAS",
    "PART_OF",
    "WORKED_ON",
    "WORKED_WITH",
    "WORKED_AT",
    "LOCATED_IN",
    "RELATED_TO",
    "BELONGS_TO",
]

# 定义验证模式字典，约束哪些实体类型可以使用哪些关系类型
validation_schema = {
    "PERSON": [
        "HAS",
        "PART_OF",
        "WORKED_ON",
        "WORKED_WITH",
        "WORKED_AT",
        "LOCATED_IN",
        "RELATED_TO",
        "BELONGS_TO",
    ],
    "PLACE": ["HAS", "PART_OF", "WORKED_AT", "LOCATED_IN", "RELATED_TO"],
    "ORGANIZATION": ["HAS", "PART_OF", "WORKED_WITH", "RELATED_TO", "LOCATED_IN"],
    "OBJECT": ["HAS", "PART_OF", "BELONGS_TO", "LOCATED_IN", "RELATED_TO"],
    "EVENT": ["HAS", "PART_OF", "RELATED_TO", "LOCATED_IN"],
    "CONCEPT": ["HAS", "PART_OF", "RELATED_TO"],
}


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
    # 3. 配置知识图谱提取器
    # ==========================================
    print("正在配置自定义知识图谱提取器...")
    kg_extractor = SchemaLLMPathExtractor(
        llm=llm,
        possible_entities=entities,
        possible_relations=relations,
        kg_validation_schema=validation_schema,
        strict=True,  # 强制遵守模式，不允许模式外的实体和关系
    )
    print("知识图谱提取器配置完成")

    # ==========================================
    # 4. 读取西游记文本文件
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
    print("使用自定义实体和关系模式进行提取")
    print("注意: 相同的实体名称会自动合并到同一个节点")
    print("--------------------------------------------------")

    # ==========================================
    # 5. 构建图谱索引并存储到 Neo4j
    # PropertyGraphIndex 会自动:
    # - 将大文本分块处理
    # - 使用 SchemaLLMPathExtractor 提取实体和关系
    # - 根据定义的验证模式约束提取结果
    # - 相同名称的实体会自动合并到同一个节点
    # ==========================================
    index = PropertyGraphIndex.from_documents(
        documents,
        kg_extractors=[kg_extractor],
        embed_model=embed_model,
        property_graph_store=graph_store,
        show_progress=True,
    )

    print("\n图谱已成功存储到 Neo4j 数据库 (xiyouji)")

    # ==========================================
    # 6. 统计信息
    # ==========================================
    print("\n可以通过 Neo4j Browser 访问 http://localhost:7474 查看完整图谱")
    print("数据库名称: xiyouji")
    print("使用的实体类型:", list(entities.__args__))
    print("使用的关系类型:", list(relations.__args__))


if __name__ == "__main__":
    main()
