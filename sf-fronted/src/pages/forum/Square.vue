<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

interface ForumPost {
  id: number
  title: string
  authorName: string
  createdAt: string
  content: string
  commentCount: number
  likeCount: number
  category?: string
}

interface ForumComment {
  id: number
  authorName: string
  avatarColor?: string
  content: string
  createdAt: string
}

const loading = ref(false)
const posts = ref<ForumPost[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const currentPost = ref<ForumPost | null>(null)
const comments = ref<ForumComment[]>([])
const newComment = ref('')

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
  '#fff0ff',
]

function getCardColor(index: number) {
  return pastelColors[index % pastelColors.length]
}

async function fetchPosts() {
  loading.value = true
  try {
    const resp = await fetch('/api/forum/posts')
    if (!resp.ok) {
      throw new Error('接口返回异常')
    }
    const data = await resp.json()
    // 预期后端返回 { code, msg, data } 结构
    const list = (data?.data ?? []) as ForumPost[]
    posts.value = list
  } catch (e) {
    // 后端还没实现时，使用本地示例数据，方便先调试前端 UI
    if (posts.value.length === 0) {
      posts.value = [
        {
          id: 1,
          title: '发现了一个神奇的研学项目',
          authorName: '张同学',
          createdAt: '2024-09-03',
          content: '今天第一次参加跨校研学交流，认识了很多同好，同一个专业的人一起讨论课题真的很开心！',
          commentCount: 8,
          likeCount: 21,
          category: '研学心得'
        },
        {
          id: 2,
          title: '分享一次难忘的调研经历',
          authorName: 'Coder123',
          createdAt: '2024-09-05',
          content: '在老师带领下参观企业实验室，现场听工程师讲解技术应用场景，对专业有了更清晰的认识。',
          commentCount: 4,
          likeCount: 15,
          category: '实践记录'
        },
        {
          id: 3,
          title: '关于下一期研学主题的建议',
          authorName: 'DebugMaster',
          createdAt: '2024-09-06',
          content: '建议可以组织一次“校园数智化”专题研学，从校园一卡通、大数据平台等角度切入，贴合我们现在的专业方向。',
          commentCount: 10,
          likeCount: 30,
          category: '意见建议'
        },
        {
          id: 4,
          title: '第一次参加研学交流的感受',
          authorName: '小明',
          createdAt: '2024-09-04',
          content: '感觉很新鲜，之前只参加过校内活动，这次能和其他学校的同学一起交流，收获很多！希望以后能多举办这样的活动。',
          commentCount: 6,
          likeCount: 18,
          category: '研学心得'
        },
        {
          id: 5,
          title: '对研学活动的几点看法',
          authorName: '小红',
          createdAt: '2024-09-07',
          content: '觉得研学活动很有意义，能让我们走出课堂，接触实际项目和企业，对未来职业规划也有帮助。希望能增加一些企业参访的环节！',
          commentCount: 3,
          likeCount: 12,
          category: '意见建议'
        },
        {
          id: 6,
          title: '参加研学交流的几点建议',
          authorName: '小蓝',
          createdAt: '2024-09-08',
          content: '建议在研学交流中增加一些互动环节，比如小组讨论、项目展示等，让同学们有更多机会分享自己的想法和成果。',
          commentCount: 5,
          likeCount: 14,
          category: '意见建议'
        },
        {
          id: 7,
          title: '关于研学交流的几点体会',
          authorName: '小绿',
          createdAt: '2024-09-09',
          content: '通过参加研学交流，感觉自己对专业的理解更深入了，也结识了很多志同道合的朋友。希望以后能有更多这样的机会！',
          commentCount: 7,
          likeCount: 20,
          category: '研学心得'
        },
        {
          id: 8,
          title: '对研学交流活动的几点建议',
          authorName: '小紫',
          createdAt: '2024-09-10',
          content: '觉得研学交流活动很有意义，能让我们接触到不同学校的同学和资源。建议可以增加一些跨校合作的项目，让同学们有机会一起完成一个小项目，增强交流的深度和实效。',
          commentCount: 4,
          likeCount: 16,
          category: '意见建议'
        },
        {
          id: 9,
          title: '参加研学交流的几点感受',
          authorName: '小黄',
          createdAt: '2024-09-11',
          content: '感觉研学交流活动很有意义，能让我们走出校园，接触实际项目和企业，对未来职业规划也有帮助。希望能增加一些企业参访的环节！',
          commentCount: 6,
          likeCount: 18,
          category: '研学心得'
        },
        {
          id: 10,
          title: '对研学交流活动的几点看法',
          authorName: '小灰',
          createdAt: '2024-09-12',
          content: '觉得研学交流活动很有意义，能让我们接触到不同学校的同学和资源。建议可以增加一些跨校合作的项目，让同学们有机会一起完成一个小项目，增强交流的深度和实效。',
          commentCount: 5,
          likeCount: 14,
          category: '意见建议'
        },
        {
          id: 11,
          title: '参加研学交流的几点体会',
          authorName: '小白',
          createdAt: '2024-09-13',
          content: '通过参加研学交流，感觉自己对专业的理解更深入了，也结识了很多志同道合的朋友。希望以后能有更多这样的机会！',
          commentCount: 7,
          likeCount: 20,
          category: '研学心得'
        },
        {
          id: 12,
          title: '对研学交流活动的几点建议',
          authorName: '小橙',
          createdAt: '2024-09-14',
          content: '觉得研学交流活动很有意义，能让我们接触到不同学校的同学和资源。建议可以增加一些跨校合作的项目，让同学们有机会一起完成一个小项目，增强交流的深度和实效。',
          commentCount: 4,
          likeCount: 16,
          category: '意见建议'
        }
      ]
    }
    ElMessage.warning('暂未连上后端，已展示示例帖子数据')
  } finally {
    loading.value = false
  }
}

