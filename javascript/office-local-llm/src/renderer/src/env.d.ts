/// <reference types="vite/client" />

interface FileSystemNode {
  id: string
  name: string
  type: 'file' | 'folder'
  path: string
  size?: number
  createdAt?: Date | string
  modifiedAt?: Date | string
  children?: FileSystemNode[]
  isLeaf: boolean
}

interface CustomAPI {
  getAvailableModels: () => Promise<any[]>
  getDownloadedModels: () => Promise<string[]>
  getCurrentModel: () => Promise<any>
  setCurrentModel: (modelId: string) => Promise<boolean>
  disposeModel: () => Promise<{ success: boolean; error?: string }>
  checkModelExists: (modelId?: string) => Promise<boolean>
  chat: (message: string) => Promise<any>
  streamChat: (
    message: string,
    onResponse: (data: any) => void,
    onComplete: (data: any) => void,
    onError: (data: any) => void
  ) => void
  abortStreamChat: () => void
  downloadModel: (
    modelId: string,
    callback: (progress: number, downloaded: number, total: number) => void
  ) => Promise<{ success: boolean; error?: string }>
  cancelModelDownload: () => Promise<boolean>
  deleteModel: (modelId?: string) => Promise<boolean>
  openModelsDirectory: () => Promise<boolean>
  getFileSystemData: () => Promise<FileSystemNode[]>
  readFileContent: (
    filePath: string
  ) => Promise<{ success: boolean; content?: string; error?: string }>
}

declare global {
  interface Window {
    api: CustomAPI
  }
}
