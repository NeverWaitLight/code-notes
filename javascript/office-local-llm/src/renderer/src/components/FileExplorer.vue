<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import {
  ElTree,
  ElCard,
  ElDescriptions,
  ElDescriptionsItem,
  ElIcon,
  ElEmpty,
  ElButton,
  ElDialog,
  ElInput,
  ElMessage,
  ElMessageBox
} from 'element-plus'
import { Folder, Document, FolderOpened, Plus, Edit, Delete, Upload } from '@element-plus/icons-vue'
import SyncStatus from './SyncStatus.vue'

interface FileInfo {
  id: string
  name: string
  type: 'file' | 'folder'
  path: string
  size?: number
  createdAt?: Date
  modifiedAt?: Date
  children?: FileInfo[]
  isLeaf: boolean
}

// 文件系统数据
const fileSystem = reactive<FileInfo[]>([])
// 当前选中的文件
const selectedFile = ref<FileInfo | null>(null)
// 加载状态
const isLoading = ref(true)
// 新文件夹对话框
const newFolderDialog = reactive({
  visible: false,
  name: '',
  parentPath: '/'
})
// 当前路径
const currentPath = ref('/')
// 树组件引用
const treeRef = ref<InstanceType<typeof ElTree> | null>(null)

// 处理文件系统更新事件
const handleFileSystemUpdate = (event: CustomEvent): void => {
  const updatedData = event.detail?.fileSystem || []
  // 更新文件系统数据
  fileSystem.length = 0
  fileSystem.push(...updatedData)
  isLoading.value = false
}

// 加载文件系统数据
const loadFileSystemData = async (): Promise<void> => {
  try {
    isLoading.value = true
    const data = await window.api.getFileSystemData()
    fileSystem.length = 0
    fileSystem.push(...data)
  } catch (error) {
    console.error('加载文件系统数据失败:', error)
  } finally {
    isLoading.value = false
  }
}

// 组件挂载时，加载文件系统数据并注册事件监听器
onMounted(async () => {
  // 加载初始数据
  await loadFileSystemData()

  // 添加文件系统更新事件监听器
  window.addEventListener('file-system-updated', handleFileSystemUpdate as any)
})

// 组件销毁时，移除事件监听器
onUnmounted(() => {
  window.removeEventListener('file-system-updated', handleFileSystemUpdate as any)
})

