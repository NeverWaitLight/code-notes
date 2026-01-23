# Operations & Monitoring Requirements

## Deployment & Environments
- 支持最少 2 套环境：测试环境与生产环境
- 服务可水平扩展（WebFlux + Kafka Worker）
- MVP 不要求多可用区/多地域

## Observability
- 日志：结构化日志覆盖请求、路由、投递、回执、失败原因（ELK）
- 指标：投递成功率、失败率、P95 延迟、队列积压、故障切换次数（Prometheus/Grafana）
- 追踪：跨服务/组件的链路追踪（含消息 ID 关联）

## Alerting & SLO
- 关键告警：成功率下降、延迟异常、队列积压、供应商异常
- SLO 参考：送达率 ≥ 99.5%，P95 < 5s，故障切换 < 30s

## Audit & Security Operations
- 审计日志：模板/路由/供应商/限流配置变更可追溯
- JWT 密钥与权限管理需符合企业安全规范

## Backup & Recovery
- 关键数据（模板/路由/消息状态）需具备备份与恢复机制
- 恢复时间目标：RTO 30 分钟，RPO 5 分钟
