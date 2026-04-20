<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

interface ProjectCard {
  id: number
  title: string
  category: string
  year: number
  cover: string
  createdAt: string
  views: number
  likes: number
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
}

const keyword = ref('')
const activeYear = ref<'all' | number>('all')
const router = useRouter()
const loading = ref(false)
const projects = ref<ProjectCard[]>([])
const pagination = ref({
  page: 1,
  pageSize: 15,
  total: 0
})

const toYear = (dateStr?: string) => {
  if (!dateStr) return new Date().getFullYear()
  const year = new Date(dateStr.replace(/-/g, '/')).getFullYear()
  return Number.isNaN(year) ? new Date().getFullYear() : year
}

const resolveCover = (coverPath?: string) => {
  if (!coverPath) return 'https://picsum.photos/seed/sf-project/600/360'
  if (/^https?:\/\//i.test(coverPath)) return coverPath
  return coverPath.startsWith('/')
    ? `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}${coverPath}`
    : `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/${coverPath}`
}

const loadProjects = async () => {
  loading.value = true
  try {
    const { data } = await request.get('/project/page', {
      params: {
        page: pagination.value.page,
        pageSize: pagination.value.pageSize
      }
    })

    const pageData = data?.data || {}
    const records = (pageData.records || []) as ProjectStudyVO[]
    pagination.value.total = Number(pageData.total || 0)
    projects.value = records.map((item) => ({
      id: item.id,
      title: item.theme || '未命名项目',
      category: item.category || '未分类',
      year: toYear(item.createTime || item.updateTime),
      cover: resolveCover(item.coverPath),
      createdAt: item.createTime || item.updateTime || '-',
      views: item.clickCount ?? 0,
      likes: item.likeCount ?? 0
    }))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProjects()
})

watch([keyword, activeYear], () => {
  pagination.value.page = 1
})

const yearOptions = computed(() => {
  const years = Array.from(new Set(projects.value.map(p => p.year))).sort(
    (a, b) => b - a
  )
  return years
})

const filteredProjects = computed(() => {
  return projects.value.filter(p => {
    const matchYear =
      activeYear.value === 'all' ? true : p.year === activeYear.value
    const kw = keyword.value.trim().toLowerCase()
    const matchKeyword = !kw
      ? true
      : p.title.toLowerCase().includes(kw) ||
        p.category.toLowerCase().includes(kw)
    return matchYear && matchKeyword
  })
})

const handleCardClick = (project: ProjectCard) => {
  router.push(`/projects/detail/${project.id}`)
}

const handleCurrentChange = async (page: number) => {
  pagination.value.page = page
  await loadProjects()
}

const handleSizeChange = async (size: number) => {
  pagination.value.pageSize = size
  pagination.value.page = 1
  await loadProjects()
}
</script>

<template>
  <div class="project-page">
    <header class="project-header">
      <el-input
        v-model="keyword"
        class="search-input"
        placeholder="输入项目名称或类别进行搜索"
        clearable
        
      />

      <div class="header-right">
        <div class="year-tabs">
          <!-- <button
            class="year-tab"
            :class="{ active: activeYear === 'all' }"
            type="button"
            @click="activeYear = 'all'"
          >
            All
          </button>
          <button
            v-for="year in yearOptions"
            :key="year"
            class="year-tab"
            :class="{ active: activeYear === year }"
            type="button"
            @click="activeYear = year"
          >
            {{ year }}
          </button> -->
        </div>

        <!-- <el-button type="primary" @click="handleCreate" plain class="btn-post">
          新建项目
        </el-button> -->
      </div>
    </header>

    <main v-loading="loading" class="card-grid">
      <article
        v-for="project in filteredProjects"
        :key="project.id"
        class="project-card"
        @click="handleCardClick(project)"
      >
        <div class="cover-wrapper">
          <img :src="project.cover" class="cover-img" alt="" />
          <span class="category-tag">
            {{ project.category }}
          </span>
        </div>

        <div class="card-body">
          <h3 class="card-title">
            {{ project.title }}
          </h3>
          <div class="card-footer">
            <div class="meta-left">
              <span class="date">{{ project.createdAt }}</span>
              <span class="meta-dot">·</span>
              <span class="meta-item">
                <el-icon><View /></el-icon>
                <span>{{ project.views }}</span>
              </span>
              <span class="meta-item">
                <el-icon><Star /></el-icon>
                <span>{{ project.likes }}</span>
              </span>
            </div>
          </div>
        </div>
      </article>

      <div v-if="!loading && filteredProjects.length === 0" class="empty-state">
        暂无符合条件的项目
      </div>
    </main>

    <div class="pagination-wrap">
      <el-pagination
        class="custom-pagination"
        background
        layout="prev, pager, next, sizes, total"
        :current-page="pagination.page"
        :page-size="pagination.pageSize"
        :page-sizes="[15, 30, 45, 60]"
        :total="pagination.total"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.project-page {
  padding: 10px 8px 8px;
  transform: scale(0.96);
  transform-origin: top center;
}

.project-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 10px;
}

.search-input {
  flex: 1;

}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.year-tabs {
  display: flex;
  gap: 8px;
  background-color: #f5f7fa;
  border-radius: 999px;
  padding: 4px;
}

.year-tab {
  border: none;
  background: transparent;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
}

.year-tab.active {
  background-color: #2f43db7e;
  color: #fff;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
  min-height: 240px;
}

.project-card {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
}
.project-card:hover {
  transform: scale(1.02);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.empty-state {
  grid-column: 1 / -1;
  padding: 32px 0;
  text-align: center;
  color: #909399;
  background: #fff;
  border-radius: 12px;
}

.cover-wrapper {
  position: relative;
  width: 100%;
  padding-top: 56%;
  overflow: hidden;
}

.cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.category-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.36);
}

.card-body {
  padding: 10px 12px 8px;
}

.card-title {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 6px;
  color: #303133;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: #909399;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.meta-dot {
  opacity: 0.6;
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

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>

