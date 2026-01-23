# Technical Assumptions

## Repository Structure: Monorepo

假设单一仓库承载平台代码与核心组件，便于统一治理与依赖管理。

## Service Architecture

模块化单体 + 全链路异步反应式：API 服务采用 Spring Boot 3 + WebFlux（Reactive NIO），与 Kafka 驱动的异步处理链路协同；MVP 以单体为主，后续可拆分为多服务。

## Testing Requirements

Unit + Integration

- 单元测试覆盖核心逻辑（路由、重试、模板、鉴权）
- 集成测试覆盖 Kafka/数据库交互与回执流程

## Additional Technical Assumptions and Requests

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
- 后端工程结构：包含 ass-kicker-server 与 ass-kicker-channel-builder
- 集成交付：提供可分发的 Java JDK 包（ass-kicker-java-client，内置消息发送 client），配置调用者信息即可接入
