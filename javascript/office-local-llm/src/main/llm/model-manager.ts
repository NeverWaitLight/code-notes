import * as childProcess from 'child_process'
import { BrowserWindow, dialog, shell } from 'electron'
import log from 'electron-log/main'
import * as fs from 'fs'
import * as http from 'http'
import * as https from 'https'
import * as os from 'os'
import * as path from 'path'
import * as url from 'url'
import {
  AVAILABLE_MODELS,
  ModelConfig,
  getDefaultModel,
  getModelById
} from '../config/model-config'
import { getUserModelsDir } from '../config/path-config'

// 当前选择的模型配置
let currentModelConfig: ModelConfig = getDefaultModel()

// 进度更新配置
const PROGRESS_UPDATE_CONFIG = {
  // 最小更新间隔 (毫秒)
  minInterval: 500,
  // 最小进度变化 (百分比)
  minProgressChange: 1
}

// 检查磁盘空间是否足够
const checkDiskSpace = async (requiredBytes: number): Promise<boolean> => {
  try {
    const modelsDir = getUserModelsDir()
    const dirPath = path.dirname(modelsDir)

    return new Promise<boolean>((resolve) => {
      const diskPath = os.platform() === 'win32' ? path.parse(dirPath).root : '/'

      if (os.platform() === 'win32') {
        // Windows命令
        childProcess.exec(
          `wmic logicaldisk where "DeviceID='${diskPath.replace('\\', '')}'" get freespace`,
          (error: Error | null, stdout: string) => {
            if (error) {
              log.error('Failed to check disk space:', error)
              resolve(false)
              return
            }

            const lines = stdout.trim().split('\n')
            if (lines.length >= 2) {
              const freeSpace = parseInt(lines[1].trim(), 10)
              resolve(freeSpace > requiredBytes)
            } else {
              resolve(false)
            }
          }
        )
      } else {
        // Unix/Linux/macOS命令
        childProcess.exec(
          `df -k "${diskPath}" | tail -1 | awk '{print $4}'`,
          (error: Error | null, stdout: string) => {
            if (error) {
              log.error('Failed to check disk space:', error)
              resolve(false)
              return
            }

            const freeSpace = parseInt(stdout.trim(), 10) * 1024 // 转换为字节
            resolve(freeSpace > requiredBytes)
          }
        )
      }
    })
  } catch (error) {
    log.error('Error checking disk space:', error)
    return false
  }
}

// 设置当前模型
export const setCurrentModel = (modelId: string): boolean => {
  const model = getModelById(modelId)
  if (model) {
    currentModelConfig = model
    return true
  }
  return false
}

// 获取当前模型配置
export const getCurrentModel = (): ModelConfig => {
  return currentModelConfig
}

// 获取所有可用模型
export const getAvailableModels = (): ModelConfig[] => {
  return AVAILABLE_MODELS
}

// 检查指定模型是否存在
export const checkSpecificModelExists = (modelId: string): boolean => {
  try {
    ensureModelsDir()
    const model = getModelById(modelId)
    if (!model) return false

    const modelPath = path.join(getUserModelsDir(), model.filename)
    return fs.existsSync(modelPath)
  } catch (error) {
    log.error('Failed to check model file:', error)
    return false
  }
}

// 获取已下载的模型ID列表
export const getDownloadedModels = (): string[] => {
  try {
    ensureModelsDir()
    const modelsDir = getUserModelsDir()
    const files = fs.readdirSync(modelsDir)

    return AVAILABLE_MODELS.filter((model) => files.includes(model.filename)).map(
      (model) => model.id
    )
  } catch (error) {
    log.error('Failed to get downloaded models:', error)
    return []
  }
}

// 模型文件路径
export const getModelPath = (): string => {
  return path.join(getUserModelsDir(), currentModelConfig.filename)
}

// 获取指定模型的路径
export const getSpecificModelPath = (modelId: string): string => {
  const model = getModelById(modelId)
  if (!model) {
    throw new Error(`Model with ID ${modelId} not found`)
  }
  return path.join(getUserModelsDir(), model.filename)
}

// 临时文件路径
const getTempPath = (): string => {
  return path.join(getUserModelsDir(), `${currentModelConfig.filename}.download`)
}

// 确保模型目录存在
const ensureModelsDir = (): void => {
  const modelsDir = getUserModelsDir()
  if (!fs.existsSync(modelsDir)) {
    fs.mkdirSync(modelsDir, { recursive: true })
  }
}

// 当前下载请求
let currentDownload: http.ClientRequest | null = null

// 检查当前选择的模型是否存在
export const checkModelExists = (): boolean => {
  try {
    ensureModelsDir()
    return fs.existsSync(getModelPath())
  } catch (error) {
    log.error('Failed to check model file:', error)
    return false
  }
}

// 获取模型目录
export const getModelsDirectory = (): string => {
  ensureModelsDir()
  return getUserModelsDir()
}

// 打开模型文件所在目录
export const openModelsDirectory = (): boolean => {
  try {
    const modelsDir = getUserModelsDir()
    ensureModelsDir()
    shell.openPath(modelsDir)
    return true
  } catch (error) {
    log.error('Failed to open models directory:', error)
    return false
  }
}

// 获取URL协议模块
const getProtocolModule = (urlString: string): typeof http | typeof https => {
  const parsedUrl = new url.URL(urlString)
  return parsedUrl.protocol === 'https:' ? https : http
}

