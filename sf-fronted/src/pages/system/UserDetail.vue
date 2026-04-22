<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Plus } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'

interface UserDetailInfo {
  id?: string | number
  username?: string
  name?: string
  avatar?: string
  gender?: string
  phone?: string
  role?: string
  projectTheme?: string
}

const route = useRoute()
const router = useRouter()

const detail = computed<UserDetailInfo>(() => ({
  id: route.query.id as string | undefined,
  username: route.query.username as string | undefined,
  name: route.query.name as string | undefined,
  avatar: route.query.avatar as string | undefined,
  gender: route.query.gender as string | undefined,
  phone: route.query.phone as string | undefined,
  role: route.query.role as string | undefined,
  projectTheme: route.query.projectTheme as string | undefined
}))

const resolveAvatar = (avatar?: string) => {
  if (!avatar) return ''
  if (/^https?:\/\//i.test(avatar) || avatar.startsWith('data:')) return avatar
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return `${base}/${avatar.replace(/^\/+/, '')}`
}

const getDisplayName = () => detail.value.name || detail.value.username || '未命名用户'

const handleAddFriend = () => {
  ElMessage.info('好友系统正在开发中，暂时无法添加好友')
}

const handlePrivateMessage = () => {
  ElMessage.info('私信功能正在开发中，暂时无法发送消息')
}
</script>

<template>
  <div class="user-detail-page">
    <el-card class="detail-card" shadow="never">
      <div class="header-row">
        <el-button link type="primary" @click="router.back()">返回</el-button>
        <div class="title">用户详情</div>
      </div>

      <div class="profile">
        <el-avatar :size="84" :src="resolveAvatar(detail.avatar)">{{ getDisplayName().slice(0, 1) }}</el-avatar>
        <div class="profile-info">
          <h2>{{ getDisplayName() }}</h2>
          <p>{{ detail.username || '-' }}</p>
          <div v-if="detail.projectTheme" class="source-tag">来自项目：{{ detail.projectTheme }}</div>
        </div>
      </div>

      <div class="action-row">
        <el-button type="primary" class="sf-btn" :icon="Plus" @click="handleAddFriend">添加好友</el-button>
        <el-button type="primary" class="sf-btn sf-btn-message" :icon="ChatDotRound" @click="handlePrivateMessage">私信</el-button>
      </div>

      <el-descriptions :column="2" border class="info-grid">
        <el-descriptions-item v-if="detail.id" label="用户ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.username" label="账号">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.name" label="姓名">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.gender" label="性别">{{ detail.gender }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.phone" label="手机号">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.role" label="角色">{{ detail.role }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.projectTheme" label="来源项目" :span="2">{{ detail.projectTheme }}</el-descriptions-item>
      </el-descriptions>

      <el-empty v-if="!detail.id && !detail.username && !detail.name && !detail.phone && !detail.gender && !detail.role && !detail.projectTheme" description="暂无用户数据" />
    </el-card>
  </div>
</template>

<style scoped>
.user-detail-page {
  padding: 20px 16px 24px;
}

.detail-card {
  max-width: 980px;
  margin: 0 auto;
  border-radius: 12px;
}

.header-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.profile {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.profile-info h2 {
  margin: 0 0 6px;
}

.profile-info p {
  margin: 0;
  color: #909399;
}

.source-tag {
  margin-top: 10px;
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #f5f7fa;
  color: #606266;
  font-size: 12px;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.sf-btn-message {
  background: linear-gradient(180deg, #7adf7d 0%, #6ccf70 100%);
  box-shadow: 0 6px 14px rgba(108, 207, 112, 0.22);
}

.sf-btn-message:hover {
  box-shadow: 0 8px 18px rgba(108, 207, 112, 0.26);
}

.sf-btn-message:active {
  box-shadow: 0 4px 10px rgba(108, 207, 112, 0.18);
}

.info-grid {
  margin-top: 8px;
}
</style>
