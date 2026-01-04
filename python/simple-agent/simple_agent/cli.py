import asyncio
import sys
from pathlib import Path

import click

from simple_agent.chat_service import ChatService
from simple_agent.graph import TextGenerator

BANNER = r"""
 ________  ________  _______   ________   _________   
|\   __  \|\   ____\|\  ___ \ |\   ___  \|\___   ___\ 
\ \  \|\  \ \  \___|\ \   __/|\ \  \\ \  \|___ \  \_| 
 \ \   __  \ \  \  __\ \  \_|/_\ \  \\ \  \   \ \  \  
  \ \  \ \  \ \  \|\  \ \  \_|\ \ \  \\ \  \   \ \  \ 
   \ \__\ \__\ \_______\ \_______\ \__\\ \__\   \ \__\
    \|__|\|__|\|_______|\|_______|\|__| \|__|    \|__|
"""


def validate_work_directory(path: Path) -> bool:
    """校验工作目录是否正确且存在

    Args:
        path: 要校验的路径对象

    Returns:
        如果路径存在且是目录返回 True，否则返回 False
    """
    try:
        resolved_path = path.expanduser().resolve()
        return resolved_path.exists() and resolved_path.is_dir()
    except (OSError, ValueError):
        return False


def get_work_directory(work_dir_arg: str | None = None) -> Path:
    """获取工作目录

    Args:
        work_dir_arg: 命令行参数指定的工作目录路径，如果为 None 则使用当前目录

    Returns:
        校验成功的工作目录 Path 对象
    """
    if work_dir_arg:
        selected_path = Path(work_dir_arg).expanduser().resolve()
        if not validate_work_directory(selected_path):
            click.echo(
                click.style(
                    f"路径不存在或不是目录: {work_dir_arg}，使用当前目录", fg="red"
                )
            )
            selected_path = Path.cwd()
    else:
        selected_path = Path.cwd()

    click.echo(click.style(f"工作目录: {selected_path}\n", fg="green", bold=True))
    return selected_path


def chat_with_model(agent: ChatService, user_input: str) -> None:
    """调用对话模型进行流式对话并实时显示"""
    try:
        click.echo(click.style("<<: ", fg="cyan"), nl=False)
        for chunk in agent.chat(user_input):
            click.echo(chunk, nl=False)
            sys.stdout.flush()
        click.echo()
    except Exception as e:
        click.echo(click.style(f"错误: {str(e)}", fg="red"))


@click.command()
@click.argument("work_dir", required=False)
def run_interactive(work_dir: str | None):
    """交互式命令行对话工具"""
    click.echo(click.style(BANNER, fg="cyan", bold=True))

    work_dir_path = get_work_directory(work_dir)

    if not validate_work_directory(work_dir_path):
        click.echo(click.style("工作目录校验失败，程序退出", fg="red", bold=True))
        sys.exit(1)

    agent = ChatService(work_dir_path)

    click.echo(
        click.style(
            "欢迎使用 demo-agent！输入消息开始对话，按 Ctrl+C 退出。\n", fg="green"
        )
    )

    while True:
        try:
            click.echo(click.style(">: ", fg="yellow", bold=True), nl=False)
            user_input = input()

            if not user_input.strip():
                continue
            # chat_with_model(agent, user_input)
            # asyncio.run(LangGraphTextGenerator().generate_text(user_input))
            asyncio.run(TextGenerator().generate_text(user_input))
            click.echo()

        except KeyboardInterrupt:
            click.echo(click.style("\n\n再见！", fg="green", bold=True))
            sys.exit(0)
        except EOFError:
            click.echo(click.style("\n\n再见！", fg="green", bold=True))
            sys.exit(0)
        except Exception as e:
            click.echo(click.style(f"发生错误: {str(e)}\n", fg="red"))
            continue
