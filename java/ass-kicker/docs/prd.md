# Ass Kicker Product Requirements Document (PRD)

## Goals and Background Context

### Goals

- 降低多渠道通知集成与维护成本（目标：开发人力减少约 30%）
- 提升整体送达率与可用性（目标：送达率 ≥ 99.5%）
- 将故障切换时间控制在 30 秒以内，缩短 MTTR
- 新业务接入周期缩短至 1 周以内
- 统一模板与状态治理，提升运营与审计效率
- 在大规模广播/批量场景下保持高吞吐与可扩展性

### Background Context

企业内部系统在通知与触达领域普遍存在多渠道、多供应商集成复杂的问题，导致重复建设、维护成本高，且送达稳定性受供应商波动影响显著。现有点对点或单供应商方案缺乏统一治理与智能路由能力，跨渠道一致性（模板、回执、优先级）和故障转移机制不足，使得可靠性与成本难以控制。

本项目旨在构建统一通知中心平台，以标准化 API 接入多渠道（SMS/Email/IM/Push）与多供应商，提供智能路由、故障转移、重试与状态回执等能力，并通过 Kafka + Worker 异步架构满足高吞吐与可扩展性需求，从而提升业务连续性与用户体验。

### Change Log

| Date       | Version | Description                                 | Author    |
| ---------- | ------- | ------------------------------------------- | --------- |
| 2026-01-23 | v0.1    | 初稿：基于 Project Brief 起草 Goals/Context | John (PM) |
| 2026-01-26 | v0.2    | 同步架构最终选择                             | Codex     |

## MVP Scope

### In Scope (Must Have)
- 统一发送 API 与鉴权，支持通知请求标准化接入
- 异步投递处理闭环（入队、处理、状态流转）
- 多渠道接入框架（SMS/Email/IM/Push），至少 1 个邮件渠道具备端到端可用能力，其余可先具备基础适配能力
- 智能路由与基础故障转移/重试策略
- 模板与多语言支持（基础版本）
- 消息日志、状态追踪与回执记录
- 广播/批量发送能力（基础策略）
- 管理控制台核心页面（日志、模板、路由、供应商、状态）
- 交付物：ass-kicker-server、ass-kicker-channel-builder、ass-kicker-java-client、ass-kicker-admin

### Out of Scope (Not in MVP)
- 高级可视化控制台与 BI 报表
- 多租户商业化计费体系
- 全面合规审计与复杂审批流
- 复杂的推荐/AI 智能路由优化

### MVP Success Criteria
- 在首批业务系统中替换现有通知实现并可稳定运行
- 送达率 ≥ 99.5%，故障切换时间 < 30s，常规通知 P95 < 5s
- 新业务接入周期 < 1 周（在既定 SDK/适配规范下）

### MVP Validation Approach
- 选定首批接入场景：登录验证码
- 以真实流量验证 KPI（送达率、延迟、故障切换、接入周期）
- 验证 Java JDK 包（ass-kicker-java-client）接入效率与兼容性

### MVP Timeline & Rollout
- 目标时间框架：3 周
- 交付节奏：先后端闭环，再接入渠道/路由治理，再完善控制台

## Requirements

### Functional

1. FR1: 提供统一发送 API，支持 SMS/Email/IM/Push 多渠道投递。
2. FR2: 支持多供应商接入与可插拔适配，按渠道配置多个供应商。
3. FR3: 支持基于地址/语言/时间等规则的智能路由与供应商选择。
4. FR4: 支持优先级队列与调度（Emergency/High/Normal/Low/Background）。
5. FR5: 支持失败自动重试与故障转移（含指数退避策略）。
6. FR6: 提供消息模板管理，支持参数化与多语言模板。
7. FR7: 记录发送日志与状态流转，支持回执与失败追踪。
8. FR8: 支持单播、多播、广播等发送模式。
9. FR9: 提供发送方鉴权与通道权限校验。
10. FR10: 提供基础限流与配额控制，防止资源被过度占用。
11. FR11: 提供后端 server 项目（ass-kicker-server）作为统一通知平台服务入口与核心运行载体。
12. FR12: 提供通道开发模板项目（ass-kicker-channel-builder），支持供应商适配的快速开发与规范化接入。
13. FR13: 提供可分发的 Java JDK 包（ass-kicker-java-client，内置消息发送 client），配置调用者信息即可调用本系统。
14. FR14: 提供前端管理台项目（ass-kicker-admin），用于平台运营与治理。

