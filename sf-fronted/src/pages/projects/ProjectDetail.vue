<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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

const resolveCover = (coverPath?: string) => {
  if (!coverPath) return 'https://picsum.photos/seed/sf-project-detail/1200/520'
  if (/^https?:\/\//i.test(coverPath)) return coverPath
  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  return coverPath.startsWith('/') ? `${base}${coverPath}` : `${base}/${coverPath}`
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
          <div class="detail-breadcrumb">项目管理 / 详情</div>
        </div>

        <template v-if="project">
          <img :src="resolveCover(project.coverPath)" class="cover" alt="" />
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
            <p class="article-intro">{{ project.introduction || '暂无简介' }}</p>
          </section>

          <section class="article-block article-body-block">
            <div class="content article-body">{{ project.content || '暂无内容' }}</div>
          </section>
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
  padding: 28px 48px 36px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.detail-breadcrumb {
  font-size: 13px;
  color: #909399;
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
  white-space: pre-wrap;
  color: #303133;
  font-size: 15px;
  line-height: 2;
  text-indent: 2em;
}
</style>
