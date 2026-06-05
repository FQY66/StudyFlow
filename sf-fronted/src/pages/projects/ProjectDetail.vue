<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { likeIcon, shareIcon } from '@/components/icons'
import request from '@/utils/request'

interface ProjectSignupUser {
  id?: number
  userId?: number
  userName?: string
  nickname?: string
  realName?: string
  avatar?: string
  createTime?: string
  [key: string]: unknown
}

interface ProjectStudyVO {
  id: number
  coverPath?: string
  theme?: string
  introduction?: string
  content?: string
  category?: string
  capacity?: number
  status?: string
  createTime?: string
  updateTime?: string
  likeCount?: number
  clickCount?: number
  projectSignupList?: ProjectSignupUser[]
}

interface ProjectShareCard {
  type: 'project_share'
  projectId: number
  title: string
  summary: string
  cover: string
  link: string
}

interface FriendItem {
  userId: number
  name?: string
  username?: string
  email?: string
  avatar?: string
  online?: boolean
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const joining = ref(false)
const project = ref<ProjectStudyVO | null>(null)
const sharing = ref(false)

const shareDialogVisible = ref(false)
const shareFriendList = ref<FriendItem[]>([])
const shareFriendLoading = ref(false)
const shareFriendKeyword = ref('')

const id = computed(() => String(route.params.id ?? ''))
const currentUserId = computed(() => {
  const candidates = [
    sessionStorage.getItem('id'),
    localStorage.getItem('id'),
    sessionStorage.getItem('userId'),
    localStorage.getItem('userId')
  ]
  const value = candidates.find((item) => item && item !== 'undefined' && item !== 'null')
  return value ? Number(value) : 0
})
const currentUserRole = computed(() => sessionStorage.getItem('userRole') || sessionStorage.getItem('role') || localStorage.getItem('userRole') || localStorage.getItem('role') || '')

const currentUserSignup = computed(() => {
  if (!project.value?.projectSignupList?.length || !currentUserId.value) return null
  return project.value.projectSignupList.find((user) => Number(user.userId || user.id) === currentUserId.value) || null
})

const isSignupApproved = computed(() => {
  return currentUserSignup.value?.status === '已通过'
})

const isSignupPending = computed(() => {
  return currentUserSignup.value?.status === '待审核'
})

const joinButtonText = computed(() => {
  if (isSignupApproved.value) return '已报名'
  if (isSignupPending.value) return '待审核'
  if (project.value?.status && project.value.status !== '已发布') return '暂不可报名'
  return '我要报名'
})

const joinButtonDisabled = computed(() => {
  return joining.value || isSignupApproved.value || isSignupPending.value || project.value?.status !== '已发布'
})

const filteredShareFriends = computed(() => {
  const kw = shareFriendKeyword.value.trim().toLowerCase()
  if (!kw) return shareFriendList.value
  return shareFriendList.value.filter(
    (f) => (f.name || '').toLowerCase().includes(kw) || (f.username || '').toLowerCase().includes(kw)
  )
})

const resolveFileUrl = (path?: string) => {
  if (!path) return 'https://picsum.photos/seed/sf-project-detail/1200/520'
  if (/^https?:\/\//i.test(path) || path.startsWith('data:')) return path
  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  return path.startsWith('/') ? `${base}${path}` : `${base}/${path}`
}

const toAbsoluteFileUrl = (src: string) => {
  if (!src) return src
  if (/^https?:\/\//i.test(src) || src.startsWith('data:')) return src
  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  return src.startsWith('/') ? `${base}${src}` : `${base}/${src}`
}

const formatFriendAvatar = (avatar?: string, idx = 0) => {
  const raw = avatar?.trim()
  if (!raw) return `https://i.pravatar.cc/80?img=${(idx % 60) + 1}`
  if (raw.startsWith('http://') || raw.startsWith('https://')) return raw
  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  return `${base}/${raw.replace(/^\/+/, '')}`
}

const renderRichText = (html?: string, fallback = '暂无内容') => {
  if (!html) return `<p>${fallback}</p>`
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')
  doc.querySelectorAll('script, iframe, object, embed').forEach((node) => node.remove())
  doc.querySelectorAll('img').forEach((img) => {
    const src = img.getAttribute('src') || ''
    img.setAttribute('src', toAbsoluteFileUrl(src))
  })
  return doc.body.innerHTML || `<p>${fallback}</p>`
}

const stripHtml = (html?: string) => {
  const text = (html || '').replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
  return text
}

const buildShareCard = (): ProjectShareCard | null => {
  if (!project.value?.id) return null
  const link = `${window.location.origin}/student/projects/detail/${project.value.id}`
  return {
    type: 'project_share',
    projectId: project.value.id,
    title: project.value.theme || '未命名项目',
    summary: stripHtml(project.value.introduction || project.value.content) || '查看项目详情',
    cover: resolveFileUrl(project.value.coverPath),
    link
  }
}

const sendShareToFriend = async (friendId: number) => {
  const card = buildShareCard()
  if (!card) {
    ElMessage.warning('项目数据未加载完成')
    return
  }
  sharing.value = true
  try {
    const { data } = await request.post('/chat/share/project', {
      toUserId: friendId,
      ...card
    })
    if (data?.code !== 1) {
      ElMessage.error(data?.msg || '分享失败')
      return
    }
    ElMessage.success('已分享给好友')
    shareDialogVisible.value = false
  } catch {
    ElMessage.error('分享失败，请稍后再试')
  } finally {
    sharing.value = false
  }
}

const loadShareFriendList = async () => {
  shareFriendLoading.value = true
  try {
    const { data } = await request.get('/chat/friends')
    if (data?.code === 1) {
      shareFriendList.value = (data.data || []).map((u: any) => ({
        userId: Number(u.userId),
        name: u.name || u.username || `用户${u.userId}`,
        username: u.username || '',
        email: u.email || '',
        avatar: u.avatar || '',
        online: !!u.online
      }))
    }
  } catch {
    ElMessage.error('加载好友列表失败')
  } finally {
    shareFriendLoading.value = false
  }
}

const shareProjectToFriend = () => {
  if (!currentUserId.value) {
    ElMessage.warning('请先登录后再分享')
    return
  }
  shareFriendKeyword.value = ''
  loadShareFriendList()
  shareDialogVisible.value = true
}

const handleShareToFriend = (friendId: number) => {
  sendShareToFriend(friendId)
}

const increaseClick = async () => {
  if (!id.value) return
  try {
    await request.put('/project/increaseClick', null, {
      params: { id: id.value }
    })
  } catch {
    // ignore click count failure
  }
}

const loadDetail = async () => {
  loading.value = true
  try {
    const { data } = await request.get('/project/detail', {
      params: { id: id.value }
    })
    project.value = data?.data || null
  } finally {
    loading.value = false
  }
}

const handleLikeProject = async () => {
  if (!project.value?.id) return
  try {
    const { data } = await request.put('/project/increaseLike', null, {
      params: { id: project.value.id }
    })
    if (data?.code !== 1) {
      ElMessage.error(data?.msg || '点赞失败')
      return
    }
    project.value.likeCount = (project.value.likeCount ?? 0) + 1
    ElMessage.success('点赞成功')
  } catch {
    ElMessage.error('点赞失败，请稍后再试')
  }
}

const handleJoinProject = async () => {
  if (!project.value) return
  if (!currentUserRole.value) {
    ElMessage.warning('请先登录后再报名')
    return
  }
  if (!['学生', '老师'].includes(currentUserRole.value)) {
    ElMessage.warning('当前身份暂不支持报名项目')
    return
  }
  if (!currentUserId.value) {
    ElMessage.warning('请先登录后再报名')
    return
  }
  if (project.value.status && project.value.status !== '已发布') {
    ElMessage.warning('当前项目暂不可报名')
    return
  }
  if (hasSignedUp.value) {
    ElMessage.info('你已经报名过该项目了')
    return
  }

  joining.value = true
  try {
    const { data } = await request.post('/project/signup', null, {
      params: {
        projectId: project.value.id,
        userId: currentUserId.value
      }
    })

    if (data?.code !== 1) {
      ElMessage.error(data?.msg || '报名失败')
      return
    }

    ElMessage.success('报名成功，等待审核')
    await loadDetail()
  } catch {
    ElMessage.error('报名失败，请稍后再试')
  } finally {
    joining.value = false
  }
}

onMounted(async () => {
  await increaseClick()
  loadDetail()
})

watch(
  () => route.params.id,
  () => {
    loadDetail()
  }
)
</script>

<template>
  <div class="project-detail">
    <div class="detail-shell">
      <el-skeleton v-if="loading" animated :rows="8" />

      <el-card v-else shadow="never" class="detail-card">
        <div class="detail-header">
          <el-button link type="primary" @click="router.back()">返回</el-button>
          <div class="detail-breadcrumb">项目详情</div>
        </div>

        <template v-if="project">
          <div class="detail-scroll">
            <img :src="resolveFileUrl(project.coverPath)" class="cover" alt="" />
            <div class="meta-row">
              <span class="category">{{ project.category || '未分类' }}</span>
              <span class="status">{{ project.status || '未知状态' }}</span>
            </div>
            <h2 class="detail-title">{{ project.theme || '未命名项目' }}</h2>
            <div class="stats">
              <span>创建时间：{{ project.createTime || '-' }}</span>
              <span>浏览量：{{ project.clickCount ?? 0 }}</span>
              <span>点赞数：{{ project.likeCount ?? 0 }}</span>
              <span>报名上限：{{ project.capacity ?? '-' }}</span>
            </div>

            <section class="article-block">
              <h2 class="article-intro-title">简介</h2>
              <div class="article-intro" v-html="renderRichText(project.introduction, '暂无简介')"></div>
            </section>

            <section class="article-block article-body-block">
              <div class="content article-body" v-html="renderRichText(project.content, '暂无内容')"></div>
            </section>

            <div class="project-actions">
              <button class="join-button" type="button" :disabled="joinButtonDisabled" @click="handleJoinProject">
                {{ joining ? '报名中...' : joinButtonText }}
              </button>
              <div class="project-actions-right">
                <button class="icon-action-button type-like" type="button" aria-label="点赞" @click="handleLikeProject">
                  <el-icon class="icon-action-svg"><component :is="likeIcon" /></el-icon>
                  <span>点赞</span>
                </button>
                <button class="icon-action-button type-share" type="button" aria-label="分享" @click="shareProjectToFriend">
                  <el-icon class="icon-action-svg"><component :is="shareIcon" /></el-icon>
                  <span>{{ sharing ? '分享中...' : '分享' }}</span>
                </button>
              </div>
            </div>
          </div>
        </template>

        <el-empty v-else description="暂无项目详情" />
      </el-card>
    </div>

    <!-- 分享好友选择弹窗 -->
    <el-dialog v-model="shareDialogVisible" title="分享给好友" width="460px" :close-on-click-modal="true" class="share-dialog">
      <div class="share-dialog-search">
        <el-input v-model="shareFriendKeyword" placeholder="搜索好友" clearable class="share-search-input" />
      </div>

      <div v-loading="shareFriendLoading" class="share-friend-list">
        <el-empty v-if="!shareFriendLoading && filteredShareFriends.length === 0" description="暂无好友，请先添加好友" />
        <div v-else class="share-friend-items">
          <div
            v-for="(friend, idx) in filteredShareFriends"
            :key="friend.userId"
            class="share-friend-item"
          >
            <div class="share-friend-info">
              <el-avatar :size="44" :src="formatFriendAvatar(friend.avatar, idx)" class="share-friend-avatar" />
              <div class="share-friend-text">
                <div class="share-friend-name">
                  {{ friend.name || friend.username || '用户' + friend.userId }}
                  <span v-if="friend.online" class="share-friend-online-dot" title="在线"></span>
                </div>
                <div class="share-friend-email">{{ friend.email || '@' + (friend.username || friend.userId) }}</div>
              </div>
            </div>
            <button class="share-purple-btn" :disabled="sharing" @click="handleShareToFriend(friend.userId)">
              <span v-if="sharing">分享中</span>
              <span v-else>分享</span>
            </button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.project-detail {
  padding: 20px 16px 24px;
}

.detail-shell {
  max-width: 980px;
  margin: 0 auto;
}

.detail-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 28px 48px 18px;
  min-height: calc(100vh - 40px - 24px);
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex: none;
}

.detail-breadcrumb {
  font-size: 13px;
  color: #909399;
}

.detail-scroll {
  flex: 1;
}

.cover {
  width: 100%;
  height: 320px;
  object-fit: cover;
  border-radius: 12px;
  margin-bottom: 18px;
}

.meta-row,
.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #606266;
  font-size: 13px;
  margin-bottom: 12px;
}

.category,
.status {
  background: #f5f7fa;
  padding: 4px 10px;
  border-radius: 999px;
}

.detail-title {
  margin: 0 0 10px;
  font-size: 24px;
  font-weight: 800;
  color: #303133;
  text-align: center;
}

.project-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 18px 0 0;
  flex-wrap: wrap;
  flex: none;
  position: relative;
}

