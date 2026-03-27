<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { API_BASE_URL } from '@/config/api'

interface UserItem {
  id: number
  username?: string
  name?: string
  email?: string
  avatar?: string
}

interface PageResult<T> {
  total: number
  records: T[]
}

interface ApiResult<T> {
  code: number
  msg?: string
  data: T
}

interface ChatContact {
  id: number
  name: string
  email: string
  avatar: string
  timeText: string
}

interface ChatMessage {
  id: number
  sender: 'me' | 'bot'
  text: string
  time: string
}

const loadingUsers = ref(false)
const chatKeyword = ref('')
const chatInput = ref('')
const activeContactId = ref<number | null>(null)

const contacts = ref<ChatContact[]>([])
const defaultAvatar = 'https://i.pravatar.cc/80?img=11'

const messages = ref<Record<number, ChatMessage[]>>({})

const formatAvatar = (avatar?: string, idx = 0) => {
  const raw = avatar?.trim()
  if (!raw) return `https://i.pravatar.cc/80?img=${(idx % 60) + 1}`
  if (raw.startsWith('http://') || raw.startsWith('https://')) return raw
  return `${API_BASE_URL}/${raw.replace(/^\/+/, '')}`
}

const toContact = (item: UserItem, idx: number): ChatContact => ({
  id: item.id,
  name: item.name?.trim() || item.username?.trim() || `用户${item.id}`,
  email: item.email?.trim() || `${item.username || 'user'}@studyflow.com`,
  avatar: formatAvatar(item.avatar, idx),
  timeText: `${(idx % 23) + 1}小时前`
})

const filteredContacts = computed(() => {
  const kw = chatKeyword.value.trim().toLowerCase()
  if (!kw) return contacts.value
  return contacts.value.filter((c) => c.name.toLowerCase().includes(kw) || c.email.toLowerCase().includes(kw))
})

const activeContact = computed(() => contacts.value.find((c) => c.id === activeContactId.value) || null)

const currentMessages = computed(() => {
  const id = activeContactId.value
  if (!id) return []
  return messages.value[id] || []
})

const ensureDefaultConversation = (contactId: number, contactName: string) => {
  if (!messages.value[contactId]) {
    messages.value[contactId] = [
      { id: 1, sender: 'bot', text: `你好，我是 ${contactName}，很高兴和你聊天。`, time: '10:00' },
      { id: 2, sender: 'me', text: '你好，最近在做什么研学项目？', time: '10:01' },
      { id: 3, sender: 'bot', text: '我在整理资料分析模块，有空可以一起交流。', time: '10:02' },
      { id: 4, sender: 'me', text: '我这边在做行程规划和资料归档。', time: '10:03' },
      { id: 5, sender: 'bot', text: '那很不错，建议先确定主题再整理素材。', time: '10:04' },
      { id: 6, sender: 'me', text: '好的，我打算按时间线记录全过程。', time: '10:05' },
      { id: 7, sender: 'bot', text: '记得把关键节点和反思都写下来。', time: '10:06' },
      { id: 8, sender: 'me', text: '你觉得图片和文字比例怎么安排更好？', time: '10:07' },
      { id: 9, sender: 'bot', text: '建议 3:7，重点还是文字表达。', time: '10:08' },
      { id: 10, sender: 'me', text: '收到，我会在每个章节加一张代表图。', time: '10:09' },
      { id: 11, sender: 'bot', text: '非常好，这样阅读体验会更清晰。', time: '10:10' },
      { id: 12, sender: 'me', text: '我也在准备答辩PPT，怕内容太多。', time: '10:11' },
      { id: 13, sender: 'bot', text: '每页控制一个重点，避免堆叠信息。', time: '10:12' },
      { id: 14, sender: 'me', text: '明白了，我会按问题-过程-结果来讲。', time: '10:13' },
      { id: 15, sender: 'bot', text: '结构很棒，最后记得补充下一步计划。', time: '10:14' },
      { id: 16, sender: 'me', text: '谢谢建议，我今晚先出一版草稿。', time: '10:15' },
      { id: 17, sender: 'bot', text: '发我看看，我可以帮你快速检查。', time: '10:16' },
      { id: 18, sender: 'me', text: '好，稍后我整理完就发你。', time: '10:17' }
    ]
  }
}

const handleSelectContact = (id: number) => {
  activeContactId.value = id
  const contact = contacts.value.find((c) => c.id === id)
  ensureDefaultConversation(id, contact?.name || '好友')
}

const handleSendChat = () => {
  const text = chatInput.value.trim()
  const id = activeContactId.value
  if (!text || !id) return

  const list = messages.value[id] || []
  const nextId = (list[list.length - 1]?.id || 0) + 1
  list.push({ id: nextId, sender: 'me', text, time: new Date().toTimeString().slice(0, 5) })
  messages.value[id] = list
  chatInput.value = ''
}

const loadContacts = async () => {
  loadingUsers.value = true
  try {
    const { data } = await request.get<ApiResult<PageResult<UserItem>>>('/admin/page', {
      params: { page: 1, pageSize: 200 }
    })

    if (data.code !== 1) {
      ElMessage.error(data.msg || '加载用户失败')
      return
    }

    const users = data.data?.records || []
    contacts.value = users.map((u, idx) => toContact(u, idx))

    const firstContact = contacts.value[0]
    if (firstContact) {
      activeContactId.value = firstContact.id
      ensureDefaultConversation(firstContact.id, firstContact.name)
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || '加载用户失败，请稍后重试')
  } finally {
    loadingUsers.value = false
  }
}

onMounted(loadContacts)
</script>

