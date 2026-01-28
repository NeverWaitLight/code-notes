from __future__ import annotations

import logging
import os
import sys
from pathlib import Path

import questionary

from .config import DownloadConfig
from .downloader import DownloadSummary, execute_download, prepare_manifest, preview_objects
from .filters import parse_suffixes
from .manifest import Manifest
from .oss_client import OssClient
from .progress import ProgressReporter


def run() -> int:
    try:
        _setup_logging()
        logging.getLogger("oss_downloader").info("start")
        config = _prompt_config()
        if config is None:
            return 0
        suffixes = config.normalize_suffixes()

        client = OssClient(
            endpoint=config.endpoint,
            access_key_id=config.access_key_id,
            access_key_secret=config.access_key_secret,
            bucket=config.bucket,
        )

        if config.dry_run:
            try:
                total_count, total_size = preview_objects(
                    client,
                    config.normalized_prefix(),
                    suffixes,
                )
            except Exception as exc:  # noqa: BLE001
                _print_oss_error(exc)
                _pause_on_error()
                return 1
            _print_preview(total_count, total_size)
            return 0

        print("正在列举对象，请稍候...")
        manifest = Manifest(config.manifest_path)
        try:
            try:
                total_count, total_size = prepare_manifest(
                    client,
                    config.normalized_prefix(),
                    suffixes,
                    manifest,
                )
            except Exception as exc:  # noqa: BLE001
                _print_oss_error(exc)
                _pause_on_error()
                return 1

            if total_count == 0:
                print("未找到可下载对象。")
                return 0

            _print_preview(total_count, total_size)
            _print_match_stats(total_count, manifest)
            if not questionary.confirm("确认开始下载？", default=True).ask():
                print("已取消下载。")
                return 0

            progress = ProgressReporter()
            summary = execute_download(config, client, manifest, progress, total_size)
            manifest.export_failed_csv(config.failed_csv_path)
            _print_summary(summary, total_count)

            if summary.failed > 0:
                return 2
            return 0
        finally:
            manifest.close()
    except KeyboardInterrupt:
        print("已取消。", file=sys.stderr)
        return 1
    except Exception as exc:  # noqa: BLE001
        _log_exception("发生未处理错误")
        print("发生错误，请查看日志：~/.oss-downloader.log", file=sys.stderr)
        print(f"错误摘要: {exc.__class__.__name__}: {exc}", file=sys.stderr)
        _pause_on_error()
        return 1


def _prompt_config() -> DownloadConfig | None:
    endpoint = questionary.text("Endpoint:").ask()
    if not endpoint:
        print("Endpoint 不能为空。", file=sys.stderr)
        return None

    access_key_id = questionary.text("AccessKeyId:").ask()
    if not access_key_id:
        print("AccessKeyId 不能为空。", file=sys.stderr)
        return None

    access_key_secret = questionary.password("AccessKeySecret:").ask()
    if not access_key_secret:
        print("AccessKeySecret 不能为空。", file=sys.stderr)
        return None

    bucket = questionary.text("Bucket:").ask()
    if not bucket:
        print("Bucket 不能为空。", file=sys.stderr)
        return None

    prefix = questionary.text("Prefix (可为空):", default="").ask() or ""
    prefix = prefix.strip()

    target_raw = questionary.text("下载目录:", default=str(Path.cwd())).ask()
    if not target_raw:
        print("下载目录不能为空。", file=sys.stderr)
        return None

    target_dir = Path(target_raw).expanduser().resolve()
    target_dir.mkdir(parents=True, exist_ok=True)

    concurrency = _ask_int("并发数", default=8, minimum=1, maximum=64)
    if concurrency is None:
        return None

    suffix_raw = questionary.text("包含后缀 (可选, 逗号分隔):", default="").ask() or ""
    include_suffixes = parse_suffixes(suffix_raw)

    preview_only = questionary.confirm("仅预览不下载？", default=False).ask()

    if prefix in ("", "/"):
        confirm_root = questionary.confirm(
            "Prefix 为空将下载整个 bucket，确定继续？",
            default=False,
        ).ask()
        if not confirm_root:
            print("已取消：Prefix 为空未确认下载。")
            return None

    config = DownloadConfig.from_prompt(
        endpoint=endpoint,
        access_key_id=access_key_id,
        access_key_secret=access_key_secret,
        bucket=bucket,
        prefix=prefix,
        target_dir=target_dir,
        concurrency=concurrency,
        include_suffixes=include_suffixes,
        dry_run=preview_only,
    )
    return config


