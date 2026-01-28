# Data Model: 交互式 OSS 批量下载工具

## DownloadConfig

- endpoint
- access_key_id
- access_key_secret
- bucket
- prefix
- target_dir
- concurrency
- include_suffixes (optional)
- dry_run (preview only)
- retry_max
- retry_backoff_base

## ObjectItem

- key
- size
- etag
- last_modified

## DownloadManifest (SQLite)

- table: objects
  - key (TEXT, PRIMARY KEY)
  - size (INTEGER)
  - etag (TEXT)
  - status (TEXT: pending | in_progress | success | failed)
  - retry_count (INTEGER, total attempts)
  - updated_at (TEXT)
  - last_error (TEXT, nullable)

- table: runs
  - run_id (TEXT, PRIMARY KEY)
  - started_at (TEXT)
  - finished_at (TEXT)
  - total_count (INTEGER)
  - success_count (INTEGER)
  - failed_count (INTEGER)
  - target_dir (TEXT)
  - prefix (TEXT)

## DownloadSummary

- total
- success
- failed
- skipped
- duration_sec
