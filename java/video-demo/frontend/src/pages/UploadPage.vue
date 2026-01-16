<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUploadStore } from '../stores/uploadStore'
import type { UploadItem } from '../stores/uploadStore'

const uploadStore = useUploadStore()

const selectedFile = ref<File | null>(null)
const title = ref('')
const isSubmitting = ref(false)
const errorMessage = ref('')

const MAX_FILE_SIZE = 5 * 1024 * 1024 * 1024

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  errorMessage.value = ''

  if (file.size > MAX_FILE_SIZE) {
    errorMessage.value = '文件大小超过 5GB 限制'
    selectedFile.value = null
    target.value = ''
    return
  }

  if (!file.type.includes('mp4') && !file.name.toLowerCase().endsWith('.mp4')) {
    errorMessage.value = '仅支持 MP4 文件'
    selectedFile.value = null
    target.value = ''
    return
  }

  selectedFile.value = file
}

const handleSubmit = async () => {
  errorMessage.value = ''

  if (!selectedFile.value) {
    errorMessage.value = '请选择文件'
    return
  }

  if (!title.value.trim()) {
    errorMessage.value = '标题不能为空'
    return
  }

  isSubmitting.value = true
  try {
    await uploadStore.startUpload(selectedFile.value, title.value.trim())
    selectedFile.value = null
    title.value = ''
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement
    if (fileInput) {
      fileInput.value = ''
    }
  } catch (err) {
    errorMessage.value = '上传失败，请重试'
  } finally {
    isSubmitting.value = false
  }
}

const activeUploads = computed(() => uploadStore.activeUploads)
</script>

<template>
  <div class="upload-page">
    <h1>上传视频</h1>
    
    <div class="upload-form">
      <div class="form-group">
        <label for="file-input">选择视频文件</label>
        <input
          id="file-input"
          type="file"
          accept="video/mp4"
          @change="handleFileSelect"
          :disabled="isSubmitting"
        />
        <p class="hint">选择 H.264 MP4 文件（≤ 5GB）</p>
      </div>

      <div class="form-group">
        <label for="title-input">视频标题</label>
        <input
          id="title-input"
          type="text"
          v-model="title"
          placeholder="请输入视频标题"
          :disabled="isSubmitting"
        />
      </div>

      <div class="form-group">
        <button
          @click="handleSubmit"
          :disabled="!selectedFile || isSubmitting"
          class="submit-button"
        >
          {{ isSubmitting ? '上传中...' : '开始上传' }}
        </button>
      </div>

      <div v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>
    </div>

    <div class="upload-list" v-if="activeUploads.length > 0">
      <h2>上传中</h2>
      <div
        v-for="item in activeUploads"
        :key="item.id"
        class="upload-item"
      >
        <div class="upload-info">
          <span class="upload-title">{{ item.title }}</span>
          <span class="upload-progress">正在上传... {{ item.progress }}%</span>
        </div>
        <div class="progress-bar">
          <div
            class="progress-fill"
            :style="{ width: `${item.progress}%` }"
          ></div>
        </div>
      </div>
    </div>


    <div class="upload-list" v-if="uploadStore.uploadingItems.filter(item => item.status === 'failed').length > 0">
      <h2>上传失败</h2>
      <div
        v-for="item in uploadStore.uploadingItems.filter(item => item.status === 'failed')"
        :key="item.id"
        class="upload-item failed"
      >
        <div class="upload-info">
          <span class="upload-title">{{ item.title }}</span>
          <span class="upload-error">上传失败：{{ item.error }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.upload-page {
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
}

.upload-form {
  margin-bottom: 2rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.form-group input[type="file"],
.form-group input[type="text"] {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.form-group input[type="file"] {
  padding: 0.25rem;
}

.hint {
  margin-top: 0.25rem;
  font-size: 0.875rem;
  color: #666;
}

.submit-button {
  padding: 0.75rem 1.5rem;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
}

.submit-button:hover:not(:disabled) {
  background-color: #0056b3;
}

.submit-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.error-message {
  padding: 0.75rem;
  background-color: #f8d7da;
  color: #721c24;
  border-radius: 4px;
  margin-top: 1rem;
}

.upload-list {
  margin-top: 2rem;
}

.upload-list h2 {
  margin-bottom: 1rem;
  font-size: 1.25rem;
}

.upload-item {
  padding: 1rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 1rem;
  background-color: #f9f9f9;
}

.upload-item.success {
  background-color: #d4edda;
  border-color: #c3e6cb;
}

.upload-item.failed {
  background-color: #f8d7da;
  border-color: #f5c6cb;
}

.upload-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.upload-title {
  font-weight: 500;
}

.upload-progress,
.upload-status {
  font-size: 0.875rem;
  color: #666;
}

.upload-error {
  font-size: 0.875rem;
  color: #721c24;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background-color: #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: #007bff;
  transition: width 0.3s ease;
}

.remove-button {
  padding: 0.25rem 0.75rem;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 0.875rem;
  cursor: pointer;
}

.remove-button:hover {
  background-color: #5a6268;
}
</style>

