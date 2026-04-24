<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Odometer,
  Setting,
  Tickets,
  User,
  Folder,
  InfoFilled,
  Collection,
  Files,
  Fold,
  Expand,
  Refresh,
  ArrowDown,
  ArrowRight,
  SwitchButton,
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { AiRobotIcon, chatIcon, forumIcon, lanchIcon, projectIcon, squareIcon,CourseManagementIcon,CourseListIcon, CourseLanchIcon, NewsManagementIcon } from '@/components/icons'

const route = useRoute()
const router = useRouter()

const activePath = computed(() => route.path)
const isCollapsed = ref(false)
const avatarMenuVisible = ref(false)

const formatAvatar = (value: string) => {
  if (!value) return ''
  if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('data:')) {
    return value
  }
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return `${base}/${value.replace(/^\/+/, '')}`
}

const userAvatar = computed(() => {
  const manualAvatar = sessionStorage.getItem('avatar') || localStorage.getItem('avatar')
  if (manualAvatar) return formatAvatar(manualAvatar)

  const keys = ['avatarUrl', 'headImg', 'headimg', 'photo', 'picture']
  for (const key of keys) {
    const value = sessionStorage.getItem(key) || localStorage.getItem(key)
    if (value) return formatAvatar(value)
  }

  return ''
})

const userName = computed(() => {
  return sessionStorage.getItem('nickname') || sessionStorage.getItem('name') || sessionStorage.getItem('username') || localStorage.getItem('nickname') || localStorage.getItem('name') || localStorage.getItem('username') || 'AD'
})

const userRole = computed(() => {
  const role = sessionStorage.getItem('role') || localStorage.getItem('role') || ''
  const roleMap: Record<string, string> = {
    student: '学生',
    teacher: '老师',
    admin: '管理员',
    学生: '学生',
    老师: '老师',
    管理员: '管理员'
  }
  return roleMap[role] || role || '管理员'
})

const routeTitle = computed(() => {
  const path = route.path
  const map: Record<string, string> = {
    '/': '首页总览',
    '/square': '社区论坛',
    '/square/forum': '留言广场',
    '/square/chat': '消息窗口',
    '/projects/management': '项目管理',
    '/projects/new': '新增项目',
    '/projects/info': '研学资讯',
    '/projects/courses': '精品课程',
    '/projects/courses/create': '新增精品课程',
    '/projects/news/create': '新增资讯',
    '/common/upload-test': '测试栏',
    '/system/users': '用户管理',
    '/system/project': '项目管理',
    '/system/news': '资讯管理',
    '/system/courses': '课程管理',
  }
  return map[path] || '首页总览'
})

const toggleAside = () => {
  isCollapsed.value = !isCollapsed.value
}

