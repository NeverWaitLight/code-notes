# Coding Standards

## Critical Fullstack Rules

- **Type Sharing:** 前后端共享类型以 `frontend/src/types` 为准，后端 DTO 与之保持字段一致
- **Error Format:** 所有失败响应必须返回 `{ error: { code, message } }`
- **Async Processing:** 上传接口只负责入库与落盘，切片必须异步处理
- **Storage Paths:** 存储路径全部通过配置读取，不允许硬编码

## Naming Conventions

| Element         | Frontend   | Backend    | Example             |
| --------------- | ---------- | ---------- | ------------------- |
| Components      | PascalCase | -          | `VideoListPage.vue` |
| API Routes      | -          | kebab-case | `/api/videos`       |
| Database Tables | -          | snake_case | `t_videos`          |
