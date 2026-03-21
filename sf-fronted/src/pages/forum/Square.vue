<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

interface ForumComment {
  id?: number
  postId: number
  authorId?: number
  authorName?: string
  authorAvatar?: string
  avatarColor?: string
  content: string
  createdAt?: string
  createTime?: string
}

interface ForumPost {
  id: number
  title: string
  authorName?: string
  authorAvatar?: string
  createdAt?: string
  createTime?: string
  updateTime?: string
  content: string
  commentCount: number
  likeCount: number
  category?: string
  comments?: ForumComment[]
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

const loading = ref(false)
const posts = ref<ForumPost[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const currentPost = ref<ForumPost | null>(null)
const comments = ref<ForumComment[]>([])
const newComment = ref('')
const currentPostColor = ref('#f9f9f9')

const publishVisible = ref(false)
const publishLoading = ref(false)
const publishBgColor = ref('#bfe8ff')
const tiltEnabled = ref(true)
const publishForm = ref({
  title: '',
  category: '',
  content: ''
})
const highlightedPostId = ref<number | null>(null)
const postCardRefs = ref<Record<number, HTMLElement | null>>({})

const categoryOptions = [
  // 可扩展：后续你新增分类时，直接在这里继续追加字符串即可
  '竞赛心得',
  '实训心得',
  '读书心得',
  '研学心得',
  '志愿心得',
  '意见建议',
  '实践记录',
  '学习心得'
]

const pastelColors = [
  '#e6fffb',
  '#fff1f0',
  '#f9f0ff',
  '#f0f5ff',
  '#f6ffed',
  '#fff7e6',
  '#f0ffff',
  '#fff0f6',
  '#f0fff0',
  '#f9f9f9',
  '#f0f0f0',
  '#f5f5f5',
  '#f0f0ff',
  '#fff0ff'
]

function getCardColor(index: number) {
  return pastelColors[index % pastelColors.length]
}

const formatDateTime = (value?: string) => {
  if (!value) return ''
  return value.replace('T', ' ')
}

const toDisplayAuthorName = (comment: ForumComment) => {
  if (comment.authorName && comment.authorName.trim()) return comment.authorName
  if (typeof comment.authorId !== 'undefined') return `用户${comment.authorId}`
  return '匿名用户'
}

const formatAvatar = (avatar?: string) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return `${import.meta.env.VITE_API_BASE_URL || ''}/${avatar.replace(/^\/+/, '')}`
}

const mapPost = (item: Partial<ForumPost>): ForumPost => ({
  id: Number(item.id || 0),
  title: item.title || '未命名帖子',
  content: item.content || '',
  category: item.category,
  createdAt: formatDateTime(item.createdAt || item.createTime || (item as any).updateTime),
  authorName: item.authorName || '匿名用户',
  authorAvatar: formatAvatar(item.authorAvatar),
  commentCount: item.commentCount ?? item.comments?.length ?? 0,
  likeCount: item.likeCount ?? 0,
  comments: (item.comments || []).map((c, index) => ({
    ...c,
    id: c.id ?? Date.now() + index,
    postId: Number(c.postId || item.id || 0),
    authorName: toDisplayAuthorName(c),
    authorAvatar: formatAvatar(c.authorAvatar),
    createdAt: formatDateTime(c.createdAt || c.createTime)
  }))
})

const registerPostCardRef = (el: any, id: number) => {
  postCardRefs.value[id] = (el as HTMLElement | null) || null
}

const scrollToAndHighlightPost = async (postId: number) => {
  await nextTick()
  const el = postCardRefs.value[postId]
  if (!el) return

  el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  highlightedPostId.value = postId
  window.setTimeout(() => {
    if (highlightedPostId.value === postId) {
      highlightedPostId.value = null
    }
  }, 2000)
}

async function fetchPosts() {
  loading.value = true
  try {
    const endpoints = ['/forum/page', '/Forum/page']
    let apiData: ApiResult<PageResult<ForumPost>> | null = null
    let lastError: any = null

    for (const url of endpoints) {
      try {
        const { data } = await request.get<ApiResult<PageResult<ForumPost>>>(url, {
          params: {
            page: 1,
            pageSize: 50
          }
        })
        apiData = data
        break
      } catch (e) {
        lastError = e
      }

      try {
        const { data } = await request.post<ApiResult<PageResult<ForumPost>>>(url, {
          page: 1,
          pageSize: 50
        })
        apiData = data
        break
      } catch (e) {
        lastError = e
      }
    }

    if (!apiData) {
      throw lastError || new Error('请求帖子接口失败')
    }

    if (apiData.code !== 1) {
      ElMessage.error(apiData.msg || '获取帖子失败')
      return
    }

    const safeRecords = (apiData.data?.records || []).filter((item) => !!item)
    posts.value = safeRecords.map((item) => mapPost(item as Partial<ForumPost>))
  } catch (error: any) {
    const message = error?.response?.data?.msg || error?.message || '获取帖子失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

async function openDetail(post: ForumPost) {
  detailVisible.value = true
  detailLoading.value = true

  const idx = posts.value.findIndex((p) => p.id === post.id)
  currentPostColor.value = getCardColor(idx >= 0 ? idx : 0)
  try {
    const endpoints = ['/forum/detail', '/Forum/detail']
    let apiData: ApiResult<ForumPost> | null = null
    let lastError: any = null

    for (const url of endpoints) {
      try {
        const { data } = await request.get<ApiResult<ForumPost>>(url, {
          params: { postId: post.id }
        })
        apiData = data
        break
      } catch (e) {
        lastError = e
      }
    }

    if (!apiData) {
      throw lastError || new Error('请求帖子详情接口失败')
    }

    if (apiData.code !== 1 || !apiData.data) {
      ElMessage.error(apiData.msg || '获取帖子详情失败')
      comments.value = []
      return
    }

    const detail = mapPost(apiData.data || {})
    currentPost.value = detail
    comments.value = detail.comments || []
  } catch (error: any) {
    const message = error?.response?.data?.msg || error?.message || '获取帖子详情失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    detailLoading.value = false
  }
}

const hasDraftContent = () => {
  return !!(
    publishForm.value.title.trim() ||
    publishForm.value.category.trim() ||
    publishForm.value.content.trim()
  )
}

const discardConfirmVisible = ref(false)

const closePublishDialog = () => {
  if (!hasDraftContent()) {
    publishVisible.value = false
    return
  }
  discardConfirmVisible.value = true
}

const confirmDiscardDraft = () => {
  discardConfirmVisible.value = false
  publishVisible.value = false
}

const continueEditDraft = () => {
  discardConfirmVisible.value = false
}

const openPublishDialog = () => {
  publishForm.value = {
    title: '',
    category: '',
    content: ''
  }
  publishBgColor.value = pastelColors[Math.floor(Math.random() * pastelColors.length)] || '#bfe8ff'
  publishVisible.value = true
}

const handlePickCategory = (item: string) => {
  publishForm.value.category = item
}

async function handlePublishPost() {
  if (!publishForm.value.title.trim()) {
    ElMessage.warning('请输入帖子标题')
    return
  }
  if (!publishForm.value.category.trim()) {
    ElMessage.warning('请选择帖子分类')
    return
  }
  if (!publishForm.value.content.trim()) {
    ElMessage.warning('请输入帖子内容')
    return
  }

  publishLoading.value = true
  try {
    const username = localStorage.getItem('username') || '匿名用户'
    const { data } = await request.put<ApiResult<null>>('/forum/addPost', {
      title: publishForm.value.title.trim(),
      category: publishForm.value.category.trim(),
      content: publishForm.value.content.trim(),
      authorName: username
    })

    if (data.code !== 1) {
      ElMessage.error(data.msg || '发布失败')
      return
    }

    ElMessage.success('发布成功')
    publishVisible.value = false
    await fetchPosts()

    const latestPostId = posts.value[0]?.id
    if (latestPostId) {
      await scrollToAndHighlightPost(latestPostId)
    }
  } catch (error: any) {
    const message = error?.response?.data?.msg || '发布失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    publishLoading.value = false
  }
}

async function handleSubmitComment() {
  if (!newComment.value.trim() || !currentPost.value) {
    ElMessage.warning('请输入评论内容')
    return
  }

  try {
    const username = localStorage.getItem('username') || '匿名用户'
    const { data } = await request.put<ApiResult<null>>('/forum/addComment', {
      postId: currentPost.value.id,
      authorName: username,
      content: newComment.value.trim()
    })

    if (data.code !== 1) {
      ElMessage.error(data.msg || '发表评论失败')
      return
    }

    ElMessage.success('评论成功')
    newComment.value = ''
    await openDetail(currentPost.value)
    await fetchPosts()
  } catch {
    ElMessage.error('发表评论失败，请稍后重试')
  }
}

onMounted(fetchPosts)
</script>

<template>
  <div class="square-page">
    <header class="square-header">
      <div>
        <h1 class="title">研学交流广场</h1>
        <p class="sub-title">每一条留言都是一次研学记录，也是同学之间互相交流的旅程。</p>
      </div>
      <div class="header-actions">
        <div class="toggle-wrapper" title="切换卡片倾斜效果">
          <div class="toggle-container">
            <input v-model="tiltEnabled" type="checkbox" class="toggle-input">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 292 142" class="toggle">
              <path d="M71 142C31.7878 142 0 110.212 0 71C0 31.7878 31.7878 0 71 0C110.212 0 119 30 146 30C173 30 182 0 221 0C260 0 292 31.7878 292 71C292 110.212 260.212 142 221 142C181.788 142 173 112 146 112C119 112 110.212 142 71 142Z" class="toggle-background" />
              <rect rx="6" height="64" width="12" y="39" x="64" class="toggle-icon on" />
              <path d="M221 91C232.046 91 241 82.0457 241 71C241 59.9543 232.046 51 221 51C209.954 51 201 59.9543 201 71C201 82.0457 209.954 91 221 91ZM221 103C238.673 103 253 88.6731 253 71C253 53.3269 238.673 39 221 39C203.327 39 189 53.3269 189 71C189 88.6731 203.327 103 221 103Z" fill-rule="evenodd" class="toggle-icon off" />
              <g filter="url('#goo')">
                <rect fill="#fff" rx="29" height="58" width="116" y="42" x="13" class="toggle-circle-center" />
                <rect fill="#fff" rx="58" height="114" width="114" y="14" x="14" class="toggle-circle left" />
                <rect fill="#fff" rx="58" height="114" width="114" y="14" x="164" class="toggle-circle right" />
              </g>
              <filter id="goo">
                <feGaussianBlur stdDeviation="10" result="blur" in="SourceGraphic" />
                <feColorMatrix result="goo" values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 18 -7" mode="matrix" in="blur" />
              </filter>
            </svg>
          </div>
        </div>
        <el-button type="primary" size="large" plain class="btn-post publish-cute-trigger" @click="openPublishDialog">
          发布新动态
        </el-button>
      </div>
    </header>

    <el-skeleton v-if="loading" :rows="4" animated />

    <main v-else class="card-grid">
      <article
        v-for="(post, idx) in posts"
        :key="post.id"
        :ref="(el) => registerPostCardRef(el, post.id)"
        class="post-card"
        :class="{ 'post-card-highlight': highlightedPostId === post.id, 'post-card-straight': !tiltEnabled }"
        :style="{ backgroundColor: getCardColor(idx) }"
        @click="openDetail(post)"
      >
        <header class="post-header">
          <span class="post-date">{{ post.createdAt }}</span>
          <span v-if="post.category" class="post-tag">{{ post.category }}</span>
        </header>
        <h2 class="post-title">
          {{ post.title }}
        </h2>
        <p class="post-content">
          {{ post.content }}
        </p>
        <footer class="post-footer">
          <div class="meta">
            <span class="meta-item">
              <el-icon><Comment /></el-icon>
              <span class="meta-number">{{ post.commentCount }}</span>
            </span>
            <span class="meta-item">
              <el-icon><Star /></el-icon>
              <span class="meta-number">{{ post.likeCount }}</span>
            </span>
          </div>
          <span class="author author-inline">
            <el-avatar
              v-if="post.authorAvatar"
              :src="post.authorAvatar"
              :size="20"
            />
            <span v-else class="author-fallback-avatar">{{ (post.authorName || '匿').slice(0, 1) }}</span>
            <span>{{ post.authorName }}</span>
          </span>
        </footer>
      </article>
    </main>

    <el-dialog
      v-model="publishVisible"
      width="640px"
      class="publish-cute-dialog"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      @closed="discardConfirmVisible = false"
    >
      <div class="publish-cute-form" :style="{ backgroundColor: publishBgColor }">
        <div class="publish-cute-title">
          发布新动态，<br><span>记录你的研学瞬间</span>
        </div>

        <el-input
          v-model="publishForm.title"
          maxlength="80"
          class="publish-cute-input"
          placeholder="输入标题（例如：创新创业大赛感悟）"
        />

        <div class="category-guide">
           帖子属于哪一类？（单选）
        </div>
        <div class="publish-checklist" :style="{ backgroundColor: publishBgColor }">
          <template v-for="(item, idx) in categoryOptions" :key="item">
            <input
              :id="`category-${idx}`"
              type="checkbox"
              name="category"
              :checked="publishForm.category === item"
              @change="handlePickCategory(item)"
            >
            <label :for="`category-${idx}`">{{ item }}</label>
          </template>
        </div>

        <el-input
          v-model="publishForm.content"
          type="textarea"
          :rows="6"
          maxlength="1000"
          show-word-limit
          class="publish-cute-textarea"
          placeholder="写下你的研学心得、建议或实践记录…"
        />

        <div class="publish-cute-footer">
          <button class="publish-cute-btn publish-cute-cancel" @click="closePublishDialog">
            取消
          </button>
          <button class="publish-cute-btn publish-cute-confirm" :disabled="publishLoading" @click="handlePublishPost">
            {{ publishLoading ? '发布中…' : '发布动态 →' }}
          </button>
        </div>

        <transition name="cute-fade">
          <div v-if="discardConfirmVisible" class="cute-discard-mask">
            <div class="cute-discard-card">
              <div class="cute-discard-title">提示</div>
              <div class="cute-discard-text">你有未发布的内容，确认放弃编辑吗？</div>
              <div class="cute-discard-actions">
                <button class="publish-cute-btn" @click="continueEditDraft">继续编辑</button>
                <button class="publish-cute-btn publish-cute-confirm" @click="confirmDiscardDraft">放弃编辑</button>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </el-dialog>

    <el-drawer
      v-model="detailVisible"
      size="460px"
      :with-header="false"
      append-to-body
      class="forum-detail-drawer"
    >
      <div v-if="currentPost" class="drawer-wrapper">
        <section class="drawer-post" :style="{ backgroundColor: currentPostColor }">
          <div class="drawer-date">
            {{ currentPost.createdAt }}
          </div>
          <h2 class="drawer-title">
            {{ currentPost.title }}
          </h2>
          <p class="drawer-content">
            {{ currentPost.content }}
          </p>
          <div class="drawer-footer">
            <span class="author author-inline">
              <el-avatar
                v-if="currentPost.authorAvatar"
                :src="currentPost.authorAvatar"
                :size="20"
              />
              <span v-else class="author-fallback-avatar">{{ (currentPost.authorName || '匿').slice(0, 1) }}</span>
              <span>{{ currentPost.authorName }}</span>
            </span>
            <div class="meta">
              <span class="meta-item">
                <el-icon><Comment /></el-icon>
                <span class="meta-number">{{ currentPost.commentCount }}</span>
              </span>
              <span class="meta-item">
                <el-icon><Star /></el-icon>
                <span class="meta-number">{{ currentPost.likeCount }}</span>
              </span>
            </div>
          </div>
        </section>

        <section class="drawer-comments">
          <h3 class="comments-title">
            评论
            <span class="comments-count">{{ comments.length }}</span>
          </h3>

          <el-skeleton v-if="detailLoading" :rows="3" animated />

          <div v-else class="comments-list">
            <div
              v-for="item in comments"
              :key="item.id"
              class="comment-item"
            >
              <div class="avatar-wrap">
                <el-avatar v-if="item.authorAvatar" :src="item.authorAvatar" class="avatar-img" />
                <div
                  v-else
                  class="avatar"
                  :style="{ backgroundColor: item.avatarColor || '#e5e9f2' }"
                >
                  {{ (item.authorName || '匿').slice(0, 1) }}
                </div>
              </div>
              <div class="comment-content">
                <div class="comment-header">
                  <span class="comment-author">{{ item.authorName || '匿名用户' }}</span>
                  <span class="comment-time">{{ item.createdAt }}</span>
                </div>
                <p class="comment-text">
                  {{ item.content }}
                </p>
              </div>
            </div>
          </div>

          <div class="comment-editor-wrap">
            <div class="comment-editor cute-comment-editor">
              <el-input
                v-model="newComment"
                type="textarea"
                :rows="3"
                class="cute-comment-input"
                placeholder="写下你的研学心得或建议…"
              />
              <div class="comment-editor-footer">
                <button class="publish-cute-btn comment-submit-btn" @click="handleSubmitComment">
                  发表
                </button>
              </div>
            </div>
          </div>
        </section>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.square-page {
  padding: 24px;
  max-width: 1280px;
  margin: 0 auto;
}

.square-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.toggle-wrapper {
  display: inline-flex;
  align-items: center;
}

.title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 4px;
}

.sub-title {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 14px;
  grid-auto-flow: dense;
}

.post-card {
  --card-rotate: 0deg;
  --card-shift-x: 0px;
  --card-shift-y: 0px;
  --card-opacity: 0.97;
  --card-border: rgba(64, 158, 255, 0.28);
  padding: 16px 18px 14px;
  border-radius: 12px;
  border: 1px solid var(--card-border);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  min-height: 140px;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease, opacity 0.16s ease;
  grid-column: span 3;
  transform: translate(var(--card-shift-x), var(--card-shift-y)) rotate(var(--card-rotate));
  opacity: var(--card-opacity);
}

.post-card:nth-child(6n + 1) {
  --card-rotate: -2.8deg;
  --card-shift-x: -10px;
  --card-shift-y: 6px;
  --card-opacity: 0.93;
  --card-border: rgba(255, 120, 117, 0.42);
}

.post-card:nth-child(6n + 2) {
  --card-rotate: 2.4deg;
  --card-shift-x: 9px;
  --card-shift-y: -8px;
  --card-opacity: 0.9;
  --card-border: rgba(115, 209, 61, 0.42);
}

.post-card:nth-child(6n + 3) {
  --card-rotate: -2.1deg;
  --card-shift-x: -7px;
  --card-shift-y: 5px;
  --card-opacity: 0.95;
  --card-border: rgba(64, 158, 255, 0.42);
}

.post-card:nth-child(6n + 4) {
  --card-rotate: 3deg;
  --card-shift-x: 11px;
  --card-shift-y: 7px;
  --card-opacity: 0.92;
  --card-border: rgba(186, 85, 211, 0.42);
  box-shadow: 0 5px 14px rgba(0, 0, 0, 0.08), 0 14px 30px rgba(109, 76, 190, 0.2);
}

.post-card:nth-child(6n + 5) {
  --card-rotate: -2.5deg;
  --card-shift-x: -8px;
  --card-shift-y: -6px;
  --card-opacity: 0.91;
  --card-border: rgba(250, 173, 20, 0.45);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06), 0 10px 24px rgba(255, 120, 117, 0.2);
}

.post-card:nth-child(6n + 6) {
  --card-rotate: 2.2deg;
  --card-shift-x: 7px;
  --card-shift-y: 8px;
  --card-opacity: 0.96;
  --card-border: rgba(19, 194, 194, 0.42);
}

.post-card:nth-child(7n + 1) {
  grid-column: span 4;
}

.post-card:nth-child(7n + 2) {
  grid-column: span 2;
}

.post-card:nth-child(7n + 3) {
  grid-column: span 3;
}

.post-card:nth-child(7n + 4) {
  grid-column: span 5;
}

.post-card:nth-child(7n + 5) {
  grid-column: span 2;
}

.post-card:nth-child(7n + 6) {
  grid-column: span 4;
}

.post-card:nth-child(7n + 7) {
  grid-column: span 3;
}

.post-card:nth-child(3n) {
  min-height: 190px;
}

.post-card:nth-child(4n) {
  min-height: 230px;
}

.post-card:nth-child(5n) {
  min-height: 160px;
}

.post-card:hover {
  transform: translate(calc(var(--card-shift-x) + 3px), calc(var(--card-shift-y) - 6px)) rotate(var(--card-rotate)) scale(1.04);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.12);
}

