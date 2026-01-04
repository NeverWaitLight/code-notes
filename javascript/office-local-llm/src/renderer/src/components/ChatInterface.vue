<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { 
  ElInput, 
  ElButton, 
  ElScrollbar, 
  ElCard, 
  ElDivider 
} from 'element-plus'
import { 
  VideoPause, 
  CaretRight 
} from '@element-plus/icons-vue'

interface Message {
  id: number
  content: string
  role: 'user' | 'assistant'
  timestamp: Date
  streaming?: boolean
}

// 状态管理
const messages = ref<Message[]>([])
const newMessage = ref('')
const chatContainerRef = ref<HTMLElement | null>(null)
const isLoading = ref(false)
const isStreaming = ref(false) // 控制是否正在流式接收回复
const useStreamMode = ref(true) // 控制是否使用流式模式，默认启用
const messageIdCounter = ref(1) // 消息ID计数器确保唯一性
let currentStreamAborted = false // 用于标记是否已中断流式响应

// 添加新消息到聊天列表
const addMessage = (content: string, role: 'user' | 'assistant', streaming = false): number => {
  const id = messageIdCounter.value++
  messages.value.push({
    id,
    content,
    role,
    timestamp: new Date(),
    streaming
  })

  setTimeout(() => {
    scrollToBottom()
  }, 100)

  return id
}

// 更新现有消息内容
const updateMessage = (id: number, content: string, role?: 'user' | 'assistant'): void => {
  const message = messages.value.find((m) => m.id === id)
  if (message) {
    // 确保只更新匹配角色的消息
    if (role && message.role !== role) {
      console.error(
        `尝试更新ID ${id} 的消息失败: 角色不匹配 (预期: ${role}, 实际: ${message.role})`
      )
      return
    }
    message.content = content
    setTimeout(() => {
      scrollToBottom()
    }, 50)
  }
}

// 设置消息流式状态
const setMessageStreaming = (id: number, streaming: boolean): void => {
  const message = messages.value.find((m) => m.id === id)
  if (message) {
    message.streaming = streaming
  }
}

// 发送消息或停止回复
const sendOrStopMessage = async (): Promise<void> => {
  // 如果正在流式回复中，则停止回复
  if (isStreaming.value) {
    stopStreamResponse()
    return
  }

  // 否则发送新消息
  const userInput = newMessage.value.trim()
  if (!userInput || isLoading.value) return

  // 添加用户消息
  addMessage(userInput, 'user')
  newMessage.value = ''

  // 设置加载状态
  isLoading.value = true

  try {
    if (useStreamMode.value) {
      await handleStreamChat(userInput)
    } else {
      await handleRegularChat(userInput)
    }
  } catch (error) {
    console.error('聊天请求失败:', error)
    addMessage('发生错误，请稍后再试', 'assistant')
    isLoading.value = false
    isStreaming.value = false
  }
}

// 中断流式响应
const stopStreamResponse = (): void => {
  if (isStreaming.value) {
    // 中断当前流式响应
    currentStreamAborted = true
    
    // 在消息末尾添加中断提示
    const lastMessage = messages.value.find(m => m.streaming)
    if (lastMessage) {
      updateMessage(lastMessage.id, lastMessage.content + '\n[回复已中断]', 'assistant')
      setMessageStreaming(lastMessage.id, false)
    }
    
    // 发送中断信号到主进程
    window.api.abortStreamChat()
    
    // 更新状态
    isStreaming.value = false
  }
}

// 处理流式聊天模式
const handleStreamChat = async (userInput: string): Promise<void> => {
  // 创建一个空的助手消息，用于流式更新
  const assistantMessageId = addMessage('', 'assistant', true)
  let currentContent = ''
  isStreaming.value = true
  currentStreamAborted = false
  
  // 使用流式API
  window.api.streamChat(
    userInput,
    // 处理流式响应块
    (response) => {
      if (response.isSegmentStart && response.segmentType === 'thought') {
        currentContent += '\n[思考开始]\n'
      } else if (response.isSegmentEnd && response.segmentType === 'thought') {
        currentContent += '\n[思考结束]\n'
      } else {
        currentContent += response.text
      }

      updateMessage(assistantMessageId, currentContent, 'assistant')
    },
    // 处理完成事件
    () => {
      setMessageStreaming(assistantMessageId, false)
      isLoading.value = false
      isStreaming.value = false
    },
    // 处理错误
    (error) => {
      // 如果是中断导致的错误，不显示错误信息
      if (currentStreamAborted) {
        return
      }
      updateMessage(assistantMessageId, `出错了: ${error.error || '未知错误'}`, 'assistant')
      setMessageStreaming(assistantMessageId, false)
      isLoading.value = false
      isStreaming.value = false
    }
  )
}

// 处理常规聊天模式
const handleRegularChat = async (userInput: string): Promise<void> => {
  const response = await window.api.chat(userInput)

  if (response.success && response.response) {
    // 添加AI回复
    addMessage(response.response, 'assistant')
  } else {
    // 处理错误情况
    addMessage(`出错了: ${response.error || '未知错误'}`, 'assistant')
  }
  isLoading.value = false
}

// 处理键盘事件
const handleKeyUp = (e: KeyboardEvent): void => {
  // 只有按下Ctrl+Enter时发送或停止
  if (e.ctrlKey && e.key === 'Enter') {
    sendOrStopMessage()
  }
}

