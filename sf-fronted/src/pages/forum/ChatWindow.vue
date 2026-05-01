<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { API_BASE_URL } from '@/config/api'

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
  online: boolean
  timeText: string
}

interface ServerMessage {
  id: number
  senderId: number
  receiverId: number
  content: string
  createTime: string
}

interface ChatMessage {
  id: number
  sender: 'me' | 'bot'
  text: string
  time: string
}

interface FriendRequestItem {
  id: number
  requesterId: number
  requesterName?: string
  requesterUsername?: string
  requesterEmail?: string
  requesterAvatar?: string
  status?: number
  createTime?: string
}

const loadingUsers = ref(false)
const searchingUsers = ref(false)
const addFriendVisible = ref(false)
const addFriendKeyword = ref('')
const addFriendResult = ref<any[]>([])
const pendingRequests = ref<FriendRequestItem[]>([])
const chatKeyword = ref('')
const chatInput = ref('')
const activeContactId = ref<number | null>(null)
const myUserId = ref<number | null>(null)
const ws = ref<WebSocket | null>(null)
const wsConnected = ref(false)
const wsConnecting = ref(false)
let wsConnectingPromise: Promise<void> | null = null
let reconnectTimer: number | null = null
let reconnectAttempt = 0
let shouldReconnect = false

const contacts = ref<ChatContact[]>([])
const defaultAvatar = 'https://i.pravatar.cc/80?img=11'
const messages = ref<Record<number, ChatMessage[]>>({})
const messageListRef = ref<HTMLElement | null>(null)

const formatAvatar = (avatar?: string, idx = 0) => {
  const raw = avatar?.trim()
  if (!raw) return `https://i.pravatar.cc/80?img=${(idx % 60) + 1}`
  if (raw.startsWith('http://') || raw.startsWith('https://')) return raw
  return `${API_BASE_URL}/${raw.replace(/^\/+/, '')}`
}

const safeAvatarSrc = (avatar?: string, idx = 0) => formatAvatar(avatar || '', idx)

const filteredContacts = computed(() => {
  const kw = chatKeyword.value.trim().toLowerCase()
  if (!kw) return contacts.value
  return contacts.value.filter((c) => c.name.toLowerCase().includes(kw) || c.email.toLowerCase().includes(kw))
})

const activeContact = computed(() => contacts.value.find((c) => c.id === activeContactId.value) || null)

const connectionStatusText = computed(() => {
  if (wsConnected.value) return '在线'
  if (wsConnecting.value) return '连接中...'
  return '离线'
})

const myAvatar = computed(() => {
  const avatar = sessionStorage.getItem('avatar') || localStorage.getItem('avatar') || ''
  return formatAvatar(avatar, myUserId.value || 0)
})

const currentMessages = computed(() => {
  const id = activeContactId.value
  if (!id) return []
  return messages.value[id] || []
})

