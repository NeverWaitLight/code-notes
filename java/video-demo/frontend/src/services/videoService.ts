import { apiClient } from './apiClient'
import type { Video, VideoDetail } from '../types/video'
import type { ApiErrorResponse } from '../types/api'

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

export interface UploadProgressCallback {
  (progress: number): void
}

export const videoService = {
  list: () => apiClient.request<Video[]>('/api/videos'),
  
  detail: (id: number): Promise<VideoDetail> => {
    return apiClient.request<VideoDetail>(`/api/videos/${id}`)
  },
  
  delete: (id: number): Promise<void> => {
    return apiClient.request<void>(`/api/videos/${id}`, {
      method: 'DELETE',
    })
  },
  
  upload: (
    file: File,
    title: string,
    onProgress?: UploadProgressCallback
  ): Promise<Video> => {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest()
      const formData = new FormData()
      formData.append('file', file)
      formData.append('title', title)

      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable && onProgress) {
          const progress = Math.round((event.loaded / event.total) * 100)
          onProgress(progress)
        }
      }

      xhr.onload = () => {
        if (xhr.status === 201) {
          try {
            const data = JSON.parse(xhr.responseText) as Video
            resolve(data)
          } catch (err) {
            reject({
              error: {
                code: 'INVALID_RESPONSE',
                message: '服务器返回了无效的响应格式',
              },
            })
          }
        } else {
          try {
            const errorData = JSON.parse(xhr.responseText) as ApiErrorResponse
            reject({ error: errorData?.error ?? { code: 'UNKNOWN', message: '上传失败' } })
          } catch (err) {
            reject({
              error: {
                code: 'UNKNOWN',
                message: xhr.statusText || '上传失败',
              },
            })
          }
        }
      }

      xhr.onerror = () => {
        reject({
          error: {
            code: 'NETWORK_ERROR',
            message: '网络请求失败',
          },
        })
      }

      xhr.open('POST', `${API_BASE}/api/videos`)
      xhr.send(formData)
    })
  },
}
