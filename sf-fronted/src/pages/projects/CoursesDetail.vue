<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'

interface ApiResult<T> {
  code: number
  msg?: string
  data: T
}

interface CourseDetail {
  id: number
  title: string
  category: string
  summary: string
  content: string
  coverPath: string
  videoPath: string
  teacherName: string
  duration: string
  status: string
  createTime: string
  updateTime: string
  sortOrder: number
  clickCount: number
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const course = ref<CourseDetail | null>(null)

const id = computed(() => Number(route.params.id ?? 0))

const formatUrl = (path: string) => {
  if (!path) return ''
  if (/^(https?:|data:)/.test(path)) return path
  return `${import.meta.env.VITE_API_BASE_URL || ''}/${path.replace(/^\/+/, '')}`
}

const fetchDetail = async () => {
  loading.value = true
  try {
    const { data } = await request.get<ApiResult<CourseDetail>>('/premiumCourse/detail', {
      params: { id: id.value }
    })
    if (data.code === 1) {
      course.value = data.data
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void fetchDetail()
})
</script>

<template>
  <div class="course-detail">
    <el-card shadow="never" class="detail-card" v-loading="loading">
      <div class="detail-header">
        <el-button link type="primary" @click="router.back()">返回</el-button>
        <div class="detail-breadcrumb">精品课程</div>
      </div>

      <template v-if="course">
        <div class="detail-body">
          <header class="detail-intro">
            <h2 class="detail-title">{{ course.title }}</h2>
            <div class="detail-meta">
              <span>分类：{{ course.category }}</span>
              <span>讲师：{{ course.teacherName || '暂无' }}</span>
              <span>时长：{{ course.duration || '暂无' }}</span>
              <span>点击：{{ course.clickCount }}</span>
            </div>
            <p class="detail-desc">{{ course.summary }}</p>
          </header>

          <section class="detail-section content-section">
            <div class="section-title">课程详情</div>
            <div class="content-box" v-html="course.content"></div>
          </section>

          <section class="detail-section video-section" v-if="course.videoPath">
            <div class="section-title">课程视频</div>
            <div class="video-box">
              <video controls class="video-player" :src="formatUrl(course.videoPath)"></video>
            </div>
          </section>
        </div>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.course-detail {
  padding: 20px 16px 16px;
}

.detail-card {
  background: #ffffff;
  border-radius: 16px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid #ebeef5;
}

.detail-breadcrumb {
  font-size: 14px;
  color: #909399;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-intro {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-title {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  color: #1f2329;
  line-height: 1.35;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  font-size: 15px;
  color: #606266;
  padding: 12px 14px;
  background: #f7f8fa;
  border-radius: 12px;
}

.detail-desc {
  margin: 0;
  font-size: 16px;
  color: #303133;
  line-height: 1.9;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2329;
  padding-left: 12px;
  border-left: 4px solid #409eff;
}

.content-box {
  padding: 16px 18px;
  background: #fafafa;
  border-radius: 12px;
  color: #303133;
  line-height: 1.9;
  font-size: 16px;
}

.video-box {
  padding: 16px;
  background: #0f0f10;
  border-radius: 14px;
}

.video-player {
  display: block;
  width: 100%;
  max-height: 560px;
  border-radius: 10px;
  background: #000;
}
</style>

