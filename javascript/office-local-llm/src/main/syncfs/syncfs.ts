import chokidar, { FSWatcher } from 'chokidar'
import { BrowserWindow, ipcMain } from 'electron'
import log from 'electron-log'
import fs from 'fs'
import path from 'path'
import { getUserFilesDir } from '../config/path-config'
import { S3Service } from './s3'

// 文件系统监控实例
let watcher: FSWatcher | null = null
// 缓存的文件系统数据
let fileSystemData: FileSystemNode[] = []
// 文件服务器同步定时器
let syncTimer: NodeJS.Timeout | null = null

// 文件系统节点接口
interface FileSystemNode {
  id: string
  name: string
  type: 'file' | 'folder'
  path: string
  size?: number
  createdAt?: Date
  modifiedAt?: Date
  children?: FileSystemNode[]
  isLeaf: boolean
}

/**
 * 初始化文件系统监控
 * @param mainWindow 主窗口
 */
export function initFileSystemWatcher(mainWindow: BrowserWindow): void {
  const userFilesDir = getUserFilesDir()

  try {
    // 确保文件目录存在
    if (!fs.existsSync(userFilesDir)) {
      log.info(`创建文件目录: ${userFilesDir}`)
      fs.mkdirSync(userFilesDir, { recursive: true })
    }

    // 开始监控文件系统
    watcher = chokidar.watch(userFilesDir, {
      ignored: /(^|[/\\])\./, // 忽略点文件
      persistent: true, // 持续监控
      ignoreInitial: true, // 忽略初始文件
      awaitWriteFinish: {
        stabilityThreshold: 3000, // 等待文件写入完成的时间
        pollInterval: 1000 // 轮询间隔
      }
    })

    // 监听文件系统事件
    watcher
      .on('ready', () => {
        updateFileSystemData(mainWindow)
        syncAllFiles(userFilesDir)
      })
      .on('add', (filePath) => {
        updateFileSystemData(mainWindow)
        syncFile(filePath, userFilesDir)
      })
      .on('addDir', () => {
        updateFileSystemData(mainWindow)
      })
      .on('change', (filePath) => {
        updateFileSystemData(mainWindow)
        syncFile(filePath, userFilesDir)
      })
      .on('unlink', (filePath) => {
        updateFileSystemData(mainWindow)
        deleteFile(filePath, userFilesDir)
      })
      .on('unlinkDir', () => {
        updateFileSystemData(mainWindow)
      })
      .on('error', (error) => {
        log.error(`文件系统监控错误: ${error}`)
      })
  } catch (error) {
    log.error('文件系统监控初始化失败:', error)
  }

  // 注册IPC处理函数
  registerFileSystemHandlers()
}

/**
 * 注册文件系统相关的IPC处理函数
 */
