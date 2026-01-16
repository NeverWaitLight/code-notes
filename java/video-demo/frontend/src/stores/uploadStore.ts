import { defineStore } from 'pinia'
import { videoService } from '../services/videoService'
import type { Video } from '../types/video'
import type { ApiError } from '../types/api'

export interface UploadItem {
  id: number
  title: string
  progress: number
  status: 'uploading' | 'success' | 'failed'
  error?: string
}

interface UploadState {
  uploadingItems: UploadItem[]
}

export const useUploadStore = defineStore('upload', {
  state: (): UploadState => ({
    uploadingItems: [],
  }),
  
  actions: {
    async startUpload(file: File, title: string) {
      const tempId = Date.now()
      const uploadItem: UploadItem = {
        id: tempId,
        title,
        progress: 0,
        status: 'uploading',
      }
      
      this.uploadingItems.push(uploadItem)
      
      try {
        const video = await videoService.upload(file, title, (progress) => {
          const item = this.uploadingItems.find((item) => item.id === tempId)
          if (item) {
            item.progress = progress
          }
        })
        
        // 上传成功后自动移除，符合 AC3：上传成功后该条目不再在上传页显示
        const itemIndex = this.uploadingItems.findIndex((item) => item.id === tempId)
        if (itemIndex !== -1) {
          this.uploadingItems.splice(itemIndex, 1)
        }
      } catch (err: any) {
        const item = this.uploadingItems.find((item) => item.id === tempId)
        if (item) {
          item.status = 'failed'
          const error = err?.error as ApiError | undefined
          item.error = error?.message || '上传失败'
        }
      }
    },
    
    removeUploadItem(id: number) {
      const index = this.uploadingItems.findIndex((item) => item.id === id)
      if (index !== -1) {
        this.uploadingItems.splice(index, 1)
      }
    },
  },
  
  getters: {
    activeUploads: (state): UploadItem[] => {
      return state.uploadingItems.filter((item) => item.status === 'uploading')
    },
  },
})
