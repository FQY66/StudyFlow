<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

interface ApiResult<T> {
  code: number
  msg?: string
  data: T
}

interface CourseApiRow {
  id: number
  title: string
  summary: string
  content: string
  coverPath: string
  videoPath: string
  category: string
  teacherName: string
  duration: string
  status: string
  createTime: string
  updateTime: string
  clickCount: number
  sortOrder: number
}

interface CourseForm {
  id?: number
  title: string
  summary: string
  content: string
  coverPath: string
  videoPath: string
  category: string
  teacherName: string
  duration: string
  status: string
  clickCount: number | ''
  sortOrder: number | ''
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const coverInputRef = ref<HTMLInputElement | null>(null)
const videoInputRef = ref<HTMLInputElement | null>(null)
const coverFile = ref<File | null>(null)
const videoFile = ref<File | null>(null)
const coverPreview = ref('')
const videoName = ref('')

const categoryOptions = ['课程设计', '安全管理', '案例实操', '师资培训', '综合实践']
const statusOptions = ['已发布', '待审核', '已下架']

const formModel = reactive<CourseForm>({
  title: '',
  summary: '',
  content: '',
  coverPath: '',
  videoPath: '',
  category: '',
  teacherName: '',
  duration: '',
  status: '待审核',
  clickCount: 0,
  sortOrder: 0
})

const rules = {
  title: [{ required: true, message: '请输入课程标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择课程分类', trigger: 'change' }],
  summary: [{ required: true, message: '请输入课程摘要', trigger: 'blur' }],
  content: [{ required: true, message: '请输入课程详情', trigger: 'blur' }],
  status: [{ required: true, message: '请选择课程状态', trigger: 'change' }]
}

const formRef = ref()

const courseId = computed(() => Number(route.params.id))

const formatUrl = (path: string) => {
  if (!path) return ''
  if (/^(https?:|data:)/.test(path)) return path
  return `${import.meta.env.VITE_API_BASE_URL || ''}/${path.replace(/^\/+/, '')}`
}

const fetchDetail = async () => {
  loading.value = true
  try {
    const { data } = await request.get<ApiResult<CourseApiRow>>('/premiumCourse/detail', {
      params: { id: courseId.value }
    })

    if (data.code !== 1 || !data.data) {
      ElMessage.error(data.msg || '获取课程详情失败')
      return
    }

    Object.assign(formModel, {
      id: data.data.id,
      title: data.data.title || '',
      summary: data.data.summary || '',
      content: data.data.content || '',
      coverPath: data.data.coverPath || '',
      videoPath: data.data.videoPath || '',
      category: data.data.category || '',
      teacherName: data.data.teacherName || '',
      duration: data.data.duration || '',
      status: data.data.status || '待审核',
      clickCount: data.data.clickCount || 0,
      sortOrder: data.data.sortOrder || 0
    })

    coverPreview.value = formatUrl(data.data.coverPath || '')
    videoName.value = data.data.videoPath ? data.data.videoPath.split('/').pop() || '' : ''
  } catch {
    ElMessage.error('获取课程详情失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const handleCoverChange = (file?: File) => {
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  coverFile.value = file
  const reader = new FileReader()
  reader.onload = () => {
    coverPreview.value = String(reader.result || '')
    formModel.coverPath = coverPreview.value
  }
  reader.readAsDataURL(file)
}

const handleVideoChange = (file?: File) => {
  if (!file) return
  videoFile.value = file
  videoName.value = file.name
}

const uploadFile = async (file: File | null) => {
  if (!file) return ''
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await request.post<ApiResult<Record<string, string>>>('/common/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  if (data.code !== 1) throw new Error(data.msg || '上传失败')
  return data.data?.path || data.data?.url || ''
}

const submitForm = async () => {
  const form = formRef.value
  if (!form) return

  await form.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      const [coverPath, videoPath] = await Promise.all([
        coverFile.value ? uploadFile(coverFile.value) : Promise.resolve(formModel.coverPath),
        videoFile.value ? uploadFile(videoFile.value) : Promise.resolve(formModel.videoPath)
      ])

      const payload = {
        ...formModel,
        coverPath,
        videoPath,
        clickCount: Number(formModel.clickCount || 0),
        sortOrder: Number(formModel.sortOrder || 0)
      }

      const { data } = await request.put<ApiResult<null>>('/premiumCourse/update', payload)
      if (data.code !== 1) {
        ElMessage.error(data.msg || '更新课程失败')
        return
      }

      ElMessage.success('更新成功')
      router.back()
    } catch {
      ElMessage.error('更新失败，请稍后再试')
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  void fetchDetail()
})
</script>

<template>
  <div class="course-edit-page">
    <el-card class="page-card" shadow="never" v-loading="loading">
      <div class="page-header">
        <div>
          <el-button text class="back-btn" @click="router.back()">返回</el-button>
          <div class="page-title">编辑课程</div>
          <div class="page-subtitle">按照卡片式页面编辑课程信息，保存后立即生效。</div>
        </div>
      </div>

      <el-form ref="formRef" :model="formModel" :rules="rules" label-width="110px" class="course-form">
        <el-row :gutter="24">
          <el-col :xs="24" :sm="12">
            <el-form-item label="课程标题" prop="title">
              <el-input v-model="formModel.title" placeholder="请输入课程标题" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="课程分类" prop="category">
              <el-select v-model="formModel.category" class="w-100" filterable allow-create default-first-option placeholder="请选择或输入分类">
                <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="讲师">
              <el-input v-model="formModel.teacherName" placeholder="请输入讲师名称" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="课程时长">
              <el-input v-model="formModel.duration" placeholder="例如：45分钟" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="课程状态" prop="status">
              <el-select v-model="formModel.status" class="w-100">
                <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="排序权重">
              <el-input-number v-model="formModel.sortOrder" :min="0" class="w-100" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="课程摘要" prop="summary">
              <el-input v-model="formModel.summary" type="textarea" :rows="3" placeholder="请输入课程摘要" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="课程详情" prop="content">
              <el-input v-model="formModel.content" type="textarea" :rows="7" placeholder="请输入课程详情" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="课程封面">
              <div class="upload-box" @click="coverInputRef?.click()">
                <img v-if="coverPreview || formModel.coverPath" :src="coverPreview || formatUrl(formModel.coverPath)" alt="cover" class="preview-img" />
                <div v-else class="upload-placeholder">点击上传封面</div>
              </div>
              <input ref="coverInputRef" type="file" accept="image/*" class="hidden-input" @change="(event) => handleCoverChange((event.target as HTMLInputElement).files?.[0])" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="课程视频">
              <div class="upload-box upload-box--video" @click="videoInputRef?.click()">
                <div v-if="videoName || formModel.videoPath" class="upload-file-name">{{ videoName || formModel.videoPath }}</div>
                <div v-else class="upload-placeholder">点击上传视频</div>
              </div>
              <input ref="videoInputRef" type="file" accept="video/*" class="hidden-input" @change="(event) => handleVideoChange((event.target as HTMLInputElement).files?.[0])" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="page-footer">
        <el-button class="sf-btn is-plain" @click="router.back()">取消</el-button>
        <el-button class="sf-btn" type="primary" :loading="submitting" @click="submitForm">保存修改</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.course-edit-page {
  min-height: calc(100vh - 80px);
  padding: 16px;
  background: linear-gradient(180deg, #f6f8fd 0%, #f3f5fb 100%);
}

.page-card {
  max-width: 1180px;
  margin: 0 auto;
  border-radius: 18px;
  border: none;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.page-header {
  margin-bottom: 24px;
}

.back-btn {
  padding-left: 0;
  margin-bottom: 8px;
  color: #5b8def;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 6px;
}

.page-subtitle {
  color: #8a94a6;
  font-size: 14px;
}

.course-form :deep(.el-form-item__label) {
  color: #4b5563;
  font-weight: 500;
}

.w-100 {
  width: 100%;
}

.upload-box {
  width: 100%;
  height: 240px;
  border: 2px dashed #d8dfea;
  border-radius: 16px;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: #fbfcff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.upload-box:hover {
  border-color: #7a9cff;
  box-shadow: 0 6px 18px rgba(90, 124, 255, 0.08);
}

.upload-box--video {
  padding: 16px;
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-placeholder {
  color: #8b95a7;
}

.upload-file-name {
  color: #2f3542;
  word-break: break-all;
  padding: 0 16px;
  text-align: center;
  font-size: 16px;
}

.hidden-input {
  display: none;
}

.page-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .course-edit-page {
    padding: 12px;
  }

  .page-card {
    border-radius: 14px;
  }

  .upload-box {
    height: 200px;
  }
}
</style>
