import { describe, it, expect, vi, beforeEach } from 'vitest'
import { videoService } from '../../src/services/videoService'
import { apiClient } from '../../src/services/apiClient'

vi.mock('../../src/services/apiClient')

describe('videoService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应该调用 apiClient.request 获取视频列表', async () => {
    const mockVideos = [
      { id: 1, title: '视频1', status: 'READY', sizeBytes: 1000, createdAt: 1234567890 },
      { id: 2, title: '视频2', status: 'UPLOADING', sizeBytes: 2000, createdAt: 1234567891 },
    ]

    ;(apiClient.request as any).mockResolvedValueOnce(mockVideos)

    const result = await videoService.list()

    expect(apiClient.request).toHaveBeenCalledWith('/api/videos')
    expect(result).toEqual(mockVideos)
  })

  it('应该处理 API 调用错误', async () => {
    const error = { error: { code: 'NETWORK_ERROR', message: '网络错误' } }
    ;(apiClient.request as any).mockRejectedValueOnce(error)

    await expect(videoService.list()).rejects.toEqual(error)
  })

  describe('detail', () => {
    it('应该成功获取视频详情', async () => {
      const mockVideoDetail = {
        id: 1,
        title: '测试视频',
        status: 'READY' as const,
        sizeBytes: 1000,
        createdAt: 1234567890,
        manifestUrl: 'http://localhost:8080/media/1/index.m3u8',
      }

      ;(apiClient.request as any).mockResolvedValueOnce(mockVideoDetail)

      const result = await videoService.detail(1)

      expect(apiClient.request).toHaveBeenCalledWith('/api/videos/1')
      expect(result).toEqual(mockVideoDetail)
    })

    it('应该处理 VIDEO_NOT_FOUND 错误', async () => {
      const error = { error: { code: 'VIDEO_NOT_FOUND', message: '视频不存在' } }
      ;(apiClient.request as any).mockRejectedValueOnce(error)

      await expect(videoService.detail(999)).rejects.toEqual(error)
      expect(apiClient.request).toHaveBeenCalledWith('/api/videos/999')
    })

    it('应该处理 HLS_NOT_READY 错误', async () => {
      const error = { error: { code: 'HLS_NOT_READY', message: '视频未就绪' } }
      ;(apiClient.request as any).mockRejectedValueOnce(error)

      await expect(videoService.detail(1)).rejects.toEqual(error)
    })
  })

  describe('delete', () => {
    it('应该成功删除视频', async () => {
      ;(apiClient.request as any).mockResolvedValueOnce(undefined)

      await videoService.delete(1)

      expect(apiClient.request).toHaveBeenCalledWith('/api/videos/1', {
        method: 'DELETE',
      })
    })

    it('应该处理 VIDEO_NOT_FOUND 错误', async () => {
      const error = { error: { code: 'VIDEO_NOT_FOUND', message: '视频不存在' } }
      ;(apiClient.request as any).mockRejectedValueOnce(error)

      await expect(videoService.delete(999)).rejects.toEqual(error)
      expect(apiClient.request).toHaveBeenCalledWith('/api/videos/999', {
        method: 'DELETE',
      })
    })

    it('应该处理 STORAGE_IO_ERROR 错误', async () => {
      const error = { error: { code: 'STORAGE_IO_ERROR', message: '文件系统读写失败' } }
      ;(apiClient.request as any).mockRejectedValueOnce(error)

      await expect(videoService.delete(1)).rejects.toEqual(error)
    })
  })

  describe('upload', () => {
    let xhrInstance: any

    beforeEach(() => {
      xhrInstance = {
        open: vi.fn(),
        send: vi.fn(),
        setRequestHeader: vi.fn(),
        upload: {
          onprogress: null as any,
        },
        onload: null as any,
        onerror: null as any,
        status: 0,
        responseText: '',
        statusText: '',
      }

      class MockXMLHttpRequest {
        constructor() {
          return xhrInstance
        }
      }

      global.XMLHttpRequest = MockXMLHttpRequest as any
    })

    it('应该成功上传文件并返回 Video 对象', async () => {
      const file = new File(['test'], 'test.mp4', { type: 'video/mp4' })
      const title = '测试视频'
      const mockVideo = {
        id: 1,
        title,
        status: 'UPLOADING' as const,
        sizeBytes: 1000,
        createdAt: 1234567890,
      }

      xhrInstance.status = 201
      xhrInstance.responseText = JSON.stringify(mockVideo)

      const uploadPromise = videoService.upload(file, title)

      if (xhrInstance.onload) {
        xhrInstance.onload()
      }

      const result = await uploadPromise

      expect(xhrInstance.open).toHaveBeenCalledWith('POST', expect.stringContaining('/api/videos'))
      expect(xhrInstance.send).toHaveBeenCalled()
      expect(result).toEqual(mockVideo)
    })

    it('应该在上传过程中调用进度回调', async () => {
      const file = new File(['test'], 'test.mp4', { type: 'video/mp4' })
      const title = '测试视频'
      const mockVideo = {
        id: 1,
        title,
        status: 'UPLOADING' as const,
        sizeBytes: 1000,
        createdAt: 1234567890,
      }
      const progressCallback = vi.fn()

      xhrInstance.status = 201
      xhrInstance.responseText = JSON.stringify(mockVideo)

      const uploadPromise = videoService.upload(file, title, progressCallback)

      if (xhrInstance.upload.onprogress) {
        xhrInstance.upload.onprogress({
          lengthComputable: true,
          loaded: 50,
          total: 100,
        })
      }

      if (xhrInstance.onload) {
        xhrInstance.onload()
      }
      await uploadPromise

      expect(progressCallback).toHaveBeenCalledWith(50)
    })

    it('应该处理 INVALID_MEDIA_TYPE 错误', async () => {
      const file = new File(['test'], 'test.mp4', { type: 'video/mp4' })
      const title = '测试视频'

      xhrInstance.status = 400
      xhrInstance.responseText = JSON.stringify({
        error: { code: 'INVALID_MEDIA_TYPE', message: '非 H.264 MP4' },
      })

      const uploadPromise = videoService.upload(file, title)

      if (xhrInstance.onload) {
        xhrInstance.onload()
      }

      await expect(uploadPromise).rejects.toEqual({
        error: { code: 'INVALID_MEDIA_TYPE', message: '非 H.264 MP4' },
      })
    })

    it('应该处理 UPLOAD_TOO_LARGE 错误', async () => {
      const file = new File(['test'], 'test.mp4', { type: 'video/mp4' })
      const title = '测试视频'

      xhrInstance.status = 413
      xhrInstance.responseText = JSON.stringify({
        error: { code: 'UPLOAD_TOO_LARGE', message: '文件超过 5GB 限制' },
      })

      const uploadPromise = videoService.upload(file, title)

      if (xhrInstance.onload) {
        xhrInstance.onload()
      }

      await expect(uploadPromise).rejects.toEqual({
        error: { code: 'UPLOAD_TOO_LARGE', message: '文件超过 5GB 限制' },
      })
    })

    it('应该处理 STORAGE_IO_ERROR 错误', async () => {
      const file = new File(['test'], 'test.mp4', { type: 'video/mp4' })
      const title = '测试视频'

      xhrInstance.status = 500
      xhrInstance.responseText = JSON.stringify({
        error: { code: 'STORAGE_IO_ERROR', message: '文件系统读写失败' },
      })

      const uploadPromise = videoService.upload(file, title)

      if (xhrInstance.onload) {
        xhrInstance.onload()
      }

      await expect(uploadPromise).rejects.toEqual({
        error: { code: 'STORAGE_IO_ERROR', message: '文件系统读写失败' },
      })
    })

    it('应该处理网络错误', async () => {
      const file = new File(['test'], 'test.mp4', { type: 'video/mp4' })
      const title = '测试视频'

      const uploadPromise = videoService.upload(file, title)

      if (xhrInstance.onerror) {
        xhrInstance.onerror()
      }

      await expect(uploadPromise).rejects.toEqual({
        error: { code: 'NETWORK_ERROR', message: '网络请求失败' },
      })
    })

    it('应该处理无效的 JSON 响应', async () => {
      const file = new File(['test'], 'test.mp4', { type: 'video/mp4' })
      const title = '测试视频'

      xhrInstance.status = 201
      xhrInstance.responseText = 'invalid json'

      const uploadPromise = videoService.upload(file, title)

      if (xhrInstance.onload) {
        xhrInstance.onload()
      }

      await expect(uploadPromise).rejects.toEqual({
        error: { code: 'INVALID_RESPONSE', message: '服务器返回了无效的响应格式' },
      })
    })
  })
})
