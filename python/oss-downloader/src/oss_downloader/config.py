from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Optional


@dataclass(frozen=True)
class DownloadConfig:
    endpoint: str
    region: str
    access_key_id: str
    access_key_secret: str
    bucket: str
    prefix: str
    target_dir: Path
    concurrency: int = 8
    include_suffixes: Optional[List[str]] = None
    dry_run: bool = False
    retry_max: int = 3
    retry_backoff_base: float = 0.8

    @property
    def manifest_path(self) -> Path:
        return self.target_dir / ".oss-manifest.sqlite"

    @property
    def failed_csv_path(self) -> Path:
        return self.target_dir / ".oss-failed.csv"

    def normalized_prefix(self) -> str:
        if not self.prefix:
            return ""
        return self.prefix.lstrip("/")

    def normalize_suffixes(self) -> Optional[List[str]]:
        if not self.include_suffixes:
            return None
        result: List[str] = []
        for item in self.include_suffixes:
            if not item:
                continue
            suffix = item.strip()
            if not suffix:
                continue
            if not suffix.startswith("."):
                suffix = "." + suffix
            result.append(suffix.lower())
        return result or None

    @staticmethod
    def from_prompt(
        endpoint: str,
        region: str,
        access_key_id: str,
        access_key_secret: str,
        bucket: str,
        prefix: str,
        target_dir: Path,
        concurrency: int,
        include_suffixes: Optional[Iterable[str]],
        dry_run: bool,
    ) -> "DownloadConfig":
        suffixes = list(include_suffixes) if include_suffixes else None
        return DownloadConfig(
            endpoint=endpoint.strip(),
            region=region.strip(),
            access_key_id=access_key_id.strip(),
            access_key_secret=access_key_secret.strip(),
            bucket=bucket.strip(),
            prefix=prefix.strip(),
            target_dir=target_dir,
            concurrency=concurrency,
            include_suffixes=suffixes,
            dry_run=dry_run,
        )
