<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, User, Camera, SwitchButton, ArrowRight, Search } from '@element-plus/icons-vue'
import { messageIcon } from '../components/icons'

const router = useRouter()
const route = useRoute()

const quickEntries = [
  { label: '研学论坛', path: '/student/square/forum' },
  // { label: '消息窗口', path: '/student/square/chat' },
  { label: '研学资讯', path: '/student/projects/news' },
  { label: '精品课程', path: '/student/projects/courses' },
  { label: '特色项目', path: '/student/projects/management' },
  { label: 'AI智能问答', path: '/student/sf/ai' },
]

const toolbarKeyword = ref('')
const avatarMenuVisible = ref(false)
const profileDialogVisible = ref(false)
const messagePanelVisible = ref(false)
let messageNavigateTimer: number | undefined
const avatarInputRef = ref<HTMLInputElement | null>(null)

watch(
  () => route.query.q,
  (q) => {
    toolbarKeyword.value = typeof q === 'string' ? q : ''
  },
  { immediate: true }
)

const applyToolbarSearch = () => {
  const nextQuery = { ...route.query }
  const q = toolbarKeyword.value.trim()

  if (q) {
    nextQuery.q = q
  } else {
    delete nextQuery.q
  }

  router.replace({
    path: route.path,
    query: nextQuery
  })
}

const onSearchInput = () => {
  applyToolbarSearch()
}

const go = (path: string) => {
  router.push(path)
}

const formatAvatar = (value: string) => {
  if (!value) return ''
  if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('data:')) {
    return value
  }
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return `${base}/${value.replace(/^\/+/, '')}`
}

const userAvatar = computed(() => {
  const manualAvatar = localStorage.getItem('avatar')
  if (manualAvatar) return formatAvatar(manualAvatar)

  const keys = ['avatarUrl', 'headImg', 'headimg', 'photo', 'picture']
  for (const key of keys) {
    const value = localStorage.getItem(key)
    if (value) return formatAvatar(value)
  }

  return ''
})

const userName = computed(() => {
  return localStorage.getItem('nickname') || localStorage.getItem('name') || localStorage.getItem('username') || 'AD'
})

const userRole = computed(() => {
  const role = localStorage.getItem('role') || sessionStorage.getItem('role') || ''
  const roleMap: Record<string, string> = {
    student: '学生',
    teacher: '老师',
    admin: '管理员',
    学生: '学生',
    老师: '老师',
    管理员: '管理员'
  }
  return roleMap[role] || role || '学生'
})

const openProfile = () => {
  avatarMenuVisible.value = false
  profileDialogVisible.value = true
}

const closeMessagePanel = () => {
  messagePanelVisible.value = false
}

const openMessageWindowPage = () => {
  if (messageNavigateTimer) {
    window.clearTimeout(messageNavigateTimer)
  }

  messagePanelVisible.value = true
  messageNavigateTimer = window.setTimeout(() => {
    messagePanelVisible.value = false
    router.push('/student/square/chat')
  }, 200)
}

const triggerAvatarUpload = () => {
  avatarMenuVisible.value = false
  avatarInputRef.value?.click()
}

const saveAvatar = (avatar: string) => {
  localStorage.setItem('avatar', avatar)
  localStorage.setItem('avatarUpdatedAt', String(Date.now()))
}

const onAvatarSelected = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    target.value = ''
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    const avatar = String(reader.result || '')
    saveAvatar(avatar)
    ElMessage.success('头像已更新')
    target.value = ''
  }
  reader.readAsDataURL(file)
}

const handleAvatarVisibleChange = (visible: boolean) => {
  avatarMenuVisible.value = visible
}

