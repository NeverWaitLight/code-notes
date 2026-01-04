from enum import Enum

from langchain_community.agent_toolkits import FileManagementToolkit
from pydantic import BaseModel


class ToolsName(Enum):
    FILE_TOOLS = "FILE_TOOLS"


class ToolFactory(BaseModel):
    workspace: str

    __tools: dict = {}

    __FILE_TOOLS = [
        "copy_file",
        "file_delete",
        "file_search",
        "move_file",
        "read_file",
        "write_file",
        "list_directory",
    ]

    def model_post_init(self, __context):
        self.__tools[ToolsName.FILE_TOOLS] = self.__FILE_TOOLS

    def get_tools(self, tools_name: ToolsName):
        match tools_name:
            case ToolsName.FILE_TOOLS:
                selected_tools = self.__tools.get(tools_name)
                return FileManagementToolkit(root_dir=self.workspace, selected_tools=selected_tools)
            case _:
                return []