.post-card-highlight {
  outline: 2px solid rgba(255, 153, 0, 0.9);
  box-shadow: 0 0 0 6px rgba(255, 200, 87, 0.25), 0 20px 34px rgba(255, 153, 0, 0.3) !important;
}

.post-card-straight,
.post-card-straight:nth-child(6n + 1),
.post-card-straight:nth-child(6n + 2),
.post-card-straight:nth-child(6n + 3),
.post-card-straight:nth-child(6n + 4),
.post-card-straight:nth-child(6n + 5),
.post-card-straight:nth-child(6n + 6) {
  --card-rotate: 0deg;
  --card-shift-x: 0px;
  --card-shift-y: 0px;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.post-tag {
  padding: 2px 8px;
  border-radius: 999px;
  background-color: rgba(255, 255, 255, 0.8);
  color: #409eff;
}

.post-title {
  font-size: 16px;
  font-weight: 600;
  margin: 4px 0 8px;
  color: #303133;
}

.post-content {
  flex: 1;
  margin: 0 0 10px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  display: -webkit-box;
  line-clamp: 7;
  -webkit-line-clamp: 7;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #606266;
}

.author {
  font-weight: 500;
}

.author-inline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.author-fallback-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #fff;
  background-color: #c0c4cc;
}

.meta {
  display: flex;
  gap: 10px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.meta-number {
  font-size: 12px;
  color: #606266;
}

.drawer-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #f2ecdf;
  border: 2px solid #2f2a2a;
  border-radius: 14px;
  box-shadow: 6px 6px #2f2a2a;
  padding: 14px;
}