const formatTime = (dateText?: string) => {
  if (!dateText) return ''
  const d = new Date(dateText)
  if (Number.isNaN(d.getTime())) return ''
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const decodeJwtPayload = (token: string) => {
  const part = token.split('.')[1] || ''
  const base64 = part.replace(/-/g, '+').replace(/_/g, '/')
  const pad = '='.repeat((4 - (base64.length % 4)) % 4)
  return JSON.parse(atob(base64 + pad))
}

const scrollMessageListToBottom = async () => {
  await nextTick()
  const el = messageListRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

const normalizeMessageText = (text?: string) => {
  return (text ?? '').replace(/\s+$/g, '')
}

const appendRealtimeMessage = (m: ServerMessage) => {
  const partnerId = m.senderId === myUserId.value ? m.receiverId : m.senderId
  const list = messages.value[partnerId] || []
  list.push({
    id: m.id,
    sender: m.senderId === myUserId.value ? 'me' : 'bot',
    text: normalizeMessageText(m.content),
    time: formatTime(m.createTime)
  })
  messages.value[partnerId] = [...list]
  if (partnerId === activeContactId.value) {
    void scrollMessageListToBottom()
  }
}

const clearReconnectTimer = () => {
  if (reconnectTimer !== null) {
    window.clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

const scheduleReconnect = () => {
  if (!shouldReconnect) return
  if (reconnectTimer !== null) return

  const delay = Math.min(1000 * Math.pow(2, reconnectAttempt), 10000)
  reconnectAttempt += 1
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = null
    connectWs().catch(() => {
      scheduleReconnect()
    })
  }, delay)
}

const loadMyInfo = async () => {
  const token = sessionStorage.getItem('token') || localStorage.getItem('token')
  if (!token) return

  const payload = decodeJwtPayload(token)
  const uid = Number(payload?.userId)
  if (!Number.isNaN(uid) && uid > 0) {
    myUserId.value = uid
  }
}

const loadConversation = async (friendId: number) => {
  try {
    const { data } = await request.get<ApiResult<ServerMessage[]>>('/chat/conversation', { params: { friendId } })
    if (data.code !== 1) {
      ElMessage.error(data.msg || '加载会话失败')
      return
    }

    const list: ChatMessage[] = (data.data || []).map((m) => ({
      id: m.id,
      sender: m.senderId === myUserId.value ? 'me' : 'bot',
      text: normalizeMessageText(m.content),
      time: formatTime(m.createTime)
    }))
    messages.value[friendId] = list
    if (friendId === activeContactId.value) {
      void scrollMessageListToBottom()
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || '加载会话失败，请稍后重试')
  }
}

const loadContacts = async () => {
  loadingUsers.value = true
  try {
    const { data } = await request.get<ApiResult<any[]>>('/chat/friends')
    if (data.code !== 1) {
      ElMessage.error(data.msg || '加载好友失败')
      return
    }

    const list = data.data || []
    const mapped: ChatContact[] = list.map((u: any, idx: number) => ({
      id: Number(u.userId),
      name: (u.name || u.username || `用户${u.userId}`).trim(),
      email: (u.email || `${u.username || 'user'}@studyflow.com`).trim(),
      avatar: formatAvatar(u.avatar, idx),
      online: !!u.online,
      timeText: `${(idx % 23) + 1}小时前`
    }))
    contacts.value = mapped

    if (!mapped.length) {
      activeContactId.value = null
      return
    }

    const keepCurrent = mapped.some((c) => c.id === activeContactId.value)
    if (!keepCurrent) {
      activeContactId.value = mapped[0].id
    }

    if (activeContactId.value) {
      await loadConversation(activeContactId.value)
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || '加载好友失败，请稍后重试')
  } finally {
    loadingUsers.value = false
  }
}

const loadPendingRequests = async () => {
  try {
    const { data } = await request.get<ApiResult<FriendRequestItem[]>>('/chat/friend/requests')
    if (data.code === 1) {
      pendingRequests.value = data.data || []
    }
  } catch {
    // ignore
  }
}

const refreshFriendData = async () => {
  await Promise.all([loadContacts(), loadPendingRequests()])
}

const connectWs = () => {
  if (!shouldReconnect) {
    shouldReconnect = true
  }

  if (ws.value && ws.value.readyState === WebSocket.OPEN) {
    return Promise.resolve()
  }
  if (wsConnectingPromise) {
    return wsConnectingPromise
  }

  wsConnectingPromise = new Promise<void>((resolve, reject) => {
    const token = sessionStorage.getItem('token') || localStorage.getItem('token')
    const uid = myUserId.value

    if (!token && !uid) {
      wsConnecting.value = false
      wsConnectingPromise = null
      reject(new Error('token and user id not ready'))
      return
    }

    const wsBase = API_BASE_URL.replace(/^http/i, 'ws').replace(/\/$/, '')
    const candidates = [
      token ? `${wsBase}/ws/chat?token=${encodeURIComponent(token)}` : '',
      uid ? `${wsBase}/ws/user/${uid}` : ''
    ].filter(Boolean)

    const tryConnect = (index: number) => {
      if (index >= candidates.length) {
        wsConnecting.value = false
        wsConnectingPromise = null
        wsConnected.value = false
        reject(new Error('all websocket endpoints failed'))
        return
      }

      const socket = new WebSocket(candidates[index])
      ws.value = socket
      let settled = false

      socket.onopen = () => {
        settled = true
        wsConnected.value = true
        wsConnecting.value = false
        reconnectAttempt = 0
        clearReconnectTimer()
        wsConnectingPromise = null
        resolve()
      }

      socket.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data)
          if (msg?.type === 'friend_request_approved' || msg?.type === 'friend_request_rejected') {
            void refreshFriendData()
            return
          }
          if (!msg || !msg.id) return
          appendRealtimeMessage(msg as ServerMessage)
        } catch {
          // ignore
        }
      }

      socket.onclose = () => {
        if (!settled) {
          if (ws.value === socket) {
            ws.value = null
          }
          tryConnect(index + 1)
          return
        }
        wsConnected.value = false
        wsConnecting.value = false
        if (ws.value === socket) {
          ws.value = null
        }
        wsConnectingPromise = null
        scheduleReconnect()
      }

      socket.onerror = () => {
        wsConnected.value = false
        wsConnecting.value = false
        if (ws.value === socket) {
          ws.value = null
        }
        try {
          socket.close()
        } catch {
          // ignore
        }
      }
    }

    tryConnect(0)
  })

  return wsConnectingPromise
}

