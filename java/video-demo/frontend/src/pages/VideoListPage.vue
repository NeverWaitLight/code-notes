<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useVideoStore } from '../stores/videoStore'
import VideoListItem from '../components/VideoListItem.vue'

const router = useRouter()
const videoStore = useVideoStore()

onMounted(() => {
  videoStore.fetchList()
})

const handleItemClick = (videoId: number) => {
  router.push(`/play/${videoId}`)
}
</script>

<template>
  <section class="video-list-page">
    <div class="video-list-container">
      <header class="page-header">
        <h1>视频列表</h1>
      </header>
      <div v-if="videoStore.loading" class="state-panel loading">加载中...</div>
      <div v-else-if="videoStore.error" class="state-panel error">
        加载失败：{{ videoStore.error.message }}
      </div>
      <div v-else-if="videoStore.items.length === 0" class="state-panel empty">
        暂无视频，去上传一个吧
      </div>
      <div v-else class="video-grid">
        <VideoListItem
          v-for="(video, index) in videoStore.items"
          :key="video.id"
          :video="video"
          :index="index"
          @select="handleItemClick"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.video-list-page {
  position: relative;
  min-height: 100vh;
  padding: clamp(1.5rem, 3.5vw, 3rem) 1.25rem 3.5rem;
  animation: page-fade 0.6s ease-out both;
}

.video-list-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(600px 400px at 20% 10%, rgba(211, 47, 47, 0.08), transparent 60%),
    repeating-linear-gradient(
      135deg,
      rgba(0, 0, 0, 0.03) 0,
      rgba(0, 0, 0, 0.03) 2px,
      transparent 2px,
      transparent 7px
    );
  opacity: 0.7;
  pointer-events: none;
}

.video-list-container {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.6rem;
}

.page-header h1 {
  margin: 0;
  font-size: clamp(1.6rem, 3.4vw, 2.6rem);
  font-weight: 700;
  color: var(--neutral-900);
  letter-spacing: 0.02em;
}

.page-header h1::after {
  content: '';
  display: block;
  width: 52px;
  height: 4px;
  margin-top: 0.55rem;
  border-radius: 999px;
  background: var(--yt-red);
}

.state-panel {
  padding: 2.6rem 1.5rem;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-soft);
  text-align: center;
  color: var(--neutral-600);
}

.state-panel.error {
  color: var(--yt-red-strong);
  border-color: rgba(211, 47, 47, 0.25);
}

.video-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.4rem;
}

@media (min-width: 768px) {
  .video-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1200px) {
  .video-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@keyframes page-fade {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .video-list-page {
    animation: none;
  }
}
</style>