.drawer-post {
  flex: 0 0 auto;
  max-height: 52%;
  overflow-y: auto;
  padding: 16px 18px 12px;
  border-radius: 12px;
  margin-bottom: 14px;
  border: 2px solid rgba(47, 42, 42, 0.22);
  box-shadow: 2px 2px rgba(47, 42, 42, 0.18);
}

.drawer-date {
  font-size: 12px;
  color: #7f8699;
  margin-bottom: 4px;
}

.drawer-title {
  font-size: 18px;
  font-weight: 800;
  line-height: 1.25;
  margin: 2px 0 8px;
  color: #2f2a2a;
}

.drawer-content {
  font-size: 14px;
  color: #4b4e56;
  line-height: 1.5;
  margin: 0 0 10px;
}

.drawer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.drawer-comments {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  margin-top: 8px;
  border-top: 2px solid rgba(47, 42, 42, 0.18);
  padding-top: 10px;
}

.comments-title {
  font-size: 16px;
  font-weight: 800;
  margin: 0 0 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #2f2a2a;
}

.comments-count {
  font-size: 13px;
  color: #8e8a7a;
  font-weight: 700;
}

.comments-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
  scrollbar-width: none;
}

.comments-list::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.comment-item {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px;
  border: 2px solid rgba(47, 42, 42, 0.22);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.46);
  box-shadow: 2px 2px rgba(47, 42, 42, 0.15);
}