### Non Functional

1. NFR1: 送达率目标 ≥ 99.5%（按渠道可配置/监控）。
2. NFR2: 故障切换时间 < 30 秒。
3. NFR3: 常规通知 P95 处理延迟 < 5 秒（按渠道 SLA 调整）。
4. NFR4: 新渠道接入周期 < 1 周（在既定 SDK/适配规范下）。
5. NFR5: 系统需支持高吞吐异步处理与可水平扩展。
6. NFR6: 关键路径具备可观测性（日志、指标、追踪），覆盖路由、投递、回执。
7. NFR7: 对外 API 必须保持向后兼容（版本化策略明确）。
8. NFR8: 数据存储与消息处理需满足企业安全与合规要求（鉴权、权限、审计）。

## Technical Assumptions

### Repository Structure: Monorepo

假设单一仓库承载平台代码与核心组件，便于统一治理与依赖管理。

### Service Architecture

模块化单体 + 全链路异步反应式：API 服务采用 Spring Boot 3 + WebFlux（Reactive NIO），与 Kafka 驱动的异步处理链路协同；MVP 以单体为主，后续可拆分为多服务。
部署形态：API 与 Worker 独立进程（同仓多模块）。

### Testing Requirements

Unit + Integration

- 单元测试覆盖核心逻辑（路由、重试、模板、鉴权）
- 集成测试覆盖 Kafka/数据库交互与回执流程

### Additional Technical Assumptions and Requests

- 后端：Java 21
- 框架：Spring Boot 3 + Spring WebFlux（Reactive NIO）
- 反应式要求：全链路异步非阻塞（含 I/O 与消息处理）
- 数据库：PostgreSQL（用于模板、日志、状态与审计）
- 缓存：Caffeine
- 消息中间件：Kafka（异步队列与广播支撑）
- 部署平台：企业内 Kubernetes（单 Region，MVP）
- 推送通道：APNs/FCM/WNS 等系统推送服务
- 鉴权方式：JWT 为主，兼容 API Key（服务间/系统间调用）
- 可观测性：日志/指标/追踪需覆盖路由、投递、回执关键路径
- 前端：Vue 3
- 组件库：Ant Design Vue
- 后端工程结构：包含 ass-kicker-server 与 ass-kicker-channel-builder
- 集成交付：提供可分发的 Java JDK 包（ass-kicker-java-client，内置消息发送 client），配置调用者信息即可接入

## Data & Integration Requirements

### Core Data Entities (MVP)
- Sender/Client：调用方信息与鉴权配置
- BusinessLine：业务线标识与归属
- Message：消息主记录（ID、状态、优先级、渠道、目标地址、时间戳、业务线标识）
- Template/TemplateVersion/Locale：模板与多语言版本
- ChannelProvider：渠道与供应商配置
- RoutingRule：路由条件与供应商选择规则
- DeliveryAttempt：投递尝试记录（次数、供应商、结果、耗时）
- Receipt：回执与最终状态
- AuditLog：配置变更审计（模板、路由、供应商、限流）

### Data Retention & Quality
- 消息日志与投递尝试：默认保留 90 天（可配置）
- 回执与最终状态：默认保留 90 天（可配置）
- 审计记录：默认保留 180 天（可配置）
- 状态流转需满足可追溯性与一致性（避免跳转/丢失）

