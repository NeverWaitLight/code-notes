import { ElectronAPI } from '@electron-toolkit/preload'

interface ModelProgressCallback {
  (progress: number, downloaded: number, total: number): void
}

interface DownloadResult {
  success: boolean
  error?: string
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

interface CustomAPI {
  getAvailableModels: () => Promise<ModelConfig[]>
  getDownloadedModels: () => Promise<string[]>
  getCurrentModel: () => Promise<ModelConfig>
  setCurrentModel: (modelId: string) => Promise<boolean>
  disposeModel: () => Promise<{ success: boolean; error?: string }>
  checkModelExists: (modelId?: string) => Promise<boolean>
  chat: (message: string) => Promise<ChatResponse>
  streamChat: (
    message: string,
    onResponse: StreamResponseCallback,
    onComplete: StreamCompleteCallback,
    onError: StreamErrorCallback
  ) => void
  abortStreamChat: () => void
  downloadModel: (modelId: string, callback: ModelProgressCallback) => Promise<DownloadResult>
  cancelModelDownload: () => Promise<boolean>
  deleteModel: (modelId?: string) => Promise<boolean>
  openModelsDirectory: () => Promise<boolean>
  getFileSystemData: () => Promise<FileSystemNode[]>
  readFileContent: (
    filePath: string
  ) => Promise<{ success: boolean; content?: string; error?: string }>
  forceSyncFromS3: () => Promise<{ success: boolean; error?: string }>
  createFile: (
    parentPath: string,
    fileName: string,
    content?: string
  ) => Promise<{ success: boolean; error?: string }>
  createFolder: (
    parentPath: string,
    folderName: string
  ) => Promise<{ success: boolean; error?: string }>
  deleteItem: (path: string) => Promise<{ success: boolean; error?: string }>
  importFile: (
    sourcePath: string,
    targetDirectory: string
  ) => Promise<{ success: boolean; error?: string }>
  importFileFromBuffer: (
    fileName: string,
    buffer: ArrayBuffer,
    targetDirectory: string
  ) => Promise<{ success: boolean; error?: string }>
  moveItem: (
    sourcePath: string,
    targetDirectory: string
  ) => Promise<{ success: boolean; error?: string }>
}

declare global {
  interface Window {
    electron: ElectronAPI
    api: CustomAPI
  }
}