.join-button {
  position: absolute;
  left: 50%;
  top: 80%;
  transform: translate(-50%, -50%);
  min-width: 92px;
  height: 40px;
  padding: 0 14px;
  border: 0;
  border-radius: 10px;
  background: #1677ff;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 6px 14px rgba(22, 119, 255, 0.2);
  transition: transform 0.15s ease, box-shadow 0.15s ease, background 0.15s ease, opacity 0.15s ease;
}

.join-button:hover:not(:disabled) {
  transform: translate(-50%, -50%) translateY(-1px);
  background: #0f66e6;
  box-shadow: 0 8px 16px rgba(22, 119, 255, 0.24);
}

.join-button:active:not(:disabled) {
  transform: translate(-50%, -50%) translateY(1px);
  box-shadow: 0 4px 10px rgba(22, 119, 255, 0.18);
}

.join-button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
  box-shadow: none;
}

.project-actions-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-action-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  height: 34px;
  border: 1px solid #d9e2f2;
  border-radius: 10px;
  background: #fff;
  color: #3b4558;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.icon-action-button:hover {
  border-color: #1677ff;
  box-shadow: 0 6px 14px rgba(22, 119, 255, 0.12);
  transform: translateY(-1px);
}

.icon-action-svg {
  font-size: 16px;
  color: inherit;
}

