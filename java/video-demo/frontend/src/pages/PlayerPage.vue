<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Hls from 'hls.js'
import { videoService } from '../services/videoService'
import type { VideoDetail } from '../types/video'
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
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">错误：{{ error.message }}</div>
    <div v-else-if="!video" class="error">视频不存在</div>
    <div v-else>
      <h1>{{ video.title }}</h1>
      
      <div v-if="video.status !== 'READY' || !video.manifestUrl" class="placeholder">
        视频处理中，暂不可播放
      </div>
      <div v-else-if="playError" class="error">{{ playError }}</div>
      <div v-else class="video-container">
        <video ref="videoElement" controls></video>
      </div>
      
      <div class="actions">
        <button @click="handleDelete" :disabled="loading">删除视频</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.player-page {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.loading,
.error,
.placeholder {
  padding: 2rem;
  text-align: center;
  font-size: 1.2rem;
}

.error {
  color: #d32f2f;
}

.placeholder {
  color: #666;
  background: #f5f5f5;
  border-radius: 4px;
}

.video-container {
  margin: 2rem 0;
}

.video-container video {
  width: 100%;
  max-width: 100%;
  height: auto;
  background: #000;
  border-radius: 4px;
}

.actions {
  margin-top: 2rem;
  text-align: center;
}

.actions button {
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
  background: #d32f2f;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.actions button:hover:not(:disabled) {
  background: #b71c1c;
}

.actions button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>
