from openai import AsyncOpenAI
from ragas.llms import llm_factory
from ragas.metrics.collections import AnswerRelevancy
from ragas.embeddings import embedding_factory

openai_client = AsyncOpenAI(
    api_key="sk-a4e10b09167d48f686eca678dedd4dd6",
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)
llm = llm_factory("deepseek-v3.2", client=openai_client)
embeddings = embedding_factory(
    "openai", model="text-embedding-v4", client=openai_client
)

# Create metric
scorer = AnswerRelevancy(llm=llm, embeddings=embeddings)

# Evaluate
result = scorer.score(
    user_input="When was the first super bowl?",
    response="The first superbowl was held on Jan 15, 1967",
)
print(f"Answer Relevancy Score: {result.value}")
