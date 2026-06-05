<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { connectChatSocket, getChatSocket, onChatSocketClose, onChatSocketMessage, sendChatSocketMessage } from '@/utils/chatSocket'
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
  lastMessage?: string
  lastMessageTime?: string
  unreadCount?: number
  lastMessageIsRead?: number
}

interface ServerMessage {
  id: number
  senderId: number
  receiverId: number
  content: string
  createTime: string
}

interface ShareCardMessage {
  type?: string
  projectId?: number
  title?: string
  summary?: string
  cover?: string
  link?: string
}

interface ChatMessage {
  id: number
  sender: 'me' | 'bot'
  text: string
  time: string
  shareCard?: ShareCardMessage | null
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
const wsConnected = ref(false)
const wsConnecting = ref(false)
const contacts = ref<ChatContact[]>([])
const defaultAvatar = 'https://i.pravatar.cc/80?img=11'
const messages = ref<Record<number, ChatMessage[]>>({})
const messageListRef = ref<HTMLElement | null>(null)

const chatRouter = useRouter()

// 加载失败的头像自动回退到默认头像
const brokenAvatars = reactive(new Set<string>())
const getSafeSrc = (src: string) => (src && !brokenAvatars.has(src) ? src : defaultAvatar)
const onAvatarError = (src: string) => { if (src) brokenAvatars.add(src) }

let offMessage: (() => void) | null = null
let offClose: (() => void) | null = null

const formatAvatar = (avatar?: string, idx = 0) => {
  const raw = avatar?.trim()
  if (!raw) return `https://i.pravatar.cc/80?img=${(idx % 60) + 1}`
  if (raw.startsWith('http://') || raw.startsWith('https://')) return raw
  return `${API_BASE_URL}/${raw.replace(/^\/+/, '')}`
}

const safeAvatarSrc = (avatar?: string, idx = 0) => formatAvatar(avatar || '', idx)

const openShareLink = (url: string) => {
  if (!url) return
  // 提取项目ID，根据当前角色构造正确路由
  if (url.includes('/projects/detail/')) {
    const projectId = extractProjectId(url)
    if (projectId) {
      const role = sessionStorage.getItem('userRole') || sessionStorage.getItem('role') || ''
      const rolePathMap: Record<string, string> = {
        '管理员': `/projects/detail/${projectId}`,
        '老师': `/teacher/projects/detail/${projectId}`,
        '学生': `/student/projects/detail/${projectId}`
      }
      const targetPath = rolePathMap[role] || `/student/projects/detail/${projectId}`
      chatRouter.push(targetPath)
      return
    }
  }
  window.open(url, '_blank', 'noopener,noreferrer')
}

const extractProjectId = (url: string): string | null => {
  try {
    const path = new URL(url).pathname
    const match = path.match(/\/projects\/detail\/(\d+)/)
    return match?.[1] || null
  } catch {
    const match = url.match(/\/projects\/detail\/(\d+)/)
    return match?.[1] || null
  }
}

const sortContacts = (list: ChatContact[]) => {
  return [...list].sort((a, b) => {
    const aUnread = Number(a.unreadCount || 0)
    const bUnread = Number(b.unreadCount || 0)
    if (aUnread > 0 || bUnread > 0) {
      if (aUnread !== bUnread) return bUnread - aUnread
    } else if (a.online !== b.online) {
      return Number(b.online) - Number(a.online)
    }
    const aTime = new Date(a.lastMessageTime || '').getTime()
    const bTime = new Date(b.lastMessageTime || '').getTime()
    if (!Number.isNaN(aTime) && !Number.isNaN(bTime) && aTime !== bTime) {
      return bTime - aTime
    }
    return a.name.localeCompare(b.name, 'zh-Hans-CN')
  })
}

const filteredContacts = computed(() => {
  const kw = chatKeyword.value.trim().toLowerCase()
  const list = sortContacts(contacts.value)
  if (!kw) return list
  return list.filter((c) => c.name.toLowerCase().includes(kw) || c.email.toLowerCase().includes(kw))
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

const formatPreviewText = (text?: string) => {
  const value = (text || '').trim()
  if (!value) return '暂无消息'
  // 检测分享消息 JSON，显示友好预览
  try {
    const parsed = JSON.parse(value)
    if (parsed?.type === 'project_share' && parsed?.title) {
      return `[分享] ${parsed.title}`
    }
  } catch {
    // 非 JSON，正常截断
  }
  return value.length > 24 ? `${value.slice(0, 24)}...` : value
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

const parseShareCard = (text?: string): ShareCardMessage | null => {
  if (!text) return null
  try {
    const data = JSON.parse(text)
    if (data?.type === 'project_share' && data.projectId && data.title && data.link) {
      return data as ShareCardMessage
    }
  } catch {
    // ignore
  }
  return null
}

const appendRealtimeMessage = (m: ServerMessage) => {
  const partnerId = m.senderId === myUserId.value ? m.receiverId : m.senderId
  const list = messages.value[partnerId] || []
  list.push({
    id: m.id,
    sender: m.senderId === myUserId.value ? 'me' : 'bot',
    text: normalizeMessageText(m.content),
    time: formatTime(m.createTime),
    shareCard: parseShareCard(m.content)
  })
  messages.value[partnerId] = [...list]
  const contact = contacts.value.find((item) => item.id === partnerId)
  if (contact) {
    contact.lastMessage = m.content
    contact.lastMessageTime = m.createTime
    contact.unreadCount = m.senderId === myUserId.value ? Number(contact.unreadCount || 0) : Number(contact.unreadCount || 0) + 1
    contact.lastMessageIsRead = m.senderId === myUserId.value ? 1 : 0
    contacts.value = sortContacts([...contacts.value])
  }
  if (partnerId === activeContactId.value) {
    void scrollMessageListToBottom()
  }
}

const loadMyInfo = async () => {
  const token = sessionStorage.getItem('token') || localStorage.getItem('token')
  if (!token) return
  const payload = decodeJwtPayload(token)
  const uid = Number(payload?.userId)
  if (!Number.isNaN(uid) && uid > 0) {
    myUserId.value = uid
    wsConnecting.value = true
    try {
      await connectChatSocket(token, uid)
    } catch {
      wsConnecting.value = false
    }
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
      time: formatTime(m.createTime),
      shareCard: parseShareCard(m.content)
    }))
    messages.value[friendId] = list
    const contact = contacts.value.find((item) => item.id === friendId)
    if (contact) {
      contact.unreadCount = 0
      if (list.length) {
        const last = list[list.length - 1]
        if (last) {
          contact.lastMessage = last.text
          contact.lastMessageTime = last.time
          contact.lastMessageIsRead = 1
        }
      }
      contacts.value = sortContacts([...contacts.value])
    }
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
      timeText: u.lastMessageTime || `${(idx % 23) + 1}小时前`,
      lastMessage: u.lastMessage,
      lastMessageTime: u.lastMessageTime,
      unreadCount: Number(u.unreadCount || 0),
      lastMessageIsRead: Number(u.lastMessageIsRead || 0)
    }))
    contacts.value = sortContacts(mapped)
    if (!mapped.length) {
      activeContactId.value = null
      return
    }
    const keepCurrent = mapped.some((c) => c.id === activeContactId.value)
    const firstContact = mapped[0]
    if (!keepCurrent && firstContact) {
      activeContactId.value = firstContact.id
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

const syncSocketState = () => {
  wsConnected.value = !!getChatSocket()
  wsConnecting.value = false
}

const handleSelectContact = async (id: number) => {
  activeContactId.value = id
  await loadConversation(id)
}

const handleSendChat = async () => {
  const text = chatInput.value.trim()
  const id = activeContactId.value
  if (!text || !id) return
  if (!sendChatSocketMessage(JSON.stringify({ toUserId: id, content: text }))) {
    ElMessage.warning('聊天连接未建立，请重新登录')
    return
  }
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
      request.get<ApiResult<{ records: any[] }>>('/admin/page', { params: { page: 1, pageSize: 100, name: keyword } }),
      request.get<ApiResult<{ records: any[] }>>('/admin/page', { params: { page: 1, pageSize: 100, username: keyword } })
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

watch(currentMessages, () => { void scrollMessageListToBottom() }, { immediate: true })

onMounted(async () => {
  offMessage = onChatSocketMessage((event) => {
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
  })
  offClose = onChatSocketClose(() => {
    syncSocketState()
  })
  await loadMyInfo()
  syncSocketState()
  await refreshFriendData()
  await scrollMessageListToBottom()
})

onBeforeUnmount(() => {
  offMessage?.()
  offClose?.()
  offMessage = null
  offClose = null
})
</script>

<template>
  <div class="chat-page">
    <section class="chat-panel">
      <aside class="chat-sidebar">
        <div class="chat-owner">
          <el-avatar :size="42" :src="getSafeSrc(activeContact?.avatar || defaultAvatar)" @error="onAvatarError(activeContact?.avatar || '')" />
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
          <div v-for="item in filteredContacts" :key="item.id" class="chat-contact-item" :class="{ active: item.id === activeContactId, unread: (item.unreadCount || 0) > 0 }" @click="handleSelectContact(item.id)">
            <el-avatar :size="34" :src="getSafeSrc(item.avatar || defaultAvatar)" @error="onAvatarError(item.avatar)" />
            <div class="chat-contact-main">
              <div class="chat-contact-top">
                <span class="chat-contact-name">{{ item.name }}</span>
                <span class="chat-contact-state" :class="{ online: item.online }">
                  <span class="chat-contact-dot"></span>
                  {{ item.online ? '在线' : '离线' }}
                </span>
              </div>
              <div class="chat-contact-preview">
                <span class="chat-contact-email" :class="{ unread: (item.unreadCount || 0) > 0 }">{{ formatPreviewText(item.lastMessage) }}</span>
                <span v-if="item.unreadCount && item.unreadCount > 0" class="chat-contact-badge">{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</span>
              </div>
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
          <div v-for="msg in currentMessages" :key="msg.id" class="chat-msg-row" :class="msg.sender === 'me' ? 'is-me' : 'is-bot'">
            <el-avatar v-if="msg.sender === 'bot'" :size="28" :src="getSafeSrc(safeAvatarSrc(activeContact?.avatar, activeContact?.id || 0))" class="chat-msg-avatar" @error="onAvatarError(safeAvatarSrc(activeContact?.avatar, activeContact?.id || 0))" />
            <div class="chat-msg-bubble-wrap">
              <div class="chat-msg-meta">{{ msg.sender === 'bot' ? activeContact?.name : '我' }} {{ msg.time }}</div>
              <div v-if="msg.shareCard" class="share-card" @click="msg.shareCard?.link && openShareLink(msg.shareCard.link)">
                <img :src="msg.shareCard.cover || defaultAvatar" class="share-card-cover" alt="" />
                <div class="share-card-body">
                  <div class="share-card-title">{{ msg.shareCard.title }}</div>
                  <div class="share-card-summary">{{ msg.shareCard.summary }}</div>
                  <div class="share-card-hint">点击查看项目详情 →</div>
                </div>
              </div>
              <div v-else class="chat-msg-bubble">{{ msg.text }}</div>
            </div>
            <el-avatar v-if="msg.sender === 'me'" :size="28" :src="getSafeSrc(myAvatar || defaultAvatar)" @error="onAvatarError(myAvatar)" class="chat-msg-avatar" />
          </div>
        </div>

        <footer class="chat-input-wrap">
          <div class="chat-composer">
            <el-input v-model="chatInput" class="chat-composer-input" type="textarea" :rows="4" resize="none" placeholder="输入消息" @keydown.enter.exact.prevent="handleSendChat" />
            <div class="chat-composer-footer">
              <button class="send-btn" :disabled="!activeContactId" @click="handleSendChat">
                <div class="send-btn-svg-wrapper-1">
                  <div class="send-btn-svg-wrapper">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true">
                      <path fill="none" d="M0 0h24v24H0V0z"></path>
                      <path fill="currentColor" d="M1.946 9.315c-.522-.174-.527-.455.01-.634l19.087-6.362c.529-.176.832.12.684.638l-5.454 19.086c-.15.529-.455.547-.679.045L12 14l6-8-8 6-8.054-2.685z"></path>
                    </svg>
                  </div>
                </div>
                <span>发送</span>
              </button>
            </div>
          </div>
        </footer>
      </section>
    </section>

    <el-dialog v-model="addFriendVisible" title="添加好友" width="520px">
      <div class="add-friend-search-wrap">
        <el-input v-model="addFriendKeyword" placeholder="输入用户名或姓名搜索" clearable @keyup.enter="handleSearchUser">
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
              <el-avatar :size="34" :src="getSafeSrc(formatAvatar(u.avatar, Number(u.id)) || defaultAvatar)" @error="onAvatarError(formatAvatar(u.avatar, Number(u.id)))" />
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
            <el-avatar :size="34" :src="getSafeSrc(formatAvatar(req.requesterAvatar, Number(req.requesterId)) || defaultAvatar)" @error="onAvatarError(formatAvatar(req.requesterAvatar, Number(req.requesterId)))" />
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
.chat-page { width: 100%; margin: 0; }
.chat-panel { display: grid; grid-template-columns: 320px 1fr; background: #fff; border: 1px solid #ebeef5; border-radius: 14px; overflow: hidden; height: calc(100vh - 140px); min-height: 680px; }
.chat-sidebar { border-right: 1px solid #ebeef5; padding: 14px 12px; display: flex; flex-direction: column; gap: 10px; min-height: 0; overflow: hidden; }
.chat-owner { display: flex; align-items: center; gap: 10px; }
.add-friend-btn { margin-left: auto; }
.chat-owner-name { font-size: 15px; font-weight: 600; color: #303133; }
.chat-owner-mail { font-size: 12px; color: #909399; }
.chat-search { margin-top: 6px; }
.chat-sort { color: #909399; font-size: 12px; padding-left: 2px; }
.chat-contact-list { flex: 1; min-height: 0; max-height: 100%; overflow-y: auto; overflow-x: hidden; overscroll-behavior: contain; display: flex; flex-direction: column; gap: 8px; padding-right: 4px; }
.chat-contact-list::-webkit-scrollbar, .chat-message-list::-webkit-scrollbar, .add-friend-list::-webkit-scrollbar { width: 10px; }
.chat-contact-list::-webkit-scrollbar-track, .chat-message-list::-webkit-scrollbar-track, .add-friend-list::-webkit-scrollbar-track { background: #f3f4f6; border-radius: 999px; }
.chat-contact-list::-webkit-scrollbar-thumb, .chat-message-list::-webkit-scrollbar-thumb, .add-friend-list::-webkit-scrollbar-thumb { background: #b8bfcd; border-radius: 999px; border: 2px solid #f3f4f6; }
.chat-contact-list::-webkit-scrollbar-thumb:hover, .chat-message-list::-webkit-scrollbar-thumb:hover, .add-friend-list::-webkit-scrollbar-thumb:hover { background: #9aa3b5; }
.chat-contact-item { display: flex; align-items: center; gap: 10px; padding: 10px 8px; border-radius: 8px; cursor: pointer; }
.chat-contact-item:hover, .chat-contact-item.active { background: #f5f7fa; }
.chat-contact-item.unread { background: #fff7f7; }
.chat-contact-main { flex: 1; min-width: 0; }
.chat-contact-top { display: flex; justify-content: space-between; gap: 8px; }
.chat-contact-name { font-size: 14px; font-weight: 600; color: #303133; }
.chat-contact-state { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: #b1b3bc; }
.chat-contact-state.online { color: #66c39a; }
.chat-contact-dot { width: 8px; height: 8px; border-radius: 999px; background: #b1b3bc; display: inline-block; }
.chat-contact-state.online .chat-contact-dot { background: #66c39a; }
.chat-contact-preview { margin-top: 2px; display: flex; align-items: center; justify-content: space-between; gap: 8px; min-width: 0; }
.chat-contact-email { font-size: 12px; color: #909399; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; min-width: 0; flex: 1; }
.chat-contact-email.unread { color: #303133; font-weight: 600; }
.chat-contact-badge { flex-shrink: 0; min-width: 18px; height: 18px; padding: 0 5px; border-radius: 999px; background: #f56c6c; color: #fff; font-size: 11px; line-height: 18px; text-align: center; font-weight: 600; }
.chat-main { display: flex; flex-direction: column; min-height: 0; }
.chat-main-header { min-height: 66px; border-bottom: 1px solid #ebeef5; padding: 14px 20px; display: flex; align-items: center; }
.chat-main-title { font-size: 30px; line-height: 1; font-weight: 500; }
.chat-main-status { margin-top: 4px; color: #909399; font-size: 12px; }
.chat-main-status.connected { color: #66c39a; }
.chat-message-list { flex: 1; min-height: 0; max-height: 100%; overflow-y: auto; overflow-x: hidden; overscroll-behavior: contain; padding: 20px; background: #fff; }
.chat-msg-row { display: flex; align-items: flex-start; gap: 8px; margin-bottom: 12px; width: 100%; }
.chat-msg-row.is-me { justify-content: flex-end; }
.chat-msg-row.is-bot { justify-content: flex-start; }
.chat-msg-bubble-wrap { max-width: min(72%, 680px); display: flex; flex-direction: column; }
.chat-msg-row.is-me .chat-msg-bubble-wrap { align-items: flex-end; }
.chat-msg-row.is-bot .chat-msg-bubble-wrap { align-items: flex-start; }
.chat-msg-meta { font-size: 12px; color: #9aa1ae; margin-bottom: 3px; }
.chat-msg-bubble { background: #f2f3f5; color: #303133; border-radius: 8px; padding: 10px 12px; line-height: 1.5; font-size: 14px; display: inline-block; width: fit-content; max-width: 100%; white-space: pre-wrap; word-break: break-word; }
.chat-msg-row.is-me .chat-msg-bubble { background: #e9edff; }
.share-card {
  display: flex;
  gap: 0;
  width: min(340px, 100%);
  border: 1px solid #e8edf5;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.06);
  transition: all 0.2s ease;
}
.share-card:hover {
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.12);
  transform: translateY(-1px);
  border-color: #cfd8f0;
}
.share-card:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
}
.share-card-cover {
  width: 84px;
  height: 84px;
  object-fit: cover;
  flex: none;
  border-radius: 10px;
  background: #f3f5f9;
  margin: 10px 0 10px 10px;
}
.share-card-body {
  flex: 1;
  min-width: 0;
  padding: 14px 16px 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
}
.share-card-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e1e2f;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.share-card-summary {
  font-size: 12px;
  color: #7b8292;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.share-card-hint {
  font-size: 11px;
  color: #8b9cf7;
  font-weight: 600;
  margin-top: 2px;
}
.chat-input-wrap { border-top: 1px solid #ebeef5; padding: 14px; background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%); }
.chat-composer { border: 1px solid #d9e2f3; border-radius: 14px; background: #fff; box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75); overflow: hidden; }
.chat-composer-input :deep(.el-textarea__inner) {
  min-height: 110px;
  border: none;
  border-radius: 0;
  box-shadow: none;
  padding: 14px 16px;
  font-size: 14px;
  background: transparent;
}
.chat-composer-input :deep(.el-textarea__inner):focus { box-shadow: none; }
.chat-composer-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 4px 12px 8px;
  background: #fff;
}
.send-btn {
  border: none;
  background: #1e90ff;
  color: #fff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 84px;
  height: 34px;
  font-size: 14px;
  padding: 0 14px;
  border-radius: 10px;
  box-shadow: 0 8px 18px rgba(30, 144, 255, 0.22);
  transition: all 0.2s ease;
}
.send-btn:hover:not(:disabled) {
  background: linear-gradient(90deg, rgba(30, 144, 255, 1) 0%, rgba(0, 212, 255, 1) 100%);
}
.send-btn:active:not(:disabled) { transform: translateY(1px); }
.send-btn span { display: block; margin-left: 0.4em; }
.send-btn svg { width: 16px; height: 16px; fill: white; }
.send-btn .send-btn-svg-wrapper { display: flex; align-items: center; justify-content: center; width: 24px; height: 24px; border-radius: 999px; background-color: rgba(255, 255, 255, 0.18); margin-right: 0.35em; }
.send-btn:disabled { cursor: not-allowed; opacity: 0.65; }

.add-friend-search-wrap { margin-bottom: 12px; }
.add-friend-result { max-height: 260px; overflow: hidden; }
.add-friend-list { max-height: 220px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; padding-right: 4px; }
.add-friend-item { display: flex; align-items: center; justify-content: space-between; border: 1px solid #ebeef5; border-radius: 8px; padding: 10px 12px; }
.add-friend-userinfo, .pending-request-userinfo { display: flex; align-items: center; gap: 10px; }
.add-friend-name, .pending-request-name { font-size: 14px; color: #303133; font-weight: 500; }
.add-friend-username, .pending-request-meta { font-size: 12px; color: #909399; }
.pending-request-list { max-height: 260px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; padding-right: 4px; }
.pending-request-item { display: flex; align-items: center; justify-content: space-between; border: 1px solid #ebeef5; border-radius: 8px; padding: 10px 12px; }
.pending-request-actions { display: flex; gap: 8px; }
</style>
