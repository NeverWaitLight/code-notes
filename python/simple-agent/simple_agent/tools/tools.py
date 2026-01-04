from pathlib import Path
from langchain.tools import tool, ToolRuntime
from simple_agent.context import Context


@tool
def get_weather_for_location(city: str) -> str:
    """获取指定城市的天气。"""
    return f"{city}总是阳光明媚！"


@tool
def get_user_location(runtime: ToolRuntime[Context]) -> str:
    """根据用户 ID 获取用户信息。"""
    user_id = runtime.context.user_id
    return "Florida" if user_id == "1" else "SF"


@tool
def check_file_word_count(file_path: str) -> dict:
    """检查指定文件的字数（中文字符数）。
    
    Args:
        file_path: 要检查的文件路径
        
    Returns:
        包含文件路径、当前字数和是否满足要求的字典
    """
    path = Path(file_path)
    if not path.exists():
        return {
            "file_path": str(file_path),
            "word_count": 0,
            "exists": False,
            "message": f"文件不存在: {file_path}"
        }

    try:
        content = path.read_text(encoding="utf-8")
        word_count = len(content)
        return {
            "file_path": str(file_path),
            "word_count": word_count,
            "exists": True,
            "message": f"文件当前字数: {word_count}"
        }
    except Exception as e:
        return {
            "file_path": str(file_path),
            "word_count": 0,
            "exists": False,
            "error": str(e),
            "message": f"读取文件失败: {str(e)}"
        }
