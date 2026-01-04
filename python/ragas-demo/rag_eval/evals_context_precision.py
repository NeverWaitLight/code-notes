from openai import AsyncOpenAI
from ragas.llms import llm_factory
from ragas.metrics.collections import ContextPrecision

openai_client = AsyncOpenAI(
    api_key="sk-a4e10b09167d48f686eca678dedd4dd6",
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)
llm = llm_factory("qwen3-max", client=openai_client)

# Create metric
scorer = ContextPrecision(llm=llm)

# Evaluate
result = scorer.score(
    user_input="Where is the Eiffel Tower located?",
    reference="The Eiffel Tower is located in Paris.",
    retrieved_contexts=[
        "The Eiffel Tower is located in Paris.",
        "The Brandenburg Gate is located in Berlin.",
    ],
)
print(f"Context Precision Score: {result.value}")