const handleSelectContact = async (id: number) => {
  activeContactId.value = id
  if (!myUserId.value) {
    await loadMyInfo()
  }
  await connectWs().catch(() => undefined)
  await loadConversation(id)
}

const handleSendChat = async () => {
  const text = chatInput.value.trim()
  const id = activeContactId.value
  if (!text || !id) return

  if (!myUserId.value) {
    await loadMyInfo()
  }

  if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
    wsConnecting.value = true
    try {
      await connectWs()
    } catch {
      ElMessage.warning('聊天连接未建立，请稍后重试')
      return
    }
  }

  ws.value?.send(JSON.stringify({ toUserId: id, content: text }))
  chatInput.value = ''
  void scrollMessageListToBottom()
}

const handleSearchUser = async () => {
  const keyword = addFriendKeyword.value.trim()
  if (!keyword) {
    addFriendResult.value = []
    return
  }

  searchingUsers.value = true
  try {
    const [nameResp, usernameResp] = await Promise.all([
      request.get<ApiResult<{ records: any[] }>>('/admin/page', {
        params: { page: 1, pageSize: 100, name: keyword }
      }),
      request.get<ApiResult<{ records: any[] }>>('/admin/page', {
        params: { page: 1, pageSize: 100, username: keyword }
      })
    ])

    if (nameResp.data.code !== 1 && usernameResp.data.code !== 1) {
      ElMessage.error(nameResp.data.msg || usernameResp.data.msg || '搜索用户失败')
      return
    }

    const merged = [...(nameResp.data.data?.records || []), ...(usernameResp.data.data?.records || [])]
    const dedupMap = new Map<number, any>()
    merged.forEach((u: any) => {
      const uid = Number(u.id)
      if (!uid || uid === myUserId.value) return
      dedupMap.set(uid, u)
    })
    addFriendResult.value = Array.from(dedupMap.values())
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || '搜索用户失败，请稍后重试')
  } finally {
    searchingUsers.value = false
  }
}

const handleAddFriend = async (friendId: number) => {
  const alreadyFriend = contacts.value.some((item) => item.id === friendId)
  const alreadyRequested = pendingRequests.value.some((item) => Number(item.requesterId) === friendId)
  if (alreadyFriend) {
    ElMessage.info('已经是好友了')
    return
  }
  if (alreadyRequested) {
    ElMessage.warning('该用户已在待处理列表中')
    return
  }

  try {
    const { data } = await request.put<ApiResult<null>>('/chat/friend/add', { friendId })
    if (data.code !== 1) {
      ElMessage.error(data.msg || '添加好友失败')
      return
    }
    ElMessage.success('好友申请已发送，请等待对方同意')
    addFriendVisible.value = false
    addFriendKeyword.value = ''
    addFriendResult.value = []
    await loadPendingRequests()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || '添加好友失败，请稍后重试')
  }
}

