from pydantic import BaseModel
from qdrant_client import QdrantClient
from qdrant_client.models import VectorParams, Distance, SparseVectorParams, Modifier


class DB(BaseModel):
    __qdrant_client: QdrantClient

    def create_cellection(self, name: str):
        self.__qdrant_client.create_collection(
            collection_name=name,
            vectors_config={
                "dense-vectors": VectorParams(size=1024, distance=Distance.COSINE)
            },
            sparse_vectors_config={
                "sparse-vectors": SparseVectorParams(modifier=Modifier.IDF)
            },
        )