const logout = async () => {
  avatarMenuVisible.value = false
  try {
    await ElMessageBox.confirm('确认退出登录吗？', '退出登录', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
    if (messageNavigateTimer) {
      window.clearTimeout(messageNavigateTimer)
      messageNavigateTimer = undefined
    }
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('avatar')
    localStorage.removeItem('avatarUrl')
    localStorage.removeItem('headImg')
    localStorage.removeItem('headimg')
    localStorage.removeItem('photo')
    localStorage.removeItem('picture')
    router.push('/login')
  } catch {
    // cancelled
  }
}
</script>

<template>
  <div class="student-portal">
    <header class="portal-topbar">
      <div class="topbar-inner">
        <div class="nav-group">
          <button v-for="item in quickEntries" :key="item.path" type="button" class="nav-action" @click="go(item.path)">
            {{ item.label }}
          </button>
        </div>

        <form class="toolbar-search-form" @submit.prevent="applyToolbarSearch">
          <label for="toolbar-search-input" class="search-input-wrap">
            <el-icon class="search-ui" aria-hidden="true">
              <Search />
            </el-icon>
            <input
              id="toolbar-search-input"
              v-model="toolbarKeyword"
              class="input-style"
              required
              autocomplete="off"
              placeholder="请输入页面内容"
              type="text"
              @input="onSearchInput"
            />
            <button type="submit" class="enter-btn" aria-label="执行搜索">
              <el-icon>
                <ArrowRight />
              </el-icon>
            </button>
          </label>
        </form>

        <div class="avatar-area">
          <div class="message-popover-anchor" @mouseenter="messagePanelVisible = true" @mouseleave="messagePanelVisible = false">
            <el-popover
              v-model:visible="messagePanelVisible"
              placement="left-start"
              trigger="manual"
              :width="420"
              popper-class="message-popover"
            >
              <template #reference>
                <button
                  type="button"
                  class="message-entry"
                  :class="{ active: messagePanelVisible }"
                  aria-label="打开消息窗口"
                  @click="openMessageWindowPage"
                >
                  <el-icon>
                    <messageIcon />
                  </el-icon>
                </button>
              </template>

              <div class="message-panel-card">
                <div class="message-panel-header">
                  <div>
                    <div class="message-panel-title">消息窗口</div>
                    <div class="message-panel-subtitle">鼠标悬停查看预览，点击进入完整消息页</div>
                  </div>
                  <button type="button" class="message-panel-close" @click="closeMessagePanel">×</button>
                </div>

                <div class="message-panel-body">
                  <div class="message-panel-empty">
                    <el-icon class="message-panel-icon">
                      <messageIcon />
                    </el-icon>
                    <div class="message-panel-empty-title">点击可进入消息窗口</div>
                    <div class="message-panel-empty-text">这里可以放最新会话预览、系统通知与未读消息入口。</div>
                  </div>
                </div>
              </div>
            </el-popover>
          </div>

          <el-dropdown trigger="hover" :show-timeout="80" :hide-timeout="120" @visible-change="handleAvatarVisibleChange">
            <div class="user-avatar-trigger">
              <div class="user-avatar" :title="userName">
                <img v-if="userAvatar" :src="userAvatar" :alt="userName" />
                <span v-else>{{ userName.slice(0, 2).toUpperCase() }}</span>
              </div>
              <div class="user-summary">
                <div class="user-summary-name">{{ userName }}</div>
                <div class="user-summary-role">{{ userRole }}</div>
              </div>
              <el-icon class="avatar-arrow" :class="{ open: avatarMenuVisible }">
                <ArrowDown />
              </el-icon>
            </div>

            <template #dropdown>
              <div class="avatar-dropdown-shell">
                <div class="avatar-dropdown-header">
                  <div class="avatar-dropdown-avatar">
                    <img v-if="userAvatar" :src="userAvatar" :alt="userName" />
                    <span v-else>{{ userName.slice(0, 2).toUpperCase() }}</span>
                  </div>
                  <div class="avatar-dropdown-info">
                    <div class="avatar-dropdown-name">{{ userName }}</div>
                    <div class="avatar-dropdown-role">{{ userRole }}</div>
                  </div>
                </div>

                <el-dropdown-menu class="avatar-dropdown">
                  <el-dropdown-item class="avatar-menu-item" @click="openProfile">
                    <el-icon><User /></el-icon>
                    <span>个人中心</span>
                    <el-icon class="menu-item-arrow"><ArrowRight /></el-icon>
                  </el-dropdown-item>
                  <el-dropdown-item class="avatar-menu-item" @click="triggerAvatarUpload">
                    <el-icon><Camera /></el-icon>
                    <span>修改头像</span>
                    <el-icon class="menu-item-arrow"><ArrowRight /></el-icon>
                  </el-dropdown-item>
                  <el-dropdown-item class="avatar-menu-item avatar-menu-danger" divided @click="logout">
                    <el-icon><SwitchButton /></el-icon>
                    <span>退出登录</span>
                    <el-icon class="menu-item-arrow"><ArrowRight /></el-icon>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </div>
            </template>
          </el-dropdown>

          <input ref="avatarInputRef" type="file" accept="image/*" class="avatar-file-input" @change="onAvatarSelected" />
        </div>
      </div>
    </header>

    <div class="student-content">
      <router-view />
    </div>

    <el-dialog v-model="profileDialogVisible" width="420px" title="个人中心">
      <div class="profile-panel">
        <div class="profile-avatar">
          <img v-if="userAvatar" :src="userAvatar" :alt="userName" />
          <span v-else>{{ userName.slice(0, 2).toUpperCase() }}</span>
        </div>
        <div class="profile-info">
          <div class="profile-name">{{ userName }}</div>
          <div class="profile-role">{{ userRole }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.student-portal {
  height: 100vh;
  background: #ffffff;
  color: #222;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.student-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.portal-topbar {
  background: #ffffff;
  border-bottom: 1px solid #e8ebf2;
  box-shadow: 0 2px 10px rgba(19, 31, 58, 0.04);
}

.topbar-inner {
  width: 100%;
  min-height: 58px;
  padding: 8px 24px;
  display: grid;
  grid-template-columns: max-content minmax(520px, 1fr) max-content;
  align-items: center;
  gap: 16px;
  box-sizing: border-box;
}

.nav-group {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-self: start;
}

.toolbar-search-form {
  justify-self: center;
  width: min(100%, 560px);
  margin: 0 auto;
}

.search-input-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  box-sizing: border-box;
  padding: 0 12px;
  min-height: 38px;
  border: 2px solid #ccc;
  border-radius: 999px;
  background: #fff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.search-input-wrap:focus-within {
  border-color: #007bff;
  box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.18);
}

