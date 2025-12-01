from pathlib import Path
from typing import Dict, List, Any, Annotated, Literal

from langchain_community.agent_toolkits.file_management import FileManagementToolkit
from langchain_core.messages import HumanMessage, SystemMessage, AIMessage, ToolMessage
from langgraph.graph import StateGraph, END
from langgraph.graph.message import add_messages
from langgraph.graph.state import CompiledStateGraph
from langgraph.prebuilt import ToolNode
from pydantic import BaseModel

from simple_agent.models import model
from simple_agent.tools.file_static_tools import count_words


class TextGenerationState(BaseModel):
    """文本生成状态"""

    # 消息历史
    messages: Annotated[List, add_messages]
    # 当前生成的文本内容
    current_text: str
    # 目标字数
    target_word_count: int
    # 当前字数
    current_word_count: int
    # 生成轮次
    generation_round: int
    # 是否完成
    is_completed: bool
    # 输出文件路径
    output_file: str


class LangGraphTextGenerator:

    def __init__(self, work_directory: str = "."):
        """初始化文本生成器

        Args:
            work_directory: 工作目录路径，用于文件管理工具
        """
        self.work_directory = Path(work_directory).resolve()

        file_toolkit = FileManagementToolkit(
            root_dir=str(self.work_directory),
            selected_tools=[
                "copy_file",
                "file_delete",
                "file_search",
                "move_file",
                "read_file",
                "write_file",
                "list_directory",
            ],
        )
        file_tools = file_toolkit.get_tools()

        self.tools = file_tools + [count_words]
        self.tool_node = ToolNode(self.tools)
        self.llm = model
        self.llm_with_tools = model.bind_tools(self.tools)
        self.graph = self._build_graph()

    def _create_generation_prompt(
        self, current_content: str, target_count: int, current_count: int
    ) -> List[Any]:
        """创建生成提示词"""
        remaining = target_count - current_count

        context_content = current_content[-500:] if current_content else "（开头）"

        system_message = SystemMessage(
            content=f"""你是一个专业的文本生成助手。请根据当前已有的内容，继续创作高质量的文本。

当前已有内容：
{context_content}

当前已生成字数：{current_count}（通过工具调用获取）
目标字数：{target_count}
剩余需要生成字数：{remaining}

可用工具：
- write_file: 写入或追加内容到文件
- read_file: 读取文件内容
- count_words: 检查文件的字数（字符数，不包括空格）

工作流程：
1. 使用 write_file 工具将生成的内容写入到文件（如果文件不存在会自动创建，如果存在可以追加内容）
2. 使用 count_words 工具检查文件的当前字数
3. 如果字数未达到目标，继续生成内容并使用 write_file 追加
4. 重复步骤2-3，直到字数达到目标
5. 当字数达到目标后，直接回复用户任务完成，不要再调用工具

要求：
1. 保持内容连贯性和逻辑性
2. 语言流畅自然
3. 如果是续写，请确保与上文自然衔接
4. 请尽可能多地生成内容，充分发挥你的输出能力
5. 必须使用工具来管理文件和检查字数，不要手动计算

请开始生成："""
        )

        return [system_message]

    def generation_node(self, state: TextGenerationState) -> Dict[str, Any]:
        """生成文本节点"""
        print(f"第{state.generation_round}轮生成开始...")

        prompt_messages = self._create_generation_prompt(
            state.current_text,
            state.target_word_count,
            state.current_word_count,
        )

        messages = state.messages + prompt_messages
        response = self.llm_with_tools.invoke(messages)

        if hasattr(response, "tool_calls") and response.tool_calls:
            tool_names = [tc.get("name", "unknown") for tc in response.tool_calls]
            print(f"模型调用工具: {', '.join(tool_names)}")
            return {
                "messages": [response],
            }

        new_content = response.content
        if new_content:
            updated_text = (
                state.current_text + "\n\n" + new_content
                if state.current_text
                else new_content
            )
            print(f"第{state.generation_round}轮生成完成，模型返回文本内容")
            return {
                "current_text": updated_text,
                "generation_round": state.generation_round + 1,
                "messages": [response],
            }

        return {
            "messages": [response],
        }

    def _extract_word_count_from_tool_results(self, messages: List) -> int:
        """从工具调用结果中提取字数"""
        for msg in reversed(messages):
            if isinstance(msg, ToolMessage):
                if msg.name == "count_words":
                    try:
                        return int(msg.content)
                    except (ValueError, TypeError):
                        pass
        return 0

    def _extract_text_from_tool_results(self, messages: List) -> str:
        """从工具调用结果中提取文件内容"""
        for msg in reversed(messages):
            if isinstance(msg, ToolMessage):
                if msg.name == "read_file":
                    return str(msg.content)
        return ""

    def _should_call_tools(
        self, state: TextGenerationState
    ) -> Literal["tools", "continue", "completed"]:
        """判断下一步操作：调用工具、继续生成或完成"""
        messages = state.messages
        if not messages:
            return "continue"

        last_message = messages[-1]
        if (
            isinstance(last_message, AIMessage)
            and hasattr(last_message, "tool_calls")
            and last_message.tool_calls
        ):
            return "tools"

        word_count = self._extract_word_count_from_tool_results(messages)
        if word_count > 0:
            if word_count >= state.target_word_count:
                print(f"文本生成完成！总计字数：{word_count}")
                file_text = self._extract_text_from_tool_results(messages)
                if file_text:
                    self._save_to_file(file_text, state.output_file)
                elif state.current_text:
                    self._save_to_file(state.current_text, state.output_file)
                return "completed"

        if isinstance(last_message, AIMessage) and last_message.content:
            content = str(last_message.content)
            if any(
                keyword in content
                for keyword in ["完成", "达到目标", "任务完成", "已生成"]
            ):
                print("模型判断任务已完成")
                if state.current_text:
                    self._save_to_file(state.current_text, state.output_file)
                return "completed"

        return "continue"

    def _save_to_file(self, content: str, file_path: str):
        """保存内容到文件"""
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"文本已保存到：{file_path}")

    def _build_graph(self) -> CompiledStateGraph:
        """构建LangGraph图"""
        workflow = StateGraph(TextGenerationState)

        workflow.add_node("generate", self.generation_node)
        workflow.add_node("tools", self.tool_node)

        workflow.set_entry_point("generate")

        workflow.add_conditional_edges(
            "generate",
            self._should_call_tools,
            {
                "tools": "tools",
                "continue": "generate",
                "completed": END,
            },
        )

        workflow.add_edge("tools", "generate")

        return workflow.compile()

    async def generate_text(
        self,
        initial_prompt: str,
        target_word_count: int = 10000,
        output_file: str = "generated_text.txt",
    ) -> str:
        """生成文本主函数"""

        output_path = Path(output_file)
        if not output_path.is_absolute():
            output_path = self.work_directory / output_file

        initial_prompt_with_file = f"""{initial_prompt}

目标字数：{target_word_count} 字
输出文件路径：{output_path}

请使用 write_file 工具将生成的内容写入到上述文件路径，使用 count_words 工具检查字数，直到达到目标字数。"""

        initial_state = TextGenerationState(
            messages=[
                SystemMessage(
                    content="你是一个专业的文本生成助手，负责生成长篇连贯的文本内容。你可以使用文件管理工具来写入文件、读取文件和检查字数。"
                ),
                HumanMessage(content=initial_prompt_with_file),
            ],
            current_text="",
            target_word_count=target_word_count,
            current_word_count=0,
            generation_round=1,
            is_completed=False,
            output_file=str(output_path),
        )

        print(f"开始生成文本，目标字数：{target_word_count}")
        print("=" * 50)

        # 执行图
        final_state = await self.graph.ainvoke(initial_state)
        print("=" * 50)
        print("文本生成流程完成！")

        return final_state["current_text"]
