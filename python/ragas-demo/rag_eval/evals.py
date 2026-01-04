import sys
from pathlib import Path

import pandas as pd
from openai import OpenAI
from ragas import Dataset, experiment
from ragas.llms import llm_factory
from ragas.metrics import DiscreteMetric

# Add the current directory to the path so we can import rag module when run as a script
sys.path.insert(0, str(Path(__file__).parent))
from lightrag import LightRAGClient

openai_client = OpenAI(
    api_key="sk-a4e10b09167d48f686eca678dedd4dd6",
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
)
rag_client = LightRAGClient(logdir="evals/logs")
llm = llm_factory("qwen3-max", client=openai_client)


def load_dataset():
    current_dir = Path(__file__).parent
    csv_path = current_dir / "evals" / "datasets" / "test_dataset.csv"

    df = pd.read_csv(csv_path)

    dataset = Dataset(
        name="test_dataset",
        backend="local/csv",
        root_dir=str("evals"),
    )

    for idx, row in df.iterrows():
        dataset.append(
            {
                "id": str(idx),
                "question": row["question"],
                "ground_truth": row["ground_truth"],
            }
        )

    return dataset


my_metric = DiscreteMetric(
    name="correctness",
    prompt="Check if the response contains points mentioned from the ground truth and return 'pass' or 'fail'.\nResponse: {response} Ground truth: {ground_truth}",
    allowed_values=["pass", "fail"],
)


@experiment()
async def run_experiment(row):
    response = rag_client.query(row["question"])

    score = my_metric.score(
        llm=llm,
        response=response.get("answer", " "),
        ground_truth=row["ground_truth"],
    )

    experiment_view = {
        **row,
        "response": response.get("answer", ""),
        "score": score.value,
        "log_file": response.get("logs", " "),
    }
    return experiment_view


async def main():
    dataset = load_dataset()
    print("dataset loaded successfully", dataset)
    experiment_results = await run_experiment.arun(dataset)
    print("Experiment completed successfully!")
    print("Experiment results:", experiment_results)

    # Save experiment results to CSV
    experiment_results.save()
    csv_path = Path(".") / "experiments" / f"{experiment_results.name}.csv"
    print(f"\nExperiment results saved to: {csv_path.resolve()}")


if __name__ == "__main__":
    import asyncio

    asyncio.run(main())