<template>
  <div class="chat-page">
    <section class="chat-panel">
      <aside class="chat-sidebar">
        <div class="chat-owner">
          <el-avatar :size="42" :src="activeContact?.avatar || defaultAvatar" />
          <div>
            <div class="chat-owner-name">{{ activeContact?.name || '聊天平台' }}</div>
            <div class="chat-owner-mail">{{ activeContact?.email || 'StudyFlow' }}</div>
          </div>
        </div>

        <el-input v-model="chatKeyword" placeholder="搜索联系人" class="chat-search" />

        <div class="chat-sort">排序方式</div>

        <el-skeleton v-if="loadingUsers" :rows="5" animated />

        <div v-else class="chat-contact-list">
          <div
            v-for="item in filteredContacts"
            :key="item.id"
            class="chat-contact-item"
            :class="{ active: item.id === activeContactId }"
            @click="handleSelectContact(item.id)"
          >
            <el-avatar :size="34" :src="item.avatar || defaultAvatar" />
            <div class="chat-contact-main">
              <div class="chat-contact-top">
                <span class="chat-contact-name">{{ item.name }}</span>
                <span class="chat-contact-time">{{ item.timeText }}</span>
              </div>
              <div class="chat-contact-email">{{ item.email }}</div>
            </div>
          </div>
        </div>
      </aside>

      <section class="chat-main">
        <header class="chat-main-header">
          <div>
            <div class="chat-main-title">{{ activeContact?.name || '请选择联系人' }}</div>
            <div class="chat-main-status">在线</div>
          </div>
        </header>

        <div class="chat-message-list">
          <div
            v-for="msg in currentMessages"
            :key="msg.id"
            class="chat-msg-row"
            :class="msg.sender === 'me' ? 'is-me' : 'is-bot'"
          >
            <el-avatar v-if="msg.sender === 'bot'" :size="28" :src="activeContact?.avatar || defaultAvatar" class="chat-msg-avatar" />
            <div class="chat-msg-bubble-wrap">
              <div class="chat-msg-meta">{{ msg.sender === 'bot' ? activeContact?.name : '我' }} {{ msg.time }}</div>
              <div class="chat-msg-bubble">{{ msg.text }}</div>
            </div>
            <el-avatar v-if="msg.sender === 'me'" :size="28" src="https://i.pravatar.cc/80?img=15" class="chat-msg-avatar" />
          </div>
        </div>

        <footer class="chat-input-wrap">
          <el-input
            v-model="chatInput"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="输入消息"
            @keydown.enter.exact.prevent="handleSendChat"
          />
          <div class="chat-input-footer">
            <el-button type="primary" :disabled="!activeContactId" @click="handleSendChat">发送</el-button>
          </div>
        </footer>
      </section>
    </section>
  </div>
</template>

<style scoped>
.chat-page {
  width: 100%;
  margin: 0;
}

.chat-panel {
  display: grid;
  grid-template-columns: 320px 1fr;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 14px;
  overflow: hidden;
  height: calc(100vh - 140px);
  min-height: 680px;
}

.chat-sidebar {
  border-right: 1px solid #ebeef5;
  padding: 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
  overflow: hidden;
}

.chat-owner {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-owner-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.chat-owner-mail {
  font-size: 12px;
  color: #909399;
}

.chat-search {
  margin-top: 6px;
}

.chat-sort {
  color: #909399;
  font-size: 12px;
  padding-left: 2px;
}

.chat-contact-list {
  flex: 1;
  min-height: 0;
  max-height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 4px;
}

.chat-contact-list::-webkit-scrollbar,
.chat-message-list::-webkit-scrollbar {
  width: 10px;
}

.chat-contact-list::-webkit-scrollbar-track,
.chat-message-list::-webkit-scrollbar-track {
  background: #f3f4f6;
  border-radius: 999px;
}

.chat-contact-list::-webkit-scrollbar-thumb,
.chat-message-list::-webkit-scrollbar-thumb {
  background: #b8bfcd;
  border-radius: 999px;
  border: 2px solid #f3f4f6;
}

.chat-contact-list::-webkit-scrollbar-thumb:hover,
.chat-message-list::-webkit-scrollbar-thumb:hover {
  background: #9aa3b5;
}

.chat-contact-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
}

.chat-contact-item:hover,
.chat-contact-item.active {
  background: #f5f7fa;
}

.chat-contact-main {
  flex: 1;
  min-width: 0;
}

.chat-contact-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.chat-contact-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.chat-contact-time {
  font-size: 12px;
  color: #b1b3bc;
}

.chat-contact-email {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-main {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-main-header {
  min-height: 66px;
  border-bottom: 1px solid #ebeef5;
  padding: 14px 20px;
  display: flex;
  align-items: center;
}

.chat-main-title {
  font-size: 30px;
  line-height: 1;
  font-weight: 500;
}

.chat-main-status {
  margin-top: 4px;
  color: #66c39a;
  font-size: 12px;
}

.chat-message-list {
  flex: 1;
  min-height: 0;
  max-height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  padding: 20px;
  background: #fff;
}

.chat-msg-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
}

.chat-msg-row.is-me {
  justify-content: flex-end;
}

.chat-msg-bubble-wrap {
  max-width: min(70%, 680px);
}

.chat-msg-meta {
  font-size: 12px;
  color: #9aa1ae;
  margin-bottom: 3px;
}

.chat-msg-bubble {
  background: #f2f3f5;
  color: #303133;
  border-radius: 8px;
  padding: 10px 12px;
  line-height: 1.5;
  font-size: 14px;
}

.chat-msg-row.is-me .chat-msg-bubble {
  background: #e9edff;
}

.chat-input-wrap {
  border-top: 1px solid #ebeef5;
  padding: 14px;
}

.chat-input-footer {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}
</style>
