from __future__ import annotations

from pathlib import Path
from typing import Annotated, Iterator, Literal, TypedDict

from langchain_community.agent_toolkits.file_management import FileManagementToolkit
from langchain_core.messages import AnyMessage, HumanMessage, AIMessage, SystemMessage
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langgraph.prebuilt import ToolNode

from simple_agent.memory import checkpointer
from simple_agent.models import model
from simple_agent.tools import check_file_word_count


class FileGenerationState(TypedDict):
    """文件生成状态"""

    messages: Annotated[list[AnyMessage], add_messages]


class FileGenerationAgent:
    """基于 LangGraph 的文件生成 Agent，自动循环补充直到达到目标字数"""

    def __init__(self, work_directory: Path):
        """初始化文件生成 Agent

        Args:
            work_directory: 工作目录路径
        """
        self.work_directory = work_directory.resolve()

        file_toolkit = FileManagementToolkit(
            root_dir=str(self.work_directory),
            selected_tools=["write_file", "read_file"],
        )
        file_tools = file_toolkit.get_tools()

        self.tools = file_tools + [check_file_word_count]
        self.tool_node = ToolNode(self.tools)
        self.model_with_tools = model.bind_tools(self.tools)

        self.graph = self._build_graph()

    def _build_graph(self) -> StateGraph:
        """构建 LangGraph 状态图"""
        builder = StateGraph(FileGenerationState)

        builder.add_node("model", self._call_model)
        builder.add_node("tools", self.tool_node)

        builder.add_edge(START, "model")
        builder.add_conditional_edges(
            "model", self._should_call_tools, {"tools": "tools", END: END}
        )
        builder.add_edge("tools", "model")

        return builder.compile(checkpointer=checkpointer)

    def _call_model(self, state: FileGenerationState) -> dict:
        """调用模型生成或补充内容"""
        messages = state["messages"]

        system_prompt = """你是一个文件生成助手，可以帮助用户生成和补充文本文件内容。

可用工具：
- write_file: 写入文件内容
- read_file: 读取文件内容
- check_file_word_count: 检查文件字数

工作流程：
1. 根据用户要求使用 write_file 生成文件内容
2. 使用 check_file_word_count 检查文件字数
3. 如果字数未达到目标，使用 read_file 读取当前内容，然后用 write_file 补充更多内容
4. 重复检查和补充，直到字数达到目标
5. 当字数达到目标后，直接回复用户任务完成，不要再调用工具

重要：你需要自主决定何时生成、何时检查、何时继续补充、何时结束。"""

        if not messages or not isinstance(messages[0], SystemMessage):
            messages = [SystemMessage(content=system_prompt)] + messages

        try:
            response = self.model_with_tools.invoke(messages)
        except Exception as e:
            raise

        return {"messages": [response]}

    def _should_call_tools(self, state: FileGenerationState) -> Literal["tools", END]:
        """判断是否需要调用工具"""
        messages = state["messages"]
        if not messages:
            return END

        last_message = messages[-1]
        if hasattr(last_message, "tool_calls") and last_message.tool_calls:
            return "tools"

        return END

    def generate_file(
            self,
            file_path: str,
            target_word_count: int,
            initial_prompt: str,
            thread_id: str = "file_generation",
    ) -> Iterator[str]:
        """生成文件，自动循环补充直到达到目标字数

        Args:
            file_path: 要生成的文件路径（相对于工作目录）
            target_word_count: 目标字数
            initial_prompt: 初始提示词，描述要生成的内容
            thread_id: 线程ID

        Yields:
            流式返回的状态更新信息
        """
        full_path = self.work_directory / file_path
        full_path.parent.mkdir(parents=True, exist_ok=True)

        user_prompt = f"""请生成一个文本文件：
文件路径: {full_path}
目标字数: {target_word_count} 字
内容要求: {initial_prompt}

请按照以下步骤操作：
1. 使用 write_file 工具生成文件内容
2. 使用 check_file_word_count 工具检查字数
3. 如果字数不足，使用 read_file 读取当前内容，然后用 write_file 补充更多内容
4. 重复检查和补充，直到达到目标字数
5. 当字数达到目标后，回复任务完成"""

        initial_state: FileGenerationState = {
            "messages": [HumanMessage(content=user_prompt)],
        }

        config = {"configurable": {"thread_id": thread_id}}

        stream = self.graph.stream(initial_state, config=config)

        for event in stream:
            for node_name, node_output in event.items():
                if node_name == "model":
                    messages = node_output.get("messages", [])
                    for msg in messages:
                        if isinstance(msg, AIMessage):
                            if hasattr(msg, "tool_calls") and msg.tool_calls:
                                tool_names = [
                                    tc.get("name", "unknown") for tc in msg.tool_calls
                                ]
                                yield f"[模型调用工具] {', '.join(tool_names)}\n"
                            else:
                                content = msg.content
                                if isinstance(content, str) and content:
                                    yield f"[模型响应] {content}\n"
                elif node_name == "tools":
                    yield f"[工具执行完成]\n"

        yield f"\n[任务完成] 文件路径: {full_path}\n"