const approveRequest = async (requestId: number) => {
  try {
    const { data } = await request.put<ApiResult<null>>('/chat/friend/approve', null, { params: { requestId } })
    if (data.code !== 1) {
      ElMessage.error(data.msg || '同意失败')
      return
    }
    ElMessage.success('已同意好友请求')
    await refreshFriendData()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || '同意失败，请稍后重试')
  }
}

const rejectRequest = async (requestId: number) => {
  try {
    const { data } = await request.put<ApiResult<null>>('/chat/friend/reject', null, { params: { requestId } })
    if (data.code !== 1) {
      ElMessage.error(data.msg || '拒绝失败')
      return
    }
    ElMessage.success('已拒绝好友请求')
    await loadPendingRequests()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || '拒绝失败，请稍后重试')
  }
}

watch(
  currentMessages,
  () => {
    void scrollMessageListToBottom()
  },
  { immediate: true }
)

onMounted(async () => {
  shouldReconnect = true
  await loadMyInfo()
  await refreshFriendData()
  await connectWs().catch(() => undefined)
  void scrollMessageListToBottom()
})

onBeforeUnmount(() => {
  shouldReconnect = false
  clearReconnectTimer()
  if (ws.value && ws.value.readyState === WebSocket.OPEN) {
    ws.value.close()
  }
  ws.value = null
  wsConnected.value = false
  wsConnecting.value = false
  wsConnectingPromise = null
})
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
          <el-button class="add-friend-btn" size="small" type="primary" plain @click="addFriendVisible = true">
            添加好友
          </el-button>
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
                <span class="chat-contact-state" :class="{ online: item.online }">
                  <span class="chat-contact-dot"></span>
                  {{ item.online ? '在线' : '离线' }}
                </span>
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
            <div class="chat-main-status" :class="{ connected: wsConnected }">{{ connectionStatusText }}</div>
          </div>
        </header>

        <div ref="messageListRef" class="chat-message-list">
          <div
            v-for="msg in currentMessages"
            :key="msg.id"
            class="chat-msg-row"
            :class="msg.sender === 'me' ? 'is-me' : 'is-bot'"
          >
            <el-avatar v-if="msg.sender === 'bot'" :size="28" :src="safeAvatarSrc(activeContact?.avatar, activeContact?.id || 0)" class="chat-msg-avatar" />
            <div class="chat-msg-bubble-wrap">
              <div class="chat-msg-meta">{{ msg.sender === 'bot' ? activeContact?.name : '我' }} {{ msg.time }}</div>
              <div class="chat-msg-bubble">{{ msg.text }}</div>
            </div>
            <el-avatar v-if="msg.sender === 'me'" :size="28" :src="myAvatar || defaultAvatar" class="chat-msg-avatar" />
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

    <el-dialog v-model="addFriendVisible" title="添加好友" width="520px">
      <div class="add-friend-search-wrap">
        <el-input
          v-model="addFriendKeyword"
          placeholder="输入用户名或姓名搜索"
          clearable
          @keyup.enter="handleSearchUser"
        >
          <template #append>
            <el-button :loading="searchingUsers" @click="handleSearchUser">搜索</el-button>
          </template>
        </el-input>
      </div>

      <div class="add-friend-result">
        <el-empty v-if="!searchingUsers && addFriendResult.length === 0" description="暂无搜索结果" />
        <div v-else class="add-friend-list">
          <div v-for="u in addFriendResult" :key="u.id" class="add-friend-item">
            <div class="add-friend-userinfo">
              <el-avatar :size="34" :src="formatAvatar(u.avatar, Number(u.id))" />
              <div class="add-friend-text">
                <div class="add-friend-name">{{ u.name || '未命名用户' }}</div>
                <div class="add-friend-username">@{{ u.username || '-' }}</div>
              </div>
            </div>
            <el-button type="primary" size="small" @click="handleAddFriend(Number(u.id))">添加</el-button>
          </div>
        </div>
      </div>

      <el-divider content-position="left">
        待处理申请 <span v-if="pendingRequests.length">({{ pendingRequests.length }})</span>
      </el-divider>
      <div class="pending-request-list">
        <el-empty v-if="pendingRequests.length === 0" description="暂无待处理申请" />
        <div v-else class="pending-request-item" v-for="req in pendingRequests" :key="req.id">
          <div class="pending-request-userinfo">
            <el-avatar :size="34" :src="formatAvatar(req.requesterAvatar, Number(req.requesterId))" />
            <div>
              <div class="pending-request-name">{{ req.requesterName || req.requesterUsername || '未知用户' }}</div>
              <div class="pending-request-meta">@{{ req.requesterUsername || '-' }}</div>
            </div>
          </div>
          <div class="pending-request-actions">
            <el-button size="small" type="primary" @click="approveRequest(Number(req.id))">同意</el-button>
            <el-button size="small" @click="rejectRequest(Number(req.id))">拒绝</el-button>
          </div>
        </div>
      </div>
    </el-dialog>
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