### Integration Requirements
- 渠道供应商接入需遵循统一适配规范（通过 ass-kicker-channel-builder）
- MVP 至少完成 1 个邮件供应商端到端接入
- 对外 API 需版本化与向后兼容，发送/查询/模板管理为核心接口
- Java 客户端（ass-kicker-java-client）需支持配置化接入与版本化分发
- 模拟/沙箱能力后置（非 MVP）

### API & Client Versioning (Draft)
- API 版本策略：URI 前缀版本（例如 `/api/v1`），保持向后兼容
- 鉴权方式：JWT 为主，兼容 API Key
- 幂等性：发送接口需支持幂等键，避免重复投递
- Java 客户端分发：内部 Maven 发布（ass-kicker-java-client），版本遵循语义化（SemVer）
- 兼容策略：客户端与 API 版本可独立演进，保证同主版本向后兼容
- 配置方式：调用方需配置业务线标识、鉴权信息、默认渠道/优先级（可覆盖）

## Operations & Monitoring Requirements

### Deployment & Environments
- 支持最少 2 套环境：测试环境与生产环境
- 单 Region（MVP），可按需扩展多 Region
- 服务可水平扩展（WebFlux + Kafka Worker）
- MVP 不要求多可用区/多地域

### Observability
- 日志：结构化日志覆盖请求、路由、投递、回执、失败原因（ELK）
- 指标：投递成功率、失败率、P95 延迟、队列积压、故障切换次数（Prometheus/Grafana）
- 追踪：跨服务/组件的链路追踪（含消息 ID 关联）

### Alerting & SLO
- 关键告警：成功率下降、延迟异常、队列积压、供应商异常
- SLO 参考：送达率 ≥ 99.5%，P95 < 5s，故障切换 < 30s

### Audit & Security Operations
- 审计日志：模板/路由/供应商/限流配置变更可追溯
- JWT 密钥与权限管理需符合企业安全规范
- 浏览器端 Token 存储默认使用 HttpOnly Cookie；非浏览器客户端使用 Authorization Header

### Backup & Recovery
- 关键数据（模板/路由/消息状态）需具备备份与恢复机制
- 恢复时间目标：RTO 30 分钟，RPO 5 分钟

## User Interface Design Goals

### Overall UX Vision

面向企业平台团队的管理控制台，强调高密度信息与可操作性：统一视图掌控通知投递、路由与回执状态，操作路径短、反馈清晰、异常可追溯。

### Key Interaction Paradigms

- 表格+筛选为主的管理式工作流（日志、模板、供应商、规则）
- 规则/路由配置采用“条件构建器”式交互
- 模板编辑器支持多语言切换与变量预览
- 关键动作（切换供应商、启停渠道）具备二次确认与审计提示
- 状态/健康度使用实时或准实时刷新

### Core Screens and Views

- 登录/SSO
- 总览 Dashboard（投递成功率、延迟、故障、告警）
- 消息投递日志与回执追踪
- 模板管理（多语言/版本）
- 路由与策略规则管理
- 供应商管理与健康状态
- 队列/优先级与限流配置
- 广播/批量发送管理
- 权限/鉴权与系统设置

### Accessibility: WCAG AA

面向企业内部应用，建议满足 WCAG AA 级别可访问性。

### Branding

暂无品牌规范假设：采用 Ant Design Vue 标准视觉，保持简洁、专业与可读性。

### Target Device and Platforms: Web Responsive

以 Web 控制台为主，桌面浏览器优先，兼容常见屏幕尺寸。

### User Journeys & Key Flows (MVP)
- 登录/鉴权 → 进入 Dashboard 查看整体投递健康度
- 运营创建模板（含多语言）→ 预览 → 发布
- 配置路由规则 → 绑定供应商 → 启用生效
- 查询消息日志 → 查看详情 → 回执/失败原因追溯
- 供应商异常 → 手动切换或暂停 → 记录审计

