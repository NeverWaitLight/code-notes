<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Hls from 'hls.js'
import { videoService } from '../services/videoService'
import type { VideoDetail, VideoStatus } from '../types/video'
import type { ApiError } from '../types/api'
import { useVideoStore } from '../stores/videoStore'

const route = useRoute()
const router = useRouter()
const videoStore = useVideoStore()

const videoId = Number(route.params.id)
const video = ref<VideoDetail | null>(null)
const loading = ref(true)
const error = ref<ApiError | null>(null)
const playError = ref<string | null>(null)
const videoElement = ref<HTMLVideoElement | null>(null)
let hls: Hls | null = null

onMounted(async () => {
  try {
    loading.value = true
    error.value = null
    video.value = await videoService.detail(videoId)
  } catch (err: any) {
    error.value = err?.error ?? { code: 'UNKNOWN', message: '加载视频详情失败' }
  } finally {
    loading.value = false
  }
})

const handleBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/')
}

watch([video, videoElement], async () => {
  if (video.value?.status === 'READY' && video.value?.manifestUrl && videoElement.value) {
    await nextTick()
    initializePlayer()
  }
}, { immediate: true })

function initializePlayer() {
  if (!video.value || !videoElement.value) return
  
  if (Hls.isSupported()) {
    hls = new Hls({
      // 配置渐进式加载策略
      maxBufferLength: 10,           // 最大缓冲长度：10秒
      maxMaxBufferLength: 20,        // 最大最大缓冲长度：20秒
      maxBufferSize: 10 * 1000 * 1000, // 最大缓冲大小：10MB
      maxBufferHole: 0.5,            // 最大缓冲空洞：0.5秒
      
      // 控制预加载行为
      lowLatencyMode: false,         // 关闭低延迟模式
      backBufferLength: 10,          // 后向缓冲长度：10秒（已播放的保留时长）
      
      // 片段加载策略
      enableWorker: true,            // 启用 Web Worker（提升性能）
      
      // 调试（可选）
      debug: false,
    })
    hls.loadSource(video.value.manifestUrl)
    hls.attachMedia(videoElement.value)
    
    // 监听片段加载事件（可用于调试）
    hls.on(Hls.Events.FRAG_LOADING, (event, data) => {
      console.log('加载片段:', data.frag.sn, data.frag.url)
    })
    
    hls.on(Hls.Events.FRAG_LOADED, (event, data) => {
      console.log('片段已加载:', data.frag.sn, '大小:', (data.frag.stats.total / 1024).toFixed(2), 'KB')
    })
    
    hls.on(Hls.Events.ERROR, (event, data) => {
      if (data.fatal) {
        switch (data.type) {
          case Hls.ErrorTypes.NETWORK_ERROR:
            playError.value = '播放失败：网络错误'
            break
          case Hls.ErrorTypes.MEDIA_ERROR:
            playError.value = '播放失败：媒体错误'
            break
          default:
            playError.value = `播放失败：${data.message || '未知错误'}`
            break
        }
      }
    })
  } else if (videoElement.value.canPlayType('application/vnd.apple.mpegurl')) {
    videoElement.value.src = video.value.manifestUrl
  } else {
    playError.value = '播放失败：浏览器不支持 HLS 播放'
  }
}

const getStatusText = (status: VideoStatus): string => {
  const statusMap: Record<VideoStatus, string> = {
    UPLOADING: '上传中',
    READY: '上传完成',
    FAILED: '失败',
  }
  return statusMap[status]
}

const formatNumber = (value: number): string => {
  const fixed = value.toFixed(1)
  return fixed.endsWith('.0') ? fixed.slice(0, -2) : fixed
}

const formatSize = (sizeBytes: number): string => {
  if (!Number.isFinite(sizeBytes)) {
    return '--'
  }
  const gb = 1024 ** 3
  const mb = 1024 ** 2
  if (sizeBytes >= gb) {
    return `${formatNumber(sizeBytes / gb)} GB`
  }
  return `${formatNumber(sizeBytes / mb)} MB`
}