.add-friend-btn {
  margin-left: auto;
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
.chat-message-list::-webkit-scrollbar,
.add-friend-list::-webkit-scrollbar {
  width: 10px;
}

.chat-contact-list::-webkit-scrollbar-track,
.chat-message-list::-webkit-scrollbar-track,
.add-friend-list::-webkit-scrollbar-track {
  background: #f3f4f6;
  border-radius: 999px;
}

.chat-contact-list::-webkit-scrollbar-thumb,
.chat-message-list::-webkit-scrollbar-thumb,
.add-friend-list::-webkit-scrollbar-thumb {
  background: #b8bfcd;
  border-radius: 999px;
  border: 2px solid #f3f4f6;
}

.chat-contact-list::-webkit-scrollbar-thumb:hover,
.chat-message-list::-webkit-scrollbar-thumb:hover,
.add-friend-list::-webkit-scrollbar-thumb:hover {
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

.chat-contact-state {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #b1b3bc;
}

.chat-contact-state.online {
  color: #66c39a;
}

.chat-contact-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #b1b3bc;
  display: inline-block;
}

.chat-contact-state.online .chat-contact-dot {
  background: #66c39a;
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
  color: #909399;
  font-size: 12px;
}

.chat-main-status.connected {
  color: #66c39a;
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
  width: 100%;
}

.chat-msg-row.is-me {
  justify-content: flex-end;
}

.chat-msg-row.is-bot {
  justify-content: flex-start;
}

.chat-msg-bubble-wrap {
  max-width: min(72%, 680px);
  display: flex;
  flex-direction: column;
}

.chat-msg-row.is-me .chat-msg-bubble-wrap {
  align-items: flex-end;
}

.chat-msg-row.is-bot .chat-msg-bubble-wrap {
  align-items: flex-start;
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
  display: inline-block;
  width: fit-content;
  max-width: 100%;
  white-space: pre-wrap;
  word-break: break-word;
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

.add-friend-search-wrap {
  margin-bottom: 12px;
}

.add-friend-result {
  max-height: 260px;
  overflow: hidden;
}

.add-friend-list {
  max-height: 220px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.add-friend-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px;
}

.add-friend-userinfo,
.pending-request-userinfo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.add-friend-name,
.pending-request-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.add-friend-username,
.pending-request-meta {
  font-size: 12px;
  color: #909399;
}

.pending-request-list {
  max-height: 260px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.pending-request-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px;
}

.pending-request-actions {
  display: flex;
  gap: 8px;
}
</style>
