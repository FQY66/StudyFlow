<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { likeIcon, shareIcon } from '@/components/icons'
import request from '@/utils/request'

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
  projectSignupList?: Array<Record<string, unknown>>
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const project = ref<ProjectStudyVO | null>(null)

const id = computed(() => String(route.params.id ?? ''))

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

onMounted(() => {
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
              <span>已报名人数：{{ project.projectSignupList?.length || 0 }} 人</span>
            </div>

            <section class="article-block">
              <h2 class="article-intro-title">简介</h2>
              <div class="article-intro" v-html="renderRichText(project.introduction, '暂无简介')"></div>
            </section>

            <section class="article-block article-body-block">
              <div class="content article-body" v-html="renderRichText(project.content, '暂无内容')"></div>
            </section>

            <div class="project-actions">
              <button class="join-button" type="button">
                我要报名
              </button>
              <div class="project-actions-right">
                <button class="icon-action-button type-like" type="button" aria-label="收藏">
                  <el-icon class="icon-action-svg"><component :is="likeIcon" /></el-icon>
                  <span>收藏</span>
                </button>
                <button class="icon-action-button type-share" type="button" aria-label="分享">
                  <el-icon class="icon-action-svg"><component :is="shareIcon" /></el-icon>
                  <span>分享</span>
                </button>
              </div>
            </div>
          </div>
        </template>

        <el-empty v-else description="暂无项目详情" />
      </el-card>
    </div>
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
  transition: transform 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}

.join-button:hover {
  transform: translate(-50%, -50%) translateY(-1px);
  background: #0f66e6;
  box-shadow: 0 8px 16px rgba(22, 119, 255, 0.24);
}

.join-button:active {
  transform: translate(-50%, -50%) translateY(1px);
  box-shadow: 0 4px 10px rgba(22, 119, 255, 0.18);
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