function registerFileSystemHandlers(): void {
  // 获取文件系统数据
  ipcMain.handle('get-file-system-data', async () => {
    return fileSystemData
  })

  // 读取文件内容
  ipcMain.handle('read-file-content', async (_event, filePath) => {
    try {
      const fullPath = path.isAbsolute(filePath) ? filePath : path.join(getUserFilesDir(), filePath)
      const content = await fs.promises.readFile(fullPath, 'utf-8')
      return { success: true, content }
    } catch (error) {
      log.error('读取文件失败:', error)
      return { success: false, error: '读取文件失败' }
    }
  })

  // 强制从文件服务器同步所有文件
  ipcMain.handle('force-sync-from-s3', async () => {
    try {
      await pullAllFilesFromS3(getUserFilesDir())
      return { success: true }
    } catch (error) {
      log.error('强制同步失败:', error)
      return { success: false, error: '同步失败' }
    }
  })

  // 创建文件
  ipcMain.handle('create-file', async (_event, parentPath, fileName, content) => {
    try {
      // 构建完整路径
      const normalizedParentPath = parentPath.startsWith('/') ? parentPath.substring(1) : parentPath
      const fullParentPath = path.join(getUserFilesDir(), normalizedParentPath)
      const fullPath = path.join(fullParentPath, fileName)

      // 检查父目录是否存在
      if (!fs.existsSync(fullParentPath)) {
        fs.mkdirSync(fullParentPath, { recursive: true })
      }

      // 写入文件内容
      await fs.promises.writeFile(fullPath, content || '')
      log.info(`创建文件成功: ${fullPath}`)

      return { success: true }
    } catch (error) {
      log.error('创建文件失败:', error)
      return { success: false, error: '创建文件失败' }
    }
  })

  // 创建文件夹
  ipcMain.handle('create-folder', async (_event, parentPath, folderName) => {
    try {
      // 构建完整路径
      const normalizedParentPath = parentPath.startsWith('/') ? parentPath.substring(1) : parentPath
      const fullParentPath = path.join(getUserFilesDir(), normalizedParentPath)
      const fullPath = path.join(fullParentPath, folderName)

      // 创建目录
      await fs.promises.mkdir(fullPath, { recursive: true })
      log.info(`创建文件夹成功: ${fullPath}`)

      return { success: true }
    } catch (error) {
      log.error('创建文件夹失败:', error)
      return { success: false, error: '创建文件夹失败' }
    }
  })

  // 删除文件或文件夹
  ipcMain.handle('delete-item', async (_event, itemPath) => {
    try {
      // 构建完整路径
      const normalizedPath = itemPath.startsWith('/') ? itemPath.substring(1) : itemPath
      const fullPath = path.join(getUserFilesDir(), normalizedPath)

      // 检查是文件还是文件夹
      const stats = await fs.promises.stat(fullPath)

      if (stats.isDirectory()) {
        // 获取文件夹中的所有文件，包括子文件夹中的文件
        const allFiles = await getAllFilesInDirectory(fullPath, normalizedPath)

        log.info(`删除文件夹中的所有文件: ${JSON.stringify(allFiles)}`)

        // 先删除文件服务器中的所有文件
        for (const filePath of allFiles) {
          try {
            await S3Service.deleteFile(filePath)
            log.info(`从文件服务器删除文件成功: ${filePath}`)
          } catch (error) {
            log.error(`从文件服务器删除文件失败: ${filePath}`, error)
          }
        }

        // 删除本地文件夹及其内容
        await fs.promises.rm(fullPath, { recursive: true, force: true })
        log.info(`删除本地文件夹成功: ${fullPath}`)
      } else {
        // 删除文件服务器中的文件
        await S3Service.deleteFile(normalizedPath)
        log.info(`从文件服务器删除文件成功: ${normalizedPath}`)

        // 删除本地文件
        await fs.promises.unlink(fullPath)
        log.info(`删除本地文件成功: ${fullPath}`)
      }

      return { success: true }
    } catch (error) {
      log.error('删除文件/文件夹失败:', error)
      return { success: false, error: '删除失败' }
    }
  })

  // 导入文件到同步目录
  ipcMain.handle('import-file', async (_event, sourcePath, targetDirectory) => {
    try {
      log.info(`开始导入文件: ${sourcePath} 到目录: ${targetDirectory}`)

      // 检查源文件是否存在
      if (!fs.existsSync(sourcePath)) {
        log.error(`源文件不存在: ${sourcePath}`)
        return { success: false, error: '源文件不存在' }
      }

      // 构建目标路径
      const normalizedTargetDir = targetDirectory.startsWith('/')
        ? targetDirectory.substring(1)
        : targetDirectory
      const fullTargetDir = path.join(getUserFilesDir(), normalizedTargetDir)

      log.info(`目标目录: ${fullTargetDir}`)

      // 确保目标目录存在
      if (!fs.existsSync(fullTargetDir)) {
        log.info(`创建目标目录: ${fullTargetDir}`)
        await fs.promises.mkdir(fullTargetDir, { recursive: true })
      }

      // 获取源文件名
      const fileName = path.basename(sourcePath)
      const targetPath = path.join(fullTargetDir, fileName)

      log.info(`目标文件路径: ${targetPath}`)

      // 检查是否存在同名文件
      if (fs.existsSync(targetPath)) {
        log.warn(`目标文件已存在，将被覆盖: ${targetPath}`)
      }

      // 复制文件
      await fs.promises.copyFile(sourcePath, targetPath)
      log.info(`导入文件成功: ${sourcePath} -> ${targetPath}`)

      // 读取文件统计信息，以便验证复制是否成功
      const stats = await fs.promises.stat(targetPath)
      log.info(`文件大小: ${stats.size} 字节`)

      // 主动触发文件更新事件，确保同步到文件服务器和更新UI
      if (watcher) {
        log.info('主动触发文件更新事件，确保UI更新')
        watcher.emit('add', targetPath)
      }

      return { success: true }
    } catch (error) {
      log.error('导入文件失败:', error)
      return { success: false, error: `导入失败: ${error}` }
    }
  })

  // 从Buffer导入文件到同步目录
  ipcMain.handle('import-file-from-buffer', async (_event, fileName, buffer, targetDirectory) => {
    try {
      log.info(`开始从Buffer导入文件: ${fileName} 到目录: ${targetDirectory}`)

      // 构建目标路径
      const normalizedTargetDir = targetDirectory.startsWith('/')
        ? targetDirectory.substring(1)
        : targetDirectory
      const fullTargetDir = path.join(getUserFilesDir(), normalizedTargetDir)

      log.info(`目标目录: ${fullTargetDir}`)

      // 确保目标目录存在
      if (!fs.existsSync(fullTargetDir)) {
        log.info(`创建目标目录: ${fullTargetDir}`)
        await fs.promises.mkdir(fullTargetDir, { recursive: true })
      }

      // 构建目标文件路径
      const targetPath = path.join(fullTargetDir, fileName)
      log.info(`目标文件路径: ${targetPath}`)

      // 检查是否存在同名文件
      if (fs.existsSync(targetPath)) {
        log.warn(`目标文件已存在，将被覆盖: ${targetPath}`)
      }

      // 将Buffer写入文件
      await fs.promises.writeFile(targetPath, Buffer.from(buffer))
      log.info(`从Buffer导入文件成功: ${fileName} -> ${targetPath}`)

      // 读取文件统计信息，以便验证写入是否成功
      const stats = await fs.promises.stat(targetPath)
      log.info(`文件大小: ${stats.size} 字节`)

      // 主动触发文件更新事件，确保同步到文件服务器和更新UI
      if (watcher) {
        log.info('主动触发文件更新事件，确保UI更新')
        watcher.emit('add', targetPath)
      }

      return { success: true }
    } catch (error) {
      log.error('从Buffer导入文件失败:', error)
      return { success: false, error: `导入失败: ${error}` }
    }
  })

  // 移动文件或文件夹
  ipcMain.handle('move-item', async (_event, sourcePath, targetDirectory) => {
    try {
      log.info(`准备移动: ${sourcePath} 到目录: ${targetDirectory}`)

      // 构建源路径和目标路径
      const normalizedSourcePath = sourcePath.startsWith('/') ? sourcePath.substring(1) : sourcePath
      const normalizedTargetDir = targetDirectory.startsWith('/')
        ? targetDirectory.substring(1)
        : targetDirectory

      const baseDir = getUserFilesDir()
      const fullSourcePath = path.join(baseDir, normalizedSourcePath)
      const fullTargetDir = path.join(baseDir, normalizedTargetDir)

      // 检查源文件是否存在
      if (!fs.existsSync(fullSourcePath)) {
        log.error(`源文件不存在: ${fullSourcePath}`)
        return { success: false, error: '源文件不存在' }
      }

      // 检查目标目录是否存在
      if (!fs.existsSync(fullTargetDir)) {
        log.info(`目标目录不存在，创建: ${fullTargetDir}`)
        await fs.promises.mkdir(fullTargetDir, { recursive: true })
      }

      // 获取文件名
      const fileName = path.basename(fullSourcePath)
      const targetPath = path.join(fullTargetDir, fileName)

      log.info(`移动文件: ${fullSourcePath} -> ${targetPath}`)

      // 检查目标位置是否已存在同名文件
      if (fs.existsSync(targetPath)) {
        log.warn(`目标位置已存在同名文件，将被覆盖: ${targetPath}`)
      }

      // 执行移动操作
      await fs.promises.rename(fullSourcePath, targetPath)

      log.info(`移动成功: ${fullSourcePath} -> ${targetPath}`)

      // 如果是文件，触发文件系统变更事件，以便更新文件服务器
      const stats = await fs.promises.stat(targetPath)
      if (stats.isFile() && watcher) {
        // 通知watcher删除旧文件
        watcher.emit('unlink', fullSourcePath)
        // 通知watcher新增新文件
        watcher.emit('add', targetPath)
      }

      return { success: true }
    } catch (error) {
      log.error('移动文件失败:', error)
      return { success: false, error: `移动失败: ${error}` }
    }
  })
}

