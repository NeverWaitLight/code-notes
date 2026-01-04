import { exec } from 'child_process'
import Logger from 'electron-log/main'
import * as os from 'os'

const logger = Logger.scope('ollama updater')

const OLLAMA_VERSION = 'v0.1.27'
const WINDOWS_AMD64_URL = `https://github.com/ollama/ollama/releases/download/${OLLAMA_VERSION}/ollama-windows-amd64.zip`