### Error Handling & Recovery
- 配置保存失败：提示原因并允许重试/撤销
- 路由配置冲突：显式提示冲突规则并阻止发布
- 供应商不可用：引导切换备用或暂停通道
- 日志查询超时：提示并建议缩小查询范围

## Epic List

1. Epic 1: Foundation & Core Delivery Pipeline — 建立统一发送 API、鉴权、基础投递链路与异步处理骨架。
2. Epic 2: Routing & Resilience — 实现智能路由、优先级调度、故障转移与重试能力。
3. Epic 3: Templates & Governance — 提供模板/多语言、状态追踪与回执治理能力。
4. Epic 4: Console & Scale Controls — 提供管理台核心页面、广播/批量、限流与可观测性增强。

## Epic 1 Foundation & Core Delivery Pipeline

**Epic Goal**  
建立统一通知投递的最小可运行闭环：业务系统可以通过统一接口提交通知请求，系统完成鉴权、入队与基础处理，并能返回可追踪的状态。这一史诗确保平台基础能力可被集成验证，为后续路由与治理能力奠定基础。

### Story 1.0 工程初始化与本地可验证闭环

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

### Story 1.1 统一发送 API 与鉴权

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

### Story 1.2 异步投递处理闭环

As a 业务系统研发团队,  
I want 请求在后台被异步处理并进入投递流程,  
so that 我能获得稳定的投递处理能力且不阻塞业务线程。

**Acceptance Criteria**

1. 请求进入异步处理后，状态从 Queued 进入 Processing/Completed 的可见流转。
2. 至少完成一个“最小可运行投递闭环”（可为单一渠道/单一供应商/模拟投递）。
3. 投递失败时记录失败原因并进入失败状态。
4. 支持基础重试策略入口（细节在后续史诗完善）。

**Prerequisite**：Story 1.1

### Story 1.3 消息状态查询与日志检索基础能力

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

### Story 1.4 开发者接入产物与模板

As a 平台工程团队,  
I want 提供 server 项目、通道开发模板与可分发 JDK 包,  
so that 新渠道开发与业务系统接入更高效、标准化。

**Acceptance Criteria**

1. 后端 server 项目可独立构建与运行，作为统一通知平台服务入口。
2. 通道开发模板项目包含标准化接口与示例，支持快速接入新供应商。
3. 提供可分发的 JDK 打包产物，用于业务系统快速对接与运行依赖。
4. 模板与打包产物有基础文档说明其使用方式。

**Prerequisite**：Story 1.1

## Epic 2 Routing & Resilience

**Epic Goal**  
引入智能路由与可靠性机制：在多供应商条件下自动选择最优投递路径，并在失败时进行故障转移与重试，确保送达率与可用性达到目标。

### Story 2.1 路由规则与供应商选择

As a 平台运营/可靠性团队,  
I want 配置路由规则并基于规则选择供应商,  
so that 系统可以按地址/语言/时间等条件自动路由。

**Acceptance Criteria**

1. 支持配置至少三类规则维度（如地址/语言/时间）。
2. 路由规则可按优先级排序并生效。
3. 每个规则可绑定一个或多个供应商候选。
4. 路由决策结果可在消息记录中追溯。

### Story 2.2 故障转移与重试策略

As a 平台运营/可靠性团队,  
I want 在供应商失败时自动故障转移并按策略重试,  
so that 提升送达率并降低单供应商波动影响。

**Acceptance Criteria**

1. 支持配置基础重试策略（次数 + 指数退避）。
2. 当供应商失败达到阈值时，自动切换至备用供应商。
3. 故障转移与重试过程记录在消息状态/日志中。
4. 失败最终状态可区分“已穷尽重试”。

**Prerequisite**：Story 2.1

### Story 2.3 优先级调度

As a 平台运营团队,  
I want 不同优先级的消息被不同队列策略处理,  
so that 紧急消息能更快送达。

**Acceptance Criteria**

