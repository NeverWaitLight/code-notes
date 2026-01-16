import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { nextTick } from 'vue'
import PlayerPage from '../../src/pages/PlayerPage.vue'
import { videoService } from '../../src/services/videoService'
import { useVideoStore } from '../../src/stores/videoStore'
import Hls from 'hls.js'

vi.mock('../../src/services/videoService')
vi.mock('../../src/stores/videoStore')

const { mockHlsData, createMockHls } = vi.hoisted(() => {
  const createMockHls = () => {
    const instance = {
      loadSource: vi.fn(),
      attachMedia: vi.fn(),
      on: vi.fn(),
      destroy: vi.fn(),
    }
    
    function MockHls() {
      return instance
    }
    
    MockHls.isSupported = vi.fn(() => true)
    MockHls.Events = {
      ERROR: 'hlsError',
    }
    MockHls.ErrorTypes = {
      NETWORK_ERROR: 'networkError',
      MEDIA_ERROR: 'mediaError',
    }
    
    const constructor = vi.fn(MockHls)
    constructor.isSupported = MockHls.isSupported
    constructor.Events = MockHls.Events
    constructor.ErrorTypes = MockHls.ErrorTypes
    
    return { instance, constructor }
  }
  
  const mockHlsData = createMockHls()
  
  return { mockHlsData, createMockHls }
})

vi.mock('hls.js', () => ({
  default: mockHlsData.constructor,
}))

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: { template: '<div>List</div>' } },
    { path: '/play/:id', component: PlayerPage },
  ],
})

