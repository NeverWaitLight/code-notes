from datetime import datetime

import pandas as pd
from openai import OpenAI as OpenAIClient
from ragas import EvaluationDataset, evaluate
from ragas.llms import OpenAI as RagasOpenAI
from ragas.metrics.collections import AnswerAccuracy
from tqdm import tqdm

from lightrag import LightRAGClient

csv_path = "evals/datasets/novel_qa.csv"
df_csv = pd.read_csv(csv_path)
df_csv = df_csv.dropna()

user_input_list = df_csv["user_input"].tolist()
reference_list = df_csv["reference"].tolist()
retrieved_contexts_list = []
response_list = []

my_rag = LightRAGClient()

print("正在生成回答...")
for user_input in tqdm(user_input_list, desc="Answering questions"):
    response, retrieved_context = my_rag.query(user_input)
    if retrieved_context:
        retrieved_contexts_list.append(retrieved_context.split("\n\n---\n\n"))
    else:
        retrieved_contexts_list.append([])
    response_list.append(response)

df = pd.DataFrame(
    {
        "question": user_input_list,
        "contexts": retrieved_contexts_list,
        "answer": response_list,
        "ground_truth": reference_list,
    }
)
rag_results = EvaluationDataset.from_pandas(df)

timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
output_csv_path = f"evals/experiments/rag_results_{timestamp}.csv"
df.to_csv(output_csv_path, encoding="utf-8")
print(f"数据已保存到: {output_csv_path}")

customer_client = openai_client = OpenAIClient(
    api_key="sk-a4e10b09167d48f686eca678dedd4dd6",
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)

ragas_llm = RagasOpenAI(model="deepseek-v3.2", client=customer_client)
ragas_embeddings = RagasOpenAIEmbeddings(
    model="text-embedding-v4", client=customer_client
)

results = evaluate(
    dataset=rag_results,
    metrics=[AnswerAccuracy()],
    llm=ragas_llm,
    embeddings=ragas_embeddings,
)

x = results.to_pandas()
print(x)
