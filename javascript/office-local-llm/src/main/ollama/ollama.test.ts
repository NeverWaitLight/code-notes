import { test, expect, jest } from '@jest/globals'
import { OllamaChat } from './ollama'
import { Executor } from './executor'
import Ollama from 'ollama'

// 模拟Executor和Ollama
jest.mock('./executor')
jest.mock('ollama', () => {
  return {
    __esModule: true,
    default: {
      // @ts-ignore - 忽略config的受保护状态
      config: jest.fn(),
      chat: jest.fn()
    }
  }
})

describe('OllamaChat', () => {
  let ollamaChat: OllamaChat

  beforeEach(() => {
    jest.clearAllMocks()
    ollamaChat = new OllamaChat('testmodel', 'http://localhost:11434')
  })

  test('构造函数应正确初始化', () => {
    // @ts-ignore - 忽略config的受保护状态
    expect(Ollama.config).toHaveBeenCalledWith({
      host: 'http://localhost:11434'
    })
  })

  test('start方法应调用executor启动服务', async () => {
    // @ts-ignore - 使用any类型绕过类型检查
    const mockStartup = jest.fn().mockResolvedValue()
    // @ts-ignore - 使用any类型绕过类型检查
    Executor.mockImplementation(() => ({
      startup: mockStartup
    }))

    const chat = new OllamaChat()
    await chat.start()

    expect(mockStartup).toHaveBeenCalled()
  })

  test('chat方法应正确调用Ollama.chat', async () => {
    const mockResponse = {
      message: {
        content: '这是一个测试回复'
      }
    }
    // @ts-ignore - 使用mock绕过类型检查
    Ollama.chat.mockResolvedValue(mockResponse)

    const response = await ollamaChat.chat('测试问题', '系统提示')

    expect(Ollama.chat).toHaveBeenCalledWith(
      expect.objectContaining({
        model: 'testmodel',
        messages: [{ role: 'user', content: '测试问题' }],
        stream: false
      })
    )

    expect(response).toBe('这是一个测试回复')
  })

  test('setModel应更新模型名称', async () => {
    // 准备模拟返回值
    const mockResponse = { message: { content: '测试回复' } }
    // @ts-ignore - 使用mock绕过类型检查
    Ollama.chat.mockResolvedValue(mockResponse)

    ollamaChat.setModel('newmodel')

    // 调用并等待
    await ollamaChat.chat('测试')

    expect(Ollama.chat).toHaveBeenCalledWith(
      expect.objectContaining({
        model: 'newmodel'
      })
    )
  })
})
