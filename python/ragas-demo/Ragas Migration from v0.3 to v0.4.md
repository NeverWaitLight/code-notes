## 从 v0.3 迁移到 v0.4

Ragas v0.4 引入了向 **experiment-based architecture（基于实验的架构）** 的根本性转变。这是自 v0.2 以来最重要的变化，从孤立的 metric evaluation 转向一个统一的 experimentation 框架，其中 evaluation、分析和迭代紧密集成。

这一架构变化带来了几个具体的改进：

1.  **Collections-Based Metrics System（基于集合的指标系统）** - 一种标准化的指标方法，可在 experiment 中无缝工作
2.  **Unified LLM Factory System（统一的 LLM 工厂系统）** - 简化 LLM 初始化，支持通用 provider
3.  **Modern Prompt System（现代提示系统）** - 基于函数的 prompt，更具可组合性和可重用性

本指南将引导你了解关键变化，并提供分步迁移说明。

## 主要变更概述

向 experiment-based architecture 的转变聚焦于三个核心改进：

1.  **Experiment-Centric Design（以实验为中心的设计）** - 从一次性 metric 运行转向具有集成分析的结构化 experimentation 工作流
2.  **Collections-Based Metrics（基于集合的指标）** - 设计用于在 experiment 中工作的 metric，返回结构化结果以便更好地分析和跟踪
3.  **Enhanced LLM & Prompt System（增强的 LLM 和提示系统）** - 通用 provider 支持和现代 prompt 模式，实现更好的 experimentation

### 关键统计

- **已迁移的指标**：20+ 个核心 metric 迁移到新的 collections 系统
- **破坏性变更**：7+ 个主要 API 变更
- **废弃功能**：遗留的 wrapper 类和旧的 prompt 定义
- **新功能**：GPT-5/o 系列支持、自动约束处理、通用 provider 支持

## 理解基于实验的架构

在迁移之前，了解思维方式的转变很有帮助：

**v0.3（以指标为中心）：**

```text
Data → Individual Metric → Score → Analysis
```

每次 metric 运行相对孤立。你运行一个 metric，得到一个浮点分数，并在外部处理跟踪和分析。

**v0.4（以实验为中心）：**

```text
Data → Experiment → [Metrics Collection] → Structured Results → Integrated Analysis
```

Metric 现在在 experimentation 上下文中工作，其中 evaluation、分析和迭代是集成的。这使得：

- 更好地跟踪带有解释的 metric 结果
- 更容易比较不同 experiment 运行
- 内置支持分析 metric 行为
- 更清晰的工作流用于迭代你的系统

## 迁移路径

我们建议按以下顺序迁移：

