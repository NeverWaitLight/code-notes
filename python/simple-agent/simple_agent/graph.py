from pathlib import Path
from textwrap import dedent
from typing import Annotated, List, Optional

from langchain_community.agent_toolkits import FileManagementToolkit
from langchain_core.messages import BaseMessage, SystemMessage, HumanMessage
from langgraph.graph import add_messages, StateGraph
from langgraph.graph.state import CompiledStateGraph
from langgraph.prebuilt import ToolNode, tools_condition
from pydantic import BaseModel, Field

from simple_agent.models import model
from simple_agent.tools.file_static_tools import count_words


class TaskConfiguration(BaseModel):
    topic_prompt: str = Field(description="用户希望生成的文本的具体主题、大纲或开头提示")
    target_word_count: int = Field(default=5000, description="生成文本的目标字数。如果未指定，默认为5000")
    output_file: str = Field(description="生成文本的输出文件名字。必须包含扩展名。")


class AgentState(BaseModel):
    messages: Annotated[List[BaseMessage], add_messages]
    config: Optional[TaskConfiguration] = None


class TextGenerator:

    def __init__(self, work_directory: str = "."):
        self.work_directory = Path(work_directory).resolve()

        # 初始化文件工具
        file_toolkit = FileManagementToolkit(
            root_dir=str(self.work_directory),
            selected_tools=[
                "read_file",
                "write_file",  # LLM 会自动调用这个来保存文件，不需要你手动写 python 代码保存
                "list_directory",
            ],
        )
        # 组合所有工具
        self.tools = file_toolkit.get_tools() + [count_words]

        # 绑定工具到模型
        self.llm_with_tools = model.bind_tools(self.tools)
        self.parser_llm = model.with_structured_output(TaskConfiguration)

        # 构建图
        self.graph = self._build_graph()

    def _build_graph(self) -> CompiledStateGraph:
        # 定义工作流
        workflow = StateGraph(AgentState)

        # 定义节点
        workflow.add_node("parse_request", self.parse_request_node)
        workflow.add_node("call_model", self.call_model_node)
        workflow.add_node("tools", ToolNode(self.tools))

        # 设置入口点
        workflow.set_entry_point("parse_request")
        workflow.add_edge("parse_request", "call_model")

        # 添加条件边
        workflow.add_conditional_edges("call_model", tools_condition, )

        workflow.add_edge("tools", "call_model")

        return workflow.compile()

    async def parse_request_node(self, state: AgentState):
        print("[解析阶段]正在分析用户需求...")

        last_message = state.messages[-1]
        user_input = last_message.content

        config: TaskConfiguration = await  self.parser_llm.ainvoke(user_input)

        system_prompt = dedent(f"""
             你是一个专业的长篇文本生成助手。

            核心任务：
            1. 主题："{config.topic_prompt}"
            2. 目标字数：{config.target_word_count} 字
            3. 输出文件：{config.output_file}

            请分段生成内容，每次生成一部分后立即调用 `write_file` 工具保存。
            每次保存后，**务必**调用 `count_words` 检查字数。
            字数达到要求后，回复“任务完成”。
        """).strip()

        return {
            "config": config,
            "messages": [SystemMessage(content=system_prompt)]
        }

    async def call_model_node(self, state: AgentState):
        """
        调用模型节点。
        这里不需要处理工具逻辑，只需要把消息传给绑定了工具的 LLM。
        """
        response = await self.llm_with_tools.ainvoke(state.messages)
        return {"messages": [response]}

    async def generate_text(self, user_instructions: str) -> str:
        print(f"收到任务{user_instructions}")

        initial_state = AgentState(
            messages=[
                HumanMessage(content=user_instructions),
            ],
            config=None
        )

        # 运行图
        # stream_mode="values" 可以看到每一步的消息流转
        async for event in self.graph.astream(initial_state, stream_mode="values", config={"recursion_limit": 100}):
            last_msg = event["messages"][-1]
            # 打印日志观察 LLM 的行为
            if last_msg.type == "ai":
                if hasattr(last_msg, "tool_calls") and last_msg.tool_calls:
                    print(f"🤖 模型决定调用工具: {[tc['name'] for tc in last_msg.tool_calls]}")
                else:
                    print(f"🤖 模型回复: {last_msg.content[:100]}...")
            elif last_msg.type == "tool":
                print(f"🛠️ 工具执行完成，结果长度: {len(str(last_msg.content))}")

        print("=" * 50)
        print("流程结束")
