<script setup lang="ts">
import { ref, computed, onBeforeUnmount, onMounted } from 'vue'
import log from 'electron-log/renderer'
import { 
  ElDialog, 
  ElButton, 
  ElCard, 
  ElProgress, 
  ElAlert, 
  ElTooltip,
  ElTag 
} from 'element-plus'
import { Setting, Download, Delete, Check, FolderOpened, Close } from '@element-plus/icons-vue'

// 模型接口定义
interface ModelConfig {
  id: string
  name: string
  filename: string
  url: string
  size: string
  sizeBytes: number
  description?: string
}

// 模型状态
const downloading = ref(false)
const downloadProgress = ref(0)
const totalSize = ref(0)
const downloadedSize = ref(0)
const errorMessage = ref('')
const showError = ref(false)
const showModal = ref(false)
const downloadingModelId = ref('') // 当前正在下载的模型ID

// 模型数据
const availableModels = ref<ModelConfig[]>([])
const downloadedModelIds = ref<string[]>([])
const currentModel = ref<ModelConfig | null>(null)
const selectedModelId = ref<string>('')

// 计算属性：下载进度百分比
const progressPercentage = computed(() => {
  if (totalSize.value === 0) return 0
  return Math.round((downloadedSize.value / totalSize.value) * 100)
})

// 初始化
onMounted(async () => {
  await loadModelsData()
})

// 加载模型数据
const loadModelsData = async (): Promise<void> => {
  try {
    // 获取可用模型列表
    availableModels.value = await window.api.getAvailableModels()

    // 获取已下载模型列表
    downloadedModelIds.value = await window.api.getDownloadedModels()

    // 获取当前模型
    currentModel.value = await window.api.getCurrentModel()

    // 设置选中的模型ID
    if (currentModel.value) {
      selectedModelId.value = currentModel.value.id
    }
  } catch (error) {
    log.error('加载模型数据失败:', error)
    showError.value = true
    errorMessage.value = '加载模型数据失败'
  }
}

// 选择模型
const selectModel = async (modelId: string): Promise<void> => {
  try {
    selectedModelId.value = modelId

    // 如果模型已下载且不是当前模型，则切换
    if (downloadedModelIds.value.includes(modelId) && currentModel.value?.id !== modelId) {
      // 释放当前模型资源
      await window.api.disposeModel()

      // 设置新的当前模型
      await window.api.setCurrentModel(modelId)

      // 重新加载数据
      await loadModelsData()
    }
  } catch (error) {
    log.error('选择模型失败:', error)
    showError.value = true
    errorMessage.value = '选择模型失败'
  }
}

// 下载模型
const downloadModel = async (modelId: string): Promise<void> => {
  if (downloading.value) return

  try {
    downloading.value = true
    downloadProgress.value = 0
    downloadedSize.value = 0
    totalSize.value = 0
    showError.value = false
    downloadingModelId.value = modelId // 设置当前下载的模型ID

    // 注册事件监听器
    window.electron.ipcRenderer.on('model-download-complete', (_event, data) => {
      if (data.success) {
        log.info('下载完成:', data.message)
      }
    })

    window.electron.ipcRenderer.on('model-download-error', (_event, data) => {
      if (!data.success) {
        errorMessage.value = data.error || '下载失败'
        showError.value = true
        downloading.value = false
        downloadingModelId.value = '' // 清除下载中的模型ID
      }
    })

    // 调用主进程下载模型
    const result = await window.api.downloadModel(
      modelId,
      (progress: number, downloaded: number, total: number) => {
        downloadProgress.value = progress
        downloadedSize.value = downloaded
        totalSize.value = total
      }
    )

    if (!result.success) {
      errorMessage.value = result.error || '下载失败'
      showError.value = true
      downloading.value = false
      downloadingModelId.value = '' // 清除下载中的模型ID
      return
    }

    // 重新加载模型数据
    await loadModelsData()
  } catch (error) {
    log.error('下载模型失败:', error)
    errorMessage.value = error instanceof Error ? error.message : '下载失败'
    showError.value = true
    downloadingModelId.value = '' // 清除下载中的模型ID
  } finally {
    downloading.value = false
  }
}

// 取消下载
const cancelDownload = async (): Promise<void> => {
  try {
    await window.api.cancelModelDownload()
    downloading.value = false
    downloadingModelId.value = '' // 清除下载中的模型ID
  } catch (error) {
    log.error('取消下载失败:', error)
    errorMessage.value = '取消下载失败'
    showError.value = true
  }
}

// 删除模型
const deleteModel = async (modelId: string): Promise<void> => {
  try {
    // 如果删除的是当前加载的模型，先释放资源
    if (currentModel.value?.id === modelId) {
      await window.api.disposeModel()
    }

    // 删除模型文件
    await window.api.deleteModel(modelId)

    // 重新加载模型数据
    await loadModelsData()
  } catch (error) {
    log.error('删除模型失败:', error)
    errorMessage.value = '删除模型失败'
    showError.value = true
  }
}

// 打开模型目录
const openModelsDirectory = async (): Promise<void> => {
  try {
    await window.api.openModelsDirectory()
  } catch (error) {
    log.error('打开模型目录失败:', error)
    errorMessage.value = '打开模型目录失败'
    showError.value = true
  }
}