.avatar-wrap {
  width: 32px;
  height: 32px;
}

.avatar-img {
  width: 32px;
  height: 32px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #fff;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 2px;
}

.comment-author {
  font-size: 15px;
  font-weight: 700;
  color: #2f2a2a;
}

.comment-time {
  font-size: 12px;
  color: #8e8a7a;
  font-weight: 600;
}

.comment-text {
  font-size: 14px;
  color: #4b4e56;
  margin: 3px 0 0;
  line-height: 1.45;
}

.comment-editor-wrap {
  position: sticky;
  bottom: 0;
  padding-top: 10px;
  background: linear-gradient(180deg, rgba(242, 236, 223, 0) 0%, rgba(242, 236, 223, 0.95) 24%, rgba(242, 236, 223, 1) 100%);
}

.comment-editor {
  margin-top: 0;
  padding-top: 0;
}

.cute-comment-editor {
  border: 2px solid #2f2a2a;
  border-radius: 12px;
  background: #e7dfcf;
  box-shadow: 4px 4px #2f2a2a;
  padding: 10px;
}

.cute-comment-input :deep(.el-textarea__inner) {
  border: 2px solid #2f2a2a;
  border-radius: 10px;
  background: #f2f0df;
  box-shadow: 2px 2px #2f2a2a;
  font-size: 14px;
  font-weight: 600;
  color: #3d3c36;
}