const onRefresh = () => {
  router.go(0)
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
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('role')
    sessionStorage.removeItem('userRole')
    sessionStorage.removeItem('avatar')
    sessionStorage.removeItem('avatarUrl')
    sessionStorage.removeItem('headImg')
    sessionStorage.removeItem('headimg')
    sessionStorage.removeItem('photo')
    sessionStorage.removeItem('picture')
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
  <el-container class="layout-root">
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo" :class="{ 'logo-collapsed': isCollapsed }">
        <span v-if="!isCollapsed" class="logo-title">StudyFlow</span>
        <span v-else class="logo-title-short">SF</span>
        <span v-if="!isCollapsed" class="logo-subtitle">管理员控制台</span>
      </div>
      <el-menu class="side-menu" :default-active="activePath" :collapse="isCollapsed" router>
        <el-menu-item index="/">
          <el-icon><Odometer /></el-icon>
          <span>首页总览</span>
        </el-menu-item>

        <el-sub-menu index="/square">
          <template #title>
            <el-icon><forumIcon /></el-icon>
            <span>社区论坛</span>
          </template>
          <el-menu-item index="/square/forum">
            <el-icon><squareIcon /></el-icon>
            <span>留言广场</span>
          </el-menu-item>
          <el-menu-item index="/square/chat">
            <el-icon><chatIcon /></el-icon>
            <span>消息窗口</span>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/projects">
          <template #title>
            <el-icon><Folder /></el-icon>
            <span>研学项目</span>
          </template>
          <el-menu-item index="/projects/management">
            <el-icon><Tickets /></el-icon>
            特色项目
          </el-menu-item>
          <el-menu-item index="/projects/new">
            <el-icon><lanchIcon /></el-icon>
            发布项目
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/projects/news">
          <el-icon><InfoFilled /></el-icon>
          <span>研学资讯</span>
        </el-menu-item>
        <el-sub-menu index="/projects/courses">
          <template #title>
            <el-icon><Collection /></el-icon>
            <span>精品课程</span>
          </template>
          <el-menu-item index="/projects/courses">
            <el-icon>
              <CourseListIcon />
            </el-icon>
            <span>课程列表</span>
          </el-menu-item>
          <el-menu-item index="/projects/courses/create">
            <el-icon><CourseLanchIcon /></el-icon>
            <span>课程发布</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/common/upload-test">
          <el-icon><Files /></el-icon>
          <span>测试栏</span>
        </el-menu-item>
        <el-menu-item index="/sf/ai">
          <el-icon>
            <AiRobotIcon />
          </el-icon>
          <span>Ai智能问答</span>
        </el-menu-item>
        <el-sub-menu index="/system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/users">
            <el-icon><User /></el-icon>
            用户管理
          </el-menu-item>
          <el-menu-item index="/system/project">
            <el-icon>
              <projectIcon />
            </el-icon>
            项目管理
          </el-menu-item>
          <el-menu-item index="/system/news">
            <el-icon><NewsManagementIcon /></el-icon>
            资讯管理
          </el-menu-item>
          <el-menu-item index="/system/courses">
            <el-icon><CourseManagementIcon /></el-icon>
            课程管理
          </el-menu-item>
          <!-- <el-menu-item index="/system/courses/create">
            <el-icon><Collection /></el-icon>
            新增精品课程
          </el-menu-item> -->
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <div class="header-actions">
            <el-button circle text class="header-icon-btn" @click="toggleAside">
              <el-icon>
                <component :is="isCollapsed ? Expand : Fold" />
              </el-icon>
            </el-button>
            <el-button circle text class="header-icon-btn" @click="onRefresh">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>

          <div class="header-breadcrumb">
            <span class="crumb-app">StudyFlow</span>
            <span class="crumb-sep">/</span>
            <span class="crumb-page">{{ routeTitle }}</span>
          </div>
        </div>

        <div class="header-right">
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
                  <el-dropdown-item class="avatar-menu-item" @click="router.push('/system/users')">
                    <el-icon><User /></el-icon>
                    <span>个人中心</span>
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
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-root {
  height: 100vh;
}

.layout-aside {
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  padding: 16px 0;
  transition: width 0.2s ease;
}

.logo {
  padding: 0 20px 16px;
  border-bottom: 1px solid #f2f2f2;
  margin-bottom: 8px;
}

.logo-title {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.logo-title-short {
  display: block;
  font-size: 18px;
  font-weight: 600;
  text-align: center;
  color: #303133;
}

.logo-collapsed {
  padding: 0 0 16px;
  display: flex;
  justify-content: center;
}

.logo-subtitle {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.side-menu {
  border-right: none;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  background-color: #ffffff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-icon-btn {
  color: #909399;
}

.header-icon-btn:hover {
  color: #303133;
}

.header-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
}

.crumb-app {
  font-weight: 500;
  color: #606266;
}

.crumb-page {
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
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
  margin-left: 8px;
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

.layout-main {
  padding: 16px 24px 24px;
  background-color: #f5f7fa;
}
</style>
