import { spawn } from 'child_process'
import Logger from 'electron-log/main'

const logger = Logger.scope('ollama executor')

const OLLAMA_PATH = 'C:\\Users\\Administrator\\AppData\\Local\\ollama\\ollama.exe'

export class Executor {
  // 声明重载签名
  async startup(): Promise<void>
  async startup(ollamaPath: string): Promise<void>
  // 实现方法（必须兼容所有重载情况）
  async startup(ollamaPath?: string): Promise<void> {
    const actualPath = ollamaPath || OLLAMA_PATH
    const childProcess = spawn(actualPath, ['serve'], {
      stdio: 'pipe',
      env: process.env
    })

    logger.info(`Child process PID: ${childProcess.pid}`)
  }
}
