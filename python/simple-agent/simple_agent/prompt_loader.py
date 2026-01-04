from pathlib import Path
from typing import Dict, Optional, Any


class SystemPromptLoader:
    """系统提示词加载器，支持从文件读取和变量替换"""

    def __init__(self, template_path: Optional[str] = None):
        """
        初始化系统提示词加载器

        Args:
            template_path: 模板文件路径，如果为 None，则使用默认路径 resources/system-prompt-template.txt
        """
        if template_path is None:
            # 获取项目根目录
            current_file = Path(__file__).resolve()
            project_root = current_file.parent.parent
            template_path = project_root / "resources" / "system-prompt-template.txt"
        
        self.template_path = Path(template_path)
        self._cached_template: Optional[str] = None
        self._cache_mtime: Optional[float] = None

    def _load_template(self) -> str:
        """加载模板文件内容，带缓存机制"""
        if not self.template_path.exists():
            raise FileNotFoundError(
                f"系统提示词模板文件不存在: {self.template_path}"
            )

        # 检查文件修改时间，如果文件被修改则重新加载
        current_mtime = self.template_path.stat().st_mtime
        if (
            self._cached_template is None
            or self._cache_mtime is None
            or current_mtime > self._cache_mtime
        ):
            with open(self.template_path, "r", encoding="utf-8") as f:
                self._cached_template = f.read()
            self._cache_mtime = current_mtime

        return self._cached_template

    def _format_tools_description(self, tools: list) -> str:
        """格式化工具描述列表"""
        if not tools:
            return "无可用工具"

        descriptions = []
        for tool in tools:
            name = getattr(tool, "name", str(tool))
            description = getattr(tool, "description", "无描述")
            descriptions.append(f"- {name}: {description}")

        return "\n".join(descriptions)

    def render(
        self,
        work_directory: Optional[str] = None,
        user_id: Optional[str] = None,
        task_description: Optional[str] = None,
        steps: Optional[str] = None,
        topic_prompt: Optional[str] = None,
        target_word_count: Optional[int] = None,
        output_file: Optional[str] = None,
        tools: Optional[list] = None,
        **kwargs: Any,
    ) -> str:
        """
        渲染系统提示词模板，替换变量

        Args:
            work_directory: 工作目录路径
            user_id: 用户ID
            task_description: 任务描述
            steps: 任务步骤（已格式化的字符串）
            topic_prompt: 任务主题（已废弃，保留以兼容旧代码）
            target_word_count: 目标字数（已废弃，保留以兼容旧代码）
            output_file: 输出文件名（已废弃，保留以兼容旧代码）
            tools: 工具列表
            **kwargs: 其他自定义变量

        Returns:
            渲染后的系统提示词字符串
        """
        template_content = self._load_template()

        # 准备变量字典
        variables: Dict[str, str] = {}

        if work_directory is not None:
            variables["work_directory"] = str(work_directory)

        if user_id is not None:
            variables["user_id"] = str(user_id)

        if task_description is not None:
            variables["task_description"] = str(task_description)

        if steps is not None:
            variables["steps"] = str(steps)

        # 保留旧参数以兼容旧代码
        if topic_prompt is not None:
            variables["topic_prompt"] = str(topic_prompt)

        if target_word_count is not None:
            variables["target_word_count"] = str(target_word_count)

        if output_file is not None:
            variables["output_file"] = str(output_file)

        if tools is not None:
            variables["tools_description"] = self._format_tools_description(tools)

        # 添加自定义变量
        variables.update({k: str(v) for k, v in kwargs.items()})

        # 使用安全的格式化方法，支持 {variable} 格式
        # 对于未定义的变量，保留原样
        try:
            # 使用 str.format() 进行替换，但需要处理未定义的变量
            result = template_content.format(**variables)
            return result
        except KeyError as e:
            # 如果有未定义的变量，使用自定义的替换方法
            import re

            def safe_replace(match):
                var_name = match.group(1)
                if var_name in variables:
                    return str(variables[var_name])
                # 未定义的变量保留原样
                return match.group(0)

            # 匹配 {variable} 格式，但不匹配 {{ 转义
            result = re.sub(r"(?<!\{)\{(\w+)\}(?!\})", safe_replace, template_content)
            return result
        except Exception as e:
            # 如果替换失败，返回原始模板
            raise ValueError(f"系统提示词模板渲染失败: {e}") from e

    def get_raw_template(self) -> str:
        """获取原始模板内容，不进行变量替换"""
        return self._load_template()