1. 至少支持 Emergency/High/Normal/Low/Background 五级优先级。
2. 不同优先级映射到独立队列或调度策略。
3. 处理时延与优先级相关联（高优先级不低于低优先级的处理速度）。
4. 调度策略可配置，且默认配置可用。

**Prerequisite**：Story 1.2

## Epic 3 Templates & Governance

**Epic Goal**  
建立跨渠道模板与状态治理能力，确保通知内容一致性、可追溯性与多语言支持，并提供回执与失败原因的可见性。

### Story 3.1 模板与多语言管理

As a 运营团队,  
I want 管理通知模板并支持多语言版本,  
so that 不同地区/语言的用户收到一致且准确的内容。

**Acceptance Criteria**

1. 支持模板的创建、编辑、版本管理与发布。
2. 模板支持参数化占位符与参数校验。
3. 支持多语言模板（至少两种语言）。
4. 模板变更记录可追踪（含版本与更新时间）。

### Story 3.2 回执与状态流转治理

As a 平台运营/可靠性团队,  
I want 统一管理回执与状态流转,  
so that 我能准确掌握投递结果并快速定位失败原因。

**Acceptance Criteria**

1. 定义并记录统一的状态流转（Queued/Processing/Sent/Delivered/Failed 等）。
2. 回执接收后更新消息状态，并记录供应商回执信息。
3. 支持失败原因分类与可查询。
4. 状态流转与回执处理具备审计记录。

**Prerequisite**：Story 1.2

### Story 3.3 消息日志与审计追踪

As a 平台运营/审计团队,  
I want 查询消息日志与关键投递记录,  
so that 我能满足审计与合规要求。

**Acceptance Criteria**

1. 提供消息日志查询（按时间、渠道、供应商、状态等过滤）。
2. 日志包含关键字段（消息 ID、模板、路由结果、回执状态）。
3. 权限控制确保仅授权用户访问审计信息。
4. 日志保留策略可配置。

**Prerequisite**：Story 1.3

## Epic 4 Console & Scale Controls

**Epic Goal**  
提供可视化管理控制台与规模化治理能力，使运营团队可通过 UI 管理模板、路由、供应商、日志，并对广播/批量场景进行控制与监控。

### Story 4.1 管理控制台基础框架

As a 平台运营团队,  
I want 访问统一的管理控制台并进行登录/权限访问,  
so that 我可以集中管理通知平台功能。

**Acceptance Criteria**

1. 提供基于 Vue 3 + Ant Design Vue 的管理台基础框架与导航结构。
2. 支持登录/SSO 或最小权限验证入口（与鉴权体系对接）。
3. 核心页面路由与权限边界可配置。

### Story 4.2 投递日志与状态可视化

As a 平台运营团队,  
I want 在控制台查看投递日志与状态,  
so that 我能快速定位异常并追溯投递详情。

**Acceptance Criteria**

1. 提供日志列表视图（筛选：时间、渠道、供应商、状态）。
2. 点击日志可查看详细投递记录与回执信息。
3. 列表与详情支持分页与搜索。

**Prerequisite**：Story 3.3

### Story 4.3 广播/批量与限流控制

As a 平台运营团队,  
I want 通过控制台配置广播/批量策略与限流规则,  
so that 大规模发送可控且不会影响系统稳定性。

**Acceptance Criteria**

1. 控制台支持配置广播/批量发送策略（例如分批/窗口）。
2. 支持限流/配额配置的 UI 管理与生效。
3. 配置变更有审计记录与回滚入口。

**Prerequisite**：Story 2.3

### Story 4.4 供应商健康与可观测性

As a 平台可靠性团队,  
I want 在控制台查看供应商健康状态与关键指标,  
so that 我能及时发现并处理供应商异常。

**Acceptance Criteria**

1. 提供供应商健康状态与关键指标的可视化视图。
2. 指标包含成功率、失败率、延迟与切换次数等。
3. 支持阈值告警入口（可与现有告警系统对接）。

