import { IpcMainEvent } from 'electron'
import log from 'electron-log/main'
import type {
  Llama,
  LlamaChatSession as LlamaChatSessionType,
  LlamaContext,
  LlamaModel
} from 'node-llama-cpp'
import { getModelPath, getCurrentModel } from './model-manager'

// 创建一个缓存，用于保存各种实例
let llama: Llama | null = null
let modelInstance: LlamaModel | null = null
let sessionInstance: LlamaChatSessionType | null = null
let context: LlamaContext | null = null
// 记录当前加载的模型ID
let currentLoadedModelId: string | null = null
// 当前流式响应的中断控制器
let currentStreamAbortController: AbortController | null = null

/**
 * 清理资源，释放模型和会话实例
 * 应在应用关闭时调用此函数
 */
export async function dispose(): Promise<void> {
  try {
    if (sessionInstance) {
      sessionInstance.dispose()
      sessionInstance = null
    }

    if (context) {
      await context.dispose()
      context = null
    }

    if (modelInstance) {
      await modelInstance.dispose()
      modelInstance = null
    }

    if (llama) {
      await llama.dispose()
      llama = null
    }

    currentLoadedModelId = null
    log.info('LLM resources disposed successfully')
  } catch (error) {
    log.error('Error disposing LLM resources:', error)
  }
}

/**
 * 检查是否需要重新加载模型
 * 如果当前模型ID与加载的模型ID不同，则需要重新加载
 */
async function checkAndReloadModel(): Promise<void> {
  const currentModel = getCurrentModel()

  // 如果当前加载的模型与配置的模型不同，则需要释放资源并重新加载
  if (currentLoadedModelId !== currentModel.id) {
    log.info(
      `Model changed from ${currentLoadedModelId || 'none'} to ${currentModel.id}, reloading...`
    )
    await dispose()
  }
}

/**
 * 中断当前的流式聊天响应
 */
export function abortStreamChat(): void {
  if (currentStreamAbortController) {
    log.info('Aborting stream chat...')
    currentStreamAbortController.abort()
    currentStreamAbortController = null
  }
}

/**
 * 流式聊天
 */
export async function streamChat(input: string, event?: IpcMainEvent): Promise<void> {
  try {
    // 如果有之前的中断控制器，先中断它
    if (currentStreamAbortController) {
      currentStreamAbortController.abort()
    }

    // 创建新的中断控制器
    currentStreamAbortController = new AbortController()
    const { signal } = currentStreamAbortController

    // 检查是否需要重新加载模型
    await checkAndReloadModel()

    // 如果没有llama实例，则创建一个
    if (!llama) {
      // 动态导入node-llama-cpp
      const llamaModule = await import('node-llama-cpp')
      llama = await llamaModule.getLlama()
    }

    log.info('GPU Type: ', llama.gpu)

    // 如果已有模型实例，则重用，否则创建新的
    if (!modelInstance) {
      const currentModel = getCurrentModel()
      log.info(`Loading model: ${currentModel.name} (${currentModel.id})`)

      modelInstance = await llama.loadModel({
        modelPath: getModelPath() // 使用model-manager中的函数获取路径
      })

      // 记录当前加载的模型ID
      currentLoadedModelId = currentModel.id
    }

    // 如果已有会话实例，则重用，否则创建新的
    if (!sessionInstance) {
      context = await modelInstance.createContext()
      const { LlamaChatSession } = await import('node-llama-cpp')
      sessionInstance = new LlamaChatSession({
        contextSequence: context.getSequence()
      })
    }

    log.info('User input: ', input)
    log.info('AI: ')

    // 检查是否已经中断
    if (signal.aborted) {
      log.info('Stream chat aborted before starting')
      throw new Error('Stream chat aborted')
    }

    // 为中断信号添加事件监听器
    signal.addEventListener('abort', () => {
      log.info('Stream chat aborted during processing')
    })

    await sessionInstance.promptWithMeta(input, {
      onResponseChunk: (chunk) => {
        // 检查是否已中断
        if (signal.aborted) {
          return
        }

        if (chunk.type === 'segment' && chunk.segmentStartTime != null) {
          log.info(`[segment start: ${chunk.segmentType}]`)
        }

        log.info(chunk.text)

        // 通过IPC发送流式响应到渲染进程
        if (event && chunk.text) {
          event.sender.send('llm-stream-response', {
            text: chunk.text,
            segmentType: chunk.type === 'segment' ? chunk.segmentType : null,
            isSegmentStart: chunk.type === 'segment' && chunk.segmentStartTime != null,
            isSegmentEnd: chunk.type === 'segment' && chunk.segmentEndTime != null
          })
        }

        if (chunk.type === 'segment' && chunk.segmentEndTime != null) {
          log.info(`[segment end: ${chunk.segmentType}]`)
        }
      },
      signal // 传递中断信号
    })

    // 聊天完成后清除中断控制器
    currentStreamAbortController = null
  } catch (error: unknown) {
    // 如果是中断导致的错误，不需要抛出
    if (error instanceof Error && error.name === 'AbortError') {
      log.info('Stream chat aborted')
      return
    }

    log.error('Chat processing error:', error)
    throw error
  }
}
