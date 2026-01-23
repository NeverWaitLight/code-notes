# Project Brief: Ass Kicker

## Executive Summary

Ass Kicker 是统一通知中心平台，面向企业内部系统提供多渠道（SMS/Email/IM/Push）统一接入与智能路由能力，其中 Push 对接操作系统系统推送（如 Apple/Android/Windows），解决多渠道集成成本高、可靠性不足与供应商锁定问题，提供高可用、可扩展的通知基础设施。

## Problem Statement

当前企业在通知与触达领域面临以下痛点：

- 多渠道多供应商集成复杂，重复开发与维护成本高
- 供应商质量与可用性波动导致送达率不稳定
- 多语言模板分散管理，内容一致性与版本治理困难
- 缺乏统一模板、优先级、回执、撤回等跨渠道能力
- 大规模广播/批量场景下资源与成本控制难
- 现有方案多为单渠道或单供应商，难以满足高可靠与高可用要求

现有解决方案通常缺少统一治理与智能路由、缺乏跨渠道一致性能力，且故障转移与限流治理不足，导致通知可靠性与成本不可控。解决该问题对保障业务连续性与用户体验有强烈紧迫性。

## Proposed Solution

构建统一通知中心平台：

- 以标准化 API 接入多渠道多供应商，统一模板/优先级/状态管理
- 通过智能路由与规则引擎（地址/语言/时间规则）选择最优供应商
- 提供故障转移、限流、重试与回执机制，提升可靠性
- 支持单播、多播、广播的分层处理策略，兼顾性能与资源效率
- 以 Kafka + Worker 异步体系实现高吞吐与可扩展性

该方案通过平台化治理与可插拔供应商集成，在可靠性、成本控制、扩展性方面显著优于点对点或单供应商方案。

## Target Users

### Primary User Segment: 业务系统研发/平台团队

- **画像**：企业内部产品线的研发与平台工程团队
- **现有行为**：各业务独立对接短信/邮件/IM/推送供应商
- **痛点**：重复集成、缺乏统一治理、可靠性与成本不可控
- **目标**：用统一 API 快速接入与稳定投递，降低维护成本

### Secondary User Segment: 运营/可靠性/成本治理团队

- **画像**：平台运维、稳定性与成本优化团队
- **现有行为**：手工监控或分散系统监控通知投递效果
- **痛点**：缺乏统一可观测与策略调优能力
- **目标**：集中治理、供应商策略调优与成本控制

## Goals & Success Metrics

### Business Objectives

- 降低多渠道通知集成与维护成本（如开发人力减少 30%）
- 提升整体送达率与可用性（如送达率 ≥ 99.5%）
- 降低供应商故障影响面（快速故障转移，MTTR 缩短 50%）

### User Success Metrics

- 新业务接入时间显著缩短（从数周降至数天）
- 通知投递延迟降低（P95 < 5s 或按渠道 SLA）
- 统一模板与状态管理提升运营效率

### Key Performance Indicators (KPIs)

- **Delivery Success Rate**: 送达率 ≥ 99.5%
- **Failover Time**: 故障切换时间 < 30s
- **Message Processing Latency**: P95 < 5s（常规通知）
- **Integration Time**: 新渠道接入周期 < 1 周

## MVP Scope

### Core Features (Must Have)

- **统一发送 API**：支持 SMS/Email/IM/Push
- **多供应商接入**：SMS/Email/IM 支持多供应商并行，Push 对接系统推送（Apple/Android/Windows 等）  
- **智能路由与规则匹配**：地址/语言/时间规则
- **优先级队列与调度**：Emergency/High/Normal/Low/Background
- **基础故障转移与重试**：失败自动切换与指数退避
- **模板与多语言支持**：模板参数化、语言 code
- **消息日志与状态跟踪**：发送记录、回执、失败追踪

### Out of Scope for MVP

- 高级可视化控制台与 BI 报表
- 多租户商业化计费体系
- 全面合规审计与复杂审批流
- 复杂的推荐/AI 智能路由优化

### MVP Success Criteria

在一个或多个业务系统中成功替换现有通知实现，并在稳定性、送达率、接入效率上达到既定指标。

## Post-MVP Vision

### Phase 2 Features

- 高级运营控制台与可视化监控
- 供应商策略自动优化与成本优化建议
- 更细粒度的 SLA 管理与告警联动

### Long-term Vision

形成企业级通知平台标准，支持全球化、多区域合规、多租户及生态扩展，成为企业消息与触达中枢。

### Expansion Opportunities

- 统一用户触达编排（多渠道协同触达）
- 用户生命周期通知策略引擎
- 对外开放为平台服务（通知 PaaS）

## Technical Considerations

### Platform Requirements

- **Target Platforms:** 企业内部服务平台
- **Browser/OS Support:** 主要面向服务端系统
- **Performance Requirements:** 高吞吐异步处理，支持大规模广播

### Technology Preferences

- **Frontend:** 待定（MVP 可无 UI）
- **Backend:** Java（推断，基于项目路径）
- **Database:** MySQL（README 指示异步写入任务/结果表）
- **Hosting/Infrastructure:** Kafka 集群、异步 Worker 体系

### Architecture Considerations

- **Repository Structure:** 需与现有 Java 项目规范对齐
- **Service Architecture:** Controller + Worker + Scheduler + Kafka Topic 分层
- **Integration Requirements:** 多供应商 API 适配（Push 为系统推送，如 APNs/FCM/WNS）、回执与限流  
- **Security/Compliance:** 发送方鉴权、模板/通道权限校验

## Constraints & Assumptions

### Constraints

- **Budget:** 未明确（假设受成本控制约束）
- **Timeline:** 未明确（假设需分阶段交付）
- **Resources:** 依赖 Kafka/MySQL 等基础设施稳定性
- **Technical:** 需要支持高并发与大规模广播场景

### Key Assumptions

- 企业已有 Kafka/MySQL 基础设施
- 业务允许通过统一平台改造通知入口
- 供应商 API 可稳定对接并提供必要回执

## Risks & Open Questions

### Key Risks

- **供应商稳定性波动:** 可能影响送达率与 SLA
- **广播规模压力:** 大规模广播可能造成资源峰值
- **跨渠道一致性:** 模板/回执规则在不同渠道差异大

### Open Questions

- 目标业务规模与峰值吞吐具体指标是多少？
- 是否需要多租户隔离与计费体系？
- 是否有合规（如数据出境、短信实名、反垃圾）要求？

### Areas Needing Further Research

- 供应商接口能力与限制清单
- 不同渠道的失败重试与回执最佳策略
- 统一模板的多语言与渠道适配规则

## Appendices

### C. References

- `README.md`

## Next Steps

### Immediate Actions

1. 明确首批接入的业务系统与通知场景
2. 明确目标 SLA 与峰值吞吐指标
3. 梳理首批供应商清单与接入优先级
4. 确定 MVP 范围与交付计划

### PM Handoff

This Project Brief provides the full context for Ass Kicker. Please start in 'PRD Generation Mode', review the brief thoroughly to work with the user to create the PRD section by section as the template indicates, asking for any necessary clarification or suggesting improvements.
