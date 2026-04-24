<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'

interface ApiResult<T> {
  code: number
  msg?: string
  data: T
}

interface NewsDetail {
  id: number
  title: string
  category: string
  summary: string
  content: string
  source: string
  status: string
  createTime: string
  updateTime: string
  sortOrder: number
  clickCount: number
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const news = ref<NewsDetail | null>(null)

const type = computed(() => String(route.params.type ?? ''))
const id = computed(() => Number(route.params.id ?? 0))

const typeLabel = computed(() => {
  if (type.value === 'study') return '研学资讯'
  if (type.value === 'gov') return '政府声音'
  if (type.value === 'expert') return '专家观点'
  return '资讯'
})

const fetchDetail = async () => {
  loading.value = true
  try {
    const { data } = await request.get<ApiResult<NewsDetail>>('/researchNews/detail', {
      params: { id: id.value }
    })
    if (data.code === 1 && data.data) {
      news.value = data.data
    }
  } catch {
    news.value = null
  } finally {
    loading.value = false
  }
}

const renderRichText = (html?: string) => {
  if (!html) return '<p>暂无内容</p>'
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')
  doc.querySelectorAll('script, iframe, object, embed').forEach((node) => node.remove())
  doc.querySelectorAll('img').forEach((img) => {
    const src = img.getAttribute('src') || ''
    if (src && !/^(https?:|data:)/.test(src)) {
      img.setAttribute('src', `${import.meta.env.VITE_API_BASE_URL || ''}/${src.replace(/^\/+/, '')}`)
    }
  })
  return doc.body.innerHTML || '<p>暂无内容</p>'
}

onMounted(() => {
  void fetchDetail()
})
</script>

<template>
  <div class="info-detail">
    <el-card shadow="never" class="detail-card" v-loading="loading">
      <div class="detail-header">
        <el-button link type="primary" @click="router.back()">返回</el-button>
        <div class="detail-breadcrumb">{{ typeLabel }}</div>
      </div>

      <template v-if="news">
        <h2 class="detail-title">{{ news.title }}</h2>
        <div class="detail-meta">
          <span>分类：{{ news.category }}</span>
          <span>来源：{{ news.source || '暂无' }}</span>
          <span>点击：{{ news.clickCount }}</span>
        </div>
        <p class="detail-desc">{{ news.summary }}</p>
        <div class="content-box article-body" v-html="renderRichText(news.content)"></div>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.info-detail {
  padding: 16px 12px 12px;
}

.detail-card {
  background: #ffffff;
  border-radius: 12px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.detail-breadcrumb {
  font-size: 12px;
  color: #909399;
}

.detail-title {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 10px;
  font-size: 12px;
  color: #909399;
}

.detail-desc {
  margin: 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
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