**Prerequisite**：Story 2.2


## Checklist Results Report

### Executive Summary
- PRD 完整度：约 82%
- MVP 范围评估：基本合适（3 周交付仍有节奏风险）
- 架构阶段准备度：Nearly Ready
- 关键缺口：用户研究/竞品分析与成功基线；本地可测试性与首史诗初始化说明；关键干系人与沟通机制

### Category Statuses

| Category                         | Status   | Critical Issues |
| -------------------------------- | -------- | --------------- |
| 1. Problem Definition & Context  | PARTIAL  | 用户研究/竞品分析缺失；成功指标基线未明确 |
| 2. MVP Scope Definition          | PASS     | 交付范围明确，但 3 周节奏有风险 |
| 3. User Experience Requirements  | PARTIAL  | 关键流程已补充，边界场景仍不完整 |
| 4. Functional Requirements       | PARTIAL  | 本地可测试性要求未明确 |
| 5. Non-Functional Requirements   | PARTIAL  | 安全合规细节（密钥轮换/审计深度）未明确 |
| 6. Epic & Story Structure        | PARTIAL  | 首史诗缺少初始化/本地验证要求 |
| 7. Technical Guidance            | PARTIAL  | 关键技术取舍与风险未系统化说明 |
| 8. Cross-Functional Requirements | PARTIAL  | 数据质量/迁移/一致性策略未明确 |
| 9. Clarity & Communication       | PARTIAL  | 关键干系人与沟通机制未定义 |

### Top Issues by Priority

**BLOCKERS**
- 当前无硬性阻塞项

**HIGH**
- 补充用户研究/竞品分析与成功指标基线
- 明确本地可测试性要求（例如 CLI/测试桩/模拟供应商策略）
- 明确首史诗的工程初始化与验证清单

**MEDIUM**
- 细化安全合规细节（JWT 密钥轮换/权限模型/审计深度）
- 数据一致性与质量策略（幂等、去重、状态回放）

**LOW**
- 识别关键干系人与沟通机制

### MVP Scope Assessment
- 可能后移：Epic 4 中供应商健康可视化的高级告警与复杂策略配置。
- 已明确：首批场景为登录验证码，邮件渠道先行。
- 复杂度关注：全链路反应式 + 多交付物并行，3 周需严格分阶段。

### Technical Readiness
- 约束清晰：Java 21、Spring Boot 3 + WebFlux、PostgreSQL、Kafka、Caffeine、Vue3/AntD、JWT、ELK、Prometheus/Grafana。
- 技术风险：全链路反应式 I/O、一致性状态流转、故障转移策略、JDK 包分发与兼容。
- 架构深挖建议：数据模型/一致性方案、异步消息语义、回执处理、SDK 版本策略、部署与可观测性。

### Recommendations
1. 补充用户研究/竞品分析与成功指标基线（哪怕是简要假设）。
2. 明确本地可测试性要求与首史诗初始化清单。
3. 细化安全/合规与数据一致性策略。

### Final Decision
- **NEEDS REFINEMENT**：已接近架构阶段，但建议补齐上述 HIGH 项后再进入正式架构设计。

## Next Steps

### UX Expert Prompt
请基于本 PRD，为管理控制台（ass-kicker-admin）输出高层 UX 方案：信息架构、核心页面与关键交互（日志/模板/路由/供应商/回执），并考虑 WCAG AA 与高密度数据展示。

### Architect Prompt
请基于本 PRD 设计整体架构，遵循技术约束：Java 21、Spring Boot 3 + WebFlux、PostgreSQL、Kafka、Caffeine，全链路反应式。交付物包括 ass-kicker-server、ass-kicker-channel-builder、ass-kicker-java-client、ass-kicker-admin；重点输出数据模型、消息流转、路由/重试/回执机制、集成与部署方案、可观测性与安全策略。

