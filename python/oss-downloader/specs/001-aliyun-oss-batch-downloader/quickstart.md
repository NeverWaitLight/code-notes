# Quickstart: 交互式 OSS 批量下载工具

## Prerequisites

- Python 3.11+
- 有效的阿里云 OSS 访问凭证

## Install

```bash
python -m venv .venv
. .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

## Run

```bash
PYTHONPATH=src python -m oss_downloader
```

## Build (Executable)

```bash
bash scripts/build.sh
```

> 需在 Windows 与 Linux 各自平台上分别打包，生成后的可执行文件位于 `dist/`，可在无 Python 环境的机器上运行。

## Example Flow

1. 输入 Endpoint、AccessKeyId、AccessKeySecret
2. 输入 bucket 与 prefix
3. 选择目标下载目录与并发数
4. 预览对象数量/大小（可选）
5. 确认后开始下载

## Notes

- 若 prefix 为空，工具将要求二次确认以防误下载
- 下载清单默认保存于目标目录下的 `.oss-manifest.sqlite`
- 失败对象列表保存于 `<target_dir>/.oss-failed.csv`（包含 key,last_error,retry_count=总尝试次数）
- 下载流程为两轮：首轮每个对象仅尝试 1 次，第二轮仅重试失败对象（总尝试次数≤3）
