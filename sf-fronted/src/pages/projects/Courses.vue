<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AnnouncementPanel from '@/components/AnnouncementPanel.vue'
import request from '@/utils/request'

interface CourseItem {
  id: number
  title: string
  summary: string
  content: string
  coverPath: string
  coverUrl: string
  videoPath: string
  videoUrl: string
  category: string
  teacherName: string
  duration: string
  status: string
  createTime: string
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

const router = useRouter()
const keyword = ref('')
const category = ref('')
const loading = ref(false)
const courses = ref<CourseItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(6)


const categoryOptions = ['课程设计', '安全管理', '案例实操', '师资培训', '综合实践']

const formatUrl = (path: string) => {
  if (!path) return ''
  if (/^(https?:|data:)/.test(path)) return path
  return `${import.meta.env.VITE_API_BASE_URL || ''}/${path.replace(/^\/+/, '')}`
}

const filteredCourses = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const cat = category.value.trim().toLowerCase()
  return courses.value.filter((c) => {
    const keywordMatch = !kw || c.title.toLowerCase().includes(kw) || c.summary.toLowerCase().includes(kw)
    const categoryMatch = !cat || c.category.toLowerCase().includes(cat)
    return keywordMatch && categoryMatch
  })
})

const fetchCourses = async () => {
  loading.value = true
  try {
    const { data } = await request.get<ApiResult<PageResult<CourseItem>>>('/premiumCourse/page', {
      params: {
        keyword: keyword.value || undefined,
        category: category.value || undefined,
        page: page.value,
        pageSize: pageSize.value
      }
    })
    if (data.code === 1) {
      total.value = data.data.total
      courses.value = (data.data.records || []).map((item) => ({
        ...item,
        coverUrl: formatUrl(item.coverPath),
        videoUrl: formatUrl(item.videoPath),
        summary: item.summary || '',
        content: item.content || '',
        teacherName: item.teacherName || '',
        duration: item.duration || '',
        status: item.status || '已发布',
        clickCount: item.clickCount || 0
      }))
    }
  } finally {
    loading.value = false
  }
}

const reset = async () => {
  keyword.value = ''
  category.value = ''
  page.value = 1
  await fetchCourses()
}

const openDetail = (id: number) => {
  const prefix = router.currentRoute.value.path.startsWith('/student') ? '/student' : ''
  router.push(`${prefix}/projects/courses/${id}`)
}

onMounted(() => {
  void fetchCourses()
})
</script>

<template>
  <div class="courses-page">
    <section class="top-bar">
      <div class="top-left">
        <h2 class="page-title">精品课程</h2>
        <div class="page-subtitle">精选优质研学课程内容，快速找到你想学的主题。</div>
      </div>
      <div class="search-bar">
        <el-input v-model="keyword" class="search" placeholder="搜索课程标题 / 摘要" clearable @clear="fetchCourses" />
        <el-select v-model="category" class="search" placeholder="筛选分类" clearable @change="fetchCourses" @clear="fetchCourses">
          <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button type="primary" class="sf-btn" @click="fetchCourses">查询</el-button>
        <el-button class="sf-btn is-plain" @click="reset">重置</el-button>
      </div>
    </section>

    <section class="layout" v-loading="loading">
      <main class="main">
        <a v-for="item in filteredCourses" :key="item.id" class="course-link" href="javascript:void(0)" @click.prevent="openDetail(item.id)">
          <article class="course">
            <div class="course-cover">
              <img :src="item.coverUrl || 'https://picsum.photos/seed/sf-course-default/560/320'" alt="" />
            </div>
            <div class="course-body">
              <div class="course-title">{{ item.title }}</div>
              <div class="course-summary">{{ item.summary }}</div>
              <div class="course-tags">
                <span class="tag">{{ item.category }}</span>
                <span v-if="item.teacherName" class="tag">讲师：{{ item.teacherName }}</span>
                <span v-if="item.duration" class="tag">时长：{{ item.duration }}</span>
              </div>
              <div class="course-meta">
                <span class="meta-date">{{ item.createTime }}</span>
                <span class="meta-dot">·</span>
                <span class="meta-action">阅读 {{ item.clickCount }}</span>
              </div>
            </div>
          </article>
        </a>
      </main>

      <aside class="aside">
        <AnnouncementPanel title="课程公告" />
      </aside>
    </section>
  </div>
</template>

<style scoped>
.courses-page { padding: 16px 12px 12px; }
.top-bar { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.page-title { margin: 0 0 4px; font-size: 24px; font-weight: 700; color: #303133; }
.page-subtitle { font-size: 14px; color: #909399; }
.search-bar { display: flex; gap: 12px; flex-wrap: wrap; }
.search { width: 240px; }
.layout { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 16px; align-items: start; }
.course { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04); display: grid; grid-template-columns: 240px minmax(0, 1fr); transition: transform 0.18s ease, box-shadow 0.18s ease; }
.course-link { display: block; text-decoration: none; color: inherit; margin-bottom: 16px; }
.course-link:hover .course { transform: translateY(-2px) scale(1.01); box-shadow: 0 10px 22px rgba(0, 0, 0, 0.08); }
.course-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.course-body { padding: 14px 16px 12px; }
.course-title { font-size: 18px; font-weight: 700; color: #303133; margin-bottom: 6px; }
.course-summary { font-size: 15px; color: #606266; line-height: 1.6; margin-bottom: 10px; }
.course-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 10px; }
.tag { font-size: 13px; padding: 2px 8px; border-radius: 999px; background: #ecf5ff; color: #409eff; }
.course-meta { font-size: 13px; color: #909399; display: flex; align-items: center; gap: 8px; }
.meta-action { color: #409eff; }
@media (max-width: 1024px) { .layout { grid-template-columns: 1fr; } .search { width: 100%; } .course { grid-template-columns: 1fr; } }
</style>