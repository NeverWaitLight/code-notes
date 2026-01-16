# 在线视频网站 Demo Product Requirements Document (PRD)

## Goals and Background Context

### Goals

- 提供本地可运行的在线视频网站演示，覆盖上传、分片、在线播放核心流程
- 验证 Java 后端（OpenJDK 21 + Spring Boot + JPA + SQLite + JavaCV）的视频处理与存储能力
- 验证 Vue 3 + hls.js 前端播放与基础交互
- 全流程不依赖第三方服务，基于本地文件系统与 SQLite 数据库

### Background Context

该项目是一个可独立运行的在线视频网站演示，重点验证从视频上传到分片处理、再到在线播放的端到端链路。技术栈明确限定为 Java 后端与 Vue 3 前端，以便聚焦于可执行的最小可行功能，而不是完整产品化能力。

在架构上强调本地化与离线可运行，视频文件和数据库均落地到本地文件系统与 SQLite，避免引入对象存储、转码服务或云媒体平台等外部依赖，确保演示环境可复制、易搭建。

### Problem Statement

需要一个本地可运行、可复制的在线视频网站 Demo，用于验证“上传 → 分片 → 播放”的端到端流程与技术栈可行性。现有现成方案往往依赖云服务或复杂部署，无法满足离线环境下快速演示与验证的需求。

### Target Users

- 需要演示视频处理与播放流程的开发者/评审者
- 需要快速验证技术方案的产品或项目成员
- 学习视频上传与 HLS 播放流程的技术学习者

### Success Metrics

- 在无外部服务条件下，本机可完成至少 1 个视频的上传、分片、播放与删除全流程
- 支持 H.264 MP4 且 ≤ 5GB 的上传在本机环境稳定成功（以至少 2 个测试样例验证）
- `startup.sh` 可一键启动前后端，首页可正常访问并显示视频列表

### Out of Scope

- 用户注册、登录、权限控制与鉴权
- 评论、点赞、收藏、推荐与搜索
- 多端同步、云端存储或第三方对象存储
- 视频编辑、转码多规格输出或字幕生成
- CDN 或直播能力

### MVP Validation Approach

- 使用至少 2 个 H.264 MP4 测试文件完成上传与播放验证
- 在上传页观察进度与状态变化，完成后列表可见
- 播放页使用 hls.js 成功播放并可执行删除操作
- 记录从上传到“上传完成”的耗时并验证状态更新

### Data and Operations

- 数据存储：视频原始文件与 HLS 产物存储在本地文件系统，元数据存储在 SQLite
- 数据保留：默认永久保留，除非用户在播放页执行删除
- 清理策略：删除操作需同时清理原始文件、HLS 目录与数据库记录
- 备份/恢复：本 Demo 不提供自动备份与恢复机制
- 运维范围：不提供监控、告警与日志采集平台，仅输出应用日志到本地

### Change Log

| Date       | Version | Description                             | Author |
| ---------- | ------- | --------------------------------------- | ------ |
| 2026-01-14 | v0.1    | 根据用户输入创建初始 PRD 框架与背景目标 | John   |
| 2026-01-14 | v0.3    | 根据反馈细化 Requirements 细节          | John   |
| 2026-01-14 | v0.4    | 更新技术假设（目录结构与启动脚本）      | John   |
| 2026-01-14 | v0.5    | 补充问题陈述、目标用户与成功指标        | John   |
| 2026-01-14 | v0.6    | 补充范围边界与 MVP 验证方式             | John   |
| 2026-01-14 | v0.7    | 补充数据与运维要求                      | John   |
| 2026-01-14 | v0.8    | 补充性能期望与错误提示规范              | John   |

## Requirements

### Functional

1. FR1: 首页无需登录，打开即展示视频列表。
2. FR2: 点击列表中任意视频进入播放页面。
3. FR3: 上传页面支持选择视频并展示上传进度，仅展示上传中的视频，上传完成后不再在上传页显示。
4. FR4: 上传视频限制为 H.264 编码的 MP4，大小不超过 5GB；除类型与大小外不做其他属性校验。
5. FR5: 上传后后端将原始视频保存到本地文件系统并写入 SQLite 元数据，状态为“上传中”。
6. FR6: 后端使用 JavaCV 将视频切分为 HLS（.m3u8 + .ts），分片时长固定为 2 秒；切分完成后状态更新为“上传完成”。
7. FR7: 上传或切分失败时状态标记为失败并记录错误提示信息，不提供自动重试。
8. FR8: 播放页面使用 hls.js 播放；当视频未就绪时仅展示默认占位效果。
9. FR9: 播放页面提供删除按钮，点击后需二次确认，确认后删除视频文件与数据库记录（无需鉴权）。

### Non Functional

