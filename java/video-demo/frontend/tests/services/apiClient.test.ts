import { describe, it, expect, vi, beforeEach } from 'vitest'
import { apiClient } from '../../src/services/apiClient'

describe('apiClient', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    global.fetch = vi.fn()
  })

  it('应该成功返回 JSON 数据', async () => {
    const mockData = { id: 1, title: '测试视频' }
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => mockData,
    })

    const result = await apiClient.request('/api/videos')
    expect(result).toEqual(mockData)
  })

  it('应该处理 JSON 格式的错误响应', async () => {
    const errorResponse = {
      error: { code: 'VIDEO_NOT_FOUND', message: '视频不存在' },
    }
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 404,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => errorResponse,
    })

    await expect(apiClient.request('/api/videos/999')).rejects.toEqual({
      error: errorResponse.error,
    })
  })

  it('应该处理非 JSON 格式的错误响应', async () => {
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 500,
      headers: new Headers({ 'content-type': 'text/plain' }),
      text: async () => 'Internal Server Error',
    })

    await expect(apiClient.request('/api/videos')).rejects.toEqual({
      error: { code: 'INVALID_RESPONSE', message: 'Internal Server Error' },
    })
  })

  it('应该处理空文本响应', async () => {
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 500,
      headers: new Headers({ 'content-type': 'text/plain' }),
      text: async () => '',
    })

    await expect(apiClient.request('/api/videos')).rejects.toEqual({
      error: { code: 'INVALID_RESPONSE', message: '服务器返回了非 JSON 格式的响应' },
    })
  })

  it('应该处理网络错误', async () => {
    ;(global.fetch as any).mockRejectedValueOnce(new Error('网络连接失败'))

    await expect(apiClient.request('/api/videos')).rejects.toEqual({
      error: { code: 'NETWORK_ERROR', message: '网络连接失败' },
    })
  })

  it('应该处理未知错误', async () => {
    ;(global.fetch as any).mockRejectedValueOnce({})

    await expect(apiClient.request('/api/videos')).rejects.toEqual({
      error: { code: 'NETWORK_ERROR', message: '网络请求失败' },
    })
  })
})
