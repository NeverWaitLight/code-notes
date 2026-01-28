# OSS 批量下载工具

基于 Python 的终端交互式工具，用于从阿里云 OSS 指定 bucket/prefix 批量下载文件，支持断点续传、两轮重试、下载前预览、后缀过滤，并输出失败列表 CSV。可打包为无需 Python 环境的可执行文件（Windows/Linux）。

## 环境准备（venv）

```bash
python -m venv .venv
# Bash 环境（Windows Git Bash 等）：
source .venv/Scripts/activate
# Linux/macOS Bash：
# source .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements.txt
```

## 本地运行（开发模式）

```bash
PYTHONPATH=src python -m oss_downloader
```

## 交互流程

1. 输入 Endpoint、AccessKeyId、AccessKeySecret
2. 输入 bucket 与 Region（例如 `cn-chengdu`）
3. 输入 prefix（可空）
4. 选择目标下载目录与并发数
5. 可选输入后缀过滤（例如 `.jpg,.png`）
6. 可选仅预览不下载
7. 确认后开始下载

## 兼容性提示

- 如出现“卡在并发数”等终端交互异常，可设置 `OSS_DOWNLOADER_SIMPLE_PROMPT=1` 使用简化输入。
- 也可设置 `OSS_DOWNLOADER_CONCURRENCY=8` 跳过并发数输入（范围 1~64）。

## 关于签名版本

- 默认使用 V4 签名，需要提供 Region。
- Endpoint 建议填写公网域名，例如 `oss-cn-chengdu.aliyuncs.com` 或带 `https://` 的完整地址。

## 下载策略

- 两轮下载：
  - 首轮每个对象仅尝试 1 次
  - 第二轮仅重试失败对象
- 每个对象总尝试次数最多 3 次
- 断点续传通过 manifest 记录状态

## 输出文件

- 清单数据库：`<target_dir>/.oss-manifest.sqlite`
- 失败列表：`<target_dir>/.oss-failed.csv`
  - CSV 列：`key,last_error,retry_count`
  - `retry_count` 为总尝试次数

## 退出码

- `0`：成功或无对象可下载
- `1`：参数/权限错误或不可恢复错误
- `2`：下载结束但存在失败对象

## 打包可执行文件

> 需在对应系统上分别打包（Windows/Linux），此脚本仅构建当前平台产物。

```bash
bash scripts/build.sh
```

生成的可执行文件在 `dist/` 目录。

运行可执行文件建议在终端中执行，以便查看错误信息：

```bash
./dist/oss-downloader
```

如出现闪退，可查看日志 `~/.oss-downloader.log`。
