from __future__ import annotations

import sqlite3
import threading
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable, List, Sequence

from .oss_client import ObjectItem


@dataclass(frozen=True)
class ManifestRow:
    key: str
    size: int
    etag: str
    status: str
    retry_count: int
    last_error: str | None


class Manifest:
    def __init__(self, path: Path) -> None:
        self.path = path
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self._lock = threading.Lock()
        self._conn = sqlite3.connect(self.path.as_posix(), check_same_thread=False)
        self._conn.execute("PRAGMA journal_mode=WAL")
        self._conn.execute("PRAGMA synchronous=NORMAL")
        self._ensure_schema()

    def close(self) -> None:
        with self._lock:
            self._conn.close()

    def _ensure_schema(self) -> None:
        with self._lock:
            self._conn.execute(
                """
                CREATE TABLE IF NOT EXISTS objects (
                    key TEXT PRIMARY KEY,
                    size INTEGER NOT NULL,
                    etag TEXT NOT NULL,
                    status TEXT NOT NULL,
                    retry_count INTEGER NOT NULL,
                    updated_at TEXT NOT NULL,
                    last_error TEXT
                )
                """
            )
            self._conn.execute(
                """
                CREATE TABLE IF NOT EXISTS runs (
                    run_id TEXT PRIMARY KEY,
                    started_at TEXT NOT NULL,
                    finished_at TEXT,
                    total_count INTEGER,
                    success_count INTEGER,
                    failed_count INTEGER,
                    target_dir TEXT,
                    prefix TEXT
                )
                """
            )
            self._conn.commit()

    def add_objects(self, items: Iterable[ObjectItem]) -> int:
        now = _utc_now()
        rows = [
            (item.key, int(item.size), item.etag or "", "pending", 0, now, None)
            for item in items
        ]
        if not rows:
            return 0
        with self._lock:
            self._conn.executemany(
                """
                INSERT INTO objects (key, size, etag, status, retry_count, updated_at, last_error)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(key) DO UPDATE SET
                    size=excluded.size,
                    etag=excluded.etag
                """,
                rows,
            )
            self._conn.commit()
        return len(rows)

    def increment_retry(self, key: str) -> int:
        now = _utc_now()
        with self._lock:
            self._conn.execute(
                """
                UPDATE objects
                SET retry_count = retry_count + 1,
                    updated_at = ?
                WHERE key = ?
                """,
                (now, key),
            )
            self._conn.commit()
            cur = self._conn.execute(
                "SELECT retry_count FROM objects WHERE key = ?",
                (key,),
            )
            row = cur.fetchone()
            return int(row[0]) if row else 0

    def mark_in_progress(self, key: str) -> None:
        now = _utc_now()
        with self._lock:
            self._conn.execute(
                "UPDATE objects SET status = ?, updated_at = ? WHERE key = ?",
                ("in_progress", now, key),
            )
            self._conn.commit()

    def mark_success(self, key: str) -> None:
        now = _utc_now()
        with self._lock:
            self._conn.execute(
                "UPDATE objects SET status = ?, last_error = NULL, updated_at = ? WHERE key = ?",
                ("success", now, key),
            )
            self._conn.commit()

    def mark_failed(self, key: str, error: str) -> None:
        now = _utc_now()
        with self._lock:
            self._conn.execute(
                "UPDATE objects SET status = ?, last_error = ?, updated_at = ? WHERE key = ?",
                ("failed", error, now, key),
            )
            self._conn.commit()

    def list_by_status(self, statuses: Sequence[str]) -> List[ManifestRow]:
        if not statuses:
            return []
        placeholders = ",".join(["?"] * len(statuses))
        query = (
            "SELECT key, size, etag, status, retry_count, last_error "
            f"FROM objects WHERE status IN ({placeholders})"
        )
        with self._lock:
            cur = self._conn.execute(query, tuple(statuses))
            rows = [
                ManifestRow(
                    key=row[0],
                    size=int(row[1]),
                    etag=row[2],
                    status=row[3],
                    retry_count=int(row[4]),
                    last_error=row[5],
                )
                for row in cur.fetchall()
            ]
        return rows

    def list_failed_for_retry(self, max_attempts: int) -> List[ManifestRow]:
        with self._lock:
            cur = self._conn.execute(
                """
                SELECT key, size, etag, status, retry_count, last_error
                FROM objects
                WHERE status = 'failed' AND retry_count < ?
                """,
                (max_attempts,),
            )
            rows = [
                ManifestRow(
                    key=row[0],
                    size=int(row[1]),
                    etag=row[2],
                    status=row[3],
                    retry_count=int(row[4]),
                    last_error=row[5],
                )
                for row in cur.fetchall()
            ]
        return rows

    def get_retry_count(self, key: str) -> int:
        with self._lock:
            cur = self._conn.execute(
                "SELECT retry_count FROM objects WHERE key = ?",
                (key,),
            )
            row = cur.fetchone()
        return int(row[0]) if row else 0

    def summary(self) -> dict:
        with self._lock:
            total = self._conn.execute("SELECT COUNT(*) FROM objects").fetchone()[0]
            success = self._conn.execute(
                "SELECT COUNT(*) FROM objects WHERE status = 'success'"
            ).fetchone()[0]
            failed = self._conn.execute(
                "SELECT COUNT(*) FROM objects WHERE status = 'failed'"
            ).fetchone()[0]
        return {
            "total": int(total),
            "success": int(success),
            "failed": int(failed),
        }

    def export_failed_csv(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        rows = self.list_by_status(["failed"])
        with path.open("w", encoding="utf-8", newline="") as handle:
            handle.write("key,last_error,retry_count\n")
            for row in rows:
                last_error = (row.last_error or "").replace("\n", " ").replace("\r", " ")
                handle.write(f"{_csv_escape(row.key)},{_csv_escape(last_error)},{row.retry_count}\n")


def _utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def _csv_escape(value: str) -> str:
    needs_quote = any(ch in value for ch in [",", "\"", "\n", "\r"])
    escaped = value.replace("\"", '""')
    return f'"{escaped}"' if needs_quote else escaped
