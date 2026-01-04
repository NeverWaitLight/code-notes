from ragas import SingleTurnSample

# User's question
user_input = "What is the capital of France?"

# Retrieved contexts (e.g., from a knowledge base or search engine)
retrieved_contexts = ["Paris is the capital and most populous city of France."]

# AI's response
response = "The capital of France is Paris."

# Reference answer (ground truth)
reference = "Paris"

# Evaluation rubric
rubric = {"accuracy": "Correct", "completeness": "High", "fluency": "Excellent"}

# Create the SingleTurnSample instance
sample = SingleTurnSample(
    user_input=user_input,
    retrieved_contexts=retrieved_contexts,
    response=response,
    reference=reference,
    rubric=rubric,
)

print(sample)