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
- 推送通道：APNs/FCM/WNS 等系统推送服务
- 鉴权方式：服务间鉴权（API Key 或 OAuth2/JWT，待定）
- 可观测性：日志/指标/追踪需覆盖路由、投递、回执关键路径
- 前端：Vue 3
- 组件库：Ant Design Vue

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

## Epic List

1. Epic 1: Foundation & Core Delivery Pipeline — 建立统一发送 API、鉴权、基础投递链路与异步处理骨架。
2. Epic 2: Routing & Resilience — 实现智能路由、优先级调度、故障转移与重试能力。
3. Epic 3: Templates & Governance — 提供模板/多语言、状态追踪与回执治理能力。
4. Epic 4: Console & Scale Controls — 提供管理台核心页面、广播/批量、限流与可观测性增强。

## Epic 1 Foundation & Core Delivery Pipeline

**Epic Goal**  
建立统一通知投递的最小可运行闭环：业务系统可以通过统一接口提交通知请求，系统完成鉴权、入队与基础处理，并能返回可追踪的状态。这一史诗确保平台基础能力可被集成验证，为后续路由与治理能力奠定基础。

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

### Story 1.3 消息状态查询与追踪

As a 业务系统研发团队,  
I want 查询消息的投递状态与关键元数据,  
so that 我能验证投递结果并进行问题排查。

**Acceptance Criteria**

1. 提供消息状态查询接口（按消息 ID 查询）。
2. 返回包含当前状态、时间戳、渠道/供应商（如有）等关键字段。
3. 对不存在或无权限的消息 ID 返回明确错误。
4. 状态查询的响应时间满足常规 SLA。

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
