from __future__ import annotations

from pathlib import Path
from typing import Any, Iterator

from langchain.agents import create_agent

from simple_agent.context import Context
from simple_agent.memory import checkpointer
from simple_agent.models import model
from simple_agent.tools import (
    get_user_location,
    get_weather_for_location,
)
from simple_agent.tools.tool_factory import ToolFactory, ToolsName

SYSTEM_PROMPT = "你是一个文件管理助手，可以读取、写入和列出文件。"


class ChatService:
    """聊天服务类，管理工作目录和 langchain agent 对象"""

    work_directory: Path
    agent: Any

    def __init__(self, work_directory: Path):
        """初始化聊天服务

        Args:
            work_directory: 工作目录 Path 对象
        """
        self.work_directory = work_directory.resolve()
        tool_factory = ToolFactory(workspace=str(work_directory.resolve()))
        file_tools = tool_factory.get_tools(ToolsName.FILE_TOOLS)

        self.agent = create_agent(
            model=model,
            system_prompt=SYSTEM_PROMPT,
            tools=file_tools.get_tools(),
            context_schema=Context,
            checkpointer=checkpointer,
        )

    def chat(
            self, user_input: str, thread_id: str = "1", user_id: str = "1"
    ) -> Iterator[str]:
        """与代理进行流式对话，实时返回文本内容

        Args:
            user_input: 用户输入的消息
            thread_id: 线程ID，默认为 "1"
            user_id: 用户ID，默认为 "1"

        Yields:
            流式返回的文本内容片段
        """
        config = {"configurable": {"thread_id": thread_id}}
        stream = self.agent.stream(
            {"messages": [{"role": "user", "content": user_input}]},
            config=config,
            context=Context(user_id=user_id),
            stream_mode="messages",
        )

        for token, metadata in stream:
            node_name = metadata.get("langgraph_node")
            if node_name == "model":
                try:
                    if hasattr(token, "content"):
                        content = token.content
                        if isinstance(content, str) and content:
                            yield content
                        elif isinstance(content, list):
                            for item in content:
                                if isinstance(item, dict):
                                    if item.get("type") == "text":
                                        text = item.get("text", "")
                                        if text:
                                            yield text
                                    elif "text" in item:
                                        yield str(item["text"])
                                elif isinstance(item, str):
                                    yield item
                    elif hasattr(token, "content_blocks"):
                        content_blocks = token.content_blocks
                        if content_blocks and isinstance(content_blocks, list):
                            for block in content_blocks:
                                if isinstance(block, dict):
                                    block_type = block.get("type")
                                    if block_type == "text":
                                        text = block.get("text", "")
                                        if text:
                                            yield text
                    elif isinstance(token, dict):
                        if "content" in token:
                            content = token["content"]
                            if isinstance(content, str) and content:
                                yield content
                        elif "text" in token:
                            yield str(token["text"])
                except (AttributeError, TypeError, KeyError) as e:
                    pass