/**
 * 从文件服务器拉取所有文件（只下载有更新的文件）
 */
async function pullAllFilesFromS3(baseDir: string): Promise<void> {
  try {
    const files = await S3Service.listFiles('9527')

    for (const file of files) {
      if (!file.Key) continue

      const s3Key = file.Key
      const localPath = path.join(baseDir, s3Key)

      try {
        // 检查文件是否在本地存在
        const localExists = fs.existsSync(localPath)

        if (!localExists) {
          // 本地不存在，直接下载
          log.info(`本地不存在该文件，从文件服务器下载: ${s3Key}`)
          await S3Service.downloadFile(s3Key, localPath)
          continue
        }

        // 文件存在，检查是否有更新
        const localStat = fs.statSync(localPath)
        const localMtime = localStat.mtime

        // 获取文件服务器对象的详细元数据
        const metadata = await S3Service.getObjectMetadata(s3Key)

        if (!metadata || !metadata.LastModified) {
          log.warn(`无法获取文件服务器对象元数据: ${s3Key}`)
          continue
        }

        // 比较修改时间，应用"最后修改者优先"策略
        if (metadata.LastModified > localMtime) {
          log.info(`文件服务器版本更新，从文件服务器下载: ${s3Key}`)
          await S3Service.downloadFile(s3Key, localPath)
        }
      } catch (error) {
        log.error(`处理文件服务器文件失败: ${s3Key}`, error)
        continue // 继续处理下一个文件
      }
    }

    log.info('文件服务器文件同步完成')
  } catch (error) {
    log.error('从文件服务器拉取文件失败:', error)
    throw error
  }
}

