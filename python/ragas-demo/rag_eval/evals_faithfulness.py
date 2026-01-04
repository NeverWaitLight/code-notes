from openai import AsyncOpenAI
from ragas.llms import llm_factory
from ragas.metrics.collections import Faithfulness


openai_client = AsyncOpenAI(
    api_key="sk-a4e10b09167d48f686eca678dedd4dd6",
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)
llm = llm_factory("qwen3-max", client=openai_client)

# Create metric
scorer = Faithfulness(llm=llm)

# Evaluate
# Faithfulness 指标衡量 response 与 retrieved_contexts 在事实上的一致性。其范围从 0 到 1，分数越高表示一致性越好。
result = scorer.score(
    user_input="When was the first super bowl?",
    response="The first superbowl was held on Jan 15, 1967",
    retrieved_contexts=[
        "The First AFL–NFL World Championship Game was an American football game played on January 15, 1967, at the Los Angeles Memorial Coliseum in Los Angeles."
    ],
)

print(f"Faithfulness Score: {result.value}")
