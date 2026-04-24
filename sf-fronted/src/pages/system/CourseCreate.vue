<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface ApiResult<T> {
  code: number
  msg?: string
  data: T
}

interface CourseForm {
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

const router = useRouter()
const submitting = ref(false)
const coverInputRef = ref<HTMLInputElement | null>(null)
const videoInputRef = ref<HTMLInputElement | null>(null)
const coverFile = ref<File | null>(null)
const videoFile = ref<File | null>(null)
const coverPreview = ref('')
const videoName = ref('')

const categoryOptions = ['课程设计', '安全管理', '案例实操', '师资培训', '综合实践']
const statusOptions = ['已发布', '待审核', '已下架']

const formRef = ref()
const formModel = reactive<CourseForm>({
  title: '',
  summary: '',
  content: '',
  coverPath: '',
  videoPath: '',
  category: '',
  teacherName: '',
  duration: '',
  status: '已发布',
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

const formatUrl = (path: string) => {
  if (!path) return ''
  if (/^(https?:|data:)/.test(path)) return path
  return `${import.meta.env.VITE_API_BASE_URL || ''}/${path.replace(/^\/+/, '')}`
}

const resetFiles = () => {
  coverFile.value = null
  videoFile.value = null
  coverPreview.value = ''
  videoName.value = ''
  if (coverInputRef.value) coverInputRef.value.value = ''
  if (videoInputRef.value) videoInputRef.value.value = ''
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

      const { data } = await request.post<ApiResult<null>>('/premiumCourse/save', payload)
      if (data.code !== 1) {
        ElMessage.error(data.msg || '新增课程失败')
        return
      }

      ElMessage.success('新增成功')
      resetFiles()
      await router.push('/system/courses')
    } catch {
      ElMessage.error('新增失败，请稍后再试')
    } finally {
      submitting.value = false
    }
  })
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  resetFiles()
})
</script>

<template>
  <div class="admin-page">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="page-header">
          <el-button text :icon="ArrowLeft" @click="goBack">返回列表</el-button>
          <div class="page-title">新增精品课程</div>
        </div>
      </template>

      <el-form ref="formRef" :model="formModel" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="课程标题" prop="title">
              <el-input v-model="formModel.title" placeholder="请输入课程标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程分类" prop="category">
              <el-select v-model="formModel.category" class="w-100" filterable allow-create default-first-option placeholder="请选择或输入分类">
                <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="讲师">
              <el-input v-model="formModel.teacherName" placeholder="请输入讲师名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程时长">
              <el-input v-model="formModel.duration" placeholder="例如：45分钟" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程状态" prop="status">
              <el-select v-model="formModel.status" class="w-100">
                <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
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
              <el-input v-model="formModel.content" type="textarea" :rows="6" placeholder="请输入课程详情" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程封面">
              <div class="upload-box" @click="coverInputRef?.click()">
                <img v-if="coverPreview || formModel.coverPath" :src="coverPreview || formatUrl(formModel.coverPath)" alt="cover" class="preview-img" />
                <div v-else class="upload-placeholder">点击上传封面</div>
              </div>
              <input ref="coverInputRef" type="file" accept="image/*" class="hidden-input" @change="(event) => handleCoverChange((event.target as HTMLInputElement).files?.[0])" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
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

      <template #footer>
        <div class="footer-actions">
          <el-button class="sf-btn is-plain" @click="goBack">取消</el-button>
          <el-button class="sf-btn" type="primary" :loading="submitting" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.admin-page { display: flex; flex-direction: column; gap: 12px; }
.page-card { min-height: calc(100vh - 160px); }
.page-header { display: flex; align-items: center; gap: 12px; }
.page-title { font-size: 18px; font-weight: 600; color: #303133; }
.w-100 { width: 100%; }
.upload-box { width: 100%; height: 180px; border: 2px dashed #dcdfe6; border-radius: 12px; display: grid; place-items: center; overflow: hidden; background: #fafafa; cursor: pointer; }
.upload-box--video { padding: 16px; }
.preview-img { width: 100%; height: 100%; object-fit: cover; }
.upload-placeholder { color: #909399; }
.upload-file-name { color: #303133; word-break: break-all; padding: 0 16px; text-align: center; }
.hidden-input { display: none; }
.footer-actions { display: flex; justify-content: flex-end; gap: 12px; }
</style>
