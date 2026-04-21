<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const projects = ref<ProjectCard[]>([])
const pagination = ref({
  page: 1,
  pageSize: 15,
  total: 0
})

const toolbarKeyword = computed(() => {
  return typeof route.query.q === 'string' ? route.query.q.trim().toLowerCase() : ''
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

const filteredProjects = computed(() => {
  const kw = toolbarKeyword.value
  return projects.value.filter((p) => {
    if (!kw) return true
    return (
      p.title.toLowerCase().includes(kw) ||
      p.category.toLowerCase().includes(kw)
    )
  })
})

watch(
  () => route.query.q,
  () => {
    pagination.value.page = 1
  }
)

const handleCardClick = (project: ProjectCard) => {
  const isStudentView = route.path.startsWith('/student')
  const detailPath = isStudentView ? `/student/projects/detail/${project.id}` : `/projects/detail/${project.id}`
  router.push(detailPath)
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
  height: 100%;
  box-sizing: border-box;
  padding: 8px 8px 8px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  align-content: start;
  grid-auto-rows: minmax(250px, auto);
  padding-right: 4px;
}

.project-card {
  background-color: #ffffff;
  border-radius: 14px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
  height: 100%;
  min-height: 250px;
}

.project-card:hover {
  transform: translateY(-2px) scale(1.01);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.12);
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
  padding-top: 52%;
  min-height: 140px;
  overflow: hidden;
  flex: none;
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
  padding: 12px 14px 12px;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 8px;
  color: #303133;
  line-height: 1.4;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
  min-height: 20px;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.meta-dot {
  opacity: 0.6;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
  flex: none;
  padding-bottom: 2px;
}
</style>
