# Tasks: 交互式 OSS 批量下载工具

**Input**: Design documents from `/specs/001-aliyun-oss-batch-downloader/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/cli.md

**Tests**: 未在规格中要求测试，以下任务不包含测试项。

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Create project structure in `src/oss_downloader/` and `scripts/`
- [ ] T002 Add `requirements.txt` with oss2, rich, questionary
- [ ] T003 Create package entrypoint `src/oss_downloader/__main__.py`

---

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Implement `DownloadConfig` in `src/oss_downloader/config.py`
- [ ] T005 Implement OSS client wrapper in `src/oss_downloader/oss_client.py`
- [ ] T006 Implement manifest storage (SQLite) with retry_count/last_error in `src/oss_downloader/manifest.py`
- [ ] T007 Implement download engine with two-pass flow (first pass single attempt) in `src/oss_downloader/downloader.py`
- [ ] T008 Implement progress UI in `src/oss_downloader/progress.py`
- [ ] T009 Implement suffix filtering in `src/oss_downloader/filters.py`

---

## Phase 3: User Story 1 - 交互式批量下载 (Priority: P1)

**Goal**: 交互式输入并执行批量下载

**Independent Test**: 使用小规模 bucket/prefix 下载并核对本地文件结构

### Implementation

- [ ] T010 Implement interactive CLI flow in `src/oss_downloader/cli.py`
- [ ] T011 Wire CLI to downloader in `src/oss_downloader/__main__.py`
- [ ] T012 Add summary output and exit codes in `src/oss_downloader/cli.py`

---

## Phase 4: User Story 2 - 断点续传与失败重试 (Priority: P2)

**Goal**: 支持断点续传与失败重试

**Independent Test**: 中断后重启下载任务，已完成项被跳过

### Implementation

- [ ] T013 Persist object states and resume logic in `src/oss_downloader/manifest.py`
- [ ] T014 Add retry policy/backoff and second-pass logic (total attempts <= 3) in `src/oss_downloader/downloader.py`
- [ ] T015 Add failed summary and non-zero exit code when failures exist
- [ ] T016 Export failed list CSV in `src/oss_downloader/manifest.py` (target: `<target_dir>/.oss-failed.csv`)

---

## Phase 5: User Story 3 - 下载前预览与过滤 (Priority: P3)

**Goal**: 支持预览统计与后缀过滤

**Independent Test**: 预览统计与实际下载范围一致

### Implementation

- [ ] T017 Add preview-only mode in `src/oss_downloader/cli.py`
- [ ] T018 Apply suffix filter before download in `src/oss_downloader/filters.py`
- [ ] T019 Show preview summary in `src/oss_downloader/cli.py`

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T020 Update `specs/001-aliyun-oss-batch-downloader/quickstart.md` if final CLI differs
- [ ] T021 Add `scripts/build.sh` and document build steps
- [ ] T022 Add basic README in repo root (optional)
