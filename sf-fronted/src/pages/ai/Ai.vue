<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { Document, MagicStick, Mic, Plus, Position, Share, Star } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface ChatItem {
  id: number
  role: 'assistant' | 'user'
  content: string
  time: string
  status?: string
}

interface RagChatResponse {
  answer?: string
  response?: string
  content?: string
  result?: string
  type?: string
  references?: unknown[]
  data?: {
    answer?: string
    response?: string
    content?: string
    result?: string
    type?: string
    references?: unknown[]
  }
}

const chatInput = ref('')
const quickPrompts = ref([''])
const messages = ref<ChatItem[]>([])
const isSending = ref(false)
const isStreaming = ref(false)
const ragApiBaseUrl = import.meta.env.VITE_RAG_API_BASE_URL || 'http://127.0.0.1:8000'
let activeAbortController: AbortController | null = null

const userName = computed(() => {
  return sessionStorage.getItem('name') || sessionStorage.getItem('nickname') || sessionStorage.getItem('username') || '同学'
})

const hasConversation = computed(() => messages.value.length > 0)
const canSend = computed(() => chatInput.value.trim().length > 0 && !isSending.value)

const getAssistantText = (payload: RagChatResponse) => {
  return (
    payload.answer ||
    payload.response ||
    payload.content ||
    payload.result ||
    payload.data?.answer ||
    payload.data?.response ||
    payload.data?.content ||
    payload.data?.result ||
    ''
  )
}

const updateAssistantMessage = (assistantId: number, patch: Partial<ChatItem>) => {
  const index = messages.value.findIndex((item) => item.id === assistantId)
  if (index === -1) return

  const currentMessage = messages.value[index]
  messages.value[index] = {
    ...currentMessage,
    ...patch
  } as ChatItem
}

const stopGeneration = () => {
  activeAbortController?.abort()
  activeAbortController = null
  isStreaming.value = false
  isSending.value = false
}

