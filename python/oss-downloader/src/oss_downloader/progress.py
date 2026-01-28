from __future__ import annotations

import threading
from typing import Optional

from rich.console import Console
from rich.progress import (
    BarColumn,
    Progress,
    SpinnerColumn,
    TaskProgressColumn,
    TextColumn,
    TimeElapsedColumn,
    TimeRemainingColumn,
    TransferSpeedColumn,
)


class ProgressReporter:
    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._console = Console()
        self._progress = Progress(
            SpinnerColumn(),
            TextColumn("{task.description}"),
            BarColumn(),
            TaskProgressColumn(),
            TransferSpeedColumn(),
            TimeElapsedColumn(),
            TimeRemainingColumn(),
            console=self._console,
            transient=False,
        )
        self._task_id: Optional[int] = None
        self._total = 0
        self._success = 0
        self._failed = 0
        self._label = "下载"

    def start(self, total: int, label: str = "下载") -> None:
        with self._lock:
            self._total = total
            self._success = 0
            self._failed = 0
            self._label = label
            if not self._progress.live:
                self._progress.start()
            self._task_id = self._progress.add_task(self._description(), total=total)

    def stop(self) -> None:
        with self._lock:
            if self._progress.live:
                self._progress.stop()
            self._task_id = None

    def advance_success(self) -> None:
        with self._lock:
            self._success += 1
            self._advance()

    def advance_failed(self) -> None:
        with self._lock:
            self._failed += 1
            self._advance()

    def _advance(self) -> None:
        if self._task_id is None:
            return
        self._progress.update(self._task_id, advance=1, description=self._description())

    def _description(self) -> str:
        return (
            f"{self._label} {self._success + self._failed}/{self._total} "
            f"(成功 {self._success} 失败 {self._failed})"
        )
