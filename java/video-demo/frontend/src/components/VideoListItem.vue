<script setup lang="ts">
import { computed, toRefs } from 'vue'
import type { Video, VideoStatus } from '../types/video'

const props = defineProps<{
  video: Video
  index: number
}>()

const emit = defineEmits<{
  (e: 'select', id: number): void
}>()

const { video, index } = toRefs(props)

const handleClick = () => {
  emit('select', video.value.id)
}

const getStatusText = (status: VideoStatus): string => {
  const statusMap: Record<VideoStatus, string> = {
    UPLOADING: '上传中',
    READY: '上传完成',
    FAILED: '失败',
  }
  return statusMap[status]
}

const getStatusTone = (status: VideoStatus): string => {
  switch (status) {
    case 'READY':
      return 'ready'
    case 'FAILED':
      return 'failed'
    default:
      return 'uploading'
  }
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

const statusText = computed(() => getStatusText(video.value.status))
const statusClass = computed(() => `status-badge status-badge--${getStatusTone(video.value.status)}`)
const sizeText = computed(() => formatSize(video.value.sizeBytes))
const dateText = computed(() => formatDate(video.value.createdAt))
const animationDelay = computed(() => `${index.value * 80}ms`)
</script>

<template>
  <button class="video-item" type="button" :style="{ animationDelay }" @click="handleClick">
    <div class="video-thumb"></div>
    <div class="video-body">
      <div class="video-header">
        <h3 class="video-title">{{ video.title }}</h3>
        <span :class="statusClass">{{ statusText }}</span>
      </div>
      <div class="video-meta">
        <span class="video-size">{{ sizeText }}</span>
        <span class="meta-separator" aria-hidden="true"></span>
        <span class="video-time">{{ dateText }}</span>
      </div>
    </div>
  </button>
</template>

<style scoped>
.video-item {
  width: 100%;
  display: flex;
  flex-direction: column;
  padding: 0;
  border: 1px solid var(--border-soft);
  border-radius: 18px;
  background: var(--surface-strong);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
  cursor: pointer;
  text-align: left;
  color: inherit;
  font: inherit;
  appearance: none;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  animation: card-fade 0.6s ease-out both;
}

.video-item:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lift);
  border-color: rgba(211, 47, 47, 0.35);
}

.video-item:focus-visible {
  outline: 2px solid var(--yt-red);
  outline-offset: 3px;
}

.video-thumb {
  position: relative;
  aspect-ratio: 16 / 9;
  background: linear-gradient(135deg, #2e2e2e 0%, #3c3c3c 45%, #242424 100%);
  overflow: hidden;
}

.video-thumb::before {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.08) 0,
    rgba(255, 255, 255, 0.08) 2px,
    transparent 2px,
    transparent 8px
  );
  opacity: 0.5;
}

.video-thumb::after {
  content: '';
  position: absolute;
  width: 0;
  height: 0;
  border-left: 18px solid rgba(255, 255, 255, 0.92);
  border-top: 11px solid transparent;
  border-bottom: 11px solid transparent;
  left: 50%;
  top: 50%;
  transform: translate(-35%, -50%);
  filter: drop-shadow(0 3px 8px rgba(0, 0, 0, 0.35));
}

.video-body {
  padding: 0.95rem 1.05rem 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}

.video-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.video-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.4;
  color: var(--neutral-900);
}

.status-badge {
  flex-shrink: 0;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  border: 1px solid var(--border-soft);
  color: var(--neutral-600);
  background: var(--surface-muted);
}

.status-badge--ready {
  color: var(--yt-red);
  border-color: rgba(211, 47, 47, 0.4);
  background: rgba(211, 47, 47, 0.1);
}

.status-badge--uploading {
  color: var(--neutral-700);
  border-color: rgba(211, 47, 47, 0.25);
  background: rgba(255, 255, 255, 0.8);
}

.status-badge--failed {
  color: var(--yt-red-strong);
  border-color: rgba(198, 40, 40, 0.5);
  background: rgba(198, 40, 40, 0.12);
}

.video-meta {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-size: 0.85rem;
  color: var(--neutral-600);
}

.meta-separator {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: var(--neutral-300);
}

@keyframes card-fade {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .video-item {
    animation: none;
    transition: none;
  }
}
</style>
