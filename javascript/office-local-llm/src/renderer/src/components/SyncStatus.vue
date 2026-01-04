<template>
  <div class="sync-status">
    <div class="status-info">
      <div class="status-icon" :class="{ 'syncing': isSyncing }">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24">
          <path fill="currentColor" d="M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46A8.03 8.03 0 0 0 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74A8.03 8.03 0 0 0 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z" />
        </svg>
      </div>
      <div class="status-text">
        {{ statusMessage }}
      </div>
    </div>
    <div class="sync-buttons">
      <el-button type="primary" size="small" @click="syncNow" :loading="isSyncing" :disabled="isSyncing">
        立即同步
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElButton, ElMessage } from 'element-plus'

const isSyncing = ref(false)
const lastSyncTime = ref<Date | null>(null)
const statusMessage = ref('准备就绪')
const syncTimeout = ref<number | null>(null)

// 执行手动同步
const syncNow = async (): Promise<void> => {
  if (isSyncing.value) return

  isSyncing.value = true
  statusMessage.value = '正在同步...'

  try {
    const result = await window.api.forceSyncFromS3()
    if (result.success) {
      lastSyncTime.value = new Date()
      statusMessage.value = `同步完成 (${formatTime(lastSyncTime.value)})`
      ElMessage.success('同步成功')
    } else {
      statusMessage.value = `同步失败: ${result.error || '未知错误'}`
      ElMessage.error(`同步失败: ${result.error || '未知错误'}`)
    }
  } catch (error) {
    statusMessage.value = `同步出错: ${error}`
    ElMessage.error(`同步出错: ${error}`)
  } finally {
    isSyncing.value = false
    
    // 设置下一次自动同步倒计时
    startSyncCountdown()
  }
}

// 开始同步倒计时
const startSyncCountdown = (): void => {
  if (syncTimeout.value) {
    clearTimeout(syncTimeout.value)
  }
  
  // 3秒后自动更新状态文本
  syncTimeout.value = window.setTimeout(() => {
    if (lastSyncTime.value) {
      statusMessage.value = `上次同步: ${formatTime(lastSyncTime.value)}`
    } else {
      statusMessage.value = '准备就绪'
    }
  }, 3000)
}

// 格式化时间
const formatTime = (date: Date): string => {
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  const seconds = date.getSeconds().toString().padStart(2, '0')
  return `${hours}:${minutes}:${seconds}`
}

// 监听文件系统更新事件，可能是从S3同步过来的
const handleFileSystemUpdated = (event: CustomEvent): void => {
  if (event.detail && event.detail.fileSystem) {
    // 文件系统更新了，可能是从S3同步的结果
    if (!isSyncing.value) {
      lastSyncTime.value = new Date()
      statusMessage.value = `同步完成 (${formatTime(lastSyncTime.value)})`
      startSyncCountdown()
    }
  }
}

// 组件挂载时注册事件监听器
onMounted(() => {
  window.addEventListener('file-system-updated', handleFileSystemUpdated as any)
})

// 组件卸载时清理事件监听器和定时器
onUnmounted(() => {
  window.removeEventListener('file-system-updated', handleFileSystemUpdated as any)
  
  if (syncTimeout.value) {
    clearTimeout(syncTimeout.value)
  }
})
</script>

<style scoped>
.sync-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background-color: var(--el-bg-color-overlay);
  border-radius: 4px;
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color);
}

.status-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-icon {
  width: 18px;
  height: 18px;
  color: var(--el-color-primary);
}

.status-icon.syncing {
  animation: rotate 1.5s linear infinite;
}

.status-text {
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.sync-buttons {
  display: flex;
  gap: 8px;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style> 