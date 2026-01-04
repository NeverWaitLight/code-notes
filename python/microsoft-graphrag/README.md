### Python 虚拟环境

> Windows Git Bash 环境

查看已安装的 Python 版本

```sh
py -0
```

创建 Python `3.12` 虚拟环境，[GraphRAG](https://github.com/microsoft/graphrag) 要求 Python 版本为 `3.10-3.12`

```sh
py -3.12 -m venv .venv
```

激活虚拟环境

```sh
source .venv/Scripts/activate
```

### 安装 GraphRAG

使用清华源安装 [GraphRAG](https://github.com/microsoft/graphrag)

```sh
pip install graphrag -i https://pypi.tuna.tsinghua.edu.cn/simple
```

### 修改 GraphRAG 配置

**创建输入文件夹**

GraphRAG 将对文件夹中的文件构建索引，文件需要是 `UTF-8` 编码

```sh
mkdir -p ./ragtest/input
```

拷贝一个 txt 文件到 `input` 文件夹。先使用较少字数的 txt 文件尝试，再逐步增加 txt 文件字数

**初始化工作区**

将自动创建两个文件：`.env` 和 `settings.yaml` 在 `./ragtest` 目录中

```sh
graphrag init --root ./ragtest
```

- `.env` 包含了运行 GraphRAG 所需的环境变量。 `GRAPHRAG_API_KEY=<API_KEY>` 将 `<API_KEY>` 替换为自己的密钥。
- `settings.yaml` 包含了管道的设置

**修改 settings.yaml**

```yaml
models:
  default_chat_model:
    type: chat
    model_provider: openai # 这里不需要修改，qwen3-max 支持openai协议
    auth_type: api_key
    api_key: ${GRAPHRAG_API_KEY}
    model: qwen3-max # 根据需要修改模型名字
    api_base: https://dashscope.aliyuncs.com/compatible-mode/v1 # 根据需要替换,这里是 阿里百炼URL
    model_supports_json: true
    concurrent_requests: 8 # 降低并发线程数，避免阿里百炼报错
    async_mode: threaded
    retry_strategy: exponential_backoff
    max_retries: 10
    tokens_per_minute: null
    requests_per_minute: null
  default_embedding_model:
    type: embedding
    model_provider: openai # 这里不需要修改，qwen3-max 支持openai协议
    auth_type: api_key
    api_key: ${GRAPHRAG_API_KEY}
    model: text-embedding-v3 # 根据需要修改模型名字
    api_base: https://dashscope.aliyuncs.com/compatible-mode/v1 # 根据需要替换,这里是 阿里百炼URL
    concurrent_requests: 8 # 降低并发线程数，避免阿里百炼报错
    async_mode: threaded
    retry_strategy: exponential_backoff
    max_retries: 10
    tokens_per_minute: null
    requests_per_minute: null

embed_text:
  model_id: default_embedding_model
  vector_store_id: default_vector_store
  batch_size: 4 # 新增这个配置，阿里百炼不支持过高的批处理size
```

**修改 .env**

这里配置阿里百炼的 api key

```sh
GRAPHRAG_API_KEY=XXXXXXXXXXXXXXXXXXXXXXXXX
```

**构建索引**

```sh
graphrag index --root ./ragtest
```

**查看日志**

打开一个新的终端执行下面的命令

```sh
tail -f ./ragtest/logs/indexing-engine.log
```

### 使用 GraphRAG 提问

```sh
graphrag query --root ./test --method global --query "星期五之后听鲁滨逊说过些什么？"
```

### 将 GraphRAG 数据导入 Neo4j

使用 Cypher 和 APOC 库将 GraphRAG 生成的 parquet 文件导入到 Neo4j 数据库

**前置条件**

- 已安装并运行 Neo4j Desktop 2025.10.1 数据库
- 已构建 GraphRAG 索引（执行了 `graphrag index` 命令）
- 已安装 APOC 插件

**步骤一：准备 parquet 文件**

将 GraphRAG 生成的 parquet 文件复制到 Neo4j 的 import 目录

```sh
# 找到 GraphRAG 输出目录中的 artifacts 文件夹
# 通常位于 ./ragtest/output/*/artifacts/ 目录下
# 将以下文件复制到 $NEO4J_HOME/import 目录：

cp ragtest/output/*/artifacts/*.parquet $NEO4J_HOME/import
```

**步骤二：配置 APOC 插件**

检查 Neo4j 版本

根据 Neo4j 版本选择对应的 APOC 版本（APOC 版本应与 Neo4j 版本匹配）

- Neo4j 2025.10.x 使用 APOC 2025.10.x
- Neo4j 5.x 使用 APOC 5.x

安装 APOC 插件到 Neo4j

```sh
cd $NEO4J_HOME/plugins

# 如果存在 labs 目录，复制其中的 apoc jar 文件
cp ../labs/*apoc*.jar .

# 下载并安装 APOC 插件（根据 Neo4j 版本选择对应的 APOC 版本）
# 示例：Neo4j 5.21 使用 APOC 5.21.0
# 访问 https://github.com/neo4j-contrib/neo4j-apoc-procedures/releases 查看所有版本
curl -OL https://github.com/neo4j-contrib/neo4j-apoc-procedures/releases/download/2025.10.0/apoc-2025.10.0-extended.jar
curl -OL https://github.com/neo4j-contrib/neo4j-apoc-procedures/releases/download/2025.10.0/apoc-hadoop-dependencies-2025.10.0-all.jar
```

启用 APOC 文件导入功能

```sh
# 检查 $NEO4J_HOME/conf/apoc.conf 文件是否存在，如果不存在则创建
# 在 apoc.conf 文件中添加以下配置
echo 'apoc.import.file.enabled=true' >> $NEO4J_HOME/conf/apoc.conf

# 在 neo4j.conf 中修改或者添加：
dbms.security.procedures.unrestricted=apoc.*
dbms.security.procedures.allowlist=apoc.*
```

重启 Neo4j 服务

验证 APOC 插件是否正确安装

在 Neo4j 浏览器或 Cypher Shell 中执行以下查询验证 APOC 是否可用

```cypher
// 查看所有可用的 APOC 过程
CALL dbms.procedures() YIELD name
WHERE name STARTS WITH 'apoc'
RETURN name
ORDER BY name;

// 或者测试 apoc.load.parquet 是否可用
CALL apoc.help('load.parquet');
```

如果上述查询返回结果，说明 APOC 已正确安装。如果返回空结果或报错，请检查：

1. 确认 jar 文件已正确放置在 `$NEO4J_HOME/plugins` 目录
2. 确认 jar 文件权限正确（可读）
3. 确认 Neo4j 版本与 APOC 版本匹配
4. 检查 Neo4j 日志文件（`$NEO4J_HOME/logs/neo4j.log`）查看是否有加载错误
5. 确认已重启 Neo4j 服务

将 GraphRAG 生成的 Parquet 文件导入 Neo4j 是一个很好的选择，可以让你利用图数据库强大的查询和可视化功能。

要使用 `apoc.load.parquet`，你需要确保 Neo4j 已经安装了 **APOC** 插件，并且相关的 Parquet 依赖库（通常包含在 `apoc-extended` 中，或者是较新版本的标准库中）已就绪。

以下是分步骤的 Cypher 导入脚本。

### 0. 准备工作：设置约束和索引

在导入数据之前，必须先建立索引，以加快匹配速度并确保数据唯一性。

```cypher
// Documents: 使用 id 作为唯一键
CREATE CONSTRAINT document_id IF NOT EXISTS FOR (d:Document) REQUIRE d.id IS UNIQUE;

// TextUnits: 使用 id 作为唯一键
CREATE CONSTRAINT text_unit_id IF NOT EXISTS FOR (t:TextUnit) REQUIRE t.id IS UNIQUE;

// Entities: 使用 id 作为唯一键，同时为 title (名称) 创建索引以便建立关系
CREATE CONSTRAINT entity_id IF NOT EXISTS FOR (e:Entity) REQUIRE e.id IS UNIQUE;
CREATE INDEX entity_title IF NOT EXISTS FOR (e:Entity) ON (e.title);

// Communities: 使用 id 作为唯一键，同时为 community (整数ID) 创建索引用于层级关联
CREATE CONSTRAINT community_id IF NOT EXISTS FOR (c:Community) REQUIRE c.id IS UNIQUE;
CREATE INDEX community_int_id IF NOT EXISTS FOR (c:Community) ON (c.community);

// CommunityReports: 使用 id 作为唯一键
CREATE CONSTRAINT report_id IF NOT EXISTS FOR (r:CommunityReport) REQUIRE r.id IS UNIQUE;

// Covariates (如果存在): 使用 id 作为唯一键
CREATE CONSTRAINT covariate_id IF NOT EXISTS FOR (cov:Covariate) REQUIRE cov.id IS UNIQUE;
```

---

### 1. 导入 Documents (文档)

**文件:** `documents.parquet`

```cypher
CALL apoc.load.parquet('file:///documents.parquet') YIELD value
MERGE (d:Document {id: value.id})
SET d.title = value.title,
    d.text = value.text,
    d.human_readable_id = value.human_readable_id;
```

---

### 2. 导入 TextUnits (文本块) 并关联 Documents

**文件:** `text_units.parquet`
_逻辑：创建 TextUnit 节点，解析 `document_ids` 数组并建立与 Document 的 `PART_OF` 关系。_

```cypher
CALL apoc.load.parquet('file:///text_units.parquet') YIELD value
MERGE (t:TextUnit {id: value.id})
SET t.text = value.text,
    t.n_tokens = value.n_tokens,
    t.human_readable_id = value.human_readable_id

// 建立 TextUnit -> Document 的关系
WITH t, value
UNWIND value.document_ids AS docId
MATCH (d:Document {id: docId})
MERGE (t)-[:PART_OF]->(d);
```

---

### 3. 导入 Entities (实体) 并关联 TextUnits

**文件:** `entities.parquet`
_逻辑：创建实体节点，并根据 `text_unit_ids` 建立与 TextUnit 的关系（表示实体出现在哪些文本块中）。_

```cypher
CALL apoc.load.parquet('file:///entities.parquet') YIELD value
MERGE (e:Entity {id: value.id})
SET e.title = value.title,
    e.type = value.type,
    e.description = value.description,
    e.frequency = value.frequency,
    e.degree = value.degree,
    e.human_readable_id = value.human_readable_id,
    // 只有在 UMAP 开启时才有 x/y 坐标
    e.x = value.x,
    e.y = value.y

// 建立 Entity -> TextUnit 的关系 (APPEARS_IN)
WITH e, value
UNWIND value.text_unit_ids AS textUnitId
MATCH (t:TextUnit {id: textUnitId})
MERGE (e)-[:APPEARS_IN]->(t);
```

---

### 4. 导入 Relationships (实体关系图)

**文件:** `relationships.parquet`
_逻辑：这是核心图谱。注意 GraphRAG 的 parquet 中 source/target 字段通常是实体的 **名称 (title)** 而不是 ID。_

```cypher
CALL apoc.load.parquet('file:///relationships.parquet') YIELD value
MATCH (source:Entity {title: value.source})
MATCH (target:Entity {title: value.target})
MERGE (source)-[r:RELATED]->(target)
SET r.description = value.description,
    r.weight = value.weight,
    r.id = value.id,
    r.human_readable_id = value.human_readable_id,
    r.text_unit_ids = value.text_unit_ids; // 可选：将来源文本块ID存为属性
```

---

### 5. 导入 Communities (社区)

**文件:** `communities.parquet`
_逻辑：创建社区节点，包含实体的成员关系，以及社区之间的父子层级关系。_

```cypher
CALL apoc.load.parquet('file:///communities.parquet') YIELD value
MERGE (c:Community {id: value.id})
SET c.community = value.community,
    c.level = value.level,
    c.title = value.title,
    c.period = value.period,
    c.size = value.size,
    c.human_readable_id = value.human_readable_id

// 1. 建立 Community -> Entity 的成员关系 (IN_COMMUNITY)
// 注意：GraphRAG 输出的 entity_ids 通常是 Entity 的 id (UUID)
WITH c, value
UNWIND value.entity_ids AS entityId
MATCH (e:Entity {id: entityId})
MERGE (e)-[:IN_COMMUNITY]->(c);

// 2. (可选) 再次运行以建立社区层级关系 (PARENT_OF)
// 由于父节点可能还没导入，建议用两个独立的事务或 apoc.periodic.iterate，这里展示简化版
CALL apoc.load.parquet('file:///communities.parquet') YIELD value
MATCH (child:Community {id: value.id})
WHERE value.parent IS NOT NULL
MATCH (parent:Community {community: value.parent})
MERGE (parent)-[:PARENT_OF]->(child);
```

---

### 6. 导入 Community Reports (社区报告)

**文件:** `community_reports.parquet`
_逻辑：导入报告并将其连接到对应的社区节点。关联键是 `community` (整数 ID)。_

```cypher
CALL apoc.load.parquet('file:///community_reports.parquet') YIELD value
MERGE (r:CommunityReport {id: value.id})
SET r.title = value.title,
    r.summary = value.summary,
    r.full_content = value.full_content,
    r.rank = value.rank,
    r.rating_explanation = value.rating_explanation,
    r.human_readable_id = value.human_readable_id

// 关联到 Community 节点
WITH r, value
MATCH (c:Community {community: value.community})
MERGE (r)-[:REPORT_FOR]->(c);
```

---

### 7. (可选) 导入 Covariates (协变量/声明)

**文件:** `covariates.parquet`
_逻辑：如果有声明提取（Claim extraction），导入这些数据并连接到涉及的实体。_

```cypher
CALL apoc.load.parquet('file:///covariates.parquet') YIELD value
MERGE (cov:Covariate {id: value.id})
SET cov.type = value.type,
    cov.description = value.description,
    cov.status = value.status,
    cov.start_date = value.start_date,
    cov.end_date = value.end_date,
    cov.source_text = value.source_text

// 关联 Subject 实体 (根据名称匹配)
WITH cov, value
MATCH (s:Entity {title: value.subject_id})
MERGE (s)-[:HAS_CLAIM_SUBJECT]->(cov)

// 关联 Object 实体 (根据名称匹配)
WITH cov, value
MATCH (o:Entity {title: value.object_id})
MERGE (o)-[:HAS_CLAIM_OBJECT]->(cov);
```

### 注意事项：

1.  **文件路径**：请将 `file:///documents.parquet` 替换为你实际的文件路径。如果文件在 Neo4j 的 `import` 目录下，直接使用文件名即可；如果在其他位置，需要配置 `apoc.import.file.use_neo4j_config=false` 并使用绝对路径。
2.  **内存管理**：如果你的 Parquet 文件非常大（百万级节点），直接 `CALL` 可能会导致内存溢出。建议结合 `apoc.periodic.iterate` 使用。例如：
    ```cypher
    CALL apoc.periodic.iterate(
      "CALL apoc.load.parquet('file:///relationships.parquet') YIELD value RETURN value",
      "MATCH (s:Entity {title: value.source}) MATCH (t:Entity {title: value.target}) MERGE (s)-[:RELATED]->(t) ...",
      {batchSize: 1000, parallel: false}
    )
    ```
3.  **匹配键**：GraphRAG 输出的 `relationships` 表明确说明 `source` 和 `target` 是名称（Name），而 `text_units` 和 `communities` 中的列表通常是 ID（UUID）。如果发现关系建立失败，请检查 Parquet 数据中该字段存储的是 ID 还是 Name。
