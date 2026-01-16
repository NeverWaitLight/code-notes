import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUploadStore } from '../../src/stores/uploadStore'
import { videoService } from '../../src/services/videoService'

vi.mock('../../src/services/videoService')

describe('uploadStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('应该初始化空的上传列表', () => {
    const store = useUploadStore()
    expect(store.uploadingItems).toEqual([])
  })

  it('应该开始上传并更新进度', async () => {
    const store = useUploadStore()
    const file = new File(['test'], 'test.mp4', { type: 'video/mp4' })
    const title = '测试视频'
    const mockVideo = {
      id: 1,
      title,
      status: 'UPLOADING' as const,
      sizeBytes: 1000,
      createdAt: 1234567890,
    }

    let progressCallback: ((progress: number) => void) | undefined
    ;(videoService.upload as any).mockImplementation((f: File, t: string, onProgress?: (p: number) => void) => {
      progressCallback = onProgress
      return Promise.resolve(mockVideo)
    })

    const uploadPromise = store.startUpload(file, title)

    // 在上传过程中验证进度更新
    if (progressCallback) {
      progressCallback(50)
      expect(store.uploadingItems.length).toBe(1)
      const item = store.uploadingItems[0]
      expect(item.title).toBe(title)
      expect(item.progress).toBe(50)
      expect(item.status).toBe('uploading')
    }

    await uploadPromise

    // 上传成功后应该自动移除，符合 AC3：上传成功后该条目不再在上传页显示
    expect(store.uploadingItems.length).toBe(0)
    expect(videoService.upload).toHaveBeenCalledWith(file, title, expect.any(Function))
  })

  it('应该在上传过程中更新进度', async () => {
    const store = useUploadStore()
    const file = new File(['test'], 'test.mp4', { type: 'video/mp4' })
    const title = '测试视频'
    const mockVideo = {
      id: 1,
      title,
      status: 'UPLOADING' as const,
      sizeBytes: 1000,
      createdAt: 1234567890,
    }

    let progressCallback: ((progress: number) => void) | undefined
    ;(videoService.upload as any).mockImplementation((f: File, t: string, onProgress?: (p: number) => void) => {
      progressCallback = onProgress
      return new Promise((resolve) => {
        setTimeout(() => {
          if (progressCallback) {
            progressCallback(75)
          }
          setTimeout(() => resolve(mockVideo), 10)
        }, 10)
      })
    })

    const uploadPromise = store.startUpload(file, title)

    if (progressCallback) {
      progressCallback(25)
      // 验证进度已更新
      expect(store.uploadingItems.length).toBe(1)
      expect(store.uploadingItems[0].progress).toBe(25)
    }

    await uploadPromise

    // 上传成功后应该自动移除
    expect(store.uploadingItems.length).toBe(0)
  })

  it('应该处理上传失败', async () => {
    const store = useUploadStore()
    const file = new File(['test'], 'test.mp4', { type: 'video/mp4' })
    const title = '测试视频'
    const error = {
      error: { code: 'INVALID_MEDIA_TYPE', message: '非 H.264 MP4' },
    }

    ;(videoService.upload as any).mockRejectedValueOnce(error)

    await store.startUpload(file, title)

    expect(store.uploadingItems.length).toBe(1)
    const item = store.uploadingItems[0]
    expect(item.status).toBe('failed')
    expect(item.error).toBe('非 H.264 MP4')
  })

  it('应该移除上传项', () => {
    const store = useUploadStore()
    store.uploadingItems.push({
      id: 1,
      title: '测试',
      progress: 100,
      status: 'success',
    })

    store.removeUploadItem(1)

    expect(store.uploadingItems.length).toBe(0)
  })

  it('应该只返回状态为 uploading 的项', () => {
    const store = useUploadStore()
    store.uploadingItems = [
      { id: 1, title: '上传中', progress: 50, status: 'uploading' },
      { id: 2, title: '成功', progress: 100, status: 'success' },
      { id: 3, title: '失败', progress: 0, status: 'failed' },
      { id: 4, title: '上传中2', progress: 75, status: 'uploading' },
    ]

    const activeUploads = store.activeUploads

    expect(activeUploads.length).toBe(2)
    expect(activeUploads.every((item) => item.status === 'uploading')).toBe(true)
  })

  it('应该在上传成功后自动移除项', async () => {
    const store = useUploadStore()
    const file = new File(['test'], 'test.mp4', { type: 'video/mp4' })
    const title = '测试视频'
    const mockVideo = {
      id: 1,
      title,
      status: 'UPLOADING' as const,
      sizeBytes: 1000,
      createdAt: 1234567890,
    }

    ;(videoService.upload as any).mockResolvedValueOnce(mockVideo)

    await store.startUpload(file, title)

    // 上传成功后应该自动移除，符合 AC3：上传成功后该条目不再在上传页显示
    expect(store.uploadingItems.length).toBe(0)
  })
})