.type-like {
  color: #e25555;
}

.type-share {
  color: #1677ff;
}

.article-block {
  margin-top: 28px;
}

.article-intro-title {
  margin: 0 0 18px;
  text-align: left;
  font-size: 16px;
  font-weight: 800;
  color: #303133;
  letter-spacing: 1px;
}

.article-body-title {
  margin: 0 0 18px;
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  letter-spacing: 1px;
}

.article-intro {
  margin: 0;
  color: #303133;
  font-size: 14px;
  font-weight: 700;
  line-height: 2;
  text-indent: 0;
  text-align: left;
}

.article-body-block {
  margin-top: 34px;
}

.signup-block {
  margin-top: 30px;
}

.signup-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.signup-user-card {
  border-radius: 12px;
}

.signup-user-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* ===== 分享好友选择弹窗样式 ===== */
.share-dialog :deep(.el-dialog) {
  border-radius: 18px;
  overflow: hidden;
}

.share-dialog :deep(.el-dialog__header) {
  padding: 20px 24px 0;
  margin: 0;
}

.share-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: #1e1e2f;
}

.share-dialog :deep(.el-dialog__body) {
  padding: 16px 24px 24px;
}

.share-dialog-search {
  margin-bottom: 18px;
}

.share-search-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: #f7f8fc;
  border: 1px solid #e8ecf4;
  box-shadow: none;
  transition: all 0.2s ease;
}

