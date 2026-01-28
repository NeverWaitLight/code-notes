from __future__ import annotations

import time
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from typing import Iterable, Optional

from .config import DownloadConfig
from .filters import matches_suffix
from .manifest import Manifest, ManifestRow
from .oss_client import ObjectItem, OssClient
from .progress import ProgressReporter


@dataclass(frozen=True)
class DownloadSummary:
    total: int
    success: int
    failed: int
    skipped: int
    duration_sec: float
    total_size: int


@dataclass(frozen=True)
class PreviewInfo:
    total: int | None
    total_size: int | None
    preview_names: list[str]


FILE_PREVIEW_LIMIT = 10


def preview_objects(
    client: OssClient,
    prefix: str,
    suffixes: Optional[Iterable[str]],
) -> PreviewInfo:
    _ = suffixes
    preview_names: list[str] = []
    for obj in client.list_objects(prefix=prefix):
        preview_names.append(obj.key)
        if len(preview_names) >= FILE_PREVIEW_LIMIT:
            break
    return PreviewInfo(
        total=None,
        total_size=None,
        preview_names=preview_names,
    )


def prepare_manifest(
    client: OssClient,
    prefix: str,
    suffixes: Optional[Iterable[str]],
    manifest: Manifest,
    batch_size: int = 500,
) -> PreviewInfo:
    total_count = 0
    total_size = 0
    preview_names: list[str] = []
    batch: list[ObjectItem] = []
    for obj in client.list_objects(prefix=prefix):
        if len(preview_names) < FILE_PREVIEW_LIMIT:
            preview_names.append(obj.key)
        if _is_folder_marker(obj):
            continue
        if not matches_suffix(obj.key, suffixes):
            continue
        total_count += 1
        total_size += int(obj.size)
        batch.append(obj)
        if len(batch) >= batch_size:
            manifest.add_objects(batch)
            batch = []
    if batch:
        manifest.add_objects(batch)
    return PreviewInfo(
        total=total_count,
        total_size=total_size,
        preview_names=preview_names,
    )


def execute_download(
    config: DownloadConfig,
    client: OssClient,
    manifest: Manifest,
    progress: ProgressReporter,
    total_size: int,
) -> DownloadSummary:
    start = time.monotonic()

    initial_summary = manifest.summary()
    initial_success = initial_summary["success"]

    pending = manifest.list_by_status(["pending"])
    if pending:
        _run_pass(
            config,
            client,
            manifest,
            progress,
            pending,
            allow_multi_attempt=False,
            pass_label="首轮下载",
        )

    failed_for_retry = manifest.list_failed_for_retry(config.retry_max)
    if failed_for_retry:
        _run_pass(
            config,
            client,
            manifest,
            progress,
            failed_for_retry,
            allow_multi_attempt=True,
            pass_label="失败重试",
        )

    summary = manifest.summary()
    duration = time.monotonic() - start
    skipped = initial_success

    return DownloadSummary(
        total=summary["total"],
        success=summary["success"],
        failed=summary["failed"],
        skipped=skipped,
        duration_sec=duration,
        total_size=total_size,
    )


def _run_pass(
    config: DownloadConfig,
    client: OssClient,
    manifest: Manifest,
    progress: ProgressReporter,
    rows: Iterable[ManifestRow],
    allow_multi_attempt: bool,
    pass_label: str,
) -> None:
    rows_list = list(rows)
    if not rows_list:
        return
    print(f"{pass_label}: {len(rows_list)} 项")
    progress.start(len(rows_list), label=pass_label)

    from concurrent.futures import ThreadPoolExecutor, as_completed

    with ThreadPoolExecutor(max_workers=config.concurrency) as executor:
        futures = []
        for row in rows_list:
            if allow_multi_attempt:
                futures.append(
                    executor.submit(
                        _download_with_retries,
                        config,
                        client,
                        manifest,
                        row,
                    )
                )
            else:
                futures.append(
                    executor.submit(
                        _download_once,
                        config,
                        client,
                        manifest,
                        row,
                    )
                )

        for future in as_completed(futures):
            ok = future.result()
            if ok:
                progress.advance_success()
            else:
                progress.advance_failed()

    progress.stop()


def _download_once(
    config: DownloadConfig,
    client: OssClient,
    manifest: Manifest,
    row: ManifestRow,
) -> bool:
    key = row.key
    manifest.mark_in_progress(key)
    manifest.increment_retry(key)
    try:
        _download_key(config, client, key)
    except Exception as exc:  # noqa: BLE001
        manifest.mark_failed(key, _format_error(exc))
        return False
    manifest.mark_success(key)
    return True


def _download_with_retries(
    config: DownloadConfig,
    client: OssClient,
    manifest: Manifest,
    row: ManifestRow,
) -> bool:
    key = row.key
    while True:
        current_attempts = manifest.get_retry_count(key)
        if current_attempts >= config.retry_max:
            return False
        manifest.mark_in_progress(key)
        attempt = manifest.increment_retry(key)
        try:
            _download_key(config, client, key)
        except Exception as exc:  # noqa: BLE001
            manifest.mark_failed(key, _format_error(exc))
            if attempt >= config.retry_max:
                return False
            backoff = config.retry_backoff_base * (2 ** (attempt - 1))
            time.sleep(backoff)
            continue
        manifest.mark_success(key)
        return True


def _download_key(config: DownloadConfig, client: OssClient, key: str) -> None:
    target = _safe_target_path(config.target_dir, key)
    target.parent.mkdir(parents=True, exist_ok=True)
    client.download_to_file(key, target.as_posix())


def _safe_target_path(base_dir: Path, key: str) -> Path:
    posix_path = PurePosixPath(key.lstrip("/"))
    parts = [part for part in posix_path.parts if part not in ("", ".", "..")]
    if not parts:
        parts = ["object"]
    return base_dir.joinpath(*parts)


def _is_folder_marker(item: ObjectItem) -> bool:
    return item.key.endswith("/") and item.size == 0


def _format_error(exc: Exception) -> str:
    return f"{exc.__class__.__name__}: {exc}"