// 打开模态窗口
const openModal = (): void => {
  showModal.value = true
  loadModelsData()
}

// 关闭错误提示
const closeError = (): void => {
  showError.value = false
}

// 格式化大小显示
const formatSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'

  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 清理事件监听器
const cleanupListeners = (): void => {
  window.electron.ipcRenderer.removeAllListeners('model-download-complete')
  window.electron.ipcRenderer.removeAllListeners('model-download-error')
}

// 组件卸载前清理
onBeforeUnmount(() => {
  cleanupListeners()
})

// 对外暴露方法
defineExpose({
  openModal
})
</script>

<template>
  <div class="model-manager">
    <!-- 设置按钮 -->
    <el-tooltip content="模型管理" placement="bottom">
      <el-button 
        class="settings-button" 
        @click="openModal" 
        :icon="Setting"
        circle 
        type="primary" 
        size="large"
        style="pointer-events: auto; z-index: 1000;"
      />
    </el-tooltip>

    <!-- 模态窗口 -->
    <el-dialog
      v-model="showModal"
      title="模型管理"
      width="500px"
      destroy-on-close
    >
      <div class="dialog-content">
        <!-- 错误信息 -->
        <el-alert
          v-if="showError"
          :title="errorMessage"
          type="error"
          :closable="true"
          @close="closeError"
          show-icon
          class="mb-4"
        />

        <!-- 模型列表 -->
        <el-card 
          v-for="model in availableModels" 
          :key="model.id" 
          class="model-card mb-4"
          :class="{ 'is-selected': selectedModelId === model.id }"
          shadow="hover"
        >
          <template #header>
            <div class="card-header">
              <span class="model-name">{{ model.name }}</span>
              <el-tag v-if="downloadedModelIds.includes(model.id)" type="success" size="small">
                已下载
              </el-tag>
            </div>
          </template>

          <div class="model-info" @click="selectModel(model.id)">
            <span class="model-size">大小: {{ model.size }}</span>
            <span v-if="model.description" class="model-description">
              {{ model.description }}
            </span>
          </div>

          <!-- 模型操作按钮 -->
          <div class="model-actions">
            <!-- 已下载模型的操作 -->
            <div v-if="downloadedModelIds.includes(model.id)" class="action-buttons">
              <el-button
                v-if="currentModel?.id !== model.id"
                type="primary"
                @click="selectModel(model.id)"
                :icon="Check"
                size="small"
              >
                使用
              </el-button>
              <el-button
                v-else
                type="success"
                disabled
                :icon="Check"
                size="small"
              >
                当前使用
              </el-button>
              <el-button
                type="danger"
                @click="deleteModel(model.id)"
                :icon="Delete"
                size="small"
              >
                删除
              </el-button>
            </div>

            <!-- 未下载模型的操作 -->
            <div v-else>
              <el-button
                v-if="!downloading || downloadingModelId !== model.id"
                type="primary"
                :disabled="downloading"
                @click="downloadModel(model.id)"
                :icon="Download"
                size="small"
                class="w-full"
              >
                下载模型
              </el-button>

              <!-- 当前正在下载的模型显示进度条和取消按钮 -->
              <div v-else class="model-download-progress">
                <el-progress 
                  :percentage="progressPercentage" 
                  :show-text="false"
                  status="success"
                />
                <div class="progress-info">
                  {{ formatSize(downloadedSize) }} / {{ formatSize(totalSize) }} ({{ progressPercentage }}%)
                </div>
                <el-button
                  type="danger"
                  @click="cancelDownload"
                  :icon="Close"
                  size="small"
                  class="mt-2"
                >
                  取消
                </el-button>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 目录操作 -->
        <el-button
          type="primary"
          @click="openModelsDirectory"
          :icon="FolderOpened"
          class="w-full"
        >
          打开模型目录
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.model-manager {
  margin-left: 15px;
  z-index: 1000;
  pointer-events: auto;
}

.settings-button {
  opacity: 0.9;
  backdrop-filter: blur(5px);
  box-shadow: var(--el-box-shadow-light);
}

.settings-button:hover {
  transform: scale(1.05);
  opacity: 1;
}

.dialog-content {
  max-height: 70vh;
  overflow-y: auto;
}

.model-card {
  margin-bottom: 12px;
  border: 2px solid transparent;
  transition: all 0.2s ease;
}

.model-card.is-selected {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 10px rgba(66, 211, 146, 0.2);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.model-name {
  font-weight: bold;
  font-size: 16px;
}

.model-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
  cursor: pointer;
  margin-bottom: 15px;
}

.model-size {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.model-description {
  margin-top: 5px;
  font-style: italic;
  color: var(--el-text-color-secondary);
}

.model-actions {
  margin-top: 10px;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.model-download-progress {
  margin-top: 10px;
}

.progress-info {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-top: 5px;
  display: flex;
  justify-content: center;
}

.w-full {
  width: 100%;
}

.mb-4 {
  margin-bottom: 16px;
}

.mt-2 {
  margin-top: 8px;
}
</style>