1. NFR1: 后端必须基于 OpenJDK 21、Spring Boot、JPA、SQLite、JavaCV。
2. NFR2: 前端必须基于 Vue 3 与 hls.js。
3. NFR3: 视频文件与数据库文件必须存储在本地文件系统，不依赖第三方服务。
4. NFR4: 系统需可在离线环境下本机运行与播放。
5. NFR5: 全站不需要登录或鉴权。
6. NFR6: 上传到处理完成的最长耗时可接受为 1 小时以内（本机环境）。
7. NFR7: 失败提示需展示 `code` 与 `message` 字段。

## User Interface Design Goals

### Overall UX Vision

以“最小流程、最快验证”为核心：首页即视频列表；上传页只承担上传与进度展示；播放页提供播放与删除（含二次确认）操作。界面简单直观，默认状态可用，无需登录或鉴权。

### Key Interaction Paradigms

- 列表直达播放：点击列表项进入播放页
- 任务型上传：上传页仅展示当前上传任务与进度
- 状态驱动展示：未就绪时展示默认占位效果
- 危险操作确认：删除前二次确认

### Core Screens and Views

- 首页视频列表
- 上传视频页面
- 视频播放页面

### Accessibility: None

### Branding

- 无指定品牌要求，保持简洁中性风格

### Target Device and Platforms: Web Responsive

## Technical Assumptions

### Repository Structure: Monorepo（前后端分目录）

### Service Architecture

- Monolith（单体）Spring Boot 服务提供 API 与 HLS 静态资源

### Testing Requirements

- Unit + Integration（基础单元测试 + API 集成测试，另保留手工冒烟）

### Additional Technical Assumptions and Requests

- 后端构建工具使用 Maven
- 前后端代码位于根目录两个独立目录（如 `backend/` 与 `frontend/`）
- 根目录提供 `startup.sh` 脚本用于一键启动前后端（开发模式）
- 启动脚本仅用于演示，不考虑打包发布流程
- 前端使用 Vite 构建 Vue 3 项目，并使用 Vue Router 实现列表/上传/播放三页路由
- 视频文件与 HLS 产物存储在本地可配置目录（如 `data/videos/{id}/`）
- SQLite 数据库文件存储在本地可配置路径（如 `data/app.db`）
- 上传采用 multipart/form-data；视频处理使用 JavaCV 调用 FFmpeg 能力
- HLS 切片与清单由后端生成并通过 Spring Boot 静态资源或专用接口提供
- 异步处理采用应用内线程池/@Async，不引入外部队列
- 部署目标为本机运行（非容器化）；如需 Docker 化请说明

## Epic List

1. Epic 1: 基础架构与上传处理闭环：完成工程初始化、数据库与本地存储配置、上传与分片处理、基础列表查询接口，形成可运行的最小后端链路。
2. Epic 2: 前端体验与播放删除：实现三页前端（列表/上传/播放）、播放与删除交互、状态展示与默认占位效果，形成完整演示体验。

## Epic 1 基础架构与上传处理闭环

目标：建立后端可运行的最小闭环，完成项目骨架、数据库与本地存储配置、上传与分片处理，并提供查询与播放相关接口。该 epic 结束后，后端可独立完成“上传 → 分片 → 可播放”的链路验证。

### Story 1.1 项目骨架与基础配置

As a 开发者,
I want 初始化后端工程与本地存储/数据库配置，
so that 项目可以本机启动并具备最小运行基础。

#### Acceptance Criteria

1. 后端为 Spring Boot + Maven 项目，可在本机启动运行。
2. SQLite 连接配置完成，应用可在指定路径创建数据库文件。
3. 视频与 HLS 输出目录可配置，启动时若目录不存在会自动创建。
4. 根目录包含 `backend/` 与 `frontend/` 两个独立目录结构。
5. 根目录提供 `startup.sh` 脚本，可启动后端；若存在前端目录与依赖，则可同时启动前端（开发模式）。

### Story 1.2 上传接口与元数据落库

As a 使用者,
I want 上传视频并生成元数据记录，
so that 系统可追踪上传状态与基础信息。

#### Acceptance Criteria

1. 提供上传接口（multipart/form-data），支持上传 MP4（H.264）且大小 ≤ 5GB。
2. 上传成功后将原始视频保存到本地文件系统，生成唯一 ID。
3. 将视频元数据写入 SQLite（标题、原始文件名、大小、状态、创建时间、存储路径）。
4. 初始状态为“上传中”，失败时记录错误提示。
5. 提供视频列表查询接口，返回必要字段与状态。

### Story 1.3 HLS 分片处理与播放信息

As a 使用者,
I want 视频在上传后被分片并可播放，
so that 我可以通过 HLS 流畅播放。

#### Acceptance Criteria

1. 上传完成后触发 JavaCV 异步分片处理，分片时长固定为 2 秒。
2. 成功后生成 `.m3u8` 与 `.ts` 文件并保存到本地目录。
3. 分片成功后状态更新为“上传完成”，失败则为“失败”并记录错误提示。
4. 提供视频详情接口，包含播放所需的 HLS 清单地址与状态。
5. 后端可通过静态资源或接口提供 HLS 文件访问。

