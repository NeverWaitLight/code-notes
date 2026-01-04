import { electronApp, is, optimizer } from '@electron-toolkit/utils'
import { app, BrowserWindow, ipcMain, shell } from 'electron'
import { join } from 'path'
import icon from '../../resources/icon.png?asset'
import { getApplicationDir } from './config/path-config'
import { streamChat, dispose as disposeChat, abortStreamChat } from './llm/chat'
import {
  cancelModelDownload,
  checkModelExists,
  deleteModel,
  downloadModel,
  openModelsDirectory,
  getAvailableModels,
  getDownloadedModels,
  setCurrentModel,
  getCurrentModel,
  checkSpecificModelExists
} from './llm/model-manager'
import { initFileSystemWatcher, closeFileSystemWatcher } from './syncfs/syncfs'
import { S3Service } from './syncfs/s3'
import log from 'electron-log'

log.transports.file.format =
  '[{y}-{m}-{d} {h}:{i}:{s}.{ms}] [{level}] [{processType}] {scope} › {text}'
log.transports.console.format =
  '[{y}-{m}-{d} {h}:{i}:{s}.{ms}] [{level}] [{processType}] {scope} › {text}'

// 设置工作目录
app.setPath('userData', getApplicationDir())

// 主窗口引用
let mainWindow: BrowserWindow | null = null

/**
 * 创建主窗口
 */
function createWindow(): void {
  mainWindow = new BrowserWindow({
    width: 900,
    height: 670,
    show: false,
    autoHideMenuBar: true,
    title: 'Axi',
    ...(process.platform === 'linux' ? { icon } : {}),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false
    }
  })

  // 窗口准备好后显示并最大化
  mainWindow.on('ready-to-show', () => {
    mainWindow?.maximize() // 启动时直接最大化窗口
    mainWindow?.show()
    mainWindow?.setTitle('Axi')
  })

  // 处理窗口打开行为
  mainWindow.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url)
    return { action: 'deny' }
  })

  // 加载页面内容
  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    mainWindow.loadURL(process.env['ELECTRON_RENDERER_URL'])
  } else {
    mainWindow.loadFile(join(__dirname, '../renderer/index.html'))
  }

  // 初始化文件系统监控
  initFileSystemWatcher(mainWindow)

  // 初始化S3服务
  S3Service.init().catch((err) => log.error('初始化S3服务失败:', err))
}

/**
 * 注册IPC处理函数
 */
function registerIpcHandlers(): void {
  // 获取所有可用模型
  ipcMain.handle('get-available-models', () => {
    return getAvailableModels()
  })

  // 获取已下载的模型
  ipcMain.handle('get-downloaded-models', () => {
    return getDownloadedModels()
  })

  // 获取当前选择的模型
  ipcMain.handle('get-current-model', () => {
    return getCurrentModel()
  })

  // 设置当前模型
  ipcMain.handle('set-current-model', (_event, modelId: string) => {
    return setCurrentModel(modelId)
  })

  // 检查模型是否存在
  ipcMain.handle('check-model-exists', (_event, modelId?: string) => {
    if (modelId) {
      return checkSpecificModelExists(modelId)
    }
    return checkModelExists()
  })

  // 流式聊天功能
  ipcMain.on('stream-chat', async (event, message: string) => {
    try {
      log.info('Starting stream chat...')
      await streamChat(message, event)
      // 发送流式会话完成信号
      event.sender.send('llm-stream-complete', { success: true })
    } catch (error) {
      log.error('Stream chat error:', error)
      event.sender.send('llm-stream-error', {
        success: false,
        error: error instanceof Error ? error.message : 'Failed to process message'
      })
    }
  })

  // 中断流式聊天功能
  ipcMain.on('abort-stream-chat', () => {
    log.info('Received abort request from renderer')
    abortStreamChat()
  })

  // 释放模型资源
  ipcMain.handle('dispose-model', async () => {
    try {
      await disposeChat()
      return { success: true }
    } catch (error) {
      log.error('Failed to dispose model:', error)
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Failed to dispose model'
      }
    }
  })

  // 下载模型
  ipcMain.handle('download-model', async (_event, modelId: string) => {
    if (!mainWindow) return { success: false, error: 'Window not initialized' }

    try {
      await downloadModel(mainWindow, modelId, (progress, downloaded, total) => {
        // 发送进度更新
        log.info(`Download progress: ${Math.round(progress * 100)}%, ${downloaded}/${total} bytes`)
      })

      mainWindow.webContents.send('model-download-complete', {
        success: true,
        message: 'Model download completed'
      })

      return { success: true }
    } catch (error) {
      log.error('Failed to download model:', error)

      mainWindow.webContents.send('model-download-error', {
        success: false,
        error: error instanceof Error ? error.message : 'Download failed'
      })

      return {
        success: false,
        error: error instanceof Error ? error.message : 'Download failed'
      }
    }
  })

  // 取消模型下载
  ipcMain.handle('cancel-model-download', () => {
    try {
      cancelModelDownload()
      return true
    } catch (error) {
      log.error('Failed to cancel download:', error)
      return false
    }
  })

  // 删除模型
  ipcMain.handle('delete-model', (_event, modelId?: string) => {
    try {
      deleteModel(modelId)
      return true
    } catch (error) {
      log.error('Failed to delete model:', error)
      return false
    }
  })

  // 打开模型目录
  ipcMain.handle('open-models-directory', () => {
    try {
      openModelsDirectory()
      return true
    } catch (error) {
      log.error('Failed to open models directory:', error)
      return false
    }
  })
}

/**
 * 应用初始化
 */
app.whenReady().then(() => {
  // 设置应用名
  app.setName('Axi')

  // 设置应用ID
  electronApp.setAppUserModelId('com.axi.app')

  // 监听窗口快捷键
  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window)
  })

  // 创建主窗口
  createWindow()

  // 注册IPC处理函数
  registerIpcHandlers()

  // macOS下点击dock图标重新创建窗口
  app.on('activate', function () {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

/**
 * 应用关闭时的清理操作
 */
app.on('will-quit', async () => {
  try {
    // 执行资源清理
    await disposeChat()
    // 关闭文件系统监控
    closeFileSystemWatcher()
    // 关闭S3服务
    S3Service.close()
  } catch (error) {
    log.error('Failed to clean up resources:', error)
  }
})

/**
 * 所有窗口关闭时退出应用（macOS除外）
 */
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    log.info('All windows closed, preparing to quit application...')
    app.quit()
  }
})