describe('PlayerPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockHlsData.instance.loadSource.mockClear()
    mockHlsData.instance.attachMedia.mockClear()
    mockHlsData.instance.on.mockClear()
    mockHlsData.instance.destroy.mockClear()
    mockHlsData.constructor.mockClear()
    mockHlsData.constructor.isSupported.mockReturnValue(true)
    window.confirm = vi.fn(() => true)
  })

  it('应该从路由参数获取视频 ID 并加载详情', async () => {
    const mockVideoDetail = {
      id: 1,
      title: '测试视频',
      status: 'READY' as const,
      sizeBytes: 1000,
      createdAt: 1234567890,
      manifestUrl: 'http://localhost:8080/media/1/index.m3u8',
    }

    ;(videoService.detail as any).mockResolvedValueOnce(mockVideoDetail)
    ;(useVideoStore as any).mockReturnValue({
      fetchList: vi.fn().mockResolvedValue(undefined),
    })

    await router.push('/play/1')
    const wrapper = mount(PlayerPage, {
      global: {
        plugins: [router],
      },
    })

    await new Promise((resolve) => setTimeout(resolve, 0))

    expect(videoService.detail).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('测试视频')
  })

  it('应该显示加载状态', async () => {
    ;(videoService.detail as any).mockImplementation(
      () => new Promise(() => {})
    )
    ;(useVideoStore as any).mockReturnValue({
      fetchList: vi.fn(),
    })

    await router.push('/play/1')
    const wrapper = mount(PlayerPage, {
      global: {
        plugins: [router],
      },
    })

    expect(wrapper.text()).toContain('加载中')
  })

  it('应该显示错误信息', async () => {
    const error = { error: { code: 'VIDEO_NOT_FOUND', message: '视频不存在' } }
    ;(videoService.detail as any).mockRejectedValueOnce(error)
    ;(useVideoStore as any).mockReturnValue({
      fetchList: vi.fn(),
    })

    await router.push('/play/1')
    const wrapper = mount(PlayerPage, {
      global: {
        plugins: [router],
      },
    })

    await new Promise((resolve) => setTimeout(resolve, 0))

    expect(wrapper.text()).toContain('错误：视频不存在')
  })

  it('应该显示未就绪占位', async () => {
    const mockVideoDetail = {
      id: 1,
      title: '测试视频',
      status: 'UPLOADING' as const,
      sizeBytes: 1000,
      createdAt: 1234567890,
      manifestUrl: '',
    }

    ;(videoService.detail as any).mockResolvedValueOnce(mockVideoDetail)
    ;(useVideoStore as any).mockReturnValue({
      fetchList: vi.fn(),
    })

    await router.push('/play/1')
    const wrapper = mount(PlayerPage, {
      global: {
        plugins: [router],
      },
    })

    await new Promise((resolve) => setTimeout(resolve, 0))

    expect(wrapper.text()).toContain('视频处理中，暂不可播放')
  })

  it('应该初始化 hls.js 播放器当视频就绪时', async () => {
    const mockVideoDetail = {
      id: 1,
      title: '测试视频',
      status: 'READY' as const,
      sizeBytes: 1000,
      createdAt: 1234567890,
      manifestUrl: 'http://localhost:8080/media/1/index.m3u8',
    }

    ;(videoService.detail as any).mockResolvedValueOnce(mockVideoDetail)
    ;(useVideoStore as any).mockReturnValue({
      fetchList: vi.fn(),
    })
    mockHlsData.constructor.isSupported.mockReturnValue(true)

    await router.push('/play/1')
    const wrapper = mount(PlayerPage, {
      global: {
        plugins: [router],
      },
    })

    await flushPromises()
    await nextTick()
    await nextTick()

    expect(mockHlsData.constructor).toHaveBeenCalled()
    expect(mockHlsData.instance.loadSource).toHaveBeenCalledWith(mockVideoDetail.manifestUrl)
    expect(mockHlsData.instance.attachMedia).toHaveBeenCalled()
  })

  it('应该处理删除确认和成功删除', async () => {
    const mockVideoDetail = {
      id: 1,
      title: '测试视频',
      status: 'READY' as const,
      sizeBytes: 1000,
      createdAt: 1234567890,
      manifestUrl: 'http://localhost:8080/media/1/index.m3u8',
    }

    const mockFetchList = vi.fn().mockResolvedValue(undefined)
    ;(videoService.detail as any).mockResolvedValueOnce(mockVideoDetail)
    ;(videoService.delete as any).mockResolvedValueOnce(undefined)
    ;(useVideoStore as any).mockReturnValue({
      fetchList: mockFetchList,
    })
    ;(window.confirm as any).mockReturnValue(true)

    await router.push('/play/1')
    const wrapper = mount(PlayerPage, {
      global: {
        plugins: [router],
      },
    })

    await new Promise((resolve) => setTimeout(resolve, 0))

    const deleteButton = wrapper.find('button')
    await deleteButton.trigger('click')

    await new Promise((resolve) => setTimeout(resolve, 0))

    expect(window.confirm).toHaveBeenCalledWith('确定删除该视频吗？删除后不可恢复')
    expect(videoService.delete).toHaveBeenCalledWith(1)
    expect(mockFetchList).toHaveBeenCalled()
  })

  it('应该处理删除失败', async () => {
    const mockVideoDetail = {
      id: 1,
      title: '测试视频',
      status: 'READY' as const,
      sizeBytes: 1000,
      createdAt: 1234567890,
      manifestUrl: 'http://localhost:8080/media/1/index.m3u8',
    }

    const error = { error: { code: 'STORAGE_IO_ERROR', message: '删除失败' } }
    ;(videoService.detail as any).mockResolvedValueOnce(mockVideoDetail)
    ;(videoService.delete as any).mockRejectedValueOnce(error)
    ;(useVideoStore as any).mockReturnValue({
      fetchList: vi.fn(),
    })
    ;(window.confirm as any).mockReturnValue(true)

    await router.push('/play/1')
    const wrapper = mount(PlayerPage, {
      global: {
        plugins: [router],
      },
    })

    await new Promise((resolve) => setTimeout(resolve, 0))

    const deleteButton = wrapper.find('button')
    await deleteButton.trigger('click')

    await new Promise((resolve) => setTimeout(resolve, 0))

    expect(wrapper.text()).toContain('错误：删除失败')
  })

  it('应该取消删除当用户取消确认', async () => {
    const mockVideoDetail = {
      id: 1,
      title: '测试视频',
      status: 'READY' as const,
      sizeBytes: 1000,
      createdAt: 1234567890,
      manifestUrl: 'http://localhost:8080/media/1/index.m3u8',
    }

    ;(videoService.detail as any).mockResolvedValueOnce(mockVideoDetail)
    ;(useVideoStore as any).mockReturnValue({
      fetchList: vi.fn(),
    })
    ;(window.confirm as any).mockReturnValue(false)

    await router.push('/play/1')
    const wrapper = mount(PlayerPage, {
      global: {
        plugins: [router],
      },
    })

    await new Promise((resolve) => setTimeout(resolve, 0))

    const deleteButton = wrapper.find('button')
    await deleteButton.trigger('click')

    expect(window.confirm).toHaveBeenCalled()
    expect(videoService.delete).not.toHaveBeenCalled()
  })

  it('应该在组件卸载时清理 hls.js 实例', async () => {
    const mockVideoDetail = {
      id: 1,
      title: '测试视频',
      status: 'READY' as const,
      sizeBytes: 1000,
      createdAt: 1234567890,
      manifestUrl: 'http://localhost:8080/media/1/index.m3u8',
    }

    ;(videoService.detail as any).mockResolvedValueOnce(mockVideoDetail)
    ;(useVideoStore as any).mockReturnValue({
      fetchList: vi.fn(),
    })
    mockHlsData.constructor.isSupported.mockReturnValue(true)

    await router.push('/play/1')
    const wrapper = mount(PlayerPage, {
      global: {
        plugins: [router],
      },
    })

    await flushPromises()
    await nextTick()
    await nextTick()

    wrapper.unmount()
    await nextTick()

    expect(mockHlsData.instance.destroy).toHaveBeenCalled()
  })
})
