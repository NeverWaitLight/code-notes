"""File system utilities for handling filenames safely."""
from __future__ import annotations

import re
from pathlib import PurePosixPath


# Characters that are invalid in Windows filenames
# Also commonly restricted in other filesystems
INVALID_CHARS_PATTERN = re.compile(r'[<>:"|?*\x00-\x1f]')

# Windows reserved names (case-insensitive)
RESERVED_NAMES = {
    "CON", "PRN", "AUX", "NUL",
    "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
    "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
}


def sanitize_filename(filename: str, replacement: str = "-") -> str:
    """
    Sanitize a filename by replacing invalid filesystem characters.

    Args:
        filename: The filename to sanitize
        replacement: Character to replace invalid characters with (default: "-")

    Returns:
        Sanitized filename safe for use on most filesystems
    """
    if not filename:
        return "unnamed"

    # Replace invalid characters with the replacement character
    sanitized = INVALID_CHARS_PATTERN.sub(replacement, filename)

    # Replace backslash (path separator on Windows) with forward slash
    # This is safe since we're dealing with individual path components
    sanitized = sanitized.replace("\\", replacement)

    # Remove leading/trailing whitespace and dots
    sanitized = sanitized.strip(". ")

    # If empty after sanitization, use a default name
    if not sanitized:
        return "unnamed"

    # Check if the base name (without extension) is a reserved name on Windows
    name_parts = sanitized.rsplit(".", 1)
    base_name = name_parts[0].upper()

    if base_name in RESERVED_NAMES:
        sanitized = f"{replacement}{sanitized}"

    return sanitized


def sanitize_path_components(path: str, replacement: str = "-") -> str:
    """
    Sanitize all components of a path.

    Args:
        path: The path string to sanitize (can include forward slashes)
        replacement: Character to replace invalid characters with (default: "-")

    Returns:
        Path with all components sanitized
    """
    if not path:
        return "unnamed"

    # Treat as POSIX path to handle forward slashes consistently
    posix_path = PurePosixPath(path.lstrip("/"))

    # Sanitize each part separately
    sanitized_parts = [
        sanitize_filename(part, replacement)
        for part in posix_path.parts
        if part not in ("", ".", "..")
    ]

    if not sanitized_parts:
        return "unnamed"

    # Rejoin with forward slash
    return "/".join(sanitized_parts)
