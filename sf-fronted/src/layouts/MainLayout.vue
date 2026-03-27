<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Odometer,
  Reading,
  Setting,
  Tickets,
  User,
  Folder,
  Plus,
  InfoFilled,
  Collection,
  Files,
  Fold,
  Expand,
  Refresh,
  Search,
  Bell,
  Setting as SettingIcon,
} from '@element-plus/icons-vue'
import { AiRobotIcon, chatIcon, forumIcon, squareIcon } from '@/components/icons'
import Square from '@/pages/forum/Square.vue'

const route = useRoute()
const router = useRouter()

const activePath = computed(() => route.path)

const isCollapsed = ref(false)

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
    '/common/upload-test': '测试栏',
    '/system/users': '用户管理',
  }
  return map[path] || '首页总览'
})

const toggleAside = () => {
  isCollapsed.value = !isCollapsed.value
}

const onRefresh = () => {
  router.go(0)
}
</script>

<template>
  <el-container class="layout-root">
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo" :class="{ 'logo-collapsed': isCollapsed }">
        <span v-if="!isCollapsed" class="logo-title">StudyFlow</span>
        <span v-else class="logo-title-short">SF</span>
        <span v-if="!isCollapsed" class="logo-subtitle">高校研学交流平台</span>
      </div>
      <el-menu
        class="side-menu"
        :default-active="activePath"
        :collapse="isCollapsed"
        router
      >
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
            项目管理
          </el-menu-item>
          <el-menu-item index="/projects/new">
            <el-icon><Plus /></el-icon>
            新增项目
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/projects/news">
            <el-icon><InfoFilled /></el-icon>
            研学资讯
          </el-menu-item>
          <el-menu-item index="/projects/courses">
            <el-icon><Collection /></el-icon>
            精品课程
          </el-menu-item>
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
        </el-sub-menu>
   
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <div class="header-actions">
            <el-button
              circle
              text
              class="header-icon-btn"
              @click="toggleAside"
            >
              <el-icon>
                <component :is="isCollapsed ? Expand : Fold" />
              </el-icon>
            </el-button>
            <el-button
              circle
              text
              class="header-icon-btn"
              @click="onRefresh"
            >
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
          <el-button circle text class="header-icon-btn">
            <el-icon><Search /></el-icon>
          </el-button>
          <el-button circle text class="header-icon-btn">
            <el-icon><Bell /></el-icon>
          </el-button>
          <el-button circle text class="header-icon-btn">
            <el-icon><SettingIcon /></el-icon>
          </el-button>
          <el-avatar
            class="header-avatar"
            size="small"
          >
            SF
          </el-avatar>
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

.header-avatar {
  margin-left: 8px;
}

.ai-icon svg {
  width: 1em;
  height: 1em;
  display: block;
}

.layout-main {
  padding: 16px 24px 24px;
  background-color: #f5f7fa;
}
</style>

