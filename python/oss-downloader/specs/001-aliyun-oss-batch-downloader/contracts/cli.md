# CLI Contract

## Command

```bash
PYTHONPATH=src python -m oss_downloader
```

## Interactive Prompts

1. Endpoint (required)
2. AccessKeyId (required)
3. AccessKeySecret (required)
4. Bucket (required)
5. Prefix (optional, empty means bucket root)
6. Target directory (required)
7. Concurrency (default 8)
8. Include suffixes (optional, comma-separated, e.g. .jpg,.png)
9. Preview only? (y/N)
10. Confirm download? (y/N)

## Exit Codes

- 0: 成功或无对象可下载
- 1: 参数/权限错误或不可恢复错误
- 2: 运行时下载失败（存在失败对象）

## Output

- stdout: 进度与汇总
- stderr: 错误信息
- 失败列表文件: `<target_dir>/.oss-failed.csv`（CSV 列：`key,last_error,retry_count`；retry_count 为总尝试次数）

## Retry Policy

- 首轮每个对象仅尝试 1 次
- 第二轮仅针对失败对象重试
- 每个对象总尝试次数最多 3 次

## Manifest

- 默认路径: `<target_dir>/.oss-manifest.sqlite`
- 用于断点续传与跳过已完成对象
