# Data & Integration Requirements

## Core Data Entities (MVP)
- Sender/Client：调用方信息与鉴权配置
- BusinessLine：业务线标识与归属
- Message：消息主记录（ID、状态、优先级、渠道、目标地址、时间戳、业务线标识）
- Template/TemplateVersion/Locale：模板与多语言版本
- ChannelProvider：渠道与供应商配置
- RoutingRule：路由条件与供应商选择规则
- DeliveryAttempt：投递尝试记录（次数、供应商、结果、耗时）
- Receipt：回执与最终状态
- AuditLog：配置变更审计（模板、路由、供应商、限流）

## Data Retention & Quality
- 消息日志与投递尝试：默认保留 90 天（可配置）
- 回执与最终状态：默认保留 90 天（可配置）
- 审计记录：默认保留 180 天（可配置）
- 状态流转需满足可追溯性与一致性（避免跳转/丢失）

## Integration Requirements
- 渠道供应商接入需遵循统一适配规范（通过 ass-kicker-channel-builder）
- MVP 至少完成 1 个邮件供应商端到端接入
- 对外 API 需版本化与向后兼容，发送/查询/模板管理为核心接口
- Java 客户端（ass-kicker-java-client）需支持配置化接入与版本化分发
- 模拟/沙箱能力后置（非 MVP）

## API & Client Versioning (Draft)
- API 版本策略：URI 前缀版本（例如 `/api/v1`），保持向后兼容
- 鉴权方式：JWT
- 幂等性：发送接口需支持幂等键，避免重复投递
- Java 客户端分发：内部 Maven 发布（ass-kicker-java-client），版本遵循语义化（SemVer）
- 兼容策略：客户端与 API 版本可独立演进，保证同主版本向后兼容
- 配置方式：调用方需配置业务线标识、鉴权信息、默认渠道/优先级（可覆盖）
