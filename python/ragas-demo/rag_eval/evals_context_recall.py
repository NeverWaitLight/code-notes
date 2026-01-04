from openai import AsyncOpenAI
from ragas.llms import llm_factory
from ragas.metrics.collections import ContextRecall

openai_client = AsyncOpenAI(
    api_key="sk-a4e10b09167d48f686eca678dedd4dd6",
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)
llm = llm_factory("deepseek-v3.2", client=openai_client)

# Create metric
scorer = ContextRecall(llm=llm)

# Evaluate
result = scorer.score(
    user_input="Where is the Eiffel Tower located?",
    retrieved_contexts=["Paris is the capital of France."],
    reference="The Eiffel Tower is located in Paris.",
)
print(f"Context Recall Score: {result.value}")
