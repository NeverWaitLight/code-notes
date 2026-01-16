# Technical Assumptions

## Repository Structure: Monorepo（前后端分目录）

## Service Architecture

- Monolith（单体）Spring Boot 服务提供 API 与 HLS 静态资源

## Testing Requirements

- Unit + Integration（基础单元测试 + API 集成测试，另保留手工冒烟）

## Additional Technical Assumptions and Requests

- 后端构建工具使用 Maven
- 前后端代码位于根目录两个独立目录（如 `backend/` 与 `frontend/`）
- 根目录提供 `startup.sh` 脚本用于一键启动前后端（开发模式）
- 启动脚本仅用于演示，不考虑打包发布流程
- 前端使用 Vite 构建 Vue 3 项目，并使用 Vue Router 实现列表/上传/播放三页路由
- 视频文件与 HLS 产物存储在本地可配置目录（如 `data/videos/{id}/`）
- SQLite 数据库文件存储在本地可配置路径（如 `data/app.db`）
- 上传采用 multipart/form-data；视频处理使用 JavaCV 调用 FFmpeg 能力
- HLS 切片与清单由后端生成并通过 Spring Boot 静态资源或专用接口提供
- 异步处理采用应用内线程池/@Async，不引入外部队列
- 部署目标为本机运行（非容器化）；如需 Docker 化请说明