.comment-editor-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.comment-submit-btn {
  min-width: 168px;
  height: 44px;
  background: #d9d6c6;
  color: #2f2a2a;
  border: 2px solid #2f2a2a;
  border-radius: 12px;
  box-shadow: 4px 5px #2f2a2a;
  font-size: 34px;
  font-weight: 900;
  line-height: 1;
}

.comment-submit-btn:hover {
  background: #e2decd;
}

.comment-submit-btn:active {
  transform: translate(2px, 2px);
  box-shadow: 2px 2px #2f2a2a;
}

.btn-post {
  border-radius: 12px !important;
}

.btn-post:hover,
.btn-post.is-plain:hover {
  background-color: rgba(85, 45, 188, 0.409) !important;
  border-color: rgba(34, 36, 37, 0.2) !important;
}

.btn-post:active,
.btn-post.is-plain:active {
  background-color: rgba(10, 118, 226, 0.2) !important;
  border-color: rgba(64, 158, 255, 0.25) !important;
}

.publish-cute-trigger {
  border: 2px solid #2f2a2a !important;
  box-shadow: 4px 4px #2f2a2a;
  font-weight: 800;
  background-color: #f2f0df !important;
  color: #323232 !important;
}

.publish-cute-trigger:hover,
.publish-cute-trigger.is-plain:hover {
  background-color: #fff7cc !important;
  border-color: #2f2a2a !important;
  box-shadow: 5px 5px #2f2a2a;
}

