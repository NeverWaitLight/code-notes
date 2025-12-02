from pathlib import Path
from typing import Annotated, List, Optional

from langchain_community.agent_toolkits import FileManagementToolkit
from langchain_core.messages import BaseMessage, SystemMessage, HumanMessage
from langgraph.graph import add_messages, StateGraph
from langgraph.graph.state import CompiledStateGraph
from langgraph.prebuilt import ToolNode, tools_condition
from pydantic import BaseModel, Field

from simple_agent.models import model
from simple_agent.tools.file_static_tools import count_words
from simple_agent.prompt_loader import SystemPromptLoader


class TaskConfiguration(BaseModel):
    task_description: str = Field(
        description="用户请求的完整任务描述，清晰准确地概括任务目标"
    )
    steps: Optional[List[str]] = Field(
        default=None,
        description="任务分解后的步骤列表（to-do list）。如果任务简单只需要一步就能完成，可以为空或只包含一个步骤。对于复杂任务，应该将其分解为多个清晰的、可执行的步骤，每个步骤都应该是一个具体的行动项。模型将在事件循环中按顺序执行这些步骤，每完成一个步骤后检查进度并继续执行后续步骤。"
    )


class AgentState(BaseModel):
    messages: Annotated[List[BaseMessage], add_messages]
    config: Optional[TaskConfiguration] = None


class TextGenerator:

    def __init__(self, work_directory: str = "."):
        self.work_directory = Path(work_directory).resolve()

        # 初始化系统提示词加载器
        self.prompt_loader = SystemPromptLoader()

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

        config: TaskConfiguration = await self.parser_llm.ainvoke(user_input)

        # 构建任务步骤描述（to-do list）
        steps_text = ""
        if config.steps and len(config.steps) > 0:
            steps_text = "\n任务步骤（To-Do List）：\n"
            for i, step in enumerate(config.steps, 1):
                steps_text += f"{i}. {step}\n"
            steps_text += "\n请在事件循环中按照步骤顺序执行任务。每完成一个步骤后，检查任务进度，确认当前步骤已完成，然后继续执行下一个步骤。如果某个步骤需要多次工具调用才能完成，可以在同一轮中调用多个工具。所有步骤完成后，向用户报告任务完成。"
        else:
            steps_text = "\n这是一个简单任务，可以直接执行完成，无需分解步骤。"

        # 使用系统提示词加载器渲染提示词，注入任务配置变量
        system_prompt = self.prompt_loader.render(
            work_directory=str(self.work_directory),
            task_description=config.task_description,
            steps=steps_text,
            tools=self.tools,
        )

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
                    print(
                        f"模型决定调用工具: {[tc['name'] for tc in last_msg.tool_calls]}"
                    )
                else:
                    content = last_msg.content
                    if content:
                        print(f"模型回复: {content}")
                    else:
                        print("模型回复: (空内容)")
            elif last_msg.type == "tool":
                print(f"工具执行完成，结果长度: {len(str(last_msg.content))}")

        print("=" * 50)
        print("流程结束")