### Story 1.4 删除接口

As a 使用者,
I want 删除已上传的视频，
so that 我可以清理不需要的内容。

#### Acceptance Criteria

1. 提供删除接口，按视频 ID 删除数据库记录与本地文件（原始视频与 HLS 输出）。
2. 若文件或记录不存在，返回明确的错误提示。

## Epic 2 前端体验与播放删除

目标：实现三页前端与基本交互，包括列表、上传进度、播放与删除操作，形成可演示的完整体验。该 epic 结束后，前后端流程可端到端展示。

### Story 2.1 前端基础框架与列表页

As a 访客,
I want 打开首页即可查看视频列表，
so that 可以快速进入播放体验。

#### Acceptance Criteria

1. 使用 Vue 3 + Vite 创建前端项目，位于 `frontend/` 目录。
2. 配置路由：列表页（/）、上传页（/upload）、播放页（/play/:id）。
3. 列表页调用后端列表接口并展示标题与状态。
4. 点击列表项可进入对应播放页。

### Story 2.2 上传页与进度展示

As a 访客,
I want 在上传页看到上传进度，
so that 可以确认上传过程是否正常。

#### Acceptance Criteria

1. 上传页支持选择视频与填写标题并提交上传。
2. 上传过程中显示进度，仅展示“上传中”的视频条目。
3. 上传成功后该条目不再在上传页显示。
4. 上传失败显示错误提示。

### Story 2.3 播放页与删除交互

As a 访客,
I want 在播放页观看视频并可删除，
so that 可以完成演示体验。

#### Acceptance Criteria

1. 播放页使用 hls.js 播放，状态为“上传完成”时自动加载清单。
2. 未就绪时仅展示默认占位效果，不进行轮询或自动刷新。
3. 播放页提供删除按钮，点击后二次确认。
4. 删除成功后返回列表页并移除该视频。

## Checklist Results Report

### Executive Summary

- PRD 完整度估计：约 60%
- MVP 规模：偏小且清晰（Just Right）
- 架构阶段就绪度：Nearly Ready（存在阻塞项需补充）
- 关键缺口：问题定义与用户/指标缺失、范围边界缺失、数据与运维要求缺失

### Category Analysis Table

| Category                         | Status  | Critical Issues                       |
| -------------------------------- | ------- | ------------------------------------- |
| 1. Problem Definition & Context  | FAIL    | 缺少明确问题陈述、目标用户、成功指标  |
| 2. MVP Scope Definition          | PARTIAL | 缺少明确 Out of Scope 与 MVP 验证方式 |
| 3. User Experience Requirements  | PARTIAL | 缺少边界/错误场景与性能体验要求       |
| 4. Functional Requirements       | PASS    | 依赖与验证方式可再明确                |
| 5. Non-Functional Requirements   | PARTIAL | 性能/可靠性/恢复策略未定义            |
| 6. Epic & Story Structure        | PASS    | 基本完整且顺序合理                    |
| 7. Technical Guidance            | PARTIAL | 关键技术风险与权衡未明确              |
| 8. Cross-Functional Requirements | FAIL    | 数据模型/保留策略/运维要求缺失        |
| 9. Clarity & Communication       | PARTIAL | 版本与结构清晰，但缺少利益相关方信息  |

### Top Issues by Priority

- BLOCKERS: 缺少问题陈述、目标用户与成功指标；缺少明确范围边界（Out of Scope）；缺少数据与运维要求
- HIGH: 未定义性能与可靠性预期（例如上传/转码耗时、失败恢复）
- MEDIUM: 依赖与验证方式未充分说明（如 5GB 上传的体验与限制）
- LOW: UI 视觉与文案规范未定义（可后置）

### MVP Scope Assessment

- 可考虑裁剪：若需进一步极简，可去除“删除功能”（但你已明确需要，可保留）
- 缺失关键内容：未定义 MVP 成功指标与验证方式
- 复杂度风险：视频转码与大文件上传对本机资源要求较高
- 时间预期：未定义

### Technical Readiness

- 约束清晰：技术栈、目录结构、启动方式已明确
- 技术风险：JavaCV/FFmpeg 依赖可用性、HLS 文件服务性能、5GB 上传的稳定性
- 需要架构深入点：文件存储与清理策略、异步处理与资源占用、静态资源服务方式

### Recommendations

1. 补充问题陈述、目标用户、成功指标（最少 3 条指标）
2. 增加 Out of Scope 与 MVP 验证方式（验收手段）
3. 补充数据与运维要求（数据保留、清理、备份或明确“不做”）
4. 简要定义性能与可靠性边界（如可接受处理时长范围）

### Final Decision

**NEEDS REFINEMENT**：请先补齐关键缺口，再进入架构阶段。
