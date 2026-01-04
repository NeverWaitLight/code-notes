import dotenv
from langchain_community.embeddings import DashScopeEmbeddings
from langchain_deepseek import ChatDeepSeek

dotenv.load_dotenv()

model = ChatDeepSeek(model="deepseek-chat")

embeddings = DashScopeEmbeddings(model="text-embedding-v4")
