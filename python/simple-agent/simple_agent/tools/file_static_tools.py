from pathlib import Path

from langchain.tools import tool


@tool
def count_words(file_path: str) -> int:
    """统计文件的字符数（不包括空格）。如果文件不存在或读取失败则返回 0。

    Args:
        file_path: 要统计的文件路径
    """
    try:
        path = Path(file_path)

        if not path.exists() or not path.is_file():
            return 0

        content = path.read_text(encoding="utf-8")
        char_count = sum(1 for char in content if not char.isspace())

        return char_count

    except (UnicodeDecodeError, PermissionError, Exception):
        return 0
