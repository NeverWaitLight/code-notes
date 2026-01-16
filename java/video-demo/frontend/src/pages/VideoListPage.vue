<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useVideoStore } from '../stores/videoStore'
import type { VideoStatus } from '../types/video'

const router = useRouter()
const videoStore = useVideoStore()

onMounted(() => {
  videoStore.fetchList()
})

const getStatusText = (status: VideoStatus): string => {
  const statusMap: Record<VideoStatus, string> = {
    UPLOADING: '上传中',
    READY: '上传完成',
    FAILED: '失败',
  }
  return statusMap[status]
}

const handleItemClick = (videoId: number) => {
  router.push(`/play/${videoId}`)
}
</script>

<template>
  <div class="video-list-page">
    <h1>视频列表</h1>
    <div v-if="videoStore.loading" class="loading">加载中...</div>
    <div v-else-if="videoStore.error" class="error">
      加载失败：{{ videoStore.error.message }}
    </div>
    <div v-else-if="videoStore.items.length === 0" class="empty">
      暂无视频，去上传一个吧
    </div>
    <ul v-else class="video-list">
      <li
        v-for="video in videoStore.items"
        :key="video.id"
        class="video-item"
        @click="handleItemClick(video.id)"
      >
        <div class="video-title">{{ video.title }}</div>
        <div class="video-status">{{ getStatusText(video.status) }}</div>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.video-list-page {
  padding: 2rem;
}

h1 {
  margin-bottom: 1.5rem;
}

.loading,
.error,
.empty {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.error {
  color: #d32f2f;
}

.video-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.video-item {
  padding: 1rem;
  margin-bottom: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.video-item:hover {
  background-color: #f5f5f5;
}

.video-title {
  font-weight: 500;
  margin-bottom: 0.5rem;
}

.video-status {
  font-size: 0.875rem;
  color: #666;
}
</style>
