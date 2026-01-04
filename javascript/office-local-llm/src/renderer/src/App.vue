<script setup lang="ts">
import { onMounted } from 'vue'
import ChatInterface from './components/ChatInterface.vue'
import ModelManager from './components/ModelManager.vue'
import FileExplorer from './components/FileExplorer.vue'

// 设置深色模式
const applyDarkMode = (): void => {
  const htmlElement = document.documentElement
  htmlElement.classList.add('dark')
  htmlElement.setAttribute('data-theme', 'dark')
}

// 组件挂载时初始化深色模式
onMounted(() => {
  applyDarkMode()
})
</script>

<template>
  <div class="app-container dark-mode">
    <div class="app-header">
      <div class="app-title">Axi 机器人</div>
      <div class="controls-container">
        <ModelManager />
      </div>
    </div>
    <div class="app-content">
      <div class="file-explorer-container">
        <FileExplorer />
      </div>
      <div class="chat-container">
        <ChatInterface />
      </div>
    </div>
  </div>
</template>

<style>
:root {
  --theme-transition: 0.3s ease-in-out;
}

.app-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  transition:
    background-color var(--theme-transition),
    color var(--theme-transition);
  /* 确保所有内容可点击 */
  pointer-events: auto;
}

.app-header {
  display: flex;
  align-items: center;
  padding: 20px;
  justify-content: space-between;
  position: relative;
  z-index: 30;
  pointer-events: auto;
}

.controls-container {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 删除了主题切换相关样式 */

.app-title {
  font-size: 24px;
  font-weight: 700;
  background: -webkit-linear-gradient(315deg, #42d392 25%, #647eff);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.app-content {
  flex: 1;
  display: flex;
  padding: 0 20px 20px;
  overflow: hidden;
  gap: 15px;
}

.file-explorer-container {
  width: 25%;
  height: 100%;
  flex-shrink: 0;
}

.chat-container {
  width: 75%;
  height: 100%;
}

/* 深色模式全局样式 - 现在始终应用 */
html {
  color-scheme: dark;
}
</style>
