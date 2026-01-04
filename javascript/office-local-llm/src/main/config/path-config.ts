import { app } from 'electron'
import os from 'os'
import path from 'path'

/**
 * 获运行时应用目录
 */
export function getApplicationDir(): string {
  const platform = os.platform()

  switch (platform) {
    case 'win32':
      // Windows: ~/AppData/Local/Axi/Application
      return path.join(os.homedir(), 'AppData', 'Local', app.getName(), 'Application')
    case 'darwin':
      // macOS: ~/Library/Application Support/Axi/Application
      return path.join(os.homedir(), 'Library', 'Application Support', app.getName(), 'Application')
    case 'linux':
      // Linux: ~/.local/share/Axi/Application
      return path.join(os.homedir(), '.local', 'share', app.getName(), 'Application')
    default:
      // 默认回退方案
      return path.join(app.getPath('userData'), 'Application')
  }
}

/**
 * 获取用户模型目录
 */
export function getUserModelsDir(): string {
  const platform = os.platform()

  switch (platform) {
    case 'win32':
      // Windows: ~/AppData/Local/Axi/UserData/models
      return path.join(os.homedir(), 'AppData', 'Local', app.getName(), 'UserData', 'llm')
    case 'darwin':
      // macOS: ~/Library/Application Support/Axi/UserData/models
      return path.join(
        os.homedir(),
        'Library',
        'Application Support',
        app.getName(),
        'UserData',
        'llm'
      )
    case 'linux':
      // Linux: ~/.local/share/Axi/UserData/models
      return path.join(os.homedir(), '.local', 'share', app.getName(), 'UserData', 'llm')
    default:
      // 默认回退方案
      return path.join(app.getPath('userData'), 'UserData', 'llm')
  }
}

/**
 * 获取用户文件目录
 */
export function getUserFilesDir(): string {
  const platform = os.platform()

  switch (platform) {
    case 'win32':
      // Windows: ~/AppData/Local/Axi/UserData/models
      return path.join(os.homedir(), 'AppData', 'Local', app.getName(), 'UserData', 'Files')
    case 'darwin':
      // macOS: ~/Library/Application Support/Axi/UserData/models
      return path.join(
        os.homedir(),
        'Library',
        'Application Support',
        app.getName(),
        'UserData',
        'Files'
      )
    case 'linux':
      // Linux: ~/.local/share/Axi/UserData/models
      return path.join(os.homedir(), '.local', 'share', app.getName(), 'UserData', 'Files')
    default:
      // 默认回退方案
      return path.join(app.getPath('userData'), 'UserData', 'Files')
  }
}
