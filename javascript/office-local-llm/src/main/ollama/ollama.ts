import Ollama from 'ollama'
import Logger from 'electron-log/main'
import { ChatRequest, Message } from 'ollama'
import { Executor } from './executor'

const logger = Logger.scope('ollama chat')

/**
 * Ollama聊天客户端
 * 封装ollama-js包提供聊天功能
 */
export class OllamaChat {
  private executor: Executor
  private modelName: string
  private baseUrl: string

  /**
   * 创建Ollama聊天客户端
   * @param modelName 模型名称
   * @param baseUrl Ollama服务地址
   */
  constructor(modelName: string = 'llama3', baseUrl: string = 'http://localhost:11434') {
    this.modelName = modelName
    this.baseUrl = baseUrl
    this.executor = new Executor()

    // 配置Ollama客户端
    // @ts-ignore - Ollama的类型定义有问题，config实际上是可以调用的
    Ollama.config({
      host: this.baseUrl
    })
  }

  /**
   * 启动Ollama服务
   */
  async start(): Promise<void> {
    try {
      await this.executor.startup()
      logger.info(`Ollama服务已启动`)
    } catch (error) {
      logger.error(`启动Ollama服务失败:`, error)
      throw error
    }
  }

  /**
   * 执行单轮聊天
   * @param prompt 用户输入
   * @param systemPrompt 系统提示语
   * @returns 模型回复内容
   */
  async chat(prompt: string, systemPrompt?: string): Promise<string> {
    try {
      const messages: Message[] = [{ role: 'user', content: prompt }]

      const request = {
        model: this.modelName,
        messages: messages,
        stream: false
      } as ChatRequest & { stream?: false; system?: string }

      if (systemPrompt) {
        request.system = systemPrompt
      }

      logger.info(`发送聊天请求: ${prompt}`)
      const response = await Ollama.chat(request)
      logger.info(`收到聊天回复: ${response.message.content.substring(0, 100)}...`)

      return response.message.content
    } catch (error) {
      logger.error(`聊天失败:`, error)
      throw error
    }
  }

  /**
   * 执行多轮对话
   * @param messages 对话历史消息
   * @param systemPrompt 系统提示语
   * @returns 模型回复内容
   */
  async chatWithHistory(messages: Message[], systemPrompt?: string): Promise<Message> {
    try {
      const request = {
        model: this.modelName,
        messages: messages,
        stream: false
      } as ChatRequest & { stream?: false; system?: string }

      if (systemPrompt) {
        request.system = systemPrompt
      }

      logger.info(`发送多轮对话请求，共${messages.length}条消息`)
      const response = await Ollama.chat(request)
      logger.info(`收到多轮对话回复: ${response.message.content.substring(0, 100)}...`)

      return response.message
    } catch (error) {
      logger.error(`多轮对话失败:`, error)
      throw error
    }
  }

  /**
   * 使用流式输出进行聊天
   * @param prompt 用户输入
   * @param onProgress 流式输出回调函数
   * @param systemPrompt 系统提示语
   */
  async streamingChat(
    prompt: string,
    onProgress: (content: string) => void,
    systemPrompt?: string
  ): Promise<void> {
    try {
      const messages: Message[] = [{ role: 'user', content: prompt }]

      const request = {
        model: this.modelName,
        messages: messages,
        stream: true
      } as ChatRequest & { stream: true; system?: string }

      if (systemPrompt) {
        request.system = systemPrompt
      }

      logger.info(`发送流式聊天请求: ${prompt}`)

      const stream = await Ollama.chat(request)
      for await (const part of stream) {
        if (part.message?.content) {
          onProgress(part.message.content)
        }
      }

      logger.info(`流式聊天完成`)
    } catch (error) {
      logger.error(`流式聊天失败:`, error)
      throw error
    }
  }

  /**
   * 修改当前使用的模型
   * @param modelName 新的模型名称
   */
  setModel(modelName: string): void {
    this.modelName = modelName
    logger.info(`切换模型: ${modelName}`)
  }
}

// 导出默认实例方便直接使用
export default new OllamaChat()
