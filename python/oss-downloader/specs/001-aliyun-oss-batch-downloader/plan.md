# Implementation Plan: 交互式 OSS 批量下载工具

**Branch**: `001-aliyun-oss-batch-downloader` | **Date**: 2026-01-28 | **Spec**: `specs/001-aliyun-oss-batch-downloader/spec.md`
**Input**: Feature specification from `/specs/001-aliyun-oss-batch-downloader/spec.md`

## Summary

实现一个基于 Python 的终端交互式工具，通过阿里云 OSS SDK 列举指定 bucket/prefix 的对象，并以可配置并发下载到本地，支持断点续传、下载前预览与简单后缀过滤；下载流程为“两轮”：首轮每个对象仅尝试 1 次，第二轮仅针对失败对象重试（总尝试次数≤3），最终导出失败列表 CSV（记录最后失败原因与总尝试次数）。核心技术点：对象清单分页拉取、并发下载、两轮失败重试、manifest 记录与恢复、进度展示。

## Technical Context

**Language/Version**: Python 3.11  
**Primary Dependencies**: oss2（阿里云 OSS SDK）, rich（进度展示）, questionary（交互式输入）, PyInstaller（打包可执行文件）  
**Storage**: 本地文件系统 + SQLite manifest（stdlib sqlite3）  
**Testing**: pytest（如需测试）  
**Target Platform**: Windows / macOS / Linux CLI  
**Project Type**: single  
**Performance Goals**: 10,000 对象级别清单与下载在合理时间内完成  
**Constraints**: 进度显示不中断下载；默认并发可配置  
**Scale/Scope**: 单次任务 10k+ 对象，单机执行

## Constitution Check

当前 `constitution.md` 为占位模板，暂无可执行的强制约束；本计划不涉及额外复杂度豁免项。

## Project Structure

### Documentation (this feature)

```text
specs/001-aliyun-oss-batch-downloader/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── cli.md
└── tasks.md
```

### Source Code (repository root)

```text
src/
├── oss_downloader/
│   ├── __init__.py
│   ├── __main__.py
│   ├── cli.py           # 交互式流程与参数校验
│   ├── config.py        # DownloadConfig
│   ├── oss_client.py    # OSS 列举与下载封装
│   ├── manifest.py      # SQLite 清单与断点续传
│   ├── downloader.py    # 并发下载与重试
│   ├── filters.py       # 后缀过滤
│   └── progress.py      # 进度展示

scripts/
├── build.sh

tests/
├── integration/
└── unit/
```

**Structure Decision**: 采用单项目结构，代码集中在 `src/oss_downloader/`，CLI 入口放在 `__main__.py`，便于 `python -m oss_downloader` 启动。

## Implementation Phases

### Phase 0 - Research

- 确认 oss2 SDK 的对象列举分页、下载接口与错误类型
- 确认 rich 进度条适配多线程更新方式
- 确认 questionary 在 Windows 终端兼容性

### Phase 1 - Design

- 确定 DownloadManifest 的 SQLite 表结构与状态流转
- 明确下载目录映射规则（OSS key -> 本地相对路径）
- 设计可重试错误判定与指数退避策略
- 定义 CLI 交互流程与可选参数

### Phase 2 - Implementation (MVP: P1)

- 交互式输入与配置校验
- 列举对象并生成待下载清单
- 并发下载与进度展示
- 下载完成汇总输出

### Phase 3 - Enhancement (P2/P3)

- 断点续传（基于 manifest）
- 可重试错误与失败汇总
- 预览与后缀过滤

### Phase 4 - Packaging

- 使用 PyInstaller 生成独立可执行文件
- 生成 Windows 与 Linux 两个平台版本
- 验证无需 Python 环境可运行

## Open Questions / Needs Clarification

- 非交互模式：不需要（仅交互式）
- 失败对象列表：CSV，输出到 `<target_dir>/.oss-failed.csv`（key,last_error,retry_count=总尝试次数）
- 目标平台：Windows + Linux（需要分别在对应系统上打包）
- 是否需要支持自定义 endpoint 或内网 endpoint 切换？
- 是否需要支持服务端加密（SSE）或 KMS 相关参数？