.search-ui {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 15px;
}

.toolbar-search-form .input-style {
  flex: 1;
  width: 100%;
  box-sizing: border-box;
  padding: 7px 0;
  border: 0;
  font-size: 15px;
  color: #334155;
  outline: none;
  background: transparent;
}

.enter-btn {
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 50%;
  background: #2563eb;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.enter-btn:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.25);
}

.enter-btn:active {
  transform: translateY(0);
}

.avatar-area {
  justify-self: end;
  display: flex;
  align-items: center;
  gap: 10px;
}

.nav-action {
  min-height: 38px;
  padding: 0 8px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: #334155;
  cursor: pointer;
  font: inherit;
  font-size: 15px;
  font-weight: 600;
  transition:
    background 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease;
}

.nav-action:hover {
  background: #f3f6fb;
  color: #2563eb;
  transform: translateY(-1px);
}

.toolbar-search {
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 560px;
}

.toolbar-search.form {
  --input-bg: #fff;
  --padding: 1.5em;
  --rotate: 80deg;
  --gap: 2em;
  --icon-change-color: #15a986;
  --height: 40px;
  width: 200px;
  padding-inline-end: 1em;
  background: var(--input-bg);
  position: relative;
  border-radius: 4px;
}

.toolbar-search.form label {
  display: flex;
  align-items: center;
  width: 100%;
  height: var(--height);
}

.toolbar-search.form input {
  width: 100%;
  padding-inline-start: calc(var(--padding) + var(--gap));
  outline: none;
  background: none;
  border: 0;
}


.icon {
  position: absolute;
  left: var(--padding);
  transition: 0.3s cubic-bezier(.4,0,.2,1);
  display: flex;
  justify-content: center;
  align-items: center;
}

.toolbar-search.form svg {
  color: #111;
  transition: 0.3s cubic-bezier(.4,0,.2,1);
  position: absolute;
  height: 15px;
}

.swap-off {
  transform: rotate(-80deg);
  opacity: 0;
  visibility: hidden;
}

