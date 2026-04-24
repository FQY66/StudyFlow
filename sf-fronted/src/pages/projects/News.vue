<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'

interface NewsItem {
  id: number
  title: string
  category: string
  summary: string
  source: string
  status: string
  createTime: string
  updateTime: string
  clickCount: number
}

interface ApiResult<T> {
  code: number
  msg?: string
  data: T
}

interface PageResult<T> {
  total: number
  records: T[]
}

const route = useRoute()
const router = useRouter()
const keyword = ref(String(route.query.keyword || ''))
const category = ref('')
const loading = ref(false)
const items = ref<NewsItem[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const slides = [
  'https://picsum.photos/seed/research-news-1/1200/360',
  'https://picsum.photos/seed/research-news-2/1200/360',
  'https://picsum.photos/seed/research-news-3/1200/360'
]

const categoryOptions = ['研学动态', '政策解读', '案例分享', '活动通知', '专家观点']

const filteredItems = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const cat = category.value.trim().toLowerCase()
  return items.value.filter((item) => {
    const keywordMatch = !kw || item.title.toLowerCase().includes(kw) || item.summary.toLowerCase().includes(kw)
    const categoryMatch = !cat || item.category.toLowerCase().includes(cat)
    return keywordMatch && categoryMatch
  })
})

const fetchNews = async () => {
  loading.value = true
  try {
    const { data } = await request.get<ApiResult<PageResult<NewsItem>>>('/researchNews/page', {
      params: {
        keyword: keyword.value || undefined,
        category: category.value || undefined,
        page: page.value,
        pageSize: pageSize.value
      }
    })
    if (data.code === 1) {
      total.value = data.data.total
      items.value = (data.data.records || []).map((item) => ({
        ...item,
        summary: item.summary || '',
        source: item.source || '研学资讯',
        status: item.status || '已发布',
        clickCount: item.clickCount || 0
      }))
    }
  } finally {
    loading.value = false
  }
}

const goDetail = (item: NewsItem) => {
  const base = route.path.startsWith('/student') ? '/student' : ''
  router.push(`${base}/projects/news/detail/study/${item.id}`)
}

const reset = async () => {
  keyword.value = ''
  category.value = ''
  page.value = 1
  await fetchNews()
}

onMounted(() => {
  void fetchNews()
})
</script>

<template>
  <div class="projects-info">
    <section class="banner">
      <el-carousel height="360px" indicator-position="outside" arrow="always">
        <el-carousel-item v-for="(src, index) in slides" :key="index">
          <img :src="src" class="carousel-img" alt="研学资讯广告" />
        </el-carousel-item>
      </el-carousel>
    </section>

    <section class="news-toolbar">
      <el-input v-model="keyword" placeholder="搜索资讯标题 / 摘要" clearable class="toolbar-input" @clear="fetchNews" />
      <el-select v-model="category" placeholder="筛选分类" clearable class="toolbar-select" @change="fetchNews" @clear="fetchNews">
        <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <el-button type="primary" class="sf-btn" @click="fetchNews">查询</el-button>
      <el-button class="sf-btn is-plain" @click="reset">重置</el-button>
    </section>

    <section class="news-list" v-loading="loading">
      <div v-for="item in filteredItems" :key="item.id" class="news-row">
        <a class="news-link" href="javascript:void(0)" @click.prevent="goDetail(item)">
          <span class="news-dot" />
          <span class="news-title">{{ item.title }}</span>
          <span class="news-meta">{{ item.category }}</span>
          <span class="news-meta news-date">{{ item.createTime }}</span>
        </a>
      </div>
    </section>

    <div class="news-footer">
      共 {{ total }} 条资讯
    </div>
  </div>
</template>

<style scoped>
.projects-info { padding: 16px 12px 12px; }
.banner { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,.04); }
.carousel-img { width: 100%; height: 100%; object-fit: cover; }
.news-toolbar { display: flex; gap: 12px; align-items: center; margin: 16px 0; flex-wrap: wrap; }
.toolbar-input { width: 280px; }
.toolbar-select { width: 180px; }
.news-list { background: #fff; border-radius: 12px; padding: 10px 16px; box-shadow: 0 4px 12px rgba(0,0,0,.04); }
.news-row { border-bottom: 1px solid #f0f2f5; }
.news-row:last-child { border-bottom: none; }
.news-link { display: flex; align-items: center; gap: 10px; padding: 12px 4px; text-decoration: none; color: inherit; }
.news-link:hover { color: #409eff; }
.news-link:hover .news-title { color: #409eff; }
.news-dot { width: 6px; height: 6px; border-radius: 50%; background: #409eff; flex-shrink: 0; }
.news-title { flex: 1; font-size: 15px; color: #303133; }
.news-meta { font-size: 12px; color: #909399; white-space: nowrap; }
.news-date { min-width: 120px; text-align: right; }
.news-footer { margin-top: 10px; font-size: 13px; color: #909399; }
@media (max-width: 1024px) { .toolbar-input, .toolbar-select { width: 100%; } .news-link { flex-wrap: wrap; } .news-date { min-width: auto; text-align: left; } }
</style>