const sendMessage = async () => {
  const text = chatInput.value.trim()
  if (!text || isSending.value) return

  const userMessage: ChatItem = {
    id: Date.now(),
    role: 'user',
    content: text,
    time: '刚刚'
  }
  messages.value.push(userMessage)
  chatInput.value = ''

  const assistantId = Date.now() + 1
  messages.value.push({
    id: assistantId,
    role: 'assistant',
    content: '',
    time: '刚刚',
    status: '生成中'
  })

  const controller = new AbortController()
  activeAbortController = controller
  isSending.value = true
  isStreaming.value = true

  try {
    const response = await fetch(`${ragApiBaseUrl}/rag/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: text, top_k: 5 }),
      signal: controller.signal
    })

    if (!response.ok || !response.body) throw new Error(`HTTP ${response.status}`)

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let assistantText = ''

    const parseEvent = (rawEvent: string) => {
      let eventName: string | null = null
      const dataLines: string[] = []
      rawEvent.split(/\r?\n/).forEach((line) => {
        if (line.startsWith('event:')) eventName = line.slice(6).trim()
        if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
      })
      return { eventName, dataLines }
    }

    const handleEvent = (eventName: string | null, dataLines: string[]) => {
      if (!dataLines.length) return
      const payloadText = dataLines.join('\n')

      if (eventName === 'error') {
        const payload = JSON.parse(payloadText) as { message?: string }
        throw new Error(payload.message || 'AI 服务返回错误')
      }

      if (eventName === 'done') {
        updateAssistantMessage(assistantId, {
          content: assistantText || 'AI 暂时没有返回结果，请稍后重试。',
          status: '已完成'
        })
        return
      }

      const payload = JSON.parse(payloadText) as RagChatResponse
      if (payload.type === 'references') return

      if (payload.type === 'delta' && payload.content) {
        assistantText += payload.content
        updateAssistantMessage(assistantId, { content: assistantText, status: '生成中' })
        return
      }

      const textContent = getAssistantText(payload)
      if (textContent) {
        assistantText = textContent
        updateAssistantMessage(assistantId, { content: assistantText, status: '生成中' })
      }
    }

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      let separatorIndex = buffer.indexOf('\n\n')
      while (separatorIndex !== -1) {
        const rawEvent = buffer.slice(0, separatorIndex)
        buffer = buffer.slice(separatorIndex + 2)
        const { eventName, dataLines } = parseEvent(rawEvent)
        handleEvent(eventName, dataLines)
        separatorIndex = buffer.indexOf('\n\n')
      }
    }

    buffer += decoder.decode()
    if (buffer.trim()) {
      const { eventName, dataLines } = parseEvent(buffer)
      handleEvent(eventName, dataLines)
    }

    if (!assistantText) {
      updateAssistantMessage(assistantId, {
        content: 'AI 暂时没有返回结果，请稍后重试。',
        status: '已完成'
      })
    }
  } catch (error) {
    if ((error as Error).name === 'AbortError') {
      updateAssistantMessage(assistantId, {
        content: messages.value.find((item) => item.id === assistantId)?.content || '已停止生成',
        status: '已停止'
      })
      return
    }

    updateAssistantMessage(assistantId, {
      content: 'AI 接口调用失败，请确认 sf-ai 服务已启动并允许跨域访问。',
      status: '请求失败'
    })
    ElMessage.error('发送失败，请检查后端服务')
  } finally {
    activeAbortController = null
    isSending.value = false
    isStreaming.value = false
  }
}

const applyPrompt = (prompt: string) => {
  chatInput.value = prompt
}

onBeforeUnmount(() => {
  activeAbortController?.abort()
})
</script>

<template>
  <div class="ai-page">
    <div class="ai-shell">
      <header class="ai-header">
        <div class="ai-brand">
          <div class="ai-brand-badge">
            <el-icon><Star /></el-icon>
          </div>
          <div>
            <h1>StudyFlow AI</h1>
            <p>基于Qwen3构建的研学知识库AI</p>
          </div>
        </div>

        <div class="ai-header-actions">
          <el-tag v-if="isStreaming" effect="light" type="warning">正在生成</el-tag>
          <el-button v-if="isStreaming" type="danger" plain @click="stopGeneration">停止生成</el-button>
          <el-button circle text class="ai-header-action">
            <el-icon><Share /></el-icon>
          </el-button>
        </div>
      </header>

      <main class="ai-content" :class="{ 'is-empty': !hasConversation }">
        <section v-if="!hasConversation" class="ai-landing">
          <div class="ai-landing-title">
            <el-icon><Star /></el-icon>
            <span>欢迎你，{{ userName }} 有什么可以帮助你</span>
          </div>

          <div class="ai-landing-composer">
            <div class="ai-input-panel">
              <el-input
                v-model="chatInput"
                class="ai-input"
                type="textarea"
                :rows="3"
                resize="none"
                placeholder="给 DeepSeek 发送消息"
                @keydown.enter.exact.prevent="sendMessage"
              />

              <div class="ai-panel-actions">
                <button type="button" class="ai-mini-action" aria-label="语音输入">
                  <el-icon><Mic /></el-icon>
                </button>
                <button type="button" class="ai-send-btn" :class="{ active: canSend }" :disabled="!canSend" @click="sendMessage">
                  <el-icon><Position /></el-icon>
                </button>
              </div>
            </div>

            <div class="ai-quick-prompts">
              <button v-for="prompt in quickPrompts" :key="prompt" type="button" class="ai-chip" @click="applyPrompt(prompt)">
                <el-icon><MagicStick /></el-icon>
                <span>{{ prompt }}</span>
              </button>
            </div>
          </div>
        </section>

        <section v-else class="ai-timeline">
          <template v-for="message in messages" :key="message.id">
            <div v-if="message.role === 'user'" class="ai-message-row is-user">
              <div class="ai-message-bubble user">{{ message.content }}</div>
            </div>

            <div v-else class="ai-message-row is-assistant">
              <div class="ai-avatar assistant">AI</div>
              <div class="ai-message-body">
                <div v-if="message.content" class="ai-message-text">{{ message.content }}</div>
                <div v-else class="ai-thinking-body">
                  <span class="ai-thinking-dots" aria-label="AI思考中">
                    <span class="ai-thinking-dot"></span>
                    <span class="ai-thinking-dot"></span>
                    <span class="ai-thinking-dot"></span>
                  </span>
                </div>
                <div class="ai-message-actions">
                  <button type="button" aria-label="复制">
                    <el-icon><Document /></el-icon>
                  </button>
                  <button type="button" aria-label="重试">
                    <el-icon><MagicStick /></el-icon>
                  </button>
                  <button type="button" aria-label="点赞">
                    <el-icon><Star /></el-icon>
                  </button>
                </div>
              </div>
            </div>
          </template>
        </section>
      </main>

      <footer v-if="hasConversation" class="ai-footer">
        <div class="ai-composer">
          <div class="ai-quick-prompts">
            <button v-for="prompt in quickPrompts" :key="prompt" type="button" class="ai-chip" @click="applyPrompt(prompt)">
              <el-icon><MagicStick /></el-icon>
              <span>{{ prompt }}</span>
            </button>
          </div>

          <div class="ai-input-panel">
            <button type="button" class="ai-attach-btn" aria-label="添加附件">
              <el-icon><Plus /></el-icon>
            </button>

            <el-input
              v-model="chatInput"
              class="ai-input"
              type="textarea"
              :rows="3"
              resize="none"
              placeholder="给 DeepSeek 发送消息"
              @keydown.enter.exact.prevent="sendMessage"
            />

            <div class="ai-panel-actions">
              <button type="button" class="ai-mini-action" aria-label="语音输入">
                <el-icon><Mic /></el-icon>
              </button>
              <button type="button" class="ai-send-btn" :class="{ active: canSend }" :disabled="!canSend" @click="sendMessage">
                <el-icon><Position /></el-icon>
              </button>
            </div>
          </div>

          <div class="ai-disclaimer">内容由 AI 生成，请自行核对重要信息</div>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.ai-page { height: 100%; min-height: 0; padding: 18px; background: linear-gradient(180deg, #f7f9ff 0%, #ffffff 45%, #fafbff 100%); overflow: hidden; box-sizing: border-box; }
.ai-shell { max-width: 1120px; height: 100%; min-height: 0; margin: 0 auto; display: flex; flex-direction: column; border-radius: 28px; background: rgba(255,255,255,0.82); backdrop-filter: blur(18px); box-shadow: 0 22px 60px rgba(112, 128, 255, 0.12); border: 1px solid rgba(214, 221, 255, 0.7); overflow: hidden; }
.ai-header { position: sticky; top: 0; z-index: 20; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 24px 28px 18px; border-bottom: 1px solid rgba(227, 233, 255, 0.8); background: rgba(255,255,255,0.92); backdrop-filter: blur(18px); }
.ai-brand { display: flex; align-items: center; gap: 16px; }
.ai-brand-badge { width: 44px; height: 44px; border-radius: 16px; display: grid; place-items: center; color: #5b6bff; background: linear-gradient(135deg, #eef2ff 0%, #dfe7ff 100%); box-shadow: inset 0 1px 0 rgba(255,255,255,0.9); }
.ai-brand h1 { margin: 0; font-size: 22px; color: #1f2430; }
.ai-brand p { margin: 4px 0 0; color: #7a8194; font-size: 13px; }
.ai-header-actions { display: flex; align-items: center; gap: 10px; }
.ai-content { flex: 1; min-height: 0; padding: 8px 24px 0; overflow-y: auto; overscroll-behavior: contain; }
.ai-content.is-empty { display: flex; align-items: center; justify-content: center; }
.ai-landing { width: 100%; max-width: 760px; margin: 0 auto; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 20px; transform: translateY(-20px); }
.ai-landing-title { display: inline-flex; align-items: center; gap: 10px; font-size: 22px; font-weight: 700; color: #1f2430; }
.ai-landing-composer { width: min(100%, 620px); }
.ai-timeline { max-width: 760px; margin: 0 auto; padding-top: 8px; }
.ai-thinking-body { display: flex; align-items: center; justify-content: center; min-height: 56px; border-radius: 18px; background: #fbfcff; border: 1px dashed #d8def7; color: #68708a; }
.ai-thinking-dots { display: inline-flex; align-items: center; gap: 10px; }
.ai-thinking-dot { width: 10px; height: 10px; border-radius: 999px; background: #9aa7ff; animation: aiPulse 1.2s infinite ease-in-out; }
.ai-thinking-dot:nth-child(2) { animation-delay: 0.15s; }
.ai-thinking-dot:nth-child(3) { animation-delay: 0.3s; }
@keyframes aiPulse { 0%, 80%, 100% { transform: scale(0.55); opacity: 0.45; } 40% { transform: scale(1); opacity: 1; } }
.ai-message-row { display: flex; margin-bottom: 18px; }
.ai-message-row.is-user { justify-content: flex-end; }
.ai-message-row.is-assistant { gap: 12px; align-items: flex-start; }
.ai-message-bubble, .ai-message-body { max-width: 82%; }
.ai-message-bubble.user { padding: 14px 16px; border-radius: 18px 18px 6px 18px; background: linear-gradient(135deg, #5b6bff 0%, #7d8cff 100%); color: #fff; box-shadow: 0 10px 24px rgba(91,107,255,0.22); white-space: pre-wrap; }
.ai-avatar { width: 38px; height: 38px; border-radius: 14px; display: grid; place-items: center; flex: 0 0 auto; font-size: 13px; font-weight: 700; }
.ai-avatar.assistant { background: linear-gradient(135deg, #e9edff 0%, #dbe2ff 100%); color: #5060ea; }
.ai-message-body { padding: 14px 16px; border-radius: 18px; background: #fff; border: 1px solid #edf0fb; box-shadow: 0 8px 24px rgba(34, 45, 90, 0.06); }
.ai-message-text { line-height: 1.8; color: #2a3142; white-space: pre-wrap; }
.ai-message-actions { display: flex; gap: 8px; margin-top: 10px; }
.ai-message-actions button, .ai-mini-action, .ai-attach-btn { border: none; background: transparent; cursor: pointer; color: #8a90a4; display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 10px; transition: all 0.2s ease; }
.ai-message-actions button:hover, .ai-mini-action:hover, .ai-attach-btn:hover { background: #f4f6ff; color: #4d5eea; }
.ai-footer { padding: 18px 24px 24px; border-top: 1px solid rgba(227, 233, 255, 0.8); background: linear-gradient(180deg, rgba(255,255,255,0.2), rgba(246,248,255,0.9)); }
.ai-composer { max-width: 760px; margin: 0 auto; }
.ai-quick-prompts { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 14px; }
.ai-chip { display: inline-flex; align-items: center; gap: 8px; border: 1px solid #dbe3ff; background: #fff; color: #4b5673; padding: 10px 14px; border-radius: 999px; box-shadow: 0 8px 20px rgba(33, 45, 90, 0.05); cursor: pointer; }
.ai-chip:hover { border-color: #aebcff; color: #4d5eea; }
.ai-input-panel { display: flex; align-items: stretch; gap: 12px; padding: 14px; border-radius: 24px; background: rgba(255,255,255,0.96); border: 1px solid #e5e9fb; box-shadow: 0 14px 34px rgba(36, 46, 92, 0.08); }
.ai-input { flex: 1; }
.ai-panel-actions { display: flex; flex-direction: column; gap: 10px; justify-content: flex-end; }
.ai-send-btn { width: 44px; height: 44px; border: none; border-radius: 14px; background: #e9edff; color: #7b84af; cursor: pointer; display: grid; place-items: center; transition: all 0.2s ease; }
.ai-send-btn.active { background: linear-gradient(135deg, #5b6bff 0%, #7d8cff 100%); color: white; box-shadow: 0 12px 22px rgba(91,107,255,0.24); }
.ai-disclaimer { margin-top: 10px; font-size: 12px; color: #8a90a4; text-align: center; }
@media (max-width: 768px) {
  .ai-page { padding: 10px; }
  .ai-shell { border-radius: 20px; }
  .ai-header { padding: 18px 16px 14px; flex-direction: column; align-items: flex-start; }
  .ai-content { padding: 8px 14px 0; }
  .ai-footer { padding: 14px; }
  .ai-input-panel { flex-direction: column; }
  .ai-panel-actions { flex-direction: row; justify-content: flex-end; }
  .ai-message-bubble, .ai-message-body { max-width: 100%; }
}
</style>