.toolbar-search.form.focused .icon,
.toolbar-search.form:has(input:not(:placeholder-shown)) .icon {
  color: #15a986;
  transform: translateY(-50%) rotate(80deg) scale(1.12);
}

.toolbar-search.form.focused .swap-on,
.toolbar-search.form:has(input:not(:placeholder-shown)) .swap-on {
  opacity: 0;
  visibility: hidden;
  transform: rotate(80deg) scale(0.6);
}

.toolbar-search.form.focused .swap-off,
.toolbar-search.form:has(input:not(:placeholder-shown)) .swap-off {
  opacity: 1;
  visibility: visible;
  transform: rotate(0deg) scale(1);
}

.message-popover-anchor {
  display: flex;
  align-items: center;
}

.message-entry {
  width: 42px;
  height: 42px;
  border: 1px solid #e5e7eb;
  border-radius: 50%;
  background: #ffffff;
  color: #475569;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.message-entry:hover,
.message-entry.active {
  background: #eff6ff;
  color: #2563eb;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.12);
  transform: translateY(-1px);
}

.user-avatar-trigger {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  justify-self: end;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 18px;
  transition: background 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
  margin-left: auto;
}

.user-avatar-trigger:hover {
  background: #f5f8ff;
  transform: scale(1.04);
  box-shadow: 0 10px 24px rgba(31, 41, 55, 0.12);
}

.user-summary {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.user-summary-name {
  font-size: 13px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.2;
}

.user-summary-role {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.2;
}

.avatar-arrow {
  color: #64748b;
  transition: transform 0.2s ease;
}

.avatar-arrow.open {
  transform: rotate(180deg);
}

.user-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  overflow: hidden;
  background: #d9dde3;
  color: #ffffff;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 700;
  flex: none;
  border: 1px solid #e5e7eb;
  justify-self: end;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.user-avatar span {
  line-height: 1;
}

.avatar-file-input {
  display: none;
}

.avatar-dropdown-shell {
  min-width: 260px;
  padding: 12px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 22px 60px rgba(15, 23, 42, 0.18);
  border: 1px solid #edf2f7;
}

:deep(.avatar-dropdown) {
  border: none;
  padding: 0;
  background: transparent;
}

.avatar-dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 2px 6px 12px;
  margin-bottom: 8px;
  border-bottom: 1px solid #eef2f7;
}

.avatar-dropdown-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  overflow: hidden;
  background: #d9dde3;
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 16px;
  font-weight: 700;
  flex: none;
}

.avatar-dropdown-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-dropdown-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.avatar-dropdown-name {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.avatar-dropdown-role {
  font-size: 12px;
  color: #6b7280;
}

.avatar-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 14px;
  margin: 4px 0;
  padding: 10px 12px;
}

:deep(.avatar-dropdown .el-dropdown-menu__item) {
  border-radius: 14px;
}

:deep(.avatar-dropdown .el-dropdown-menu__item:hover) {
  background: #f5f8ff;
}

.avatar-menu-item span {
  flex: 1;
}

.menu-item-arrow {
  color: #94a3b8;
  font-size: 12px;
}

.avatar-menu-danger {
  color: #dc2626;
}

.message-panel-card {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 220px;
}

.message-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.message-panel-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.message-panel-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
}

.message-panel-close {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 50%;
  background: #f3f4f6;
  color: #475569;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
}

.message-panel-body {
  border-radius: 20px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
  border: 1px solid #eaf0f7;
  padding: 22px;
}

.message-panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
  color: #475569;
}

.message-panel-icon {
  font-size: 34px;
  color: #2563eb;
}

.message-panel-empty-title {
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
}

.message-panel-empty-text {
  font-size: 13px;
  line-height: 1.7;
  color: #6b7280;
  max-width: 260px;
}

.profile-panel {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  overflow: hidden;
  background: #d9dde3;
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 22px;
  font-weight: 700;
  flex: none;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.profile-name {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.profile-role {
  color: #6b7280;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .topbar-inner {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .avatar-area {
    justify-self: start;
  }
}

:deep(.message-popover) {
  border: 0;
  border-radius: 22px;
  padding: 16px;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.18);
}

@media (max-width: 900px) {
  .nav-group {
    flex-wrap: wrap;
  }
}
</style>
