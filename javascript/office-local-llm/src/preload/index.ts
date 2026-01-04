import { electronAPI } from '@electron-toolkit/preload'
import { contextBridge, ipcRenderer, IpcRendererEvent } from 'electron'

// 类型定义
interface ModelProgressCallback {
  (progress: number, downloaded: number, total: number): void
}

interface ChatResponse {
  success: boolean
  response?: string
  error?: string
}

interface StreamResponse {
  text: string
  segmentType: string | null
  isSegmentStart: boolean
  isSegmentEnd: boolean
}

interface StreamCompleteEvent {
  success: boolean
}

interface StreamErrorEvent {
  success: boolean
  error: string
}

interface ModelConfig {
  id: string
  name: string
  filename: string
  url: string
  size: string
  sizeBytes: number
  description?: string
}

// 文件系统节点接口
interface FileSystemNode {
  id: string
  name: string
  type: 'file' | 'folder'
  path: string
  size?: number
  createdAt?: Date
  modifiedAt?: Date
  children?: FileSystemNode[]
  isLeaf: boolean
}

type StreamResponseCallback = (response: StreamResponse) => void
type StreamCompleteCallback = (event: StreamCompleteEvent) => void
type StreamErrorCallback = (event: StreamErrorEvent) => void

// 自定义API
const api = {
  // 获取所有可用模型
  getAvailableModels: async (): Promise<ModelConfig[]> => {
    return await ipcRenderer.invoke('get-available-models')
  },

  // 获取已下载的模型
  getDownloadedModels: async (): Promise<string[]> => {
    return await ipcRenderer.invoke('get-downloaded-models')
  },

  // 获取当前选择的模型
  getCurrentModel: async (): Promise<ModelConfig> => {
    return await ipcRenderer.invoke('get-current-model')
  },

  // 设置当前模型
  setCurrentModel: async (modelId: string): Promise<boolean> => {
    return await ipcRenderer.invoke('set-current-model', modelId)
  },

  // 释放模型资源
  disposeModel: async (): Promise<{ success: boolean; error?: string }> => {
    return await ipcRenderer.invoke('dispose-model')
  },

  // 检查模型是否存在
  checkModelExists: async (modelId?: string): Promise<boolean> => {
    return await ipcRenderer.invoke('check-model-exists', modelId)
  },

  // 聊天功能
  chat: async (message: string): Promise<ChatResponse> => {
    return await ipcRenderer.invoke('chat', message)
  },

  // 流式聊天
  streamChat: (
    message: string,
    onResponse: StreamResponseCallback,
    onComplete: StreamCompleteCallback,
    onError: StreamErrorCallback
  ): void => {
    // 注册事件监听器
    const responseListener = (_event: IpcRendererEvent, data: StreamResponse): void => {
      onResponse(data)
    }

    const completeListener = (_event: IpcRendererEvent, data: StreamCompleteEvent): void => {
      // 清除所有监听器
      ipcRenderer.removeListener('llm-stream-response', responseListener)
      ipcRenderer.removeListener('llm-stream-complete', completeListener)
      ipcRenderer.removeListener('llm-stream-error', errorListener)
      onComplete(data)
    }

    const errorListener = (_event: IpcRendererEvent, data: StreamErrorEvent): void => {
      // 清除所有监听器
      ipcRenderer.removeListener('llm-stream-response', responseListener)
      ipcRenderer.removeListener('llm-stream-complete', completeListener)
      ipcRenderer.removeListener('llm-stream-error', errorListener)
      onError(data)
    }

    // 添加监听器
    ipcRenderer.on('llm-stream-response', responseListener)
    ipcRenderer.on('llm-stream-complete', completeListener)
    ipcRenderer.on('llm-stream-error', errorListener)

    // 发送流式聊天请求
    ipcRenderer.send('stream-chat', message)
  },

  // 中断流式聊天
  abortStreamChat: (): void => {
    ipcRenderer.send('abort-stream-chat')
  },

  // 下载模型
  downloadModel: async (
    modelId: string,
    callback: ModelProgressCallback
  ): Promise<{ success: boolean; error?: string }> => {
    // 注册进度监听器
    ipcRenderer.on('model-download-progress', (_event, data) => {
      callback(data.progress, data.downloaded, data.total)
    })

    return await ipcRenderer.invoke('download-model', modelId)
  },

  // 取消下载模型
  cancelModelDownload: async (): Promise<boolean> => {
    return await ipcRenderer.invoke('cancel-model-download')
  },

  // 删除模型
  deleteModel: async (modelId?: string): Promise<boolean> => {
    return await ipcRenderer.invoke('delete-model', modelId)
  },

  // 打开模型目录
  openModelsDirectory: async (): Promise<boolean> => {
    return await ipcRenderer.invoke('open-models-directory')
  },

  // 获取文件系统数据
  getFileSystemData: async (): Promise<FileSystemNode[]> => {
    return await ipcRenderer.invoke('get-file-system-data')
  },

  // 读取文件内容
  readFileContent: async (
    filePath: string
  ): Promise<{ success: boolean; content?: string; error?: string }> => {
    return await ipcRenderer.invoke('read-file-content', filePath)
  },

  // 强制从S3同步文件
  forceSyncFromS3: async (): Promise<{ success: boolean; error?: string }> => {
    return await ipcRenderer.invoke('force-sync-from-s3')
  },

  // 创建文件
  createFile: async (
    parentPath: string,
    fileName: string,
    content?: string
  ): Promise<{ success: boolean; error?: string }> => {
    return await ipcRenderer.invoke('create-file', parentPath, fileName, content)
  },

  // 创建文件夹
  createFolder: async (
    parentPath: string,
    folderName: string
  ): Promise<{ success: boolean; error?: string }> => {
    return await ipcRenderer.invoke('create-folder', parentPath, folderName)
  },

  // 删除文件或文件夹
  deleteItem: async (path: string): Promise<{ success: boolean; error?: string }> => {
    return await ipcRenderer.invoke('delete-item', path)
  },

  // 导入文件到同步目录
  importFile: async (
    sourcePath: string,
    targetDirectory: string
  ): Promise<{ success: boolean; error?: string }> => {
    return await ipcRenderer.invoke('import-file', sourcePath, targetDirectory)
  },

  // 从Buffer导入文件
  importFileFromBuffer: async (
    fileName: string,
    buffer: ArrayBuffer,
    targetDirectory: string
  ): Promise<{ success: boolean; error?: string }> => {
    return await ipcRenderer.invoke('import-file-from-buffer', fileName, buffer, targetDirectory)
  },

  // 移动文件或文件夹
  moveItem: async (
    sourcePath: string,
    targetDirectory: string
  ): Promise<{ success: boolean; error?: string }> => {
    return await ipcRenderer.invoke('move-item', sourcePath, targetDirectory)
  }
}

// Use `contextBridge` APIs to expose Electron APIs to
// renderer only if context isolation is enabled, otherwise
// just add to the DOM global.
// 添加文件系统更新事件监听器
ipcRenderer.on('file-system-updated', (_event, data) => {
  const detail = { fileSystem: data }
  window.dispatchEvent(new CustomEvent('file-system-updated', { detail }))
})

if (process.contextIsolated) {
  try {
    contextBridge.exposeInMainWorld('electron', electronAPI)
    contextBridge.exposeInMainWorld('api', api)
  } catch (error) {
    console.error(error)
  }
} else {
  // @ts-ignore (define in dts)
  window.electron = electronAPI
  // @ts-ignore (define in dts)
  window.api = api
}
