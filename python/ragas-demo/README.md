## ragas

使用 ragas 测试 rag 系统，简简单单就 3 步

```python
# 1.加载数据集
dataset = load_dataset()
# 2.运行测试
experiment_results = await run_experiment.arun(dataset)
# 3.保存测试结果
experiment_results.save()
```

## 评估 LightRAG

**目标**：使用 ragas 评估一下 LightRAG 知识库
**依赖**：

- 阿里百炼 apikey，供 LightRAG 和 ragas 使用
- 本地部署 LightRAG，并导入文本生成知识库
- ragas 评估 LightRAG

### 本地部署 LightRAG

#### 拉取代码

```sh
git clone https://github.com/HKUDS/LightRAG.git
```

#### 配置大模型

```sh
cd LightRAG
cp env.example .env # 父子环境变量示例文件
```

**修改 `.env` 文件配置 llm 模型**

> `qwen3-max` 兼容 openai 协议

```sh
LLM_BINDING=openai
LLM_MODEL=qwen3-max
LLM_BINDING_HOST=https://dashscope.aliyuncs.com/compatible-mode/v1
LLM_BINDING_API_KEY=sk-1234567890
```

**修改 `.env` 文件配置 embedding 模型**

> `text-embedding-v4` 兼容 openai 协议

```sh
EMBEDDING_BINDING=openai
EMBEDDING_MODEL=text-embedding-v4
EMBEDDING_DIM=1024
EMBEDDING_SEND_DIM=false
EMBEDDING_TOKEN_LIMIT=8192
EMBEDDING_BINDING_HOST=https://dashscope.aliyuncs.com/compatible-mode/v1
EMBEDDING_BINDING_API_KEY=sk-1234567890
```

`EMBEDDING_DIM=1024` 向量维度一定要和模型输出的维度保持一致，具体查阅模型提供商的文档。

#### 启动服务

在 LightRAG 代码根目录

```sh
docker compose up -d
```

#### 导入文件

访问 `http://127.0.0.1:9621` 导入 `novel.txt` 文件，等待处理完成。

### 使用 ragas 评估

初始化 ragas 评估 demo

```sh
uvx ragas quickstart rag_eval
```

安装依赖

```sh
cd rag_eval
uv sync
```

初始化完成后，会看到 `./rag_eval/rag.py` 文件中包含一个简单的 RAG 逻辑 `ExampleRAG` 这个是用来给我们测试 ragas 框架用的。我们不管它，将 `./lightrag.py` 文件复制到 `./rag_eval/lightrag.py`

```sh
cp ./lightrag.py ./rag_eval/lightrag.py
```

#### 然后修改 evals.py 文件

**客户端初始化部分**

```python
openai_client = OpenAI(
    api_key="sk-1234567890",
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)
rag_client = default_rag_client(logdir="evals/logs")
llm = llm_factory("qwen-max", client=openai_client)
```

**数据集加载函数**

```python
def load_dataset():
    dataset = Dataset(
        name="lightrag_test_dataset",
        backend="local/csv",
        root_dir="evals",
    )

    data_samples = [
        {
            "question": "小说的主角是谁",
            "grading_notes": "- 应该准确回答主角的名字 - 如果有多个主角应该列出 - 信息应该来自实际检索到的内容",
        },
        {
            "question": "故事发生在什么地方",
            "grading_notes": "- 应该描述故事的主要场景 - 信息应该基于检索到的上下文 - 不应该编造不存在的地点",
        },
        {
            "question": "小说的主题是什么",
            "grading_notes": "- 应该概括小说的核心主题 - 回答应该有理有据 - 基于检索到的内容而非臆测",
        },
    ]

    for sample in data_samples:
        row = {"question": sample["question"], "grading_notes": sample["grading_notes"]}
        dataset.append(row)

    # make sure to save it
    dataset.save()
    return dataset
```

**主函数部分**

```python
async def main():
    dataset = load_dataset()
    print("dataset loaded successfully", dataset)
    experiment_results = await run_experiment.arun(dataset)
    print("Experiment completed successfully!")
    print("Experiment results:", experiment_results)

    # Save experiment results to CSV
    experiment_results.save()
    csv_path = Path("evals") / "experiments" / f"{experiment_results.name}.csv"
    print(f"\nExperiment results saved to: {csv_path.resolve()}")
```

运行测试用例

```sh
cd rag_eval
source .ven/Scripts/activate
python evals.py
```

等待执行结束，结果将被输出到 `./rag_evals/evals/experiments` 文件夹中