1.  **更新 evaluation 方法**（章节：[Evaluation to Experiment](https://docs.ragas.io/en/stable/howtos/migrations/migrate_from_v03_to_v04/#evaluation-to-experiment)） - 从 `evaluate()` 切换到 `experiment()`
2.  **更新你的 LLM 设置**（章节：[LLM Initialization](https://docs.ragas.io/en/stable/howtos/migrations/migrate_from_v03_to_v04/#llm-initialization)）
3.  **迁移 metric**（章节：[Metrics Migration](https://docs.ragas.io/en/stable/howtos/migrations/migrate_from_v03_to_v04/#metrics-migration)）
4.  **迁移 embedding**（章节：[Embeddings Migration](https://docs.ragas.io/en/stable/howtos/migrations/migrate_from_v03_to_v04/#embeddings-migration)）
5.  **更新 prompt**（章节：[Prompt System Migration](https://docs.ragas.io/en/stable/howtos/migrations/migrate_from_v03_to_v04/#prompt-system-migration)） - 如果你正在自定义 prompt
6.  **更新数据模式**（章节：[Data Schema Changes](https://docs.ragas.io/en/stable/howtos/migrations/migrate_from_v03_to_v04/#data-schema-changes)）
7.  **重构自定义 metric**（章节：[Custom Metrics](https://docs.ragas.io/en/stable/howtos/migrations/migrate_from_v03_to_v04/#custom-metrics)）

---

## Evaluation 到 Experiment

v0.4 用基于 `experiment()` 的方法替换了 `evaluate()` 函数，以更好地支持迭代 evaluation 工作流和结构化结果跟踪。

### 发生了什么变化

关键转变：从返回分数的**简单 evaluation 函数**（`evaluate()`）转向支持结构化工作流的**experiment 装饰器**（`@experiment()`），具有内置跟踪和版本控制。

### Before (v0.3)

```python
from ragas import evaluate
from ragas.metrics.collections import Faithfulness, AnswerRelevancy

# Setup
dataset = ...  # Your dataset
metrics = [Faithfulness(llm=llm), AnswerRelevancy(llm=llm)]

# Simple evaluation
result = evaluate(
    dataset=dataset,
    metrics=metrics,
    llm=llm,
    embeddings=embeddings
)

print(result)  # Returns EvaluationResult with scores
```

### After (v0.4)

```python
from ragas import experiment
from ragas.metrics.collections import Faithfulness, AnswerRelevancy
from pydantic import BaseModel

# Define experiment result structure
class ExperimentResult(BaseModel):
    faithfulness: float
    answer_relevancy: float

# Create experiment function
@experiment(ExperimentResult)
async def run_evaluation(row):
    faithfulness = Faithfulness(llm=llm)
    answer_relevancy = AnswerRelevancy(llm=llm)

    faith_result = await faithfulness.ascore(
        response=row.response,
        retrieved_contexts=row.contexts
    )

    relevancy_result = await answer_relevancy.ascore(
        user_input=row.user_input,
        response=row.response
    )

    return ExperimentResult(
        faithfulness=faith_result.value,
        answer_relevancy=relevancy_result.value
    )

# Run experiment
exp_results = await run_evaluation(dataset)
```

### 使用 `experiment()` 的好处

1.  **结构化结果** - 精确定义你想要跟踪的内容
2.  **每行控制** - 如果需要，可以为每个样本自定义 evaluation
3.  **版本跟踪** - 通过 `version_experiment()` 可选的 git 集成
4.  **迭代工作流** - 易于修改和重新运行 experiment
5.  **更好的集成** - 与现代 metric 和数据集无缝协作

---

## LLM 初始化

### 发生了什么变化

v0.3 系统根据你的用例需要不同的工厂函数：

- `instructor_llm_factory()` 用于需要 instructor 的 metric
- `llm_factory()` 用于一般 LLM 操作
- 用于 LangChain 和 LlamaIndex 的各种 wrapper 类

v0.4 将所有内容整合到一个**统一的工厂**中：

```python
from ragas.llms import llm_factory
```

这个工厂：

- 返回 `InstructorBaseRagasLLM`，保证结构化输出
- 自动检测和配置 provider 特定的约束
- 支持 GPT-5 和 o 系列模型，自动处理 `temperature` 和 `top_p` 约束
- 适用于所有主要 provider：OpenAI、Anthropic、Cohere、Google、Azure、Bedrock 等

### Before (v0.3)

```python
from ragas.llms import instructor_llm_factory, llm_factory
from openai import AsyncOpenAI

# For metrics that need instructor
llm = instructor_llm_factory("openai", model="gpt-4o-mini", client=AsyncOpenAI(api_key="..."))

# Or, the old way (not recommended, still supported in 0.3)
client = AsyncOpenAI(api_key="sk-...")
llm = llm_factory("openai", model="gpt-4o-mini", client=client)
```

### After (v0.4)

```python
from ragas.llms import llm_factory
from openai import AsyncOpenAI

# Single unified approach - works everywhere
client = AsyncOpenAI(api_key="sk-...")
llm = llm_factory("gpt-4o-mini", client=client)
```

**主要区别：**

| 方面               | v0.3                                          | v0.4                     |
| ------------------ | --------------------------------------------- | ------------------------ |
| **工厂函数**       | `instructor_llm_factory()` 或 `llm_factory()` | `llm_factory()`          |
| **Provider 检测**  | 通过 provider 字符串手动指定                  | 从模型名称自动检测       |
| **返回类型**       | `BaseRagasLLM`（多种）                        | `InstructorBaseRagasLLM` |
| **约束处理**       | 手动配置                                      | GPT-5/o 系列自动处理     |
| **需要异步客户端** | 是                                            | 是                       |

### 迁移步骤

1.  **更新导入**：

```python
# Remove this
from ragas.llms import instructor_llm_factory

# Use this instead
from ragas.llms import llm_factory
```

2.  **替换工厂调用**：

```python
# Old - v0.3
llm = instructor_llm_factory("openai", model="gpt-4o", client=client)

# New - v0.4
llm = llm_factory("gpt-4o", client=client)
```

3.  **更新其他 provider**（模型名称检测自动工作）：

```python
# OpenAI
llm = llm_factory("gpt-4o-mini", client=AsyncOpenAI(api_key="..."))

# Anthropic
llm = llm_factory("claude-3-sonnet-20240229", client=AsyncAnthropic(api_key="..."))

# Google
llm = llm_factory("gemini-2.0-flash", client=...)
```

### LLM Wrapper 类（已废弃）

如果你正在使用 wrapper 类，它们现在已被废弃，将在未来移除：

```python
# Deprecated - will be removed
from ragas.llms import LangchainLLMWrapper, LlamaIndexLLMWrapper
```

```python
# Recommended - use llm_factory directly
from ragas.llms import llm_factory
```

**迁移**：用直接的 `llm_factory()` 调用替换 wrapper 初始化。工厂现在自动处理 provider 检测。

---

## Metrics 迁移

### 为什么 Metrics 发生了变化

向 experiment-based architecture 的转变要求 metric 更好地与 experimentation 工作流集成：

- **结构化结果**：Metric 现在返回 `MetricResult` 对象（包含分数和推理），而不是原始浮点数，从而在 experiment 中实现更丰富的分析和跟踪
- **关键字参数**：从样本对象转向直接的关键字参数，使 metric 更容易组合并与 experiment 管道集成
- **标准化输入/输出**：基于 collections 的 metric 遵循一致的模式，更容易在此基础上构建元分析和 experimentation 功能

### 架构变化

Metrics 系统已完全重新设计以支持 experiment 工作流。以下是核心区别：

#### 基类变化

| 方面         | v0.3                                                     | v0.4                                               |
| ------------ | -------------------------------------------------------- | -------------------------------------------------- |
| **导入**     | `from ragas.metrics import Metric`                       | `from ragas.metrics.collections import Metric`     |
| **基类**     | `MetricWithLLM`, `SingleTurnMetric`                      | `BaseMetric`（来自 collections）                   |
| **评分方法** | `async def single_turn_ascore(sample: SingleTurnSample)` | `async def ascore(**kwargs)`                       |
| **输入类型** | `SingleTurnSample` 对象                                  | 单独的关键字参数                                   |
| **输出类型** | `float` 分数                                             | `MetricResult`（包含 `.value` 和可选的 `.reason`） |
| **LLM 参数** | 初始化时需要                                             | 初始化时需要                                       |

#### Scoring Workflow

**v0.3 Approach:**

```python
# 1. Create a sample object containing all data
sample = SingleTurnSample(
    user_input="What is AI?",
    response="AI is artificial intelligence...",
    retrieved_contexts=["Context 1", "Context 2"],
    ground_truths=["AI definition"]
)

# 2. Call metric with the sample
metric = Faithfulness(llm=llm)
score = await metric.single_turn_ascore(sample)  # Returns: 0.85
```

**v0.4 Approach:**

```python
# 1. Call metric with individual arguments
metric = Faithfulness(llm=llm)
result = await metric.ascore(
    user_input="What is AI?",
    response="AI is artificial intelligence...",
    retrieved_contexts=["Context 1", "Context 2"]
)

# 2. Access result properties
print(result.value)      # Score: 0.85 (float)
print(result.reason)     # Optional explanation
```

### v0.4 中可用的 Metrics

以下 metrics 已成功迁移到 v0.4 的 collections 系统：

#### RAG Evaluation Metrics

- **Faithfulness** - 响应是否基于检索到的上下文？（v0.3.9+）
- **AnswerRelevancy** - 响应是否与用户查询相关？（v0.3.9+）
- **AnswerCorrectness** - 响应是否匹配参考答案？（v0.3.9+）
- **AnswerAccuracy** - 答案在事实上是否准确？
- **ContextPrecision** - 检索到的上下文是否按相关性排序？（v0.3.9+）
- 带参考：`ContextPrecisionWithReference`
- 不带参考：`ContextPrecisionWithoutReference`
- 遗留名称：`ContextUtilization`（现在是 ContextPrecisionWithoutReference 的包装器）
- **ContextRecall** - 是否成功检索到所有相关上下文？（v0.3.9+）
- **ContextRelevance** - 检索到的上下文中有多少百分比是相关的？（v0.3.9+）
- **ContextEntityRecall** - 参考中的重要实体是否在上下文中？（v0.3.9+）
- **NoiseSensitivity** - metric 对无关上下文的鲁棒性如何？（v0.3.9+）
- **ResponseGroundedness** - 所有声明是否都基于检索到的上下文？

#### Text Comparison Metrics

- **SemanticSimilarity** - 两个文本是否具有相似的语义含义？（v0.3.9+）
- **FactualCorrectness** - 事实声明是否正确验证？（v0.3.9+）
- **BleuScore** - 双语评估替补分数（v0.3.9+）
- **RougeScore** - 面向召回率的摘要评估替补分数（v0.3.9+）

#### String-Based Metrics（非 LLM）

- **ExactMatch** - 精确字符串匹配
- **StringPresence** - 子字符串存在检查
- **LevenshteinDistance** - 编辑距离相似度
- **MatchingSubstrings** - 匹配子字符串计数
- **NonLLMStringSimilarity** - 各种字符串相似度算法

#### Summary Metrics

- **SummaryScore** - 整体摘要质量评估（v0.3.9+）

#### 已移除的 Metrics（不再可用）

- **AspectCritic** - 改用 `@discrete_metric()` 装饰器
- **SimpleCriteria** - 改用 `@discrete_metric()` 装饰器
- **AnswerSimilarity** - 改用 `SemanticSimilarity`

#### Agent & Tool Metrics（已迁移）

- **ToolCallAccuracy** - `ragas.metrics.collections.ToolCallAccuracy`
- **ToolCallF1** - `ragas.metrics.collections.ToolCallF1`
- **TopicAdherence** - `ragas.metrics.collections.TopicAdherence`
- **AgentGoalAccuracy** - `ragas.metrics.collections.AgentGoalAccuracy`

#### SQL & Data Metrics（已迁移）

- **DataCompy Score** - `ragas.metrics.collections.DataCompyScore`
- **SQL Query Equivalence** - `ragas.metrics.collections.SQLSemanticEquivalence`

#### Rubric Metrics（已迁移）

- **DomainSpecificRubrics** - `ragas.metrics.collections.DomainSpecificRubrics`
- **InstanceSpecificRubrics** - `ragas.metrics.collections.InstanceSpecificRubrics`

#### String & NLP Metrics（已迁移）

- **CHRF Score** - `ragas.metrics.collections.CHRFScore`（字符 n-gram F 分数）
- **Quoted Spans Alignment** - `ragas.metrics.collections.QuotedSpansAlignment`（引用验证）

#### Specialized Metrics（尚未迁移）

- **Multi-Modal Faithfulness** - 仍在旧架构上（待迁移）
- **Multi-Modal Relevance** - 仍在旧架构上（待迁移）

迁移状态

大多数核心 metrics 已迁移到 collections 系统。只有多模态 metrics 仍保留在旧架构上。

剩余的 metrics 将在未来的 **v0.4.x** 版本中迁移。你仍可以使用旧 API 的遗留 metrics，但它们会显示废弃警告。

### 分步迁移

#### 步骤 1：更新导入

```python
# v0.3
from ragas.metrics import (
    Faithfulness,
    AnswerRelevancy,
    ContextPrecision,
    ContextRecall
)
```

```python
# v0.4
from ragas.metrics.collections import (
    Faithfulness,
    AnswerRelevancy,
    ContextPrecision,
    ContextRecall
)
```

#### 步骤 2：初始化 Metrics（无需更改）

```python
# v0.3
metric = Faithfulness(llm=llm)
```

```python
# v0.4 - Same initialization
metric = Faithfulness(llm=llm)
```

#### 步骤 3：更新 Metric 评分调用

将 `single_turn_ascore(sample)` 替换为 `ascore(**kwargs)`：

```python
# v0.3
sample = SingleTurnSample(
    user_input="What is AI?",
    response="AI is artificial intelligence.",
    retrieved_contexts=["AI is a technology..."],
    ground_truths=["AI definition"]
)

score = await metric.single_turn_ascore(sample)
print(score)  # Output: 0.85
```

```python
# v0.4
result = await metric.ascore(
    user_input="What is AI?",
    response="AI is artificial intelligence.",
    retrieved_contexts=["AI is a technology..."]
)

print(result.value)   # Output: 0.85
print(result.reason)  # Optional: "Response is faithful to context"
```

#### 步骤 4：处理 MetricResult 对象

在 v0.4 中，metrics 返回 `MetricResult` 对象而不是原始浮点数：

```python
from ragas.metrics.collections.base import MetricResult

result = await metric.ascore(...)

# Access the score
score_value = result.value  # float between 0 and 1

# Access the explanation (if available)
if result.reason:
    print(f"Reason: {result.reason}")

# Convert to float for compatibility
score_float = float(result.value)
```

### 特定 Metric 的迁移

#### Faithfulness

**之前（v0.3）：**

```python
sample = SingleTurnSample(
    user_input="What is machine learning?",
    response="ML is a subset of AI.",
    retrieved_contexts=["ML involves algorithms..."]
)
score = await metric.single_turn_ascore(sample)
```

**之后（v0.4）：**

```python
result = await metric.ascore(
    user_input="What is machine learning?",
    response="ML is a subset of AI.",
    retrieved_contexts=["ML involves algorithms..."]
)
score = result.value
```

#### AnswerRelevancy

**之前（v0.3）：**

```python
sample = SingleTurnSample(
    user_input="What is Python?",
    response="Python is a programming language..."
)
score = await metric.single_turn_ascore(sample)
```

**之后（v0.4）：**

```python
result = await metric.ascore(
    user_input="What is Python?",
    response="Python is a programming language..."
)
score = result.value
```

#### AnswerCorrectness

注意：此 metric 现在使用 `reference` 而不是 `ground_truths`：

**之前（v0.3）：**

```python
sample = SingleTurnSample(
    user_input="What is AI?",
    response="AI is artificial intelligence.",
    ground_truths=["AI is artificial intelligence and machine learning."]
)
score = await metric.single_turn_ascore(sample)
```

**之后（v0.4）：**

```python
result = await metric.ascore(
    user_input="What is AI?",
    response="AI is artificial intelligence.",
    reference="AI is artificial intelligence and machine learning."
)
score = result.value
```

#### ContextPrecision

**之前（v0.3）：**

```python
sample = SingleTurnSample(
    user_input="What is RAG?",
    response="RAG improves LLM accuracy.",
    retrieved_contexts=["RAG = Retrieval Augmented Generation...", "..."],
    ground_truths=["RAG definition"]
)
score = await metric.single_turn_ascore(sample)
```

**之后（v0.4）：**

```python
result = await metric.ascore(
    user_input="What is RAG?",
    response="RAG improves LLM accuracy.",
    retrieved_contexts=["RAG = Retrieval Augmented Generation...", "..."],
    reference="RAG definition"
)
score = result.value
```

---

## Prompt 系统迁移

### 为什么 Prompts 发生了变化

向模块化架构的转变意味着 prompt 现在是**一等组件**，可以：

- **按 metric 自定义** - 每个 metric 都有一个定义良好的 prompt 接口
- **类型安全** - 输入/输出模型定义预期的确切结构
- **可重用** - Prompt 类在 metrics 之间遵循一致的模式
- **可测试** - Prompt 可以独立生成和检查

v0.3 使用简单的基于字符串或数据类的 prompt，分散在各个 metric 中。v0.4 将它们整合到统一的 `BasePrompt` 架构中，具有专门的输入/输出模型。

### 架构变化

#### 基础 Prompt 系统

| 方面              | v0.3                            | v0.4                                            |
| ----------------- | ------------------------------- | ----------------------------------------------- |
| **Prompt 定义**   | `PydanticPrompt` 数据类或字符串 | 带有 `to_string()` 方法的 `BasePrompt` 类       |
| **输入/输出类型** | 通用 Pydantic 模型              | Metric 特定的输入/输出模型                      |
| **访问方法**      | 分散在 metric 代码中            | 集中在 metric 的 `util.py` 模块中               |
| **自定义**        | 困难，需要深度更改              | 通过 `instruction` 和 `examples` 属性简单子类化 |
| **组织**          | 混合在 metric 文件中            | 组织在单独的 `util.py` 文件中                   |

### v0.4 中可用的 Metric Prompts

以下 metrics 现在具有定义良好、可自定义的 prompts：

- **Faithfulness** - `FaithfulnessPrompt`, `FaithfulnessInput`, `FaithfulnessOutput`
- **Context Recall** - `ContextRecallPrompt`, `ContextRecallInput`, `ContextRecallOutput`
- **Context Precision** - `ContextPrecisionPrompt`, `ContextPrecisionInput`, `ContextPrecisionOutput`
- **Answer Relevancy** - `AnswerRelevancyPrompt`, `AnswerRelevancyInput`, `AnswerRelevancyOutput`
- **Answer Correctness** - `AnswerCorrectnessPrompt`, `AnswerCorrectnessInput`, `AnswerCorrectnessOutput`
- **Response Groundedness** - `ResponseGroundednessPrompt`, `ResponseGroundednessInput`, `ResponseGroundednessOutput`
- **Answer Accuracy** - `AnswerAccuracyPrompt`, `AnswerAccuracyInput`, `AnswerAccuracyOutput`
- **Context Relevance** - `ContextRelevancePrompt`, `ContextRelevanceInput`, `ContextRelevanceOutput`
- **Context Entity Recall** - `ContextEntityRecallPrompt`, `ContextEntityRecallInput`, `ContextEntityRecallOutput`
- **Factual Correctness** - `ClaimDecompositionPrompt`, `VerificationPrompt`，以及相关的输入/输出模型
- **Noise Sensitivity** - `NoiseAugmentationPrompt` 以及相关模型
- **Summary Score** - `SummaryScorePrompt`, `SummaryScoreInput`, `SummaryScoreOutput`

### 分步迁移

#### 步骤 1：访问 Metrics 中的 Prompts

```python
from ragas.metrics.collections import Faithfulness
from ragas.llms import llm_factory

# Create metric instance
metric = Faithfulness(llm=llm)

# Access the prompt object
print(metric.prompt)  # <ragas.metrics.collections.faithfulness.util.FaithfulnessPrompt>
```

#### 步骤 2：查看 Prompt 字符串

```python
from ragas.metrics.collections.faithfulness.util import FaithfulnessInput

# Create sample input
sample_input = FaithfulnessInput(
    response="The Eiffel Tower is in Paris.",
    context="The Eiffel Tower is located in Paris, France."
)

# Generate prompt string
prompt_string = metric.prompt.to_string(sample_input)
print(prompt_string)
```

#### 步骤 3：自定义 Prompts（如需要）

**选项 A：子类化默认 prompt**

```python
from ragas.metrics.collections import Faithfulness
from ragas.metrics.collections.faithfulness.util import FaithfulnessPrompt

# Create custom prompt by subclassing
class CustomFaithfulnessPrompt(FaithfulnessPrompt):
    @property
    def instruction(self):
        return """Your custom instruction here."""

# Apply to metric
metric = Faithfulness(llm=llm)
metric.prompt = CustomFaithfulnessPrompt()
```

**选项 B：为领域特定 evaluation 自定义示例**

```python
from ragas.metrics.collections.faithfulness.util import (
    FaithfulnessInput,
    FaithfulnessOutput,
    FaithfulnessPrompt,
    StatementFaithfulnessAnswer,
)

class DomainSpecificPrompt(FaithfulnessPrompt):
    examples = [
        (
            FaithfulnessInput(
                response="ML uses statistical techniques.",
                context="Machine learning is a field that uses algorithms to learn from data.",
            ),
            FaithfulnessOutput(
                statements=[
                    StatementFaithfulnessAnswer(
                        statement="ML uses statistical techniques.",
                        reason="Related to learning from data, but context doesn't explicitly mention statistical techniques.",
                        verdict=0
                    ),
                ]
            ),
        ),
    ]

# Apply custom prompt
metric = Faithfulness(llm=llm)
metric.prompt = DomainSpecificPrompt()
```

### 常见的 Prompt 自定义

#### 更改指令

大多数 metrics 允许覆盖 instruction 属性：

```python
class StrictFaithfulnessPrompt(FaithfulnessPrompt):
    @property
    def instruction(self):
        return """Be very strict when judging faithfulness.
Only mark statements as faithful (verdict=1) if they are directly stated or strongly implied."""
```

#### 添加领域示例

领域特定的示例显著提高 metric 准确性（提高 10-20%）：

```python
class MedicalFaithfulnessPrompt(FaithfulnessPrompt):
    examples = [
        # Medical domain examples here
    ]
```

#### 更改输出格式

对于高级自定义，子类化 prompt 并覆盖 `to_string()` 方法：

```python
class CustomPrompt(FaithfulnessPrompt):
    def to_string(self, input: FaithfulnessInput) -> str:
        # Custom prompt generation logic
        return "..."
```

### 验证自定义 Prompts

在使用自定义 prompts 之前始终验证它们：

```python
# Test prompt generation
sample_input = FaithfulnessInput(
    response="Test response.",
    context="Test context."
)

custom_metric = Faithfulness(llm=llm)
custom_metric.prompt = MyCustomPrompt()

# View the generated prompt
prompt_string = custom_metric.prompt.to_string(sample_input)
print(prompt_string)

# Then use it for evaluation
result = await custom_metric.ascore(
    response="Test response.",
    context="Test context."
)
```

### 从 v0.3 自定义 Prompts 迁移

如果你在 v0.3 中使用 `PydanticPrompt` 创建了自定义 prompts：

**之前（v0.3）- 数据类方法：**

```python
from ragas.prompt.pydantic_prompt import PydanticPrompt
from pydantic import BaseModel

class MyInput(BaseModel):
    response: str
    context: str

class MyOutput(BaseModel):
    is_faithful: bool

class MyPrompt(PydanticPrompt[MyInput, MyOutput]):
    instruction = "Check if response is faithful to context"
    input_model = MyInput
    output_model = MyOutput
    examples = [...]
```

**之后（v0.4）- BasePrompt 方法：**

```python
from ragas.metrics.collections.base import BasePrompt
from pydantic import BaseModel

class MyInput(BaseModel):
    response: str
    context: str

class MyOutput(BaseModel):
    is_faithful: bool

class MyPrompt(BasePrompt):
    @property
    def instruction(self):
        return "Check if response is faithful to context"

    @property
    def input_model(self):
        return MyInput

    @property
    def output_model(self):
        return MyOutput

    @property
    def examples(self):
        return [...]

    def to_string(self, input: MyInput) -> str:
        # Generate prompt string from input
        return f"Check if this is faithful: {input.response}"
```

### 使用 BasePrompt.adapt() 进行语言适配

v0.4 在 `BasePrompt` 实例上引入了 `adapt()` 方法用于语言翻译，替换了已废弃的 `PromptMixin.adapt_prompts()` 方法。

#### 之前（v0.3）- PromptMixin 方法

```python
from ragas.prompt.mixin import PromptMixin
from ragas.metrics import Faithfulness

# Metrics inherited from PromptMixin to use adapt_prompts
class MyFaithfulness(Faithfulness, PromptMixin):
    pass

metric = MyFaithfulness(llm=llm)

# Adapt ALL prompts to another language
adapted_prompts = await metric.adapt_prompts(
    language="spanish",
    llm=llm,
    adapt_instruction=True
)

# Apply all adapted prompts
metric.set_prompts(**adapted_prompts)
```

**v0.3 方法的问题**：- 需要 mixin 继承（紧密耦合）- 所有 prompts 一起适配（不灵活）- Mixin 方法分散在代码库中

#### 之后（v0.4）- BasePrompt.adapt() 方法

```python
from ragas.metrics.collections import Faithfulness

# Create metric with default prompt
metric = Faithfulness(llm=llm)

# Adapt individual prompt to another language
adapted_prompt = await metric.prompt.adapt(
    target_language="spanish",
    llm=llm,
    adapt_instruction=True
)

# Apply adapted prompt
metric.prompt = adapted_prompt

# Use metric with adapted language
result = await metric.ascore(
    response="...",
    retrieved_contexts=[...]
)
```

使用 BasePrompt 保存和加载 prompts 将在未来的 v0.4.x 版本中提供。目前，只有 PromptMixin 具有此功能。

#### 语言适配示例

**适配而不翻译指令文本（轻量级）：**

```python
from ragas.metrics.collections import AnswerRelevancy

metric = AnswerRelevancy(llm=llm)

# Only update language field, keep instruction in English
adapted_prompt = await metric.prompt.adapt(
    target_language="french",
    llm=llm,
    adapt_instruction=False  # Default - just updates language
)

metric.prompt = adapted_prompt
print(metric.prompt.language)  # "french"
```

**适配并翻译指令（完整翻译）：**

```python
# Translate both instruction and examples
adapted_prompt = await metric.prompt.adapt(
    target_language="german",
    llm=llm,
    adapt_instruction=True  # Translate instruction text too
)

metric.prompt = adapted_prompt

# Examples are also automatically translated
# Both instruction and examples in German now
```

**适配自定义 prompts：**

```python
from ragas.metrics.collections.faithfulness.util import FaithfulnessPrompt

class CustomFaithfulnessPrompt(FaithfulnessPrompt):
    @property
    def instruction(self):
        return "Custom instruction in English"

prompt = CustomFaithfulnessPrompt(language="english")

# Adapt to Italian
adapted = await prompt.adapt(
    target_language="italian",
    llm=llm,
    adapt_instruction=True
)

# Check language was updated
assert adapted.language == "italian"
```

#### 从 v0.3 到 v0.4 的迁移

**步骤 1：移除 PromptMixin 继承**

```python
# v0.3
from ragas.prompt.mixin import PromptMixin
from ragas.metrics import Faithfulness

class MyMetric(Faithfulness, PromptMixin):  # ← Remove PromptMixin
    pass

# v0.4
from ragas.metrics.collections import Faithfulness

# No mixin needed - just use the metric directly
metric = Faithfulness(llm=llm)
```

**步骤 2：用 adapt() 替换 adapt_prompts()**

```python
# v0.3
adapted_prompts = await metric.adapt_prompts(
    language="spanish",
    llm=llm,
    adapt_instruction=True
)
metric.set_prompts(**adapted_prompts)

# v0.4
adapted_prompt = await metric.prompt.adapt(
    target_language="spanish",
    llm=llm,
    adapt_instruction=True
)
metric.prompt = adapted_prompt
```

#### 完整迁移示例

**之前（v0.3）：**

```python
from ragas.prompt.mixin import PromptMixin
from ragas.metrics import Faithfulness, AnswerRelevancy

class MyMetrics(Faithfulness, AnswerRelevancy, PromptMixin):
    pass

# Setup
metrics = MyMetrics(llm=llm)

# Adapt multiple metrics to Spanish
adapted = await metrics.adapt_prompts(
    language="spanish",
    llm=best_llm,
    adapt_instruction=True
)

metrics.set_prompts(**adapted)
metrics.save_prompts("./spanish_prompts")
```

**之后（v0.4）：**

```python
from ragas.metrics.collections import Faithfulness, AnswerRelevancy

# Setup individual metrics
faith_metric = Faithfulness(llm=llm)
answer_metric = AnswerRelevancy(llm=llm)

# Adapt each metric's prompt independently
faith_adapted = await faith_metric.prompt.adapt(
    target_language="spanish",
    llm=best_llm,
    adapt_instruction=True
)
faith_metric.prompt = faith_adapted

answer_adapted = await answer_metric.prompt.adapt(
    target_language="spanish",
    llm=best_llm,
    adapt_instruction=True
)
answer_metric.prompt = answer_adapted

# Use metrics with adapted prompts
faith_result = await faith_metric.ascore(...)
answer_result = await answer_metric.ascore(...)
```

---

## 数据模式变更

### SingleTurnSample 更新

`SingleTurnSample` 模式已更新，包含破坏性变更：

#### `ground_truths` → `reference`

`ground_truths` 参数已全面重命名为 `reference`：

**之前（v0.3）：**

```python
sample = SingleTurnSample(
    user_input="...",
    response="...",
    ground_truths=["correct answer"]  # List of strings
)
```

**之后（v0.4）：**

```python
sample = SingleTurnSample(
    user_input="...",
    response="...",
    reference="correct answer"  # Single string
)
```

- v0.3 使用 `ground_truths` 作为**列表**
- v0.4 使用 `reference` 作为**单个字符串**
- 对于多个参考，使用单独的 evaluation 运行

#### 更新的模式

```python
from ragas import SingleTurnSample

# v0.4 complete sample
sample = SingleTurnSample(
    user_input="What is AI?",                      # Required
    response="AI is artificial intelligence.",     # Required
    retrieved_contexts=["Context 1", "Context 2"], # Optional
    reference="Correct definition of AI"           # Optional (was ground_truths)
)
```

### EvaluationDataset 更新

如果你正在使用 `EvaluationDataset`，请更新你的数据加载：

**之前（v0.3）：**

```python
dataset = EvaluationDataset(
    samples=[
        SingleTurnSample(
            user_input="Q1",
            response="A1",
            ground_truths=["correct"]
        )
    ]
)
```

**之后（v0.4）：**

```python
dataset = EvaluationDataset(
    samples=[
        SingleTurnSample(
            user_input="Q1",
            response="A1",
            reference="correct"
        )
    ]
)
```

如果从 CSV/JSON 加载，请更新你的数据文件：

**之前（v0.3）CSV 格式：**

```text
user_input,response,retrieved_contexts,ground_truths
"Q1","A1","[""ctx1""]","[""correct""]"
```

**之后（v0.4）CSV 格式：**

```text
user_input,response,retrieved_contexts,reference
"Q1","A1","[""ctx1""]","correct"
```

---

## 自定义 Metrics

### 对于使用基于 Collections 架构的 Metrics

如果你已经编写了扩展 collections 中 `BaseMetric` 的自定义 metrics，只需要最少的更改：

```python
from ragas.metrics.collections.base import BaseMetric, MetricResult
from pydantic import BaseModel

class MyCustomMetric(BaseMetric):
    name: str = "my_metric"
    dimensions: list[str] = ["my_dimension"]

    async def ascore(self, **kwargs) -> MetricResult:
        # Your metric logic
        score = 0.85
        reason = "Explanation of the score"
        return MetricResult(value=score, reason=reason)
```

**关键考虑：**

- 扩展 `BaseMetric`，而不是旧的 `MetricWithLLM`
- 实现 `async def ascore(**kwargs)` 而不是 `single_turn_ascore(sample)`
- 返回 `MetricResult` 对象，而不是原始浮点数
- 使用关键字参数而不是 `SingleTurnSample`

### 对于使用遗留架构的 Metrics

如果你有扩展 `SingleTurnMetric` 或 `MetricWithLLM` 的自定义 metrics：

```python
# v0.3 - Legacy approach
from ragas.metrics.base import MetricWithLLM

class MyMetric(MetricWithLLM):
    async def single_turn_ascore(self, sample: SingleTurnSample) -> float:
        # Extract values from sample
        user_input = sample.user_input
        response = sample.response
        contexts = sample.retrieved_contexts or []

        # Your logic
        return 0.85
```

**迁移路径：**

1.  改为扩展 collections 中的 `BaseMetric`
2.  更改方法签名以使用关键字参数
3.  返回 `MetricResult` 而不是 float
4.  如果不存在，添加 `dimensions` 属性

```python
# v0.4 - Collections approach
from ragas.metrics.collections.base import BaseMetric, MetricResult

class MyMetric(BaseMetric):
    name: str = "my_metric"
    dimensions: list[str] = ["quality"]

    async def ascore(self,
                    user_input: str,
                    response: str,
                    retrieved_contexts: list[str] | None = None,
                    **kwargs) -> MetricResult:
        # Use keyword arguments directly
        contexts = retrieved_contexts or []

        # Your logic
        score = 0.85
        return MetricResult(value=score, reason="Optional explanation")
```

### Prompt 系统更新

#### v0.3 - 基于数据类的 Prompts

```python
from ragas.prompt.pydantic_prompt import PydanticPrompt
from pydantic import BaseModel

class Input(BaseModel):
    query: str
    document: str

class Output(BaseModel):
    is_relevant: bool

class RelevancePrompt(PydanticPrompt[Input, Output]):
    instruction = "Is the document relevant to the query?"
    input_model = Input
    output_model = Output
    examples = [...]
```

#### v0.4 - 基于函数的 Prompts

新方法使用简单的函数：

```python
def relevance_prompt(query: str, document: str) -> str:
    return f"""Determine if the document is relevant to the query.

Query: {query}
Document: {document}

Respond with YES or NO."""
```

**好处：**

- 更简单且更具可组合性
- 无需样板类定义
- 更容易测试和修改
- 原生 Python 类型提示

**迁移：**

- 识别你在自定义 metrics 中定义 prompts 的位置
- 将数据类定义转换为函数
- 更新 metric 以直接使用函数

---

## 已移除的功能

以下功能已从 v0.4 中完全移除，如果使用会导致错误：

### 函数

**`instructor_llm_factory()`** - 已完全移除

- **合并到**：`llm_factory()` 函数
- **迁移**：将所有 `instructor_llm_factory()` 调用替换为 `llm_factory()`
- **影响**：直接的破坏性变更，无回退

**之前（v0.3）- 不再工作：**

```python
llm = instructor_llm_factory("openai", model="gpt-4o", client=client)
```

**之后（v0.4）- 改用这个：**

```python
llm = llm_factory("gpt-4o", client=client)
```

### Metrics

三个 metrics 已从 collections API 中完全移除。它们不再可用，也没有直接替代：

**1. AspectCritic** - 已移除

- **原因**：被更灵活的离散 metric 模式取代
- **替代**：使用 `@discrete_metric()` 装饰器进行自定义方面评估
- **用法**：

```python
# Instead of AspectCritic, use:
from ragas.metrics import discrete_metric

@discrete_metric(name="aspect_critic", allowed_values=["positive", "negative", "neutral"])
def evaluate_aspect(response: str, aspect: str) -> str:
    # Your evaluation logic
    return "positive"
```

**2. SimpleCriteria** - 已移除

- **原因**：被更灵活的离散 metric 模式取代
- **替代**：使用 `@discrete_metric()` 装饰器进行自定义标准
- **用法**：

```python
from ragas.metrics import discrete_metric

@discrete_metric(name="custom_criteria", allowed_values=["pass", "fail"])
def evaluate_criteria(response: str, criteria: str) -> str:
    return "pass" if criteria in response else "fail"
```

**3. AnswerSimilarity** - 已移除（冗余）

- **原因**：功能完全由 `SemanticSimilarity` 覆盖
- **直接替代**：`SemanticSimilarity`
- **用法**：

```python
# v0.3 - No longer available
from ragas.metrics import AnswerSimilarity  # ERROR

# v0.4 - Use this instead
from ragas.metrics.collections import SemanticSimilarity
metric = SemanticSimilarity(llm=llm)
result = await metric.ascore(
    reference="Expected answer",
    response="Actual answer"
)
```

### 已废弃的方法（在 v0.4 中移除）

**`Metric.ascore()` 和 `Metric.score()`** - 已移除

- **何时移除**：在 v0.3 中标记为移除，在 v0.4 中移除
- **原因**：被基于 collections 的 `ascore(**kwargs)` 模式取代
- **迁移**：改用 collections metrics

**遗留的基于样本的方法** - 已移除

- **`single_turn_ascore(sample: SingleTurnSample)`** - 仅在遗留 metrics 上
- **替换为**：使用 `ascore(**kwargs)` 的 collections metrics

---

## 已废弃的功能

这些功能仍然有效，但会显示废弃警告。它们将在**未来版本**中移除。

### evaluate() 函数 - 已废弃

- **状态**：仍然有效但不推荐
- **原因**：被 `@experiment()` 装饰器取代，用于更好的结构化工作流
- **迁移**：参见 [Evaluation to Experiment](https://docs.ragas.io/en/stable/howtos/migrations/migrate_from_v03_to_v04/#evaluation-to-experiment) 章节

**之前（v0.3）- 已废弃：**

```python
from ragas import evaluate

result = evaluate(dataset=dataset, metrics=metrics, llm=llm, embeddings=embeddings)
```

**之后（v0.4）- 推荐：**

```python
from ragas import experiment
from pydantic import BaseModel

class Results(BaseModel):
    score: float

@experiment(Results)
async def run(row):
    result = await metric.ascore(**row.dict())
    return Results(score=result.value)

result = await run(dataset)
```

### LLM Wrapper 类

#### LangchainLLMWrapper - 已废弃

- **状态**：仍然有效但不推荐
- **废弃警告**：

```text
Direct usage of LangChain LLMs with Ragas prompts is deprecated and will be
removed in a future version. Use Ragas LLM interfaces instead
```

- **迁移**：改用带有原生客户端的 `llm_factory()`

**之前（v0.3）- 已废弃：**

```python
from ragas.llms import LangchainLLMWrapper
from langchain_openai import ChatOpenAI

langchain_llm = ChatOpenAI(model="gpt-4o")
ragas_llm = LangchainLLMWrapper(langchain_llm)
```

**之后（v0.4）- 推荐：**

```python
from ragas.llms import llm_factory
from openai import AsyncOpenAI

client = AsyncOpenAI(api_key="...")
ragas_llm = llm_factory("gpt-4o", client=client)
```

#### LlamaIndexLLMWrapper - 已废弃

- **状态**：仍然有效但不推荐
- **类似警告**：与 LangchainLLMWrapper 相同
- **迁移**：使用带有原生客户端的 `llm_factory()`

**之前（v0.3）- 已废弃：**

```python
from ragas.llms import LlamaIndexLLMWrapper
from llama_index.llms.openai import OpenAI

llamaindex_llm = OpenAI(model="gpt-4o")
ragas_llm = LlamaIndexLLMWrapper(llamaindex_llm)
```

**之后（v0.4）- 推荐：**

```python
from ragas.llms import llm_factory
from openai import AsyncOpenAI

client = AsyncOpenAI(api_key="...")
ragas_llm = llm_factory("gpt-4o", client=client)
```

### Embeddings 迁移

#### LangchainEmbeddingsWrapper & LlamaIndexEmbeddingsWrapper - 已废弃

- **状态**：仍然有效但显示废弃警告
- **原因**：被具有直接客户端集成的原生 embedding provider 取代
- **迁移**：参见 [Embeddings Migration](https://docs.ragas.io/en/stable/howtos/migrations/migrate_from_v03_to_v04/#embeddings-migration) 章节

v0.4 用**原生 embedding provider**替换了 wrapper 类，这些 provider 直接与客户端库集成，而不是使用 LangChain wrapper。

### 发生了什么变化

| 方面       | v0.3                                                        | v0.4                                                            |
| ---------- | ----------------------------------------------------------- | --------------------------------------------------------------- |
| **类**     | `LangchainEmbeddingsWrapper`, `LlamaIndexEmbeddingsWrapper` | `OpenAIEmbeddings`, `GoogleEmbeddings`, `HuggingFaceEmbeddings` |
| **客户端** | LangChain/LlamaIndex wrapper                                | 原生客户端（OpenAI、Google 等）                                 |
| **方法**   | `embed_query()`, `embed_documents()`                        | `embed_text()`, `embed_texts()`                                 |
| **设置**   | 包装现有的 LangChain 对象                                   | 直接传递原生客户端                                              |

#### OpenAI 迁移

**之前（v0.3）：**

```python
from langchain_openai import OpenAIEmbeddings as LangChainEmbeddings
from ragas.embeddings import LangchainEmbeddingsWrapper

embeddings = LangchainEmbeddingsWrapper(
    LangChainEmbeddings(api_key="sk-...")
)
embedding = embeddings.embed_query("text")
```

**之后（v0.4）：**

```python
from openai import AsyncOpenAI
from ragas.embeddings import OpenAIEmbeddings

embeddings = OpenAIEmbeddings(
    client=AsyncOpenAI(api_key="sk-..."),
    model="text-embedding-3-small"
)
embedding = embeddings.embed_text("text")  # Different method name
```

#### Google Embeddings 迁移

**之前（v0.3）：**

```python
from langchain_community.embeddings import VertexAIEmbeddings
from ragas.embeddings import LangchainEmbeddingsWrapper

embeddings = LangchainEmbeddingsWrapper(
    VertexAIEmbeddings(model_name="textembedding-gecko@001", project="my-project")
)
```

**之后（v0.4）：**

```python
from ragas.embeddings import GoogleEmbeddings

embeddings = GoogleEmbeddings(
    model="text-embedding-004",
    use_vertex=True,
    project_id="my-project"
)
```

#### HuggingFace 迁移

**之前（v0.3）：**

```python
from ragas.embeddings import HuggingfaceEmbeddings

embeddings = HuggingfaceEmbeddings(model_name="sentence-transformers/all-MiniLM-L6-v2")
```

**之后（v0.4）：**

```python
from ragas.embeddings import HuggingFaceEmbeddings  # Capitalization changed

embeddings = HuggingFaceEmbeddings(
    model="sentence-transformers/all-MiniLM-L6-v2",
    device="cuda"  # Optional GPU acceleration
)
```

### 使用 embedding_factory()

**之前（v0.3）：**

```python
from ragas.embeddings import embedding_factory

embeddings = embedding_factory()  # Defaults to OpenAI
```

**之后（v0.4）：**

```python
from ragas.embeddings import embedding_factory
from openai import AsyncOpenAI

embeddings = embedding_factory(
    provider="openai",
    model="text-embedding-3-small",
    client=AsyncOpenAI(api_key="sk-...")
)
```

### Prompt 系统

#### 基于数据类的 prompts（PydanticPrompt）- 已废弃

- **状态**：遗留 prompts 仍然有效但不推荐
- **废弃**：现在首选模块化 BasePrompt 架构
- **迁移**：参见 [Prompt System Migration](https://docs.ragas.io/en/stable/howtos/migrations/migrate_from_v03_to_v04/#prompt-system-migration) 章节

**之前（v0.3）- 已废弃的方法：**

```python
from ragas.prompt.pydantic_prompt import PydanticPrompt
from pydantic import BaseModel

class Input(BaseModel):
    query: str

class Output(BaseModel):
    is_relevant: bool

class RelevancePrompt(PydanticPrompt[Input, Output]):
    instruction = "Is this relevant?"
    input_model = Input
    output_model = Output
```

**之后（v0.4）- 推荐的方法：**

```python
# Use BasePrompt classes instead - see Prompt System Migration section
from ragas.metrics.collections.faithfulness.util import FaithfulnessPrompt

class CustomPrompt(FaithfulnessPrompt):
    @property
    def instruction(self):
        return "Your custom instruction here"
```

### 遗留 Metric 方法

#### `single_turn_ascore(sample)` - 已废弃

- **状态**：仅在遗留（非 collections）metrics 上
- **废弃**：改用带有 `ascore()` 的 collections metrics
- **时间表**：当所有 metrics 迁移后，将在未来版本中移除

**之前（v0.3）- 已废弃：**

```python
sample = SingleTurnSample(user_input="...", response="...", ...)
score = await metric.single_turn_ascore(sample)
```

**之后（v0.4）- 推荐：**

```python
result = await metric.ascore(user_input="...", response="...")
score = result.value
```

#### ContextUtilization

`ContextUtilization` 现在是 `ContextPrecisionWithoutReference` 的包装器，用于向后兼容：

**之前（v0.3）：**

```python
from ragas.metrics import ContextUtilization
metric = ContextUtilization(llm=llm)
score = await metric.single_turn_ascore(sample)
```

**之后（v0.4）：**

```python
from ragas.metrics.collections import ContextUtilization
# or use the modern name directly:
from ragas.metrics.collections import ContextPrecisionWithoutReference

metric = ContextUtilization(llm=llm)  # Still works (wrapper)
# or
metric = ContextPrecisionWithoutReference(llm=llm)  # Preferred

result = await metric.ascore(
    user_input="...",
    response="...",
    retrieved_contexts=[...]
)
score = result.value
```

---

## 破坏性变更总结

以下是 v0.3 和 v0.4 之间破坏性变更的完整列表：

| 变更                   | v0.3                                 | v0.4                            | 迁移                                                                                                                                 |
| ---------------------- | ------------------------------------ | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| **Evaluation 方法**    | `evaluate()` 函数                    | `@experiment()` 装饰器          | 参见 [Evaluation to Experiment](https://docs.ragas.io/en/stable/howtos/migrations/migrate_from_v03_to_v04/#evaluation-to-experiment) |
| **Metrics 位置**       | `ragas.metrics`                      | `ragas.metrics.collections`     | 更新导入路径                                                                                                                         |
| **评分方法**           | `single_turn_ascore(sample)`         | `ascore(**kwargs)`              | 更改方法调用                                                                                                                         |
| **分数返回类型**       | `float`                              | `MetricResult`                  | 使用 `.value` 属性                                                                                                                   |
| **LLM 工厂**           | `instructor_llm_factory()`           | `llm_factory()`                 | 使用统一工厂                                                                                                                         |
| **Embeddings 方法**    | Wrapper 类（LangChain）              | 原生 provider                   | 参见 [Embeddings Migration](https://docs.ragas.io/en/stable/howtos/migrations/migrate_from_v03_to_v04/#embeddings-migration)         |
| **Embedding 方法**     | `embed_query()`, `embed_documents()` | `embed_text()`, `embed_texts()` | 更新方法调用                                                                                                                         |
| **ground_truths 参数** | `ground_truths: list[str]`           | `reference: str`                | 重命名，更改类型                                                                                                                     |
| **样本类型**           | `SingleTurnSample`                   | `SingleTurnSample`（已更新）    | 更新样本创建                                                                                                                         |
| **Prompt 系统**        | 基于数据类                           | 基于函数                        | 重构自定义 prompts                                                                                                                   |

---

## 废弃和移除

### 在 v0.4 中移除

这些功能已完全移除，会导致错误：

- **`instructor_llm_factory()`** - 改用 `llm_factory()`
- **AspectCritic**（来自 collections）- 无直接替代
- **SimpleCriteriaScore**（来自 collections）- 无直接替代
- **AnswerSimilarity** - 改用 `SemanticSimilarity`

### 已废弃（将在未来版本中移除）

这些功能仍然有效，但会显示废弃警告：

- **`LangchainLLMWrapper`** - 直接使用 `llm_factory()`
- **`LlamaIndexLLMWrapper`** - 直接使用 `llm_factory()`
- **遗留 prompt 类** - 迁移到基于函数的 prompts
- **遗留 metrics 上的 `single_turn_ascore()`** - 使用带有 `ascore()` 的 collections metrics

---

## v0.4 中的新功能（参考）

v0.4 引入了超出迁移要求的几个新功能。虽然从 v0.3 迁移不是必需的，但这些功能可能对你的升级有用：

- **GPT-5 和 o 系列支持** - 自动处理最新 OpenAI 模型的约束
- **通用 Provider 支持** - 单个 `llm_factory()` 适用于所有主要 provider（Anthropic、Google、Azure 等）
- **基于函数的 Prompts** - 更灵活和可组合的 prompt 定义
- **Metric 装饰器** - 使用 `@discrete_metric`、`@numeric_metric`、`@ranking_metric` 简化自定义 metric 创建
- **带推理的 MetricResult** - 带有可选解释的结构化结果
- **增强的 Metric 保存/加载** - 轻松序列化 metric 配置
- **更好的 Embeddings 支持** - 同步和异步 embedding 操作

有关新功能的详细信息，请参见 [v0.4 Release Notes](https://docs.ragas.io/en/stable/howtos/releases/v0.4.0.md)。

---

## 自定义 Metrics 迁移

如果你正在使用已移除的 metrics（如 `AspectCritic` 或 `SimpleCriteria`），v0.4 提供了基于装饰器的替代方案来替换它们。你也可以将新的简化 metric 系统用于其他自定义 metrics：

### 离散 Metrics（分类输出）

**之前（v0.3）- AspectCritic：**

```python
from ragas.metrics import AspectCritic
metric = AspectCritic(name="clarity", allowed_values=["clear", "unclear"])
result = await metric.single_turn_ascore(sample)
```

**之后（v0.4）- @discrete_metric 装饰器：**

```python
from ragas.metrics import discrete_metric

@discrete_metric(name="clarity", allowed_values=["clear", "unclear"])
def clarity(response: str) -> str:
    return "clear" if len(response) > 50 else "unclear"

metric = clarity()
result = await metric.ascore(response="...")
print(result.value)  # "clear" or "unclear"
```

使用离散 metrics 进行任何分类。所有已移除的 metrics（AspectCritic、SimpleCriteria）都可以通过这种方式替换。

### 数值 Metrics（连续值）

使用 `@numeric_metric` 进行任何数值范围的评分：

```python
from ragas.metrics import numeric_metric

@numeric_metric(name="length_score", allowed_values=(0.0, 1.0))
def length_score(response: str) -> float:
    return min(len(response) / 500, 1.0)

# Custom range
@numeric_metric(name="quality_score", allowed_values=(0.0, 10.0))
def quality_score(response: str) -> float:
    return 7.5

metric = length_score()
result = await metric.ascore(response="...")
print(result.value)  # float between 0 and 1
```

### 排序 Metrics（有序列表）

使用 `@ranking_metric` 对多个项目进行排序或排序：

```python
from ragas.metrics import ranking_metric

@ranking_metric(name="context_rank", allowed_values=5)
def context_ranking(question: str, contexts: list[str]) -> list[str]:
    """Rank contexts by relevance."""
    scored = [(len(set(question.split()) & set(c.split())), c) for c in contexts]
    return [c for _, c in sorted(scored, reverse=True)]

metric = context_ranking()
result = await metric.ascore(question="...", contexts=[...])
print(result.value)  # Ranked list
```

### 总结

这些装饰器提供自动验证、类型安全、错误处理和结果包装 - 将自定义 metric 代码从 v0.3 的 50+ 行减少到 v0.4 的仅 5-10 行。

---

## 常见问题和解决方案

### 问题：`instructor_llm_factory` 的 ImportError

**错误：**

```text
ImportError: cannot import name 'instructor_llm_factory' from 'ragas.llms'
```

**解决方案：**

```python
# Instead of this
from ragas.llms import instructor_llm_factory

# Use this
from ragas.llms import llm_factory
```

### 问题：Metric 返回 `MetricResult` 而不是 Float

**错误：**

```python
score = await metric.ascore(...)
print(score)  # Prints: MetricResult(value=0.85, reason=None)
```

**解决方案：**

```python
result = await metric.ascore(...)
score = result.value  # Access the float value
print(score)  # Prints: 0.85
```

### 问题：`SingleTurnSample` 缺少 `ground_truths`

**错误：**

```text
TypeError: ground_truths is not a valid keyword
```

**解决方案：**

```python
# Change from
sample = SingleTurnSample(..., ground_truths=["correct"])

# To
sample = SingleTurnSample(..., reference="correct")
```

## 获取帮助

如果你在迁移过程中遇到问题：

1.  **查看文档**

    - [Metrics Documentation](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/)
    - [Collections API](https://docs.ragas.io/en/stable/concepts/metrics/overview/)
    - [LLM Configuration](https://docs.ragas.io/en/stable/howtos/concepts/llms/index.md)

2.  **GitHub Issues**

    - 搜索 [现有问题](https://github.com/explodinggradients/ragas/issues)
    - 创建包含迁移特定详细信息的新问题

3.  **社区支持**

    - [加入我们的 Discord 社区](https://discord.gg/5djav8GGNZ)
    - [与维护者安排通话](https://cal.com/shahul-ragas/30min)

---

## 总结

v0.4 代表了向 experiment-based architecture 的根本性转变，实现了 evaluation、分析和迭代工作流的更好集成。虽然存在破坏性变更，但它们都服务于使 Ragas 成为更好的 experimentation 平台的目标。

迁移路径很简单：

1.  更新 LLM 初始化以使用 `llm_factory()`
2.  从 `ragas.metrics.collections` 导入 metrics
3.  用 `ascore()` 替换 `single_turn_ascore()`
4.  将 `ground_truths` 重命名为 `reference`
5.  处理 `MetricResult` 对象而不是浮点数

这些技术变更实现了：

- **更好的 Experimentation** - 带有推理的结构化 metric 结果，用于更深入的分析
- **更简洁的 API** - 关键字参数而不是样本对象，使组合更容易
- **集成的工作流** - 设计用于在 experiment 管道中无缝工作的 metrics
- **增强的功能** - 通用 provider 支持和自动约束
- **面向未来** - 基于行业标准构建（instructor 库、标准化模式）

experiment-based architecture 将在未来版本中继续改进，提供更多用于管理、分析和迭代你的 evaluations 的功能。

祝你的迁移顺利！如果你遇到困难，我们随时为你提供帮助。
