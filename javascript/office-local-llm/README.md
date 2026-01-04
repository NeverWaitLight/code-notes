# electron-app

An Electron application with Vue and TypeScript

## 技术栈

### Electron应用搭建
- 使用Electron v37搭建跨平台桌面应用
- 基于electron-vite构建工具集成TypeScript和Vue 3
- 使用Vue 3.5组件化UI开发
- Element Plus组件库实现现代化界面
- 主进程和渲染进程通信基于IPC

### 本地模型功能
- node-llama-cpp实现本地大语言模型集成
- 支持模型下载管理和自动切换
- 流式输出和中断控制
- 模型资源管理与释放
- 基于配置的多模型支持

### 文件同步功能
- 基于AWS S3 API实现文件同步
- 文件系统实时监控(chokidar)
- 增量同步和差异对比
- 支持文件的增删改操作
- 文件冲突解决策略("最后修改者优先")

### DLL调用
- 跨平台原生模块加载
- Windows命令行工具集成
- 使用child_process进行系统命令执行
- 文件系统磁盘空间检测

## Recommended IDE Setup

- [VSCode](https://code.visualstudio.com/) + [ESLint](https://marketplace.visualstudio.com/items?itemName=dbaeumer.vscode-eslint) + [Prettier](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar)

## Project Setup

### Install

```bash
$ npm install
```

### Development

```bash
$ npm run dev
```

### Build

```bash
# For windows
$ npm run build:win

# For macOS
$ npm run build:mac

# For Linux
$ npm run build:linux
```