const formatDate = (createdAt: number): string => {
  if (!Number.isFinite(createdAt)) {
    return '--'
  }
  const timestamp = createdAt < 1_000_000_000_000 ? createdAt * 1000 : createdAt
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) {
    return '--'
  }
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}`
}

async function handleDelete() {
  if (!window.confirm('确定删除该视频吗？删除后不可恢复')) {
    return
  }
  
  try {
    loading.value = true
    error.value = null
    await videoService.delete(videoId)
    await videoStore.fetchList()
    router.push('/')
  } catch (err: any) {
    error.value = err?.error ?? { code: 'UNKNOWN', message: '删除失败' }
    loading.value = false
  }
}

onUnmounted(() => {
  if (hls) {
    hls.destroy()
    hls = null
  }
})
</script>

<template>
  <div class="player-page">
    <div class="player-content">
      <a
        class="back-button"
        href="#"
        role="button"
        tabindex="0"
        @click.prevent="handleBack"
        @keydown.enter.prevent="handleBack"
        @keydown.space.prevent="handleBack"
      >
        返回上级
      </a>
      <div v-if="loading" class="state-panel loading">加载中...</div>
      <div v-else-if="error" class="state-panel error">错误：{{ error.message }}</div>
      <div v-else-if="!video" class="state-panel error">视频不存在</div>
      <div v-else class="player-body">
        <div class="player-card">
          <div v-if="video.status !== 'READY' || !video.manifestUrl" class="player-message placeholder">
            视频处理中，暂不可播放
          </div>
          <div v-else-if="playError" class="player-message error">{{ playError }}</div>
          <div v-else class="video-container">
            <video ref="videoElement" controls></video>
          </div>
        </div>

        <section class="video-info">
          <h1 class="video-title">{{ video.title }}</h1>
          <dl class="meta-grid">
            <div class="meta-item">
              <dt>状态</dt>
              <dd>{{ getStatusText(video.status) }}</dd>
            </div>
            <div class="meta-item">
              <dt>大小</dt>
              <dd>{{ formatSize(video.sizeBytes) }}</dd>
            </div>
            <div class="meta-item">
              <dt>上传时间</dt>
              <dd>{{ formatDate(video.createdAt) }}</dd>
            </div>
          </dl>
        </section>

        <div class="actions">
          <button class="delete-button" @click="handleDelete" :disabled="loading">删除视频</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.player-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8f8f8 0%, #f3f3f3 50%, #f1f1f1 100%);
  color: #1f1f1f;
  font-family:
    'Noto Sans SC',
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    sans-serif;
}

.player-content {
  max-width: 1100px;
  margin: 0 auto;
  padding: clamp(1.5rem, 3vw, 3rem) 1.25rem 3.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.6rem;
}

.back-button {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.45rem 0.95rem;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: #ffffff;
  color: #1f1f1f;
  font-size: 0.9rem;
  transition: border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
}

.back-button:hover {
  border-color: rgba(211, 47, 47, 0.45);
  color: #d32f2f;
}

.back-button:focus-visible {
  outline: 2px solid rgba(211, 47, 47, 0.5);
  outline-offset: 2px;
}

.state-panel {
  padding: 2.4rem 1.5rem;
  text-align: center;
  font-size: 1.05rem;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.08);
  color: #555;
}

.state-panel.error {
  color: #d32f2f;
  border-color: rgba(211, 47, 47, 0.25);
}

.player-body {
  display: flex;
  flex-direction: column;
  gap: 1.6rem;
}

.player-card {
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 18px;
  padding: clamp(1rem, 2.5vw, 1.5rem);
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.08);
}

.video-container {
  background: #0f0f0f;
  border-radius: 12px;
  overflow: hidden;
}

.video-container video {
  width: 100%;
  max-width: 100%;
  height: auto;
  display: block;
  background: #000;
}

.player-message {
  padding: 2.4rem 1.4rem;
  text-align: center;
  border-radius: 12px;
  background: #f5f5f5;
  color: #666;
  font-size: 1rem;
}

.player-message.error {
  background: rgba(211, 47, 47, 0.08);
  color: #d32f2f;
}

.video-info {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.video-title {
  margin: 0;
  font-size: clamp(1.4rem, 3vw, 2rem);
  font-weight: 600;
  color: #1f1f1f;
}

.meta-grid {
  margin: 0;
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
  padding: 1.1rem 1.2rem;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.08);
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.meta-item dt {
  font-size: 0.85rem;
  color: #7a7a7a;
}

.meta-item dd {
  margin: 0;
  font-size: 1rem;
  color: #2b2b2b;
  font-weight: 500;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.delete-button {
  min-height: 44px;
  padding: 0.75rem 1.8rem;
  font-size: 1rem;
  font-weight: 600;
  background: #d32f2f;
  color: #ffffff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(211, 47, 47, 0.25);
  transition: background 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.delete-button:hover:not(:disabled) {
  background: #c62828;
  box-shadow: 0 12px 26px rgba(198, 40, 40, 0.28);
  transform: translateY(-1px);
}

.delete-button:disabled {
  background: #d6d6d6;
  color: #9a9a9a;
  box-shadow: none;
  cursor: not-allowed;
}

@media (min-width: 768px) {
  .meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1024px) {
  .player-content {
    padding: 3.5rem 2.5rem 4rem;
    gap: 2rem;
  }

  .player-body {
    gap: 2rem;
  }
}

@media (max-width: 600px) {
  .back-button {
    width: 100%;
    justify-content: center;
  }

  .actions {
    justify-content: stretch;
  }

  .delete-button {
    width: 100%;
  }
}
</style>
