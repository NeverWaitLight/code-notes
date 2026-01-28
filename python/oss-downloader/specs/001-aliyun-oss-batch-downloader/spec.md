# Feature Specification: 交互式 OSS 批量下载工具

**Feature Branch**: `001-aliyun-oss-batch-downloader`  
**Created**: 2026-01-28  
**Status**: Draft  
**Input**: User description: "基于python的，终端交互式的，从阿里云oss上下载指定桶中指定前缀大批量文件的工具"

## User Scenarios & Testing _(mandatory)_

### User Story 1 - 交互式批量下载 (Priority: P1)

作为使用者，我希望通过终端交互式输入 OSS 连接信息、bucket、prefix、下载目录，并一键批量下载所有匹配对象。

**Why this priority**: 这是工具的核心价值，必须先可用。

**Independent Test**: 提供可用凭证与包含对象的 prefix，执行一次下载并验证本地文件结构与 OSS key 一致。

**Acceptance Scenarios**:

1. **Given** 有效的 AccessKey/Endpoint 与存在对象的 bucket/prefix，**When** 启动工具并确认下载，**Then** 所有对象被下载到本地目标目录且相对路径与 key 一致。
2. **Given** 有效配置但 prefix 下无对象，**When** 执行下载，**Then** 工具提示“无对象”并正常退出（返回码为 0）。

---

### User Story 2 - 断点续传与失败重试 (Priority: P2)

作为使用者，我希望下载过程中断后能继续，并对临时网络错误进行自动重试。

**Why this priority**: 大批量下载时中断很常见，恢复能力能显著降低时间成本。

**Independent Test**: 在下载过程中中断进程，再次运行工具应跳过已完成项并继续下载。

**Acceptance Scenarios**:

1. **Given** 已下载部分对象并生成清单文件，**When** 重新运行同一下载任务，**Then** 已完成对象被跳过，未完成对象继续下载。
2. **Given** 网络瞬断导致某对象下载失败，**When** 触发重试机制，**Then** 在限定重试次数内成功则继续流程，超过次数则记录失败并在汇总中体现。

---

### User Story 3 - 下载前预览与过滤 (Priority: P3)

作为使用者，我希望在下载前预览对象数量/大小，并可按后缀进行简单过滤。

**Why this priority**: 降低误下载风险，减少不必要的流量与时间消耗。

**Independent Test**: 执行预览并应用后缀过滤，验证预览统计与实际下载范围一致。

**Acceptance Scenarios**:

1. **Given** prefix 下存在多种后缀对象，**When** 选择后缀过滤并确认下载，**Then** 仅匹配后缀的对象被下载。
2. **Given** 选择仅预览不下载，**When** 查看统计信息后退出，**Then** 本地未产生下载文件且退出码为 0。

---

### Edge Cases

- prefix 为空或为根路径时如何避免误下载整个 bucket？
- AccessKey 无权限或 bucket 不存在时如何报错？
- 本地目录不可写或磁盘空间不足时如何处理？
- 目标对象包含中文或特殊字符 key 时路径映射是否正确？
- 单个大文件下载中断后的恢复策略是什么？

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: 系统 MUST 提供终端交互式流程（prompt），引导输入 Endpoint、AccessKeyId、AccessKeySecret、bucket、prefix、下载目录。
- **FR-002**: 系统 MUST 使用阿里云 OSS SDK 列举 bucket 内指定 prefix 的对象清单。
- **FR-003**: 系统 MUST 将对象下载到本地并保留与 OSS key 一致的相对目录结构。
- **FR-004**: 系统 MUST 支持并发下载，并允许配置并发数。
- **FR-005**: 系统 MUST 在下载过程中展示进度与当前速率/已完成数。
- **FR-006**: 系统 MUST 在首轮中对所有对象仅尝试下载 1 次（不因个别失败而提前终止）。
- **FR-007**: 系统 MUST 在首轮结束后，对失败对象执行独立重试流程（第二轮），并记录每个对象的重试次数与最后一次失败原因。
- **FR-008**: 系统 MUST 对可重试错误进行指数退避重试，总尝试次数（含首轮）最多为 3 次，超过次数后记录失败。
- **FR-009**: 系统 MUST 生成下载清单/状态文件，用于断点续传与跳过已完成对象。
- **FR-010**: 系统 MUST 支持按文件后缀进行 include 过滤（可选）。
- **FR-011**: 系统 MUST 在发生不可恢复错误时输出清晰错误信息到 stderr 并以非 0 退出码结束。
- **FR-012**: 系统 MUST 输出失败对象列表 CSV 到目标目录，包含 `key`,`last_error`,`retry_count`（retry_count 为总尝试次数）。
- **FR-013**: 系统 MUST 提供可分发的独立可执行文件（无需依赖任何编程环境/运行时即可运行）。

### Key Entities _(include if feature involves data)_

- **DownloadConfig**: 连接信息、bucket、prefix、目标目录、并发数、过滤条件。
- **ObjectItem**: OSS 对象元数据（key、size、etag、last_modified）。
- **DownloadManifest**: 本地清单，记录对象状态（pending/in_progress/success/failed）。
- **DownloadSummary**: 统计信息（总数、成功数、失败数、耗时）。

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: 在包含 10,000 个小文件的 prefix 下，工具能完成下载且失败率 ≤ 1%（可重试后）。
- **SC-002**: 预览阶段能在 60 秒内输出对象数量与总大小统计（10,000 对象规模）。
- **SC-003**: 中断后再次运行，已完成对象跳过率 ≥ 99%。
- **SC-004**: 发生权限错误时，错误信息可定位到具体 bucket/prefix 并返回非 0 退出码。