// 滚动到聊天底部
const scrollToBottom = (): void => {
  if (chatContainerRef.value) {
    chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight
  }
}

// 格式化消息内容，处理思考部分的样式
const formatMessage = (content: string): string => {
  return content
    .replace(
      /\n\[思考开始\]\n/g,
      '<div class="thought-block"><div class="thought-header">思考过程:</div>'
    )
    .replace(/\n\[思考结束\]\n/g, '</div>')
    .replace(/\n\[回复已中断\]/g, '<div class="interrupted-message">回复已中断</div>')
    .replace(/\n/g, '<br>') // 将换行符转换为HTML换行
}

onMounted(() => {
  // 初始化欢迎消息
  addMessage('你好，我是AI助手，有什么我可以帮助你的吗？', 'assistant')
})
</script>

<template>
  <div class="chat-interface">
    <el-card class="chat-card" shadow="never" :body-style="{ padding: '0', height: '100%', display: 'flex', flexDirection: 'column' }">
      <div class="chat-scroll-area">
        <el-scrollbar ref="chatContainerRef" class="chat-messages" height="100%">
          <div class="message-list">
            <template v-for="message in messages" :key="message.id">
              <div 
                class="message" 
                :class="[message.role, { 'is-streaming': message.streaming }]"
              >
                <div class="message-content">
                  <div v-if="message.role === 'assistant'" class="assistant-content">
                    <div v-dompurify-html="formatMessage(message.content)"></div>
                    <span v-if="message.streaming" class="streaming-indicator">▌</span>
                  </div>
                  <template v-else>
                    {{ message.content }}
                  </template>
                </div>
                <div class="message-timestamp">
                  {{ message.timestamp.toLocaleTimeString() }}
                </div>
              </div>
            </template>

            <div v-if="isLoading && !useStreamMode" class="loading-indicator">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <div class="chat-input-container">
        <div class="input-wrapper">
          <el-input
            v-model="newMessage"
            type="textarea"
            :rows="2"
            resize="none"
            :placeholder="isLoading && !isStreaming ? '处理中...' : '输入消息...'"
            :disabled="isLoading && !isStreaming"
            @keyup="handleKeyUp"
          />
        </div>
        <div class="button-wrapper">
          <el-button
            class="send-button"
            type="primary"
            :icon="isStreaming ? VideoPause : CaretRight"
            :disabled="isLoading && !isStreaming"
            @click="sendOrStopMessage"
            :class="{ 'stop-button': isStreaming }"
          >
            {{ isStreaming ? '停止' : (isLoading ? '处理中...' : '发送') }}
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.chat-interface {
  width: 100%;
  height: 100%;
}

.chat-card {
  height: 100%;
  background-color: var(--el-bg-color);
  border-radius: 8px;
  overflow: hidden;
}

.chat-scroll-area {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.chat-messages {
  padding: 20px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message {
  display: flex;
  flex-direction: column;
  max-width: 80%;
  padding: 12px;
  border-radius: 12px;
  animation: fadeIn 0.3s ease;
}

.message.user {
  align-self: flex-end;
  background-color: var(--el-color-success-light-9);
  color: var(--el-color-success-dark-2);
  border-bottom-right-radius: 4px;
}

.message.assistant {
  align-self: flex-start;
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary-dark-2);
  border-bottom-left-radius: 4px;
}

.message-content {
  margin-bottom: 4px;
  word-break: break-word;
}

.streaming-indicator {
  display: inline-block;
  margin-left: 2px;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

.message-timestamp {
  align-self: flex-end;
  font-size: 10px;
  opacity: 0.7;
}

.chat-input-container {
  display: flex;
  padding: 15px;
  border-top: 1px solid var(--el-border-color-lighter);
  background-color: var(--el-fill-color-blank);
}

.input-wrapper {
  flex: 1;
  height: 100%;
  margin-right: 12px;
}

.button-wrapper {
  height: 100%;
  display: flex;
  align-items: center;
}

.send-button {
  min-width: 80px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stop-button {
  background-color: var(--el-color-danger);
  border-color: var(--el-color-danger);
}

.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
}

.typing-indicator {
  display: flex;
  align-items: center;
}

.typing-indicator span {
  height: 8px;
  width: 8px;
  margin: 0 2px;
  background-color: var(--el-text-color-primary);
  border-radius: 50%;
  display: inline-block;
  opacity: 0.4;
  animation: pulse 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes pulse {
  0%, 50%, 100% {
    transform: scale(1);
  }
  25% {
    transform: scale(0.7);
  }
  75% {
    transform: scale(1.3);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 添加思考内容的样式 */
:deep(.thought-block) {
  background-color: var(--el-fill-color-dark);
  padding: 10px;
  margin: 8px 0;
  border-radius: 6px;
  border-left: 3px solid var(--el-color-warning);
  color: var(--el-text-color-regular);
  font-style: italic;
  font-size: 0.9em;
}

:deep(.thought-header) {
  font-weight: bold;
  margin-bottom: 5px;
  color: var(--el-color-warning);
}

:deep(.interrupted-message) {
  margin-top: 5px;
  color: var(--el-color-danger);
  font-style: italic;
}
</style>
