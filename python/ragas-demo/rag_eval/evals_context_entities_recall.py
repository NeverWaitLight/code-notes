from openai import AsyncOpenAI
from ragas.llms import llm_factory
from ragas.metrics.collections import ContextEntityRecall

openai_client = AsyncOpenAI(
    api_key="sk-a4e10b09167d48f686eca678dedd4dd6",
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)
llm = llm_factory("deepseek-v3.2", client=openai_client)

# Create metric
scorer = ContextEntityRecall(llm=llm)

# Evaluate
result = scorer.score(
    reference="The Eiffel Tower is located in Paris.",
    retrieved_contexts=["The Eiffel Tower is located in Paris."],
)
print(f"Context Entity Recall Score: {result.value}")