.publish-cute-trigger:active,
.publish-cute-trigger.is-plain:active {
  transform: translate(3px, 3px);
  box-shadow: 1px 1px #2f2a2a;
}

@media (max-width: 1200px) {
  .card-grid {
    grid-template-columns: repeat(8, minmax(0, 1fr));
  }

  .post-card,
  .post-card:nth-child(7n + 1),
  .post-card:nth-child(7n + 2),
  .post-card:nth-child(7n + 3),
  .post-card:nth-child(7n + 4),
  .post-card:nth-child(7n + 5),
  .post-card:nth-child(7n + 6),
  .post-card:nth-child(7n + 7) {
    grid-column: span 4;
  }

  .post-card:nth-child(3n) {
    grid-column: span 8;
  }
}

@media (max-width: 768px) {
  .card-grid {
    grid-template-columns: 1fr;
  }

  .post-card,
  .post-card:nth-child(7n + 1),
  .post-card:nth-child(7n + 2),
  .post-card:nth-child(7n + 3),
  .post-card:nth-child(7n + 4),
  .post-card:nth-child(7n + 5),
  .post-card:nth-child(7n + 6),
  .post-card:nth-child(7n + 7),
  .post-card:nth-child(3n) {
    grid-column: span 1;
    min-height: 150px;
    --card-rotate: 0deg;
    --card-shift-x: 0px;
    --card-shift-y: 0px;
  }
}

:deep(.publish-cute-dialog .el-dialog) {
  border-radius: 12px;
  padding: 0;
  overflow: hidden;
}

