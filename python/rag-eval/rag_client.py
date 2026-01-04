"""
RAG客户端实现
基于CommonRAG查询接口实现向量检索和回答生成
"""

from dataclasses import dataclass
from typing import Dict, List, Optional, Any

import httpx

from config import Config


@dataclass
class CommonRagParam:
    """CommonRAG查询参数"""

    key_content: str
    type: Optional[str] = None
    section_no: Optional[str] = None
    common_id: Optional[str] = None
    common_type: Optional[str] = None
    user_id: Optional[str] = None
    top_key: int = 10
    score: Optional[float] = None
    use_ranker: bool = False

    def to_dict(self) -> Dict:
        """转换为请求字典"""
        result = {
            "key_content": self.key_content,
            "top_key": self.top_key,
            "use_ranker": self.use_ranker,
        }
        if self.type:
            result["type"] = self.type
        if self.section_no:
            result["section_no"] = self.section_no
        if self.common_id:
            result["common_id"] = self.common_id
        if self.common_type:
            result["common_type"] = self.common_type
        if self.user_id:
            result["user_id"] = self.user_id
        if self.score is not None:
            result["score"] = self.score
        return result


class RAGClient:
    """
    RAG客户端实现类
    封装CommonRAG查询接口调用逻辑
    """

    def __init__(
        self,
        project_id: Optional[str] = None,
        timeout: Optional[float] = None,
        top_k: Optional[int] = None,
        use_ranker: Optional[bool] = None,
        score_threshold: Optional[float] = None,
    ):
        """
        初始化RAG客户端
        所有参数从config.py中的常量配置读取如果未传入则使用配置默认值

        参数:
            project_id: 项目ID用作collection名称如果未传入则使用配置中的值
            timeout: 请求超时时间秒如果未传入则使用配置中的值
            top_k: 检索返回的top数量如果未传入则使用配置中的值
            use_ranker: 是否使用ranker重排序如果未传入则使用配置中的值
            score_threshold: 最低评分阈值如果未传入则使用配置中的值
        """
        self.base_url = Config.RAG_BASE_URL
        self.project_id = project_id or Config.RAG_PROJECT_ID
        self.timeout = timeout if timeout is not None else Config.RAG_TIMEOUT
        self.top_k = top_k if top_k is not None else Config.RAG_TOP_K
        self.use_ranker = (
            use_ranker if use_ranker is not None else Config.RAG_USE_RANKER
        )
        self.score_threshold = (
            score_threshold
            if score_threshold is not None
            else Config.RAG_SCORE_THRESHOLD
        )

    async def query(self, user_input: str, **kwargs) -> Dict[str, Any]:
        """
        查询RAG系统

        参数:
            user_input: 用户输入的查询文本
            **kwargs: 其他可选参数如type section_no common_type等

        返回:
            包含以下键的字典:
            - response: RAG系统生成的回答文本（字符串类型）
            - retrieved_contexts: 检索到的上下文列表每个元素是一个字符串（List[str]类型）
        """
        retrieved_contexts = await self._retrieve_contexts(user_input, **kwargs)

        response = await self._generate_response(user_input, retrieved_contexts)

        return {
            "response": str(response) if response else "",
            "retrieved_contexts": list(retrieved_contexts) if retrieved_contexts else [],
        }

    async def _retrieve_contexts(self, query: str, **kwargs) -> List[str]:
        """
        从CommonRAG接口检索上下文

        参数:
            query: 查询文本
            **kwargs: 额外的查询参数

        返回:
            检索到的上下文列表
        """
        param = CommonRagParam(
            key_content=query,
            type=kwargs.get("type"),
            section_no=kwargs.get("section_no"),
            common_id=kwargs.get("common_id"),
            common_type=kwargs.get("common_type"),
            user_id=kwargs.get("user_id"),
            top_key=kwargs.get("top_k", self.top_k),
            score=kwargs.get("score_threshold", self.score_threshold),
            use_ranker=kwargs.get("use_ranker", self.use_ranker),
        )

        request_body = {
            "project_id": self.project_id,
            "commonRagParams": [param.to_dict()],
        }

        headers = {"Content-Type": "application/json"}

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.post(
                f"{self.base_url}/rag/commonRag/query", json=request_body, headers=headers
            )
            response.raise_for_status()

            result = response.json()

            if result.get("code") != "success":
                raise Exception(
                    f"API返回错误: {result.get('message', 'Unknown error')}"
                )

            contents = result.get("data", {}).get("contents", [])
            contexts = [
                item["value_content"] for item in contents if "value_content" in item
            ]

            return self._format_contexts(contexts)

    async def _generate_response(self, query: str, contexts: List[str]) -> str:
        """
        基于检索到的上下文生成回答

        参数:
            query: 用户查询
            contexts: 检索到的上下文列表

        返回:
            生成的回答文本
        """
        if not contexts:
            return "抱歉没有找到相关信息"

        context_text = "\n\n".join(
            f"参考{i + 1}: {ctx}" for i, ctx in enumerate(contexts)
        )

        response = f"基于检索到的信息回答如下:\n\n{context_text}"

        return response

    def _format_contexts(self, contexts: List[str]) -> List[str]:
        """
        格式化上下文列表
        过滤空值并转换为字符串列表
        确保返回的是纯字符串列表，符合Ragas metrics的期望格式
        """
        if not contexts:
            return []
        formatted = []
        for ctx in contexts:
            if ctx:
                ctx_str = str(ctx).strip()
                if ctx_str:
                    formatted.append(ctx_str)
        return formatted