// 下载模型
export const downloadModel = (
  win: BrowserWindow,
  modelId: string,
  progressCallback: (progress: number, downloaded: number, total: number) => void
): Promise<void> => {
  return new Promise((resolve, reject) => {
    try {
      // 设置当前模型
      if (!setCurrentModel(modelId)) {
        reject(new Error(`Model with ID ${modelId} not found`))
        return
      }

      // 获取文件大小
      const model = getModelById(modelId)
      if (!model) {
        reject(new Error(`Model with ID ${modelId} not found`))
        return
      }

      const fileSize = model.sizeBytes

      // 需要预留额外空间用于临时文件
      const requiredSpace = fileSize * 1.1 // 增加10%作为安全边界

      // 检查磁盘空间是否足够
      checkDiskSpace(requiredSpace)
        .then((hasEnoughSpace) => {
          if (!hasEnoughSpace) {
            // 显示警告对话框
            const formattedSize = (fileSize / (1024 * 1024)).toFixed(2)
            dialog.showMessageBox(win, {
              type: 'warning',
              title: '磁盘空间不足',
              message: '磁盘空间不足，无法下载模型',
              detail: `需要至少 ${formattedSize} MB 的可用空间来下载此模型。请释放磁盘空间后重试。`,
              buttons: ['确定']
            })

            reject(new Error('Insufficient disk space'))
            return
          }

          // 磁盘空间足够，继续下载流程
          ensureModelsDir()

          // 如果存在临时文件，先删除
          const tempPath = getTempPath()
          if (fs.existsSync(tempPath)) {
            fs.unlinkSync(tempPath)
          }

          // 创建写入流
          const fileStream = fs.createWriteStream(tempPath)

          // 确定使用的协议模块
          const protocolModule = getProtocolModule(currentModelConfig.url)

          // 进度更新控制变量
          let lastUpdateTime = 0
          let lastProgressPercent = 0

          // 发起下载请求
          currentDownload = protocolModule.get(currentModelConfig.url, (response) => {
            // 从响应头中获取文件大小
            const totalBytes = parseInt(response.headers['content-length'] || '0', 10)

            if (totalBytes === 0) {
              fileStream.close()
              fs.unlinkSync(tempPath)
              reject(new Error('Unable to get file size, please check if URL is valid'))
              return
            }

            log.info(`Starting model download, total size: ${totalBytes} bytes`)

            let downloadedBytes = 0

            response.on('data', (chunk) => {
              if (!response.complete) {
                downloadedBytes += chunk.length
                const progress = downloadedBytes / totalBytes
                const currentProgressPercent = Math.round(progress * 100)
                const now = Date.now()

                // 只在满足以下条件时才更新进度：
                // 1. 与上次更新的时间间隔大于最小间隔
                // 2. 与上次更新的进度百分比差值大于最小变化
                // 3. 在下载开始和结束时始终更新
                if (
                  now - lastUpdateTime > PROGRESS_UPDATE_CONFIG.minInterval ||
                  Math.abs(currentProgressPercent - lastProgressPercent) >=
                    PROGRESS_UPDATE_CONFIG.minProgressChange ||
                  currentProgressPercent === 0 ||
                  currentProgressPercent === 100
                ) {
                  if (!win.isDestroyed()) {
                    progressCallback(progress, downloadedBytes, totalBytes)
                    win.webContents.send('model-download-progress', {
                      progress,
                      downloaded: downloadedBytes,
                      total: totalBytes
                    })
                  }

                  lastUpdateTime = now
                  lastProgressPercent = currentProgressPercent
                }
              }
            })

            response.pipe(fileStream)

            fileStream.on('finish', () => {
              fileStream.close()

              if (response.complete) {
                // 下载完成后重命名文件
                fs.renameSync(tempPath, getModelPath())
                currentDownload = null
                resolve()
              } else {
                // 下载不完整
                if (fs.existsSync(tempPath)) {
                  fs.unlinkSync(tempPath)
                }
                reject(new Error('Download incomplete, please try again'))
              }
            })

            response.on('error', (error) => {
              fileStream.close()
              if (fs.existsSync(tempPath)) {
                fs.unlinkSync(tempPath)
              }
              currentDownload = null
              reject(error)
            })
          })

          currentDownload.on('error', (error) => {
            fileStream.close()

            // 发生错误时删除临时文件
            if (fs.existsSync(tempPath)) {
              fs.unlinkSync(tempPath)
            }

            currentDownload = null
            reject(error)
          })
        })
        .catch((error) => {
          log.error('Error checking disk space:', error)
          reject(new Error('Failed to check disk space'))
        })
    } catch (error) {
      currentDownload = null
      reject(error)
    }
  })
}

// 取消下载
export const cancelModelDownload = (): void => {
  if (currentDownload) {
    currentDownload.abort()
    currentDownload = null

    // 删除临时文件
    const tempPath = getTempPath()
    if (fs.existsSync(tempPath)) {
      fs.unlinkSync(tempPath)
    }
  }
}

// 删除模型
export const deleteModel = (modelId?: string): void => {
  try {
    let modelPath: string

    if (modelId) {
      // 删除指定模型
      modelPath = getSpecificModelPath(modelId)
    } else {
      // 删除当前模型
      modelPath = getModelPath()
    }

    if (fs.existsSync(modelPath)) {
      fs.unlinkSync(modelPath)
    }
  } catch (error) {
    log.error('Failed to delete model file:', error)
    throw error
  }
}
