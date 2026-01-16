# Tech Stack

## 关键选型与推荐（请确认）

1. **Frontend Language**

   - 选项 A：TypeScript 5.4.x（推荐）  
     优点：类型安全、API 契约更清晰、AI 开发更稳。  
     代价：需要类型声明与编译步骤。
   - 选项 B：JavaScript (ES2022)  
     优点：上手更快、样板更少。  
     代价：类型缺失，长期维护风险较高。
   - **推荐：TypeScript 5.4.x**

2. **Frontend Framework**

   - 选项 A：Vue 3（PRD 约束，推荐）
   - 选项 B：React/Svelte（与 PRD 冲突，不建议）
   - **推荐：Vue 3**

3. **Backend Framework**

   - 选项 A：Spring Boot 3.2.x（PRD 约束，推荐）
   - 选项 B：Micronaut/Quarkus（与 PRD 冲突，不建议）
   - **推荐：Spring Boot 3.2.x**

4. **Database**

   - 选项 A：SQLite 3.4x（PRD 约束，推荐）
   - 选项 B：PostgreSQL/MySQL（与 PRD 冲突，不建议）
   - **推荐：SQLite**

5. **API Style**

   - 选项 A：REST + JSON（推荐）  
     优点：简单直观，便于前后端分工与调试。
   - 选项 B：GraphQL  
     优点：灵活查询；代价：复杂度与依赖增加。
   - **推荐：REST**

6. **Video Processing**

   - 选项 A：JavaCV 1.5.x + FFmpeg（PRD 约束，推荐）
   - 选项 B：直接调用 FFmpeg CLI（需额外封装，不符合 PRD）
   - **推荐：JavaCV**

7. **Testing**
   - 选项 A：按 PRD（Unit + Integration + 手工冒烟）
   - 选项 B：加入自动化 E2E（Playwright 等）
   - **推荐：先按 PRD 约束，不引入额外 E2E 工具**

## Technology Stack Table

| Category             | Technology                                   | Version        | Purpose         | Rationale                             |
| -------------------- | -------------------------------------------- | -------------- | --------------- | ------------------------------------- |
| Frontend Language    | TypeScript                                   | 5.4.2          | 前端开发语言    | 类型安全、利于 AI 开发与 API 契约对齐 |
| Frontend Framework   | Vue 3                                        | 3.4.15         | 前端 UI 框架    | PRD 指定，生态成熟                    |
| UI Component Library | None                                         | -              | UI 组件库       | 演示项目保持轻量，避免引入额外依赖    |
| State Management     | Pinia                                        | 2.1.7          | 简单全局状态    | Vue 官方推荐、轻量                    |
| Backend Language     | Java (OpenJDK)                               | 21.0.2         | 后端开发语言    | PRD 指定、LTS                         |
| Backend Framework    | Spring Boot                                  | 3.2.2          | 后端框架        | PRD 指定，生态完善                    |
| ORM                  | Spring Data JPA (JPA)                        | 3.2.2          | ORM 持久化层    | 明确使用 JPA，配套 Spring Boot        |
| ORM Dialect          | hibernate-community-dialects (SQLiteDialect) | 6.4.2.Final    | SQLite 方言支持 | 避免 JPA/SQLite 方言不兼容            |
| Database Driver      | sqlite-jdbc                                  | 3.45.2.0       | SQLite 驱动     | 确保 JDBC 连接稳定                    |
| API Style            | REST + JSON                                  | -              | 前后端通信      | 简单易调试，满足需求                  |
| Database             | SQLite                                       | 3.45.1         | 元数据存储      | PRD 指定、本地离线                    |
| Cache                | None                                         | -              | 缓存            | 规模小，无必要                        |
| File Storage         | Local File System                            | -              | 视频与 HLS 产物 | PRD 指定、本地离线                    |
| Authentication       | None                                         | -              | 鉴权            | PRD 指定无需登录                      |
| Frontend Testing     | Vitest + Vue Test Utils                      | 1.2.2 / 2.4.3  | 单元测试        | 与 Vite/Vue 生态匹配                  |
| Backend Testing      | JUnit 5 + Spring Boot Test                   | 5.10.1 / 3.2.2 | 单元与集成测试  | 与 Spring Boot 原生集成               |
| E2E Testing          | Manual Smoke                                 | -              | 端到端验证      | PRD 指定手工冒烟                      |
| Build Tool           | Maven / Vite                                 | 3.9.6 / 5.1.4  | 构建与开发      | 后端与前端分别最佳实践                |
| Bundler              | Vite (Rollup)                                | 5.1.4          | 前端打包        | 默认集成、性能好                      |
| IaC Tool             | None                                         | -              | 基础设施        | 本地演示无需 IaC                      |
| CI/CD                | None                                         | -              | 自动化流水线    | 演示项目不要求                        |
| Monitoring           | None                                         | -              | 监控            | 演示项目不要求                        |
| Logging              | Logback (Spring Boot)                        | 1.4.14         | 应用日志        | Spring Boot 默认                      |
| CSS Framework        | None                                         | -              | 样式方案        | 保持简洁，中性风格                    |
