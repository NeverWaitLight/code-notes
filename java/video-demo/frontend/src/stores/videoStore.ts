import { defineStore } from 'pinia'
import type { Video } from '../types/video'
import type { ApiError } from '../types/api'
import { videoService } from '../services/videoService'

interface VideoState {
  items: Video[]
  loading: boolean
  error?: ApiError
}

export const useVideoStore = defineStore('video', {
  state: (): VideoState => ({
    items: [],
    loading: false,
    error: undefined,
  }),
  actions: {
    async fetchList() {
      this.loading = true
      this.error = undefined
      try {
        this.items = await videoService.list()
      } catch (err: any) {
        this.error = err?.error ?? { code: 'UNKNOWN', message: '加载失败' }
        this.items = []
      } finally {
        this.loading = false
      }
    },
  },
})
