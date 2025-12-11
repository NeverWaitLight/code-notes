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