def _ask_int(label: str, default: int, minimum: int, maximum: int) -> int | None:
    while True:
        raw = questionary.text(f"{label}:", default=str(default)).ask()
        if raw is None:
            return None
        raw = raw.strip()
        if not raw:
            raw = str(default)
        try:
            value = int(raw)
        except ValueError:
            print("请输入整数。", file=sys.stderr)
            continue
        if value < minimum or value > maximum:
            print(f"数值范围必须在 {minimum}~{maximum} 之间。", file=sys.stderr)
            continue
        return value


def _print_preview(total_count: int, total_size: int) -> None:
    print("预览统计:")
    print(f"  对象数量: {total_count}")
    print(f"  总大小: {_format_size(total_size)}")


def _print_summary(summary: DownloadSummary, total_matched: int) -> None:
    print("下载完成:")
    print(f"  匹配总数: {total_matched}")
    print(f"  成功: {summary.success}")
    print(f"  失败: {summary.failed}")
    print(f"  跳过: {summary.skipped}")
    print(f"  总大小: {_format_size(summary.total_size)}")
    print(f"  用时: {_format_duration(summary.duration_sec)}")


def _print_match_stats(total_count: int, manifest: Manifest) -> None:
    summary = manifest.summary()
    pending_count = len(manifest.list_by_status(["pending"]))
    done_count = summary["success"] + summary["failed"]
    print("下载统计:")
    print(f"  匹配对象总数: {total_count}")
    print(f"  已完成(历史): {done_count}")
    print(f"  待下载(本次): {pending_count}")


def _print_oss_error(exc: Exception) -> None:
    print("无法连接 OSS，请检查 Endpoint、AccessKey、Bucket 或网络。", file=sys.stderr)
    print(f"详细错误: {exc.__class__.__name__}: {exc}", file=sys.stderr)
    _log_exception("OSS 连接失败")


def _setup_logging() -> None:
    log_path = Path.home() / ".oss-downloader.log"
    log_path.parent.mkdir(parents=True, exist_ok=True)
    logging.basicConfig(
        filename=str(log_path),
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
        force=True,
    )


def _log_exception(message: str) -> None:
    logger = logging.getLogger("oss_downloader")
    logger.exception(message)


def _pause_on_error() -> None:
    if os.environ.get("OSS_DOWNLOADER_NO_PAUSE") == "1":
        return
    try:
        input("发生错误，按回车键退出...")
    except Exception:  # noqa: BLE001
        return


def _format_size(num_bytes: int) -> str:
    if num_bytes < 1024:
        return f"{num_bytes} B"
    for unit in ["KB", "MB", "GB", "TB"]:
        num_bytes /= 1024.0
        if num_bytes < 1024:
            return f"{num_bytes:.2f} {unit}"
    return f"{num_bytes:.2f} PB"


def _format_duration(seconds: float) -> str:
    if seconds < 60:
        return f"{seconds:.1f}s"
    minutes, sec = divmod(int(seconds), 60)
    if minutes < 60:
        return f"{minutes}m{sec}s"
    hours, minutes = divmod(minutes, 60)
    return f"{hours}h{minutes}m"


def main() -> None:
    exit_code = run()
    raise SystemExit(exit_code)


if __name__ == "__main__":
    main()
