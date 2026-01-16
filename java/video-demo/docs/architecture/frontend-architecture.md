# Frontend Architecture

## Component Architecture

### Component Organization

```text
frontend/
  src/
    pages/
      VideoListPage.vue
      UploadPage.vue
      PlayerPage.vue
    components/
      VideoListItem.vue
      UploadTaskItem.vue
      VideoPlayer.vue
      ConfirmDialog.vue
      EmptyPlaceholder.vue
    services/
      apiClient.ts
      videoService.ts
    stores/
      videoStore.ts
      uploadStore.ts
    router/
      index.ts
    types/
      video.ts
      api.ts
    styles/
      base.css
    main.ts
```

### Component Template

```typescript
<script setup lang="ts">
import type { Video } from "../types/video";

interface Props {
  video: Video;
}

defineProps<Props>();
</script>

<template>
  <div class="video-item">
    <div class="title">{{ video.title }}</div>
    <div class="status">{{ video.status }}</div>
  </div>
</template>
```

## State Management Architecture

### State Structure

```typescript
// stores/videoStore.ts
import { defineStore } from "pinia";
import type { Video } from "../types/video";
import { videoService } from "../services/videoService";

interface VideoState {
  items: Video[];
  selected?: Video;
  loading: boolean;
  error?: { code: string; message: string };
}

export const useVideoStore = defineStore("video", {
  state: (): VideoState => ({
    items: [],
    selected: undefined,
    loading: false,
    error: undefined,
  }),
  actions: {
    async fetchList() {
      this.loading = true;
      this.error = undefined;
      try {
        this.items = await videoService.list();
      } catch (err: any) {
        this.error = err?.error ?? { code: "UNKNOWN", message: "加载失败" };
      } finally {
        this.loading = false;
      }
    },
  },
});
```

### State Management Patterns

- 列表与详情状态分离，避免互相污染
- 所有异步调用通过 store actions 统一管理
- UI 仅消费 store 状态，不直接调用 API

## UI 状态与文案

**列表页（/）**

- 空态：`暂无视频，去上传一个吧`
- 加载中：`加载中...`
- 错误态：`加载失败：{message}`

**上传页（/upload）**

- 默认提示：`选择 H.264 MP4 文件（≤ 5GB）`
- 上传中：`正在上传... {progress}%`
- 上传成功：`上传完成，处理中`
- 上传失败：`上传失败：{message}`

**播放页（/play/:id）**

- 未就绪占位：`视频处理中，暂不可播放`
- 播放失败：`播放失败：{message}`
- 删除确认：`确定删除该视频吗？删除后不可恢复`

## Routing Architecture

### Route Organization

```text
/
/upload
/play/:id
```

### Protected Route Pattern

```typescript
// router/index.ts
import { createRouter, createWebHistory } from "vue-router";

const routes = [
  { path: "/", component: () => import("../pages/VideoListPage.vue") },
  { path: "/upload", component: () => import("../pages/UploadPage.vue") },
  { path: "/play/:id", component: () => import("../pages/PlayerPage.vue") },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 无鉴权需求，直接放行
router.beforeEach(() => true);

export default router;
```

## Frontend Services Layer

### API Client Setup

```typescript
// services/apiClient.ts
const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, init);
  const data = await res.json();
  if (!res.ok) {
    throw { error: data?.error ?? { code: "UNKNOWN", message: "请求失败" } };
  }
  return data as T;
}

export const apiClient = { request };
```

**开发环境端口约束：**  
前端调试服务器端口需与后端端口不同，避免冲突与跨源问题。建议前端使用 `5173`（Vite 默认），后端使用 `8080`。

### Service Example

```typescript
// services/videoService.ts
import { apiClient } from "./apiClient";
import type { Video } from "../types/video";

export const videoService = {
  list: () => apiClient.request<Video[]>("/api/videos"),
  detail: (id: number) => apiClient.request<Video>(`/api/videos/${id}`),
  delete: (id: number) =>
    apiClient.request<void>(`/api/videos/${id}`, { method: "DELETE" }),
  upload: (file: File, title: string) => {
    const form = new FormData();
    form.append("file", file);
    form.append("title", title);
    return apiClient.request<Video>("/api/videos", {
      method: "POST",
      body: form,
    });
  },
};
```
