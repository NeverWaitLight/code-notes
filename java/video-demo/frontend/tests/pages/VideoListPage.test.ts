import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import VideoListPage from '../../src/pages/VideoListPage.vue'
import { useVideoStore } from '../../src/stores/videoStore'

vi.mock('../../src/stores/videoStore', () => ({
  useVideoStore: vi.fn(),
}))

describe('VideoListPage', () => {
  let router: ReturnType<typeof createRouter>
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/', component: VideoListPage },
        { path: '/play/:id', component: { template: '<div>Player</div>' } },
      ],
    })
    vi.clearAllMocks()
  })

  it('应该在挂载时调用 fetchList', () => {
    const mockFetchList = vi.fn()
    ;(useVideoStore as any).mockReturnValue({
      items: [],
      loading: false,
      error: undefined,
      fetchList: mockFetchList,
    })

    mount(VideoListPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    expect(mockFetchList).toHaveBeenCalledTimes(1)
  })

  it('应该显示加载状态', () => {
    ;(useVideoStore as any).mockReturnValue({
      items: [],
      loading: true,
      error: undefined,
      fetchList: vi.fn(),
    })

    const wrapper = mount(VideoListPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    expect(wrapper.text()).toContain('加载中...')
  })

  it('应该显示错误状态', () => {
    ;(useVideoStore as any).mockReturnValue({
      items: [],
      loading: false,
      error: { code: 'NETWORK_ERROR', message: '网络错误' },
      fetchList: vi.fn(),
    })

    const wrapper = mount(VideoListPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    expect(wrapper.text()).toContain('加载失败：网络错误')
  })

  it('应该显示空态', () => {
    ;(useVideoStore as any).mockReturnValue({
      items: [],
      loading: false,
      error: undefined,
      fetchList: vi.fn(),
    })

    const wrapper = mount(VideoListPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    expect(wrapper.text()).toContain('暂无视频，去上传一个吧')
  })

  it('应该显示视频列表', () => {
    const mockVideos = [
      { id: 1, title: '视频1', status: 'READY' as const, sizeBytes: 1000, createdAt: 1234567890 },
      { id: 2, title: '视频2', status: 'UPLOADING' as const, sizeBytes: 2000, createdAt: 1234567891 },
    ]

    ;(useVideoStore as any).mockReturnValue({
      items: mockVideos,
      loading: false,
      error: undefined,
      fetchList: vi.fn(),
    })

    const wrapper = mount(VideoListPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    expect(wrapper.text()).toContain('视频1')
    expect(wrapper.text()).toContain('视频2')
    expect(wrapper.text()).toContain('上传完成')
    expect(wrapper.text()).toContain('上传中')
  })

  it('应该正确显示状态中文', () => {
    const mockVideos = [
      { id: 1, title: '视频1', status: 'READY' as const, sizeBytes: 1000, createdAt: 1234567890 },
      { id: 2, title: '视频2', status: 'UPLOADING' as const, sizeBytes: 2000, createdAt: 1234567891 },
      { id: 3, title: '视频3', status: 'FAILED' as const, sizeBytes: 3000, createdAt: 1234567892 },
    ]

    ;(useVideoStore as any).mockReturnValue({
      items: mockVideos,
      loading: false,
      error: undefined,
      fetchList: vi.fn(),
    })

    const wrapper = mount(VideoListPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    expect(wrapper.text()).toContain('上传完成')
    expect(wrapper.text()).toContain('上传中')
    expect(wrapper.text()).toContain('失败')
  })

  it('点击列表项应该跳转到播放页', async () => {
    const mockVideos = [
      { id: 1, title: '视频1', status: 'READY' as const, sizeBytes: 1000, createdAt: 1234567890 },
    ]

    const pushSpy = vi.spyOn(router, 'push')

    ;(useVideoStore as any).mockReturnValue({
      items: mockVideos,
      loading: false,
      error: undefined,
      fetchList: vi.fn(),
    })

    const wrapper = mount(VideoListPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    const listItem = wrapper.find('.video-item')
    await listItem.trigger('click')

    expect(pushSpy).toHaveBeenCalledWith('/play/1')
  })
})
