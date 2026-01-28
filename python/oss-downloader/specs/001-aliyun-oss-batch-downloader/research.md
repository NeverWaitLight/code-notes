# Research: 交互式 OSS 批量下载工具

## SDK / Library 选择

### OSS SDK

- **候选**: oss2
- **结论**: 采用 oss2（阿里云官方 Python SDK）作为对象列举与下载实现基础。

### 交互式输入

- **候选**: questionary, InquirerPy, Typer + prompt_toolkit
- **结论**: 采用 questionary，API 简洁，跨平台兼容性好。

### 进度展示

- **候选**: rich, tqdm
- **结论**: 采用 rich，支持多任务进度与自定义展示。

### 重试策略

- **候选**: 自实现指数退避, tenacity
- **结论**: 优先自实现指数退避（减少依赖）。如需更复杂策略再引入 tenacity。

## 技术验证要点

- oss2 的 list_objects_v2 分页与 prefix 过滤
- oss2 的 get_object_to_file 或 resumable_download 支持
- 多线程下载与 rich 进度条的线程安全更新
- Windows 终端下交互式 prompt 体验

## 风险与规避

- **大批量列表性能**: 采用分页拉取与流式处理，避免一次性加载全部对象。
- **断点续传一致性**: manifest 采用 SQLite 事务更新，避免并发写冲突。
- **路径安全**: 对 OSS key 进行路径规范化，禁止上级目录穿越。