# Local Validation

## JPA + SQLite

- 启动后端，确认 `data/app.db` 生成且 `t_videos`、`t_hls_packages` 已创建
- 通过上传接口创建记录，验证 `t_videos` 可读写
- 删除视频后确认 `t_hls_packages` 级联删除生效

## 5GB 上传与临时目录

- 准备接近 5GB 的 H.264 MP4 文件，验证上传不被拒绝
- 上传过程中检查 `data/tmp` 是否产生临时文件
- 上传完成后确认临时文件被清理或可手动清理

## HLS 访问映射

- 处理完成后，确认 `manifestUrl` 返回且可直接访问
- 使用浏览器访问 `http://localhost:8080/media/{videoId}/index.m3u8` 验证可读
