#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 [--windows|--linux]" >&2
  exit 1
}

want_platform=""
if [ $# -gt 1 ]; then
  usage
elif [ $# -eq 1 ]; then
  case "$1" in
    --windows) want_platform="windows" ;;
    --linux) want_platform="linux" ;;
    *) usage ;;
  esac
fi

uname_s="$(uname -s || true)"
case "$uname_s" in
  Linux*) platform="linux" ;;
  MINGW*|MSYS*|CYGWIN*) platform="windows" ;;
  *)
    echo "Unsupported OS: $uname_s" >&2
    exit 1
    ;;
 esac

if [ -n "$want_platform" ] && [ "$want_platform" != "$platform" ]; then
  echo "Requested $want_platform build, but current platform is $platform." >&2
  echo "Please run this script on a $want_platform machine to build that binary." >&2
  exit 1
fi

find_python() {
  if [ -x ".venv/bin/python" ]; then
    echo ".venv/bin/python"
    return 0
  fi
  if [ -x ".venv/Scripts/python.exe" ]; then
    echo ".venv/Scripts/python.exe"
    return 0
  fi
  if command -v python3 >/dev/null 2>&1; then
    echo python3
  elif command -v python >/dev/null 2>&1; then
    echo python
  else
    echo "Python not found in PATH" >&2
    echo "提示：Bash 环境下可先执行 source .venv/Scripts/activate" >&2
    exit 1
  fi
}

collect_windows_runtime_binaries() {
  "$pybin" - <<'PY'
import sys
from pathlib import Path

base = Path(sys.base_prefix)
search_dirs = [base, base / "DLLs"]
patterns = ["vcruntime*.dll", "msvcp*.dll", "concrt140.dll", "ucrtbase.dll"]

seen = set()
for d in search_dirs:
    if not d.is_dir():
        continue
    for pattern in patterns:
        for p in d.glob(pattern):
            rp = str(p.resolve())
            key = rp.lower()
            if key in seen:
                continue
            seen.add(key)
            print(rp)
PY
}

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

export PYTHONPATH="$repo_root/src"
pybin="$(find_python)"

add_binaries=()
if [ "$platform" = "windows" ]; then
  while IFS= read -r dll; do
    dll="${dll%$'\r'}"
    [ -n "$dll" ] || continue
    add_binaries+=(--add-binary "${dll};.")
  done < <(collect_windows_runtime_binaries)
fi

"$pybin" -m PyInstaller \
  --onefile \
  --noconfirm \
  --clean \
  -n oss-downloader \
  -p "$repo_root/src" \
  "${add_binaries[@]}" \
  "$repo_root/src/oss_downloader/__main__.py"

echo "Build complete for $platform. Output: dist/" >&2
