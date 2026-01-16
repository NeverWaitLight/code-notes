# Database Schema

```sql
CREATE TABLE `t_videos` (
  `id` BIGINT UNSIGNED PRIMARY KEY AUTOINCREMENT,
  `title` VARCHAR(255) NOT NULL,
  `original_filename` VARCHAR(255) NOT NULL,
  `size_bytes` INTEGER NOT NULL,
  `status` VARCHAR(16) NOT NULL CHECK (`status` IN ('UPLOADING','READY','FAILED')),
  `error_code` VARCHAR(64),
  `error_message` VARCHAR(512),
  `storage_path` VARCHAR(1024) NOT NULL,
  `created_at` BIGINT UNSIGNED NOT NULL,
  `updated_at` BIGINT UNSIGNED NOT NULL
);

CREATE TABLE `t_hls_packages` (
  `video_id` BIGINT UNSIGNED PRIMARY KEY,
  `manifest_path` VARCHAR(1024) NOT NULL,
  `segment_dir` VARCHAR(1024) NOT NULL,
  `segment_pattern` VARCHAR(64) NOT NULL,
  `segment_duration_seconds` BIGINT UNSIGNED NOT NULL,
  `segment_count` INTEGER NOT NULL,
  `total_duration_seconds` BIGINT UNSIGNED,
  `generated_at` BIGINT UNSIGNED NOT NULL,
  FOREIGN KEY (`video_id`) REFERENCES `t_videos`(`id`) ON DELETE CASCADE
);

CREATE INDEX `idx_t_videos_status` ON `t_videos` (`status`);
```
