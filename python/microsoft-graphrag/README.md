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
  batch_size: 8 # 新增这个配置，阿里百炼不支持过高的批处理size
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

**步骤三：创建数据库约束**

在 Neo4j 浏览器或 Cypher Shell 中执行以下约束创建语句

```cypher
create constraint chunk_id if not exists for (c:__Chunk__) require c.id is unique;
create constraint document_id if not exists for (d:__Document__) require d.id is unique;
create constraint entity_id if not exists for (c:__Community__) require c.community is unique;
create constraint entity_id if not exists for (e:__Entity__) require e.id is unique;
create constraint entity_title if not exists for (e:__Entity__) require e.title is unique;
create constraint related_id if not exists for ()-[rel:RELATED]->() require rel.id is unique;
```

**步骤四：导入数据**

在 Neo4j 浏览器或 Cypher Shell 中依次执行以下导入语句

**导入文档（Documents）**

```cypher
call apoc.load.parquet("create_final_documents.parquet") yield value
MERGE (d:__Document__ {id:value.id})
SET d += value {.title, text_unit_ids:value.text_unit_ids, raw_content:substring(value.raw_content,0,1000)};
```

**导入文本块（Text Units/Chunks）**

```cypher
:auto
call apoc.load.parquet("create_base_text_units.parquet") yield value
CALL { with value
MERGE (c:__Chunk__ {id:value.chunk_id})
SET c += value {.chunk, .n_tokens}
WITH *
UNWIND value.document_ids as doc_id
MATCH (d:__Document__ {id:doc_id})
MERGE (d)<-[:PART_OF]-(c)
RETURN count(distinct c) as chunksCreated
} in transactions of 1000 rows
RETURN sum(chunksCreated) as chunksCreated;
```

**导入实体节点（Entities/Nodes）**

```cypher
:auto
call apoc.load.parquet("create_final_nodes.parquet") yield value
call { with value
    MERGE (n:__Entity__ {id:value.id})
    SET n += value {.level, .top_level_node_id, .human_readable_id, .description,
        title:replace(value.title,'"','')}
    WITH n, value
    CALL apoc.create.addLabels(n, case when value.type is null then [] else [apoc.text.upperCamelCase(replace(value.type,'"',''))] end) yield node
    UNWIND split(value.source_id,",") as source_id
    MATCH (c:__Chunk__ {id:source_id})
    MERGE (c)-[:HAS_ENTITY]->(n)
    RETURN count(distinct n) as created
} in transactions of 25000 rows
return sum(created) as createdNodes;
```

**导入关系（Relationships）**

```cypher
:auto
call apoc.load.parquet("create_final_relationships.parquet") yield value
call { with value
    MATCH (source:__Entity__ {title:replace(value.source,'"','')})
    MATCH (target:__Entity__ {title:replace(value.target,'"','')})
    MERGE (source)-[rel:RELATED]->(target)
    SET rel += value {.id, .rank, .weight, .human_readable_id, .description, text_unit_ids:value.text_unit_ids}
    RETURN count(*) as created
} in transactions of 25000 rows
return sum(created) as createdRels;
```

**导入社区（Communities）**

```cypher
:auto
call apoc.load.parquet("create_final_communities.parquet") yield value
CALL { with value
    MERGE (c:__Community__ {community:value.id})
    SET c += value {.level, .title}
    WITH *
    UNWIND value.relationship_ids as rel_id
    MATCH (start:__Entity__)-[:RELATED {id:rel_id}]->(end:__Entity__)
    MERGE (start)-[:IN_COMMUNITY]->(c)
    MERGE (end)-[:IN_COMMUNITY]->(c)
    RETURn count(distinct c) as created
} in transactions of 1000 rows
RETURN sum(created) as createdCommunities;
```

**导入社区报告（Community Reports）**

```cypher
:auto
call apoc.load.parquet("create_final_community_reports.parquet") yield value
CALL { with value
    MERGE (c:__Community__ {community:value.community})
    SET c += value {.level, .title, .summary, .findings, .rank, .rank_explanation, .id}
    RETURn count(distinct c) as created
} in transactions of 1000 rows
RETURN sum(created) as createdReports;
```

**注意事项**

- `:auto` 前缀用于自动批处理，适合大数据量导入
- `in transactions of N rows` 指定每批处理的行数，可根据数据量调整
- 确保 Neo4j 数据库有足够的内存和磁盘空间
- 导入过程中可以通过 Neo4j 浏览器监控进度