:deep(.publish-cute-dialog .el-dialog__header) {
  display: none;
}

:deep(.publish-cute-dialog .el-dialog__body) {
  padding: 0;
}

:deep(.forum-detail-drawer .el-drawer__body) {
  padding: 10px;
  background: #e6e1d6;
}

.publish-cute-form {
  --input-focus: #2d8cf0;
  --font-color: #323232;
  --font-color-sub: #666;
  --bg-color: #f2f0df;
  --main-color: #2f2a2a;
  padding: 22px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
  gap: 16px;
  border-radius: 8px;
  border: 2px solid var(--main-color);
  box-shadow: 6px 6px var(--main-color);
}

.publish-cute-title {
  color: var(--font-color);
  font-weight: 900;
  font-size: 24px;
  line-height: 1.2;
  margin-bottom: 8px;
}

.publish-cute-title span {
  color: var(--font-color-sub);
  font-weight: 700;
  font-size: 18px;
}

.publish-cute-input :deep(.el-input__wrapper),
.publish-cute-input :deep(.el-select__wrapper),
.publish-cute-textarea :deep(.el-textarea__inner) {
  border-radius: 8px;
  border: 2px solid var(--main-color);
  background-color: var(--bg-color);
  box-shadow: 4px 4px var(--main-color);
  font-size: 15px;
  font-weight: 600;
  color: var(--font-color);
}

.publish-cute-input :deep(.el-input__wrapper),
.publish-cute-input :deep(.el-select__wrapper) {
  min-height: 42px;
}

.publish-cute-textarea :deep(.el-textarea__inner) {
  min-height: 140px;
  padding: 10px 12px;
  line-height: 1.5;
}

.publish-cute-input :deep(.is-focus),
.publish-cute-textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--input-focus);
}

