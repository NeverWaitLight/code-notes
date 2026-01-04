import json
import os
from dataclasses import asdict
from datetime import datetime
from typing import Any, Dict, Optional

import httpx

from rag import TraceEvent


class LightRAGClient:
    """
    LightRAG API 客户端
    用于通过 HTTP 调用本地部署的 LightRAG 系统
    """

    def __init__(
        self,
        api_url: str = "http://localhost:9621",
        api_key: Optional[str] = None,
        logdir: str = "logs",
    ):
        """
        初始化 LightRAG 客户端

        参数说明
        api_url: LightRAG API 地址
        api_key: API 密钥如果配置了的话
        logdir: 日志目录
        """
        self.api_url = api_url.rstrip("/")
        self.api_key = api_key
        self.logdir = logdir
        self.traces = []

        os.makedirs(self.logdir, exist_ok=True)

        self.traces.append(
            TraceEvent(
                event_type="init",
                component="lightrag_client",
                data={
                    "api_url": self.api_url,
                    "logdir": self.logdir,
                },
            )
        )

    def query(
        self,
        question: str,
        mode: str = "mix",
        top_k: int = 10,
        run_id: Optional[str] = None,
        include_chunk_content: bool = True,
    ):
        """
        调用 LightRAG API 进行查询

        参数说明
        question: 用户问题
        mode: 查询模式 支持 naive local global mix hybrid bypass
        top_k: 检索的实体关系数量
        run_id: 运行标识符
        include_chunk_content: 是否在引用中包含块内容

        返回结果
        元组 (response, retrieved_context)
        response: 生成的答案
        retrieved_context: 检索到的引用内容，格式化为字符串
        """
        if run_id is None:
            run_id = f"{datetime.now().strftime('%Y%m%d_%H%M%S')}_{hash(question) % 10000:04d}"

        self.traces = []
        self.traces.append(
            TraceEvent(
                event_type="query_start",
                component="lightrag_client",
                data={
                    "run_id": run_id,
                    "question": question,
                    "mode": mode,
                    "top_k": top_k,
                },
            )
        )

        try:
            headers = {}
            if self.api_key:
                headers["X-API-Key"] = self.api_key

            payload = {
                "query": question,
                "mode": mode,
                "include_references": True,
                "include_chunk_content": include_chunk_content,
                "top_k": top_k,
            }

            response = httpx.post(
                f"{self.api_url}/query", json=payload, headers=headers, timeout=180.0
            )
            response.raise_for_status()
            result = response.json()

            answer = result.get("response", "")
            references = result.get("references", [])

            retrieved_context_parts = []
            for ref in references:
                file_path = ref.get("file_path", "")
                content_list = ref.get("content", [])
                
                if content_list:
                    ref_text = f"[文件: {file_path}]\n"
                    ref_text += "\n\n".join(content_list)
                    retrieved_context_parts.append(ref_text)
            
            retrieved_context = "\n\n---\n\n".join(retrieved_context_parts)

            self.traces.append(
                TraceEvent(
                    event_type="query_complete",
                    component="lightrag_client",
                    data={
                        "run_id": run_id,
                        "success": True,
                        "response_length": len(answer),
                        "references_count": len(references),
                    },
                )
            )

            logs_path = self.export_traces_to_log(
                run_id, question, {"answer": answer, "references": references}
            )
            
            return answer, retrieved_context

        except Exception as e:
            self.traces.append(
                TraceEvent(
                    event_type="error",
                    component="lightrag_client",
                    data={
                        "run_id": run_id,
                        "error": str(e),
                    },
                )
            )

            logs_path = self.export_traces_to_log(run_id, question, None)
            error_message = f"Error: {str(e)}"
            return error_message, ""

    def export_traces_to_log(
        self,
        run_id: str,
        query: Optional[str] = None,
        result: Optional[Dict[str, Any]] = None,
    ):
        """导出追踪日志"""
        timestamp = datetime.now().isoformat()
        log_filename = f"lightrag_run_{run_id}_{timestamp.replace(':', '-').replace('.', '-')}.json"
        log_filepath = os.path.join(self.logdir, log_filename)

        log_data = {
            "run_id": run_id,
            "timestamp": timestamp,
            "query": query,
            "result": result,
            "traces": [asdict(trace) for trace in self.traces],
        }

        with open(log_filepath, "w") as f:
            json.dump(log_data, f, indent=2)

        print(f"Traces exported to: {log_filepath}")
        return log_filepath


def default_rag_client(llm_client=None, logdir: str = "logs") -> LightRAGClient:
    """
    创建默认的 LightRAG 客户端

    参数说明
    llm_client: 保留参数以保持接口兼容但不使用
    logdir: 日志目录

    返回结果
    LightRAGClient 实例
    """
    api_url = os.getenv("LIGHTRAG_API_URL", "http://localhost:9621")
    api_key = os.getenv("LIGHTRAG_API_KEY")
    return LightRAGClient(api_url=api_url, api_key=api_key, logdir=logdir)
