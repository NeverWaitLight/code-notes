# Epic 1 Foundation & Core Delivery Pipeline

**Epic Goal**  
建立统一通知投递的最小可运行闭环：业务系统可以通过统一接口提交通知请求，系统完成鉴权、入队与基础处理，并能返回可追踪的状态。这一史诗确保平台基础能力可被集成验证，为后续路由与治理能力奠定基础。

## Story 1.0 工程初始化与本地可验证闭环

As a 平台工程团队,  
I want 建立统一的工程骨架与本地最小验证路径,  
so that 后续故事可以在一致的项目结构与可运行环境上快速推进与回归验证。

**Acceptance Criteria**

1. Monorepo 基础结构已落地并可构建，至少包含：`ass-kicker-server`、`ass-kicker-worker`、`ass-kicker-channel-builder`、`ass-kicker-java-client`、`ass-kicker-admin` 与根级聚合构建配置。
2. 本地依赖（PostgreSQL 与 Kafka）具备最小可运行方案（例如 docker compose 或等价方式），并在文档中给出启动步骤与必要端口约定（参考 `docs/development-environment-minimal.md` 与 `infra/local-dev/docker-compose.yml`）。
3. 提供最小环境变量样例（使用根目录 `.env.example`），覆盖数据库连接、Kafka 地址与鉴权密钥引用等关键配置项（参考 `docs/development-environment-minimal.md`）。
4. 后端服务至少暴露一个健康检查或就绪性端点（如 `/actuator/health`），用于验证本地启动成功。
5. 提供“一条龙本地验证路径”：从启动依赖 → 启动服务 → 调用健康检查/最小 API 的步骤清单可被开发者直接执行并得到可观察结果（参考 `docs/development-environment-minimal.md`）。
6. 初始化阶段不得引入与既定技术约束冲突的替代方案（需对齐 Java 21、Spring Boot 3 + WebFlux、PostgreSQL、Kafka、Vue 3）。

**Prerequisite**：无

## Story 1.1 统一发送 API 与鉴权

As a 业务系统研发团队,  
I want 通过统一 API 提交通知请求并通过鉴权校验,  
so that 我可以快速接入平台并安全地发起通知。

**Acceptance Criteria**

1. 统一发送 API 支持至少一个渠道的请求格式（可扩展至多渠道）。
2. API 必须进行发送方鉴权与权限校验（失败返回明确错误码）。
3. 请求校验覆盖必填字段、模板参数与目标地址格式。
4. 成功请求返回全局唯一的消息 ID 与初始状态（如 Queued）。
5. API 响应时间满足常规请求 SLA（与 NFR3 保持一致）。

**Prerequisite**：Story 1.0

## Story 1.2 异步投递处理闭环

As a 业务系统研发团队,  
I want 请求在后台被异步处理并进入投递流程,  
so that 我能获得稳定的投递处理能力且不阻塞业务线程。

**Acceptance Criteria**

1. 请求进入异步处理后，状态从 Queued 进入 Processing/Completed 的可见流转。
2. 至少完成一个“最小可运行投递闭环”（可为单一渠道/单一供应商/模拟投递）。
3. 投递失败时记录失败原因并进入失败状态。
4. 支持基础重试策略入口（细节在后续史诗完善）。

**Prerequisite**：Story 1.1

## Story 1.3 消息状态查询与日志检索基础能力

As a 平台运营/业务系统研发团队,  
I want 查询消息状态与基础日志,  
so that 我可以追踪投递结果并为后续治理能力提供数据基础。

**Story Context**

**Existing System Integration:**
- Integrates with: ass-kicker-server API 与异步处理链路
- Technology: Spring Boot 3 + WebFlux, PostgreSQL, Kafka
- Follows pattern: 反应式链路、统一错误格式、版本化 REST API
- Touch points: message / delivery_attempt / receipt 查询与投影

**Acceptance Criteria**

**Functional Requirements:**
1. 提供消息状态查询接口（`GET /api/v1/messages/{id}`），返回 messageId、status、channel、provider（如有）与 lastUpdatedAt。
2. 提供基础日志查询接口（`GET /api/v1/logs`），至少支持按时间范围、渠道、供应商与状态过滤，并返回可用于列表展示的最小字段集。
3. 查询结果中的状态枚举与状态流转口径与异步处理链路保持一致（Queued/Processing/Sent/Delivered/Failed）。

**Integration Requirements:**
4. 不改变 `POST /api/v1/messages/send` 的对外契约与返回语义（messageId 保持可追踪与一致）。
5. 查询接口必须复用 Story 1.1 的鉴权与权限校验边界。
6. 为 Epic 3 的日志/审计能力与 Epic 4 的日志可视化提供稳定的最小可用接口契约。

**Quality Requirements:**
7. 为状态查询与日志查询补充单元测试/集成测试，覆盖存在/不存在/鉴权失败与关键过滤条件。
8. 在 API 文档或 README 中补充查询接口的最小使用说明与字段说明。

**Technical Notes**
- **Integration Approach:** 基于 Story 1.2 已落库/已流转的消息与尝试记录提供查询投影，避免阻塞式调用。
- **Existing Pattern Reference:** 遵循 `docs/architecture.md` 中的反应式与统一错误格式规则。
- **Key Constraints:** 保持 `/api/v1` 版本前缀与向后兼容原则。

**Definition of Done**
- [ ] Functional requirements met
- [ ] Integration requirements verified
- [ ] Existing functionality regression tested
- [ ] Code follows existing patterns and standards
- [ ] Tests pass (existing and new)
- [ ] Documentation updated if applicable

### 1.3 风险与兼容性检查

**Minimal Risk Assessment:**
- **Primary Risk:** 查询口径与异步状态流转不一致，导致运营误判。
- **Mitigation:** 统一状态枚举与映射口径，并在接口响应中返回 lastUpdatedAt。
- **Rollback:** 通过配置开关或下线路由回退查询入口，不影响发送链路。

**Compatibility Verification:**
- [ ] No breaking changes to existing APIs
- [ ] Database changes (if any) are additive only
- [ ] UI changes follow existing design patterns
- [ ] Performance impact is negligible

**Prerequisite**：Story 1.2

## Story 1.4 开发者接入产物与模板

As a 平台工程团队,  
I want 提供 server 项目、通道开发模板与可分发 JDK 包,  
so that 新渠道开发与业务系统接入更高效、标准化。

**Acceptance Criteria**

1. 后端 server 项目可独立构建与运行，作为统一通知平台服务入口。
2. 通道开发模板项目包含标准化接口与示例，支持快速接入新供应商。
3. 提供可分发的 JDK 打包产物，用于业务系统快速对接与运行依赖。
4. 模板与打包产物有基础文档说明其使用方式。

**Prerequisite**：Story 1.1