.share-search-input :deep(.el-input__wrapper:hover) {
  border-color: #c5cde4;
  background: #f3f5fb;
}

.share-search-input :deep(.el-input__wrapper.is-focus) {
  border-color: #8b9cf7;
  box-shadow: 0 0 0 3px rgba(107, 131, 247, 0.12);
  background: #fff;
}

.share-friend-list {
  min-height: 120px;
  max-height: 380px;
  overflow-y: auto;
}

.share-friend-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.share-friend-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border: 1px solid #eef1f8;
  border-radius: 14px;
  background: #fafbfd;
  transition: all 0.2s ease;
}

.share-friend-item:hover {
  background: #f4f6ff;
  border-color: #d4daf8;
  box-shadow: 0 4px 12px rgba(107, 131, 247, 0.08);
  transform: translateY(-1px);
}

.share-friend-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.share-friend-avatar {
  flex-shrink: 0;
}

.share-friend-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.share-friend-name {
  font-size: 15px;
  font-weight: 600;
  color: #1e1e2f;
  display: flex;
  align-items: center;
  gap: 6px;
}

.share-friend-online-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #52c97d;
  display: inline-block;
  flex-shrink: 0;
  box-shadow: 0 0 0 2px rgba(82, 201, 125, 0.2);
}

.share-friend-email {
  font-size: 12px;
  color: #9095a8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 紫色渐变分享按钮 */
.share-purple-btn {
  flex-shrink: 0;
  min-width: 68px;
  height: 34px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #8b9cf7 0%, #6b83f7 50%, #5b6ef5 100%);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(107, 131, 247, 0.3);
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.share-purple-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #9aabf8 0%, #7b93f8 50%, #6b7ef6 100%);
  box-shadow: 0 6px 16px rgba(107, 131, 247, 0.4);
  transform: translateY(-1px);
}

.share-purple-btn:active:not(:disabled) {
  transform: translateY(1px);
  box-shadow: 0 2px 8px rgba(107, 131, 247, 0.25);
}

.share-purple-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 好友列表空状态优化 */
.share-friend-list :deep(.el-empty__description) {
  color: #9095a8;
  font-size: 13px;
}

.signup-user-info {
  min-width: 0;
}

.signup-user-name {
  font-size: 14px;
  font-weight: 700;
  color: #303133;
}

.signup-user-meta {
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  color: #909399;
}

.article-body {
  color: #303133;
  font-size: 15px;
  line-height: 2;
}

.article-body :deep(p) {
  margin: 0 0 12px;
  text-indent: 2em;
}

.article-body :deep(img) {
  max-width: 100%;
  height: auto;
}
</style>