// 格式化文件大小
const formatFileSize = (bytes?: number): string => {
  if (bytes === undefined) return '-'

  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  if (bytes === 0) return '0 B'

  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${sizes[i]}`
}

// 格式化日期
const formatDate = (date?: Date): string => {
  if (!date) return '-'
  if (typeof date === 'string') {
    // 如果日期是字符串，转换为Date对象
    return new Date(date).toLocaleString('zh-CN')
  }
  return date.toLocaleString('zh-CN')
}

// 处理节点点击事件
const handleNodeClick = (data: FileInfo): void => {
  selectedFile.value = data
  if (data.type === 'folder') {
    currentPath.value = data.path
  } else {
    // 如果是文件，取其所在目录
    const pathParts = data.path.split('/')
    pathParts.pop() // 删除文件名
    currentPath.value = pathParts.join('/') || '/'
  }
}

// 处理节点右键菜单
const handleNodeContextMenu = (event: MouseEvent, node: any, data: FileInfo): void => {
  event.preventDefault()
  selectedFile.value = data
  // 在实际应用中可以显示上下文菜单
}

// 处理拖拽节点
const handleDragStart = (node: any, ev: DragEvent): void => {
  // 记录被拖拽的节点信息
  console.log('开始拖拽:', node.data.name)
}

// 判断是否允许拖放到指定节点
const allowDrop = (draggingNode: any, dropNode: any, type: 'prev' | 'inner' | 'next'): boolean => {
  // 只允许拖放到文件夹内部
  if (type === 'inner' && dropNode.data.type === 'folder') {
    return true
  }
  return false
}

// 处理拖放事件
const handleDrop = async (
  draggingNode: any,
  dropNode: any,
  type: 'prev' | 'inner' | 'next',
  ev: DragEvent
): Promise<void> => {
  if (type !== 'inner' || dropNode.data.type !== 'folder') {
    ElMessage.warning('只能将文件或文件夹拖放到文件夹内')
    return
  }

  const sourcePath = draggingNode.data.path
  const targetPath = dropNode.data.path

  console.log(`移动: ${sourcePath} -> ${targetPath}`)

  try {
    const result = await window.api.moveItem(sourcePath, targetPath)
    if (result.success) {
      ElMessage.success(`移动 ${draggingNode.data.name} 到 ${dropNode.data.name} 成功`)
      // 重新加载文件系统数据
      await loadFileSystemData()
    } else {
      ElMessage.error(`移动失败: ${result.error}`)
    }
  } catch (error) {
    ElMessage.error(`移动出错: ${error}`)
  }
}

// 打开新建文件夹对话框
const openNewFolderDialog = (): void => {
  newFolderDialog.visible = true
  newFolderDialog.name = ''
  newFolderDialog.parentPath =
    selectedFile.value?.type === 'folder' ? selectedFile.value.path : currentPath.value
}

// 创建新文件夹
const createNewFolder = async (): Promise<void> => {
  if (!newFolderDialog.name) {
    ElMessage.error('请输入名称')
    return
  }

  try {
    const result = await window.api.createFolder(newFolderDialog.parentPath, newFolderDialog.name)

    if (result.success) {
      ElMessage.success('文件夹创建成功')
      // 关闭对话框
      newFolderDialog.visible = false
    } else {
      ElMessage.error(`创建失败: ${result.error}`)
    }
  } catch (error) {
    ElMessage.error(`创建失败: ${error}`)
  }
}

// 打开文件选择对话框并上传文件
const uploadFile = async (): Promise<void> => {
  try {
    // 创建一个隐藏的文件输入元素
    const fileInput = document.createElement('input')
    fileInput.type = 'file'
    fileInput.style.display = 'none'
    document.body.appendChild(fileInput)

    // 监听选择文件事件
    fileInput.onchange = async (event) => {
      const files = (event.target as HTMLInputElement).files

      if (files && files.length > 0) {
        const file = files[0]

        // 显示正在上传的消息
        const loadingMessage = ElMessage({
          type: 'info',
          message: `正在上传文件: ${file.name}`,
          duration: 0
        })

        try {
          // 读取文件内容
          const reader = new FileReader()
          const fileContent = await new Promise<ArrayBuffer>((resolve, reject) => {
            reader.onload = () => {
              if (reader.result instanceof ArrayBuffer) {
                resolve(reader.result)
              } else {
                reject(new Error('Failed to read file as ArrayBuffer'))
              }
            }
            reader.onerror = () => reject(reader.error)
            reader.readAsArrayBuffer(file)
          })

          // 上传文件
          const result = await window.api.importFileFromBuffer(
            file.name,
            fileContent,
            currentPath.value
          )

          // 关闭加载消息
          loadingMessage.close()

          if (result.success) {
            ElMessage.success(`文件 ${file.name} 上传成功`)
            // 重新加载文件系统数据以显示新上传的文件
            await loadFileSystemData()
          } else {
            ElMessage.error(`文件上传失败: ${result.error}`)
          }
        } catch (error) {
          loadingMessage.close()
          ElMessage.error(`文件上传失败: ${error}`)
        }
      }

      // 清理文件输入元素
      document.body.removeChild(fileInput)
    }

    // 模拟点击
    fileInput.click()
  } catch (error) {
    ElMessage.error(`无法打开文件选择对话框: ${error}`)
  }
}

// 编辑文件
const editFile = (): void => {
  if (!selectedFile.value || selectedFile.value.type !== 'file') {
    ElMessage.warning('请先选择一个文件')
    return
  }

  // 目前显示开发中提示
  ElMessage({
    type: 'info',
    message: '编辑功能正在开发中'
  })
}

// 删除文件/文件夹
const deleteItem = async (): Promise<void> => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择一个文件或文件夹')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除${selectedFile.value.type === 'folder' ? '文件夹' : '文件'} "${selectedFile.value.name}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const result = await window.api.deleteItem(selectedFile.value.path)

    if (result.success) {
      ElMessage.success('删除成功')
      selectedFile.value = null
    } else {
      ElMessage.error(`删除失败: ${result.error}`)
    }
  } catch (error) {
    // 用户取消删除或发生错误
    if (error !== 'cancel') {
      ElMessage.error(`删除出错: ${error}`)
    }
  }
}
</script>

<template>
  <div class="file-explorer">
    <div class="explorer-content">
      <SyncStatus />

      <div class="file-actions">
        <el-button type="primary" size="small" @click="uploadFile" :icon="Upload">
          上传文件
        </el-button>
        <el-button type="primary" size="small" @click="openNewFolderDialog" :icon="Plus">
          新建文件夹
        </el-button>
      </div>

      <div class="current-path">
        当前路径: {{ currentPath }}
        <div class="path-tip"></div>
      </div>

      <div class="file-tree" :class="{ 'is-loading': isLoading }">
        <el-tree
          ref="treeRef"
          v-if="fileSystem.length > 0"
          :data="fileSystem"
          :props="{
            children: 'children',
            label: 'name',
            isLeaf: 'isLeaf'
          }"
          node-key="id"
          draggable
          :allow-drop="allowDrop"
          @node-drag-start="handleDragStart"
          @node-drop="handleDrop"
          @node-click="handleNodeClick"
          @node-contextmenu="handleNodeContextMenu"
          :default-expanded-keys="[]"
          :expand-on-click-node="false"
        >
          <template #default="{ node, data }">
            <span class="custom-node">
              <el-icon class="mr-1" v-if="data.type === 'folder' && !node.expanded">
                <Folder />
              </el-icon>
              <el-icon class="mr-1" v-else-if="data.type === 'folder' && node.expanded">
                <FolderOpened />
              </el-icon>
              <el-icon class="mr-1" v-else>
                <Document />
              </el-icon>
              <span>{{ node.label }}</span>
            </span>
          </template>
        </el-tree>

        <el-empty v-else-if="!isLoading" description="暂无文件数据"></el-empty>
        <div v-else class="loading-indicator">
          <span>加载中...</span>
        </div>
      </div>

      <el-card class="file-details" v-if="selectedFile" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>文件详情</span>
            <div class="card-actions">
              <el-button
                v-if="selectedFile.type === 'file'"
                type="primary"
                size="small"
                @click="editFile"
                :icon="Edit"
              >
                编辑
              </el-button>
              <el-button type="danger" size="small" @click="deleteItem" :icon="Delete">
                删除
              </el-button>
            </div>
          </div>
        </template>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="名称">{{ selectedFile.name }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{
            selectedFile.type === 'folder' ? '文件夹' : '文件'
          }}</el-descriptions-item>
          <el-descriptions-item label="路径">{{ selectedFile.path }}</el-descriptions-item>
          <el-descriptions-item v-if="selectedFile.type === 'file'" label="大小">{{
            formatFileSize(selectedFile.size)
          }}</el-descriptions-item>
          <el-descriptions-item v-if="selectedFile.type === 'file'" label="创建时间">{{
            formatDate(selectedFile.createdAt)
          }}</el-descriptions-item>
          <el-descriptions-item v-if="selectedFile.type === 'file'" label="修改时间">{{
            formatDate(selectedFile.modifiedAt)
          }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-empty v-else description="请选择一个文件或文件夹查看详情"></el-empty>
    </div>

    <!-- 新建文件夹对话框 -->
    <el-dialog v-model="newFolderDialog.visible" title="新建文件夹" width="30%">
      <el-input v-model="newFolderDialog.name" placeholder="请输入文件夹名称"></el-input>
      <p class="dialog-info">将在路径: {{ newFolderDialog.parentPath }} 下创建</p>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="newFolderDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="createNewFolder"> 确定 </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.file-explorer {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  box-shadow: var(--el-box-shadow-light);
  overflow: hidden;
}

.explorer-content {
  flex: 1;
  overflow: auto;
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 16px;
  position: relative;
}

.file-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.current-path {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
  padding: 4px;
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.path-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  opacity: 0.7;
}

.path-tip:hover {
  opacity: 1;
}

.file-tree {
  flex: 1;
  overflow: auto;
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 16px;
  position: relative;
  min-height: 200px;
}

.file-tree.is-loading {
  opacity: 0.7;
}

.loading-indicator {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: var(--el-text-color-secondary);
}

.file-details {
  margin-top: 16px;
}

.custom-node {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.mr-1 {
  margin-right: 8px;
}

:deep(.el-tree-node__content) {
  height: 32px;
}

:deep(.el-tree-node__content:hover) {
  background-color: var(--el-fill-color-light);
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: var(--el-color-primary-light-9);
}

:deep(.is-drop-inner > .el-tree-node__content) {
  background-color: var(--el-color-primary-light-8) !important;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

.card-actions {
  display: flex;
  gap: 8px;
}

.dialog-info {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 8px;
}
</style>
