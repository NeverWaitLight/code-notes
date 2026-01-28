from __future__ import annotations

from typing import Iterable, Optional


def parse_suffixes(raw: Optional[str]) -> Optional[list[str]]:
    if not raw:
        return None
    parts = [item.strip() for item in raw.split(",")]
    suffixes = [p for p in parts if p]
    if not suffixes:
        return None
    normalized: list[str] = []
    for suffix in suffixes:
        if not suffix.startswith("."):
            suffix = "." + suffix
        normalized.append(suffix.lower())
    return normalized


def matches_suffix(key: str, suffixes: Optional[Iterable[str]]) -> bool:
    if not suffixes:
        return True
    lower_key = key.lower()
    for suffix in suffixes:
        if lower_key.endswith(str(suffix).lower()):
            return True
    return False