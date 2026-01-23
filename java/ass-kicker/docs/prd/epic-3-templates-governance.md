# Epic 3 Templates & Governance

**Epic Goal**  
建立跨渠道模板与状态治理能力，确保通知内容一致性、可追溯性与多语言支持，并提供回执与失败原因的可见性。

## Story 3.1 模板与多语言管理

As a 运营团队,  
I want 管理通知模板并支持多语言版本,  
so that 不同地区/语言的用户收到一致且准确的内容。

**Acceptance Criteria**

1. 支持模板的创建、编辑、版本管理与发布。
2. 模板支持参数化占位符与参数校验。
3. 支持多语言模板（至少两种语言）。
4. 模板变更记录可追踪（含版本与更新时间）。

## Story 3.2 回执与状态流转治理

As a 平台运营/可靠性团队,  
I want 统一管理回执与状态流转,  
so that 我能准确掌握投递结果并快速定位失败原因。

**Acceptance Criteria**

1. 定义并记录统一的状态流转（Queued/Processing/Sent/Delivered/Failed 等）。
2. 回执接收后更新消息状态，并记录供应商回执信息。
3. 支持失败原因分类与可查询。
4. 状态流转与回执处理具备审计记录。

**Prerequisite**：Story 1.2

## Story 3.3 消息日志与审计追踪

As a 平台运营/审计团队,  
I want 查询消息日志与关键投递记录,  
so that 我能满足审计与合规要求。

**Acceptance Criteria**

1. 提供消息日志查询（按时间、渠道、供应商、状态等过滤）。
2. 日志包含关键字段（消息 ID、模板、路由结果、回执状态）。
3. 权限控制确保仅授权用户访问审计信息。
4. 日志保留策略可配置。

**Prerequisite**：Story 1.3
