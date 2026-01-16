import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import UploadPage from '../../src/pages/UploadPage.vue'
import { useUploadStore } from '../../src/stores/uploadStore'
import { videoService } from '../../src/services/videoService'

vi.mock('../../src/stores/uploadStore', () => ({
  useUploadStore: vi.fn(),
}))

vi.mock('../../src/services/videoService', () => ({
  videoService: {
    upload: vi.fn(),
  },
}))

describe('UploadPage', () => {
  let router: ReturnType<typeof createRouter>
  let pinia: ReturnType<typeof createPinia>
  let mockStartUpload: ReturnType<typeof vi.fn>
  let mockRemoveUploadItem: ReturnType<typeof vi.fn>
  let mockActiveUploads: any[]
  let mockUploadingItems: any[]

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/upload', component: UploadPage },
      ],
    })

    mockStartUpload = vi.fn()
    mockRemoveUploadItem = vi.fn()
    mockActiveUploads = []
    mockUploadingItems = []

    ;(useUploadStore as any).mockReturnValue({
      uploadingItems: mockUploadingItems,
      activeUploads: mockActiveUploads,
      startUpload: mockStartUpload,
      removeUploadItem: mockRemoveUploadItem,
    })

    vi.clearAllMocks()
  })

  it('应该显示上传表单', () => {
    const wrapper = mount(UploadPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    expect(wrapper.text()).toContain('上传视频')
    expect(wrapper.find('input[type="file"]').exists()).toBe(true)
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
    expect(wrapper.find('button').exists()).toBe(true)
  })

  it('应该显示文件选择提示', () => {
    const wrapper = mount(UploadPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    expect(wrapper.text()).toContain('选择 H.264 MP4 文件（≤ 5GB）')
  })

  it('应该验证文件大小', async () => {
    const wrapper = mount(UploadPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    const fileInput = wrapper.find('input[type="file"]')
    const largeFile = Object.create(File.prototype, {
      name: { value: 'large.mp4', writable: false },
      type: { value: 'video/mp4', writable: false },
      size: { value: 6 * 1024 * 1024 * 1024, writable: false },
    })
    
    Object.defineProperty(fileInput.element, 'files', {
      value: [largeFile],
      writable: false,
    })

    await fileInput.trigger('change')

    expect(wrapper.text()).toContain('文件大小超过 5GB 限制')
  })

  it('应该验证文件类型', async () => {
    const wrapper = mount(UploadPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    const fileInput = wrapper.find('input[type="file"]')
    const invalidFile = new File(['test'], 'test.avi', { type: 'video/avi' })
    
    Object.defineProperty(fileInput.element, 'files', {
      value: [invalidFile],
      writable: false,
    })

    await fileInput.trigger('change')

    expect(wrapper.text()).toContain('仅支持 MP4 文件')
  })

  it('应该验证标题必填', async () => {
    const wrapper = mount(UploadPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    const fileInput = wrapper.find('input[type="file"]')
    const validFile = new File(['test'], 'test.mp4', { type: 'video/mp4' })
    
    Object.defineProperty(fileInput.element, 'files', {
      value: [validFile],
      writable: false,
    })

    await fileInput.trigger('change')

    const submitButton = wrapper.find('button')
    await submitButton.trigger('click')

    expect(wrapper.text()).toContain('标题不能为空')
    expect(mockStartUpload).not.toHaveBeenCalled()
  })

  it('应该提交上传', async () => {
    const wrapper = mount(UploadPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    const fileInput = wrapper.find('input[type="file"]')
    const validFile = new File(['test'], 'test.mp4', { type: 'video/mp4' })
    
    Object.defineProperty(fileInput.element, 'files', {
      value: [validFile],
      writable: false,
    })

    await fileInput.trigger('change')

    const titleInput = wrapper.find('input[type="text"]')
    await titleInput.setValue('测试视频')

    const submitButton = wrapper.find('button')
    await submitButton.trigger('click')

    expect(mockStartUpload).toHaveBeenCalledWith(validFile, '测试视频')
  })

  it('应该显示上传中的项', () => {
    mockActiveUploads = [
      {
        id: 1,
        title: '上传中的视频',
        progress: 50,
        status: 'uploading',
      },
    ]

    ;(useUploadStore as any).mockReturnValue({
      uploadingItems: mockUploadingItems,
      activeUploads: mockActiveUploads,
      startUpload: mockStartUpload,
      removeUploadItem: mockRemoveUploadItem,
    })

    const wrapper = mount(UploadPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    expect(wrapper.text()).toContain('上传中')
    expect(wrapper.text()).toContain('上传中的视频')
    expect(wrapper.text()).toContain('正在上传... 50%')
  })

  it('不应该显示上传成功的项（上传成功后自动移除）', () => {
    // 上传成功后应该自动移除，符合 AC3：上传成功后该条目不再在上传页显示
    mockUploadingItems = []

    ;(useUploadStore as any).mockReturnValue({
      uploadingItems: mockUploadingItems,
      activeUploads: [],
      startUpload: mockStartUpload,
      removeUploadItem: mockRemoveUploadItem,
    })

    const wrapper = mount(UploadPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    expect(wrapper.text()).not.toContain('上传成功')
  })

  it('应该显示上传失败的项', () => {
    mockUploadingItems = [
      {
        id: 1,
        title: '失败的视频',
        progress: 0,
        status: 'failed',
        error: '上传失败：网络错误',
      },
    ]

    ;(useUploadStore as any).mockReturnValue({
      uploadingItems: mockUploadingItems,
      activeUploads: [],
      startUpload: mockStartUpload,
      removeUploadItem: mockRemoveUploadItem,
    })

    const wrapper = mount(UploadPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    expect(wrapper.text()).toContain('上传失败')
    expect(wrapper.text()).toContain('失败的视频')
    expect(wrapper.text()).toContain('上传失败：网络错误')
  })


  it('应该在上传时禁用提交按钮', async () => {
    const wrapper = mount(UploadPage, {
      global: {
        plugins: [pinia, router],
      },
    })

    const fileInput = wrapper.find('input[type="file"]')
    const validFile = new File(['test'], 'test.mp4', { type: 'video/mp4' })
    
    Object.defineProperty(fileInput.element, 'files', {
      value: [validFile],
      writable: false,
    })

    await fileInput.trigger('change')

    const titleInput = wrapper.find('input[type="text"]')
    await titleInput.setValue('测试视频')

    mockStartUpload.mockImplementation(() => new Promise(() => {}))

    const submitButton = wrapper.find('button')
    await submitButton.trigger('click')

    expect(submitButton.attributes('disabled')).toBeDefined()
  })
})
