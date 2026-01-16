import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useVideoStore } from '../../src/stores/videoStore'
import { videoService } from '../../src/services/videoService'

vi.mock('../../src/services/videoService')

describe('videoStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('初始状态应该为空', () => {
    const store = useVideoStore()
    expect(store.items).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.error).toBeUndefined()
  })

  it('fetchList 应该成功加载视频列表', async () => {
    const mockVideos = [
      { id: 1, title: '视频1', status: 'READY' as const, sizeBytes: 1000, createdAt: 1234567890 },
      { id: 2, title: '视频2', status: 'UPLOADING' as const, sizeBytes: 2000, createdAt: 1234567891 },
    ]

    ;(videoService.list as any).mockResolvedValueOnce(mockVideos)

    const store = useVideoStore()
    await store.fetchList()

    expect(store.items).toEqual(mockVideos)
    expect(store.loading).toBe(false)
    expect(store.error).toBeUndefined()
  })

  it('fetchList 应该在加载时设置 loading 状态', async () => {
    const mockVideos = [{ id: 1, title: '视频1', status: 'READY' as const, sizeBytes: 1000, createdAt: 1234567890 }]
    let resolvePromise: (value: any) => void

    ;(videoService.list as any).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolvePromise = resolve
        })
    )

    const store = useVideoStore()
    const fetchPromise = store.fetchList()

    expect(store.loading).toBe(true)
    expect(store.error).toBeUndefined()

    resolvePromise!(mockVideos)
    await fetchPromise

    expect(store.loading).toBe(false)
  })

  it('fetchList 应该处理错误并设置 error 状态', async () => {
    const error = { error: { code: 'NETWORK_ERROR', message: '网络错误' } }
    ;(videoService.list as any).mockRejectedValueOnce(error)

    const store = useVideoStore()
    await store.fetchList()

    expect(store.items).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.error).toEqual(error.error)
  })

  it('fetchList 应该处理未知错误', async () => {
    ;(videoService.list as any).mockRejectedValueOnce(new Error('未知错误'))

    const store = useVideoStore()
    await store.fetchList()

    expect(store.error).toEqual({ code: 'UNKNOWN', message: '加载失败' })
  })
})
