# Introduction

本文档描述“在线视频网站 Demo”的完整全栈架构，覆盖后端系统、前端实现与二者的集成方式。它作为 AI 驱动开发的单一事实来源，确保技术栈与实现模式在全项目范围一致。

该统一架构将传统上分离的前端与后端架构合并，适用于本项目这种前后端紧密协作的全栈应用场景。

> 备注：未发现 `docs/front-end-spec.md`，当前内容基于 `docs/prd.md` 草拟，待补充前端规格后再校准。

## Starter Template or Existing Project

N/A - Greenfield project。PRD 未提及现有代码库或可复用模板。建议采用以下起步模板：

- 后端：Spring Initializr（Spring Boot + Maven + Java 21）
- 前端：Vite `create-vue`（Vue 3）

根目录采用 `backend/` 与 `frontend/` 分离结构，与 PRD 的 Monorepo（前后端分目录）一致。

## Change Log

| Date       | Version | Description      | Author  |
| ---------- | ------- | ---------------- | ------- |
| 2026-01-14 | v0.1    | 初始全栈架构草案 | Winston |