.publish-cute-footer {
  margin-top: 4px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.publish-cute-btn {
  cursor: pointer;
  min-width: 118px;
  height: 42px;
  border-radius: 10px;
  border: 2px solid #2f2a2a;
  background-color: #d9d6c6;
  box-shadow: 3px 3px #2f2a2a;
  font-size: 18px;
  font-weight: 800;
  line-height: 1;
  color: #2f2a2a;
}

.publish-cute-btn:hover {
  background-color: #e2decd;
}

.publish-cute-btn:active {
  transform: translate(2px, 2px);
  box-shadow: 1px 1px #2f2a2a;
}

.publish-cute-confirm {
  min-width: 156px;
}

.publish-cute-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cute-discard-mask {
  position: absolute;
  inset: 0;
  z-index: 20;
  background: rgba(0, 0, 0, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

.cute-discard-card {
  width: min(92%, 460px);
  background: #f2f0df;
  border: 2px solid #2f2a2a;
  border-radius: 10px;
  box-shadow: 6px 6px #2f2a2a;
  padding: 18px;
}

.cute-discard-title {
  font-size: 22px;
  font-weight: 900;
  color: #323232;
  margin-bottom: 8px;
}

.cute-discard-text {
  font-size: 16px;
  color: #5d5d5d;
  font-weight: 700;
  margin-bottom: 14px;
}

.cute-discard-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cute-fade-enter-active,
.cute-fade-leave-active {
  transition: opacity 0.16s ease;
}

.cute-fade-enter-from,
.cute-fade-leave-to {
  opacity: 0;
}

.toggle-container {
  --active-color: #e0d6b2;
  --inactive-color: #c9bd98;
  --active-ink: #2c2822;
  position: relative;
  aspect-ratio: 292 / 142;
  height: 1.875em;
}

.toggle-input {
  appearance: none;
  margin: 0;
  position: absolute;
  z-index: 1;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.toggle {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.toggle-background {
  fill: var(--inactive-color);
  transition: fill 0.4s;
}

.toggle-input:checked + .toggle .toggle-background {
  fill: var(--active-color);
}

.toggle-circle-center {
  transform-origin: center;
  transition: transform 0.6s;
}

.toggle-input:checked + .toggle .toggle-circle-center {
  transform: translateX(150px);
}

.toggle-circle {
  transform-origin: center;
  transition: transform 0.45s;
  backface-visibility: hidden;
}

.toggle-circle.left {
  transform: scale(1);
}

.toggle-input:checked + .toggle .toggle-circle.left {
  transform: scale(0);
}

.toggle-circle.right {
  transform: scale(0);
}

.toggle-input:checked + .toggle .toggle-circle.right {
  transform: scale(1);
}

.toggle-icon {
  transition: fill 0.4s;
}

.toggle-icon.on {
  fill: #9d8f68;
}

.toggle-input:checked + .toggle .toggle-icon.on {
  fill: var(--active-ink);
}

.toggle-icon.off {
  fill: #8f835f;
}

.toggle-input:checked + .toggle .toggle-icon.off {
  fill: var(--active-ink);
}

.category-guide {
  align-self: flex-start;
  padding: 6px 12px;
  border: 2px dashed #2f2a2a;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.55);
  font-size: 14px;
  font-weight: 700;
  color: #3f3a3a;
  margin-bottom: -4px;
}

.publish-checklist {
  --text: #414856;
  --check: #4f29f0;
  --disabled: #9ea7c8;
  --border-radius: 10px;
  border-radius: var(--border-radius);
  position: relative;
  box-shadow: 0 10px 30px rgba(65, 72, 86, 0.05);
  padding: 14px 16px;
  display: grid;
  grid-template-columns: 30px auto;
  align-items: center;
  justify-content: start;
  row-gap: 8px;
  border: 2px solid #2f2a2a;
}

.publish-checklist label {
  color: var(--text);
  position: relative;
  cursor: pointer;
  display: grid;
  align-items: center;
  width: fit-content;
  transition: color 0.3s ease;
  margin-right: 20px;
  font-weight: 700;
}

.publish-checklist label::before,
.publish-checklist label::after {
  content: '';
  position: absolute;
}

.publish-checklist label::before {
  height: 2px;
  width: 8px;
  left: -27px;
  background: var(--check);
  border-radius: 2px;
  transition: background 0.3s ease;
}

.publish-checklist label::after {
  height: 4px;
  width: 4px;
  top: 8px;
  left: -25px;
  border-radius: 50%;
}

.publish-checklist input[type='checkbox'] {
  -webkit-appearance: none;
  -moz-appearance: none;
  position: relative;
  height: 15px;
  width: 15px;
  outline: none;
  border: 0;
  margin: 0 15px 0 0;
  cursor: pointer;
  background: transparent;
  display: grid;
  align-items: center;
  margin-right: 20px;
}

.publish-checklist input[type='checkbox']::before,
.publish-checklist input[type='checkbox']::after {
  content: '';
  position: absolute;
  height: 2px;
  top: auto;
  background: var(--check);
  border-radius: 2px;
}

.publish-checklist input[type='checkbox']::before {
  width: 0;
  right: 60%;
  transform-origin: right bottom;
}

.publish-checklist input[type='checkbox']::after {
  width: 0;
  left: 40%;
  transform-origin: left bottom;
}

.publish-checklist input[type='checkbox']:checked::before {
  animation: check-01 0.4s ease forwards;
}

.publish-checklist input[type='checkbox']:checked::after {
  animation: check-02 0.4s ease forwards;
}

.publish-checklist input[type='checkbox']:checked + label {
  color: var(--disabled);
  animation: move 0.3s ease 0.1s forwards;
}

.publish-checklist input[type='checkbox']:checked + label::before {
  background: var(--disabled);
  animation: slice 0.4s ease forwards;
}

.publish-checklist input[type='checkbox']:checked + label::after {
  animation: firework 0.5s ease forwards 0.1s;
}

@keyframes move {
  50% {
    padding-left: 8px;
    padding-right: 0;
  }

  100% {
    padding-right: 4px;
  }
}

@keyframes slice {
  60% {
    width: 100%;
    left: 4px;
  }

  100% {
    width: 100%;
    left: -2px;
    padding-left: 0;
  }
}

@keyframes check-01 {
  0% {
    width: 4px;
    top: auto;
    transform: rotate(0);
  }

  50% {
    width: 0;
    top: auto;
    transform: rotate(0);
  }

  51% {
    width: 0;
    top: 8px;
    transform: rotate(45deg);
  }

  100% {
    width: 5px;
    top: 8px;
    transform: rotate(45deg);
  }
}

@keyframes check-02 {
  0% {
    width: 4px;
    top: auto;
    transform: rotate(0);
  }

  50% {
    width: 0;
    top: auto;
    transform: rotate(0);
  }

  51% {
    width: 0;
    top: 8px;
    transform: rotate(-45deg);
  }

  100% {
    width: 10px;
    top: 8px;
    transform: rotate(-45deg);
  }
}

@keyframes firework {
  0% {
    opacity: 1;
    box-shadow: 0 0 0 -2px #4f29f0, 0 0 0 -2px #4f29f0, 0 0 0 -2px #4f29f0, 0 0 0 -2px #4f29f0, 0 0 0 -2px #4f29f0, 0 0 0 -2px #4f29f0;
  }

  30% {
    opacity: 1;
  }

  100% {
    opacity: 0;
    box-shadow: 0 -15px 0 0 #4f29f0, 14px -8px 0 0 #4f29f0, 14px 8px 0 0 #4f29f0, 0 15px 0 0 #4f29f0, -14px 8px 0 0 #4f29f0, -14px -8px 0 0 #4f29f0;
  }
}
</style>
