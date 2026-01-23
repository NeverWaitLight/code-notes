# MVP Scope

## In Scope (Must Have)
- 统一发送 API 与鉴权，支持通知请求标准化接入
- 异步投递处理闭环（入队、处理、状态流转）
- 多渠道接入框架（SMS/Email/IM/Push），至少 1 个邮件渠道具备端到端可用能力，其余可先具备基础适配能力
- 智能路由与基础故障转移/重试策略
- 模板与多语言支持（基础版本）
- 消息日志、状态追踪与回执记录
- 广播/批量发送能力（基础策略）
- 管理控制台核心页面（日志、模板、路由、供应商、状态）
- 交付物：ass-kicker-server、ass-kicker-channel-builder、ass-kicker-java-client、ass-kicker-admin

## Out of Scope (Not in MVP)
- 高级可视化控制台与 BI 报表
- 多租户商业化计费体系
- 全面合规审计与复杂审批流
- 复杂的推荐/AI 智能路由优化

## MVP Success Criteria
- 在首批业务系统中替换现有通知实现并可稳定运行
- 送达率 ≥ 99.5%，故障切换时间 < 30s，常规通知 P95 < 5s
- 新业务接入周期 < 1 周（在既定 SDK/适配规范下）

## MVP Validation Approach
- 选定首批接入场景：登录验证码
- 以真实流量验证 KPI（送达率、延迟、故障切换、接入周期）
- 验证 Java JDK 包（ass-kicker-java-client）接入效率与兼容性

## MVP Timeline & Rollout
- 目标时间框架：3 周
- 交付节奏：先后端闭环，再接入渠道/路由治理，再完善控制台