/**
 * 更新文件系统数据
 */
async function updateFileSystemData(mainWindow: BrowserWindow): Promise<void> {
  try {
    const userFilesDir = getUserFilesDir()
    fileSystemData = await scanDirectory(userFilesDir, '/')

    // 发送更新到渲染进程
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('file-system-updated', fileSystemData)
    }
  } catch (error) {
    log.error('更新文件系统数据失败:', error)
  }
}

/**
 * 扫描目录并构建文件系统树
 * @param dirPath 目录的完整路径
 * @param relativePath 相对路径
 */
async function scanDirectory(dirPath: string, relativePath: string): Promise<FileSystemNode[]> {
  try {
    const entries = await fs.promises.readdir(dirPath, { withFileTypes: true })
    const result: FileSystemNode[] = []

    for (const entry of entries) {
      // 跳过隐藏文件
      if (entry.name.startsWith('.')) continue

      const entryRelativePath = path.join(relativePath, entry.name)
      const entryPath = path.join(dirPath, entry.name)
      const entryId = Buffer.from(entryRelativePath).toString('base64')

      if (entry.isDirectory()) {
        // 处理目录
        const stats = await fs.promises.stat(entryPath)
        const children = await scanDirectory(entryPath, entryRelativePath)

        result.push({
          id: entryId,
          name: entry.name,
          type: 'folder',
          path: entryRelativePath,
          createdAt: stats.birthtime,
          modifiedAt: stats.mtime,
          children,
          isLeaf: false
        })
      } else if (entry.isFile()) {
        // 处理文件
        const stats = await fs.promises.stat(entryPath)

        result.push({
          id: entryId,
          name: entry.name,
          type: 'file',
          path: entryRelativePath,
          size: stats.size,
          createdAt: stats.birthtime,
          modifiedAt: stats.mtime,
          isLeaf: true
        })
      }
    }

    return result
  } catch (error) {
    log.error(`扫描目录失败 ${dirPath}:`, error)
    return []
  }
}