async function openDetail(post: ForumPost) {
  currentPost.value = post
  detailVisible.value = true
  detailLoading.value = true
  try {
    const resp = await fetch(`/api/forum/posts/${post.id}/comments`)
    if (resp.ok) {
      const data = await resp.json()
      comments.value = (data?.data ?? []) as ForumComment[]
    } else {
      throw new Error('接口异常')
    }
  } catch {
    if (comments.value.length === 0) {
      comments.value = [
        {
          id: 1,
          authorName: '小陈',
          avatarColor: '#ff9a9e',
          content: '同感！第一次参加研学活动，收获很多新朋友～',
          createdAt: '2024-09-03 11:20'
        },
        {
          id: 2,
          authorName: '云澜',
          avatarColor: '#a1c4fd',
          content: '老师讲解得特别细致，对专业方向也更有信心了。',
          createdAt: '2024-09-03 15:40'
        },
        {
          id: 3,
          authorName: '泽华',
          avatarColor: '#fbc2eb',
          content: '期待下一次跨校交流，可以多一些企业参访环节。',
          createdAt: '2024-09-04 09:10'
        },
        {
          id: 4,
          authorName: '小雨',
          avatarColor: '#cfd9df',
          content: '我也想参加这样的活动，感觉能学到很多课本以外的东西！',
          createdAt: '2024-09-04 14:25'
        },
        {
          id: 5,
          authorName: '明明',
          avatarColor: '#a18cd1',
          content: '建议可以组织一次“校园数智化”专题研学，从校园一卡通、大数据平台等角度切入，贴合我们现在的专业方向。',
          createdAt: '2024-09-05 10:00'
        }
      ]
    }
  } finally {
    detailLoading.value = false
  }
}

function handleSubmitComment() {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  // 目前先在前端本地追加，后端实现后可以改成调用接口
  comments.value.unshift({
    id: Date.now(),
    authorName: '我',
    avatarColor: '#ffd08a',
    content: newComment.value.trim(),
    createdAt: new Date().toLocaleString()
  })
  newComment.value = ''
  ElMessage.success('已添加到本地评论（后端接好接口后可保存到数据库）')
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
      <el-button type="primary" size="large" plain class="btn-post">
        发布新动态
      </el-button>
    </header>

    <el-skeleton v-if="loading" :rows="4" animated />

    <main v-else class="card-grid">
      <article
        v-for="(post, idx) in posts"
        :key="post.id"
        class="post-card"
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
          <span class="author">
            {{ post.authorName }}
          </span>
        </footer>
      </article>
    </main>

    <el-drawer
      v-model="detailVisible"
      size="420px"
      :with-header="false"
      append-to-body
    >
      <div v-if="currentPost" class="drawer-wrapper">
        <section class="drawer-post" :style="{ backgroundColor: pastelColors[0] }">
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
            <span class="author">{{ currentPost.authorName }}</span>
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

          <div v-else>
            <div
              v-for="item in comments"
              :key="item.id"
              class="comment-item"
            >
              <div
                class="avatar"
                :style="{ backgroundColor: item.avatarColor || '#e5e9f2' }"
              >
                {{ item.authorName.slice(0, 1) }}
              </div>
              <div class="comment-content">
                <div class="comment-header">
                  <span class="comment-author">{{ item.authorName }}</span>
                  <span class="comment-time">{{ item.createdAt }}</span>
                </div>
                <p class="comment-text">
                  {{ item.content }}
                </p>
              </div>
            </div>
          </div>

          <div class="comment-editor">
            <el-input
              v-model="newComment"
              type="textarea"
              :rows="3"
              placeholder="写下你的研学心得或建议…"
            />
            <div class="comment-editor-footer">
              <el-button type="primary" size="small" @click="handleSubmitComment">
                发表
              </el-button>
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
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.post-card {
  padding: 16px 18px 14px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  min-height: 180px;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.post-card:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.08);
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
  -webkit-line-clamp: 5;
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
}

.drawer-post {
  padding: 16px 18px 12px;
  border-radius: 12px;
  margin-bottom: 16px;
}

.drawer-date {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.drawer-title {
  font-size: 18px;
  font-weight: 600;
  margin: 4px 0 8px;
}

.drawer-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
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
  display: flex;
  flex-direction: column;
  margin-top: 8px;
}

.comments-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.comments-count {
  font-size: 12px;
  color: #909399;
}

.comment-item {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
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
  font-size: 13px;
  font-weight: 500;
}

.comment-time {
  font-size: 11px;
  color: #c0c4cc;
}

.comment-text {
  font-size: 13px;
  color: #606266;
  margin: 0;
}

.comment-editor {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
}

.comment-editor-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
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
</style>