/**
 * 将单个文件同步到后端服务器
 * @param filePath 文件的完整路径
 * @param baseDir 基础路径
 */
async function syncFile(filePath: string, baseDir: string): Promise<void> {
  try {
    const relativePath = path.relative(baseDir, filePath).replace(/\\/g, '/')
    log.info(`同步文件到后端服务器: ${relativePath}`)

    // 上传文件到后端服务器
    await S3Service.uploadFile(filePath, relativePath)
  } catch (error) {
    log.error(`同步文件到后端服务器失败: ${filePath}`, error)
  }
}

/**
 * 从文件服务器删除文件
 * @param filePath 文件的完整路径
 * @param baseDir 基础目录
 */
async function deleteFile(filePath: string, baseDir: string): Promise<void> {
  try {
    const relativePath = path.relative(baseDir, filePath).replace(/\\/g, '/')
    log.info(`从文件服务器删除文件: ${relativePath}`)
    await S3Service.deleteFile(relativePath)
  } catch (error) {
    log.error(`从文件服务器删除文件失败: ${filePath}`, error)
  }
}

/**
 * 将所有文件同步到后端服务器
 * @param baseDir 基础目录
 */
async function syncAllFiles(baseDir: string): Promise<void> {
  try {
    log.info('开始将所有文件同步到后端服务器...')

    // 使用递归函数遍历目录
    async function processDirectory(directory: string): Promise<void> {
      const entries = await fs.promises.readdir(directory, { withFileTypes: true })

      for (const entry of entries) {
        // 跳过隐藏文件
        if (entry.name.startsWith('.')) continue

        const fullPath = path.join(directory, entry.name)

        if (entry.isDirectory()) {
          // 如果是目录，递归处理
          await processDirectory(fullPath)
        } else if (entry.isFile()) {
          // 如果是文件，上传到后端服务器
          await syncFile(fullPath, baseDir)
        }
      }
    }

    await processDirectory(baseDir)
    log.info('所有文件已同步到后端服务器')
  } catch (error) {
    log.error('同步所有文件到后端服务器失败:', error)
  }
}

/**
 * 获取目录中的所有文件路径（包括子目录中的文件）
 * @param dirPath 目录的完整路径
 * @param baseRelativePath 基础相对路径（用于构建文件服务器键）
 * @returns 返回所有文件的相对路径（适用于文件服务器键）
 */
async function getAllFilesInDirectory(
  dirPath: string,
  baseRelativePath: string
): Promise<string[]> {
  let result: string[] = []

  try {
    const entries = await fs.promises.readdir(dirPath, { withFileTypes: true })

    for (const entry of entries) {
      const entryPath = path.join(dirPath, entry.name)
      const entryRelativePath = path.join(baseRelativePath, entry.name).replace(/\\/g, '/')

      if (entry.isDirectory()) {
        // 递归处理子目录
        const subDirFiles = await getAllFilesInDirectory(entryPath, entryRelativePath)
        result = result.concat(subDirFiles)
      } else {
        // 添加文件路径
        result.push(entryRelativePath)
      }
    }
  } catch (error) {
    log.error(`获取目录文件列表失败: ${dirPath}`, error)
  }

  return result
}

export function closeFileSystemWatcher(): void {
  if (watcher) {
    watcher.close()
    watcher = null
    log.info('文件系统监控已关闭')
  }

  // 清除同步定时器
  if (syncTimer) {
    clearInterval(syncTimer)
    syncTimer = null
    log.info('文件服务器同步轮询已停止')
  }
}
