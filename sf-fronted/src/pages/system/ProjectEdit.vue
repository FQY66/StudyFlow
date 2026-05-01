<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

interface ApiResult<T> {
  code: number
  msg?: string
  data: T
}

interface ProjectApiRow {
  id: number
  coverPath: string
  theme: string
  introduction: string
  content: string
  category: string
  capacity: number
  status: string
  likeCount: number
  clickCount: number
}

interface ProjectForm {
  id?: number
  coverPath: string
  theme: string
  introduction: string
  content: string
  category: string
  capacity: number | ''
  status: string
  likeCount: number | ''
  clickCount: number | ''
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const formRef = ref()
const coverFile = ref<File | null>(null)
const coverPreview = ref('')
const coverInputRef = ref<HTMLInputElement | null>(null)
const coverDragOver = ref(false)
const categoryOptions = ref<string[]>([])

const formModel = reactive<ProjectForm>({
  id: undefined,
  coverPath: '',
  theme: '',
  introduction: '',
  content: '',
  category: '',
  capacity: '',
  status: '审核中',
  likeCount: 0,
  clickCount: 0
})

const rules = {
  theme: [{ required: true, message: '请输入项目主题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择项目类别', trigger: 'change' }],
  introduction: [{ required: true, message: '请输入项目简介', trigger: 'blur' }],
  content: [{ required: true, message: '请输入项目内容', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入报名上限', trigger: 'blur' }],
  status: [{ required: true, message: '请选择项目状态', trigger: 'change' }]
}

const projectId = Number(route.params.id)

const formatAvatar = (coverPath: string) => {
  if (!coverPath) return ''
  if (coverPath.startsWith('http://') || coverPath.startsWith('https://') || coverPath.startsWith('data:')) return coverPath
  return `${import.meta.env.VITE_API_BASE_URL || ''}/${coverPath.replace(/^\/+/, '')}`
}

const resetCover = () => {
  coverFile.value = null
  coverPreview.value = ''
  coverDragOver.value = false
  formModel.coverPath = ''
  if (coverInputRef.value) coverInputRef.value.value = ''
}

const handleCoverFile = (file?: File) => {
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

const onCoverDragOver = (event: DragEvent) => {
  event.preventDefault()
  coverDragOver.value = true
}

const onCoverDragLeave = () => {
  coverDragOver.value = false
}

const onCoverDrop = (event: DragEvent) => {
  event.preventDefault()
  coverDragOver.value = false
  handleCoverFile(event.dataTransfer?.files?.[0])
}

const uploadCover = async () => {
  if (!coverFile.value) return formModel.coverPath || ''

  const formData = new FormData()
  formData.append('file', coverFile.value)
  const { data } = await request.post<ApiResult<Record<string, string>>>('/common/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

  if (data.code !== 1) {
    throw new Error(data.msg || '封面上传失败')
  }

  return data.data?.path || data.data?.url || ''
}

const fetchCategoryOptions = async () => {
  try {
    const { data } = await request.get<ApiResult<string[]>>('/project/categories')
    if (data.code === 1) {
      categoryOptions.value = (data.data || []).filter(Boolean)
      return
    }
  } catch {
    // ignore
  }
  categoryOptions.value = []
}

const fetchDetail = async () => {
  loading.value = true
  try {
    const { data } = await request.get<ApiResult<ProjectApiRow>>('/project/detail', { params: { id: projectId } })
    if (data.code !== 1 || !data.data) {
      ElMessage.error(data.msg || '获取项目详情失败')
      return
    }

    Object.assign(formModel, {
      id: data.data.id,
      coverPath: data.data.coverPath || '',
      theme: data.data.theme || '',
      introduction: data.data.introduction || '',
      content: data.data.content || '',
      category: data.data.category || '',
      capacity: data.data.capacity || '',
      status: data.data.status || '审核中',
      likeCount: data.data.likeCount || 0,
      clickCount: data.data.clickCount || 0
    })
    coverPreview.value = formatAvatar(data.data.coverPath || '')
  } catch {
    ElMessage.error('获取项目详情失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const submitForm = async () => {
  const form = formRef.value
  if (!form) return

  await form.validate(async (valid: boolean) => {
    if (!valid) return

    submitting.value = true
    try {
      const coverPath = coverFile.value ? await uploadCover() : formModel.coverPath
      const payload = {
        ...formModel,
        coverPath,
        capacity: Number(formModel.capacity),
        likeCount: Number(formModel.likeCount || 0),
        clickCount: Number(formModel.clickCount || 0),
        createTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
        updateTime: new Date().toISOString().slice(0, 19).replace('T', ' ')
      }

      const { data } = await request.put<ApiResult<null>>('/project/update', payload)
      if (data.code !== 1) {
        ElMessage.error(data.msg || '更新失败')
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
  void fetchCategoryOptions()
  void fetchDetail()
})

onBeforeUnmount(() => {
  coverFile.value = null
})
</script>

<template>
  <div class="project-edit-page">
    <el-card class="page-card" shadow="never" v-loading="loading">
      <div class="page-header">
        <div>
          <el-button text class="back-btn" @click="router.back()">返回</el-button>
          <div class="page-title">编辑项目</div>
          <div class="page-subtitle">将项目编辑拆分为独立页面，保存后立即返回列表。</div>
        </div>
      </div>

      <el-form ref="formRef" :model="formModel" :rules="rules" label-width="100px" class="project-form">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="项目主题" prop="theme">
              <el-input v-model="formModel.theme" placeholder="请输入项目主题" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="项目类别" prop="category">
              <el-select v-model="formModel.category" class="w-100" filterable allow-create default-first-option placeholder="请输入或选择项目类别">
                <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="报名上限" prop="capacity">
              <el-input-number v-model="formModel.capacity" :min="1" class="w-100" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="项目状态" prop="status">
              <el-select v-model="formModel.status" class="w-100">
                <el-option label="审核中" value="审核中" />
                <el-option label="已发布" value="已发布" />
                <el-option label="已下架" value="已下架" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面路径">
              <div class="cover-uploader-wrap">
                <div
                  class="cover-uploader"
                  :class="{ 'is-drag-over': coverDragOver }"
                  @click="coverInputRef?.click()"
                  @dragover="onCoverDragOver"
                  @dragleave="onCoverDragLeave"
                  @drop="onCoverDrop"
                >
                  <img v-if="coverPreview || formModel.coverPath" :src="coverPreview || formatAvatar(formModel.coverPath)" alt="cover" class="cover-preview-img" />
                  <div v-else class="cover-placeholder">
                    <span>点击或拖拽上传封面</span>
                  </div>
                </div>
                <div class="cover-actions">
                  <el-button size="small" type="primary" class="sf-btn" @click="coverInputRef?.click()">选择图片</el-button>
                  <el-button size="small" type="primary" class="sf-btn is-plain" @click="resetCover">清空</el-button>
                </div>
                <input
                  ref="coverInputRef"
                  type="file"
                  accept="image/*"
                  class="hidden-file-input"
                  @change="(event) => handleCoverFile((event.target as HTMLInputElement).files?.[0])"
                />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="项目简介" prop="introduction">
              <el-input v-model="formModel.introduction" type="textarea" :rows="3" placeholder="请输入项目简介" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="项目内容" prop="content">
              <el-input v-model="formModel.content" type="textarea" :rows="7" placeholder="请输入项目内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="page-footer">
        <el-button class="sf-btn is-plain page-btn" @click="router.back()">取消</el-button>
        <el-button class="sf-btn page-btn" type="primary" :loading="submitting" @click="submitForm">保存修改</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.project-edit-page {
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

.page-header { margin-bottom: 20px; }
.back-btn { padding-left: 0; margin-bottom: 8px; color: #5b8def; }
.page-title { font-size: 24px; font-weight: 700; color: #1f2937; margin-bottom: 6px; }
.page-subtitle { color: #8a94a6; font-size: 14px; }
.w-100 { width: 100%; }

.cover-uploader-wrap { width: 100%; max-width: 520px; }
.cover-uploader {
  width: 100%;
  height: 260px;
  border: 2px dashed #d7dce6;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #fafbfe;
  cursor: pointer;
}
.cover-uploader.is-drag-over {
  border-color: #7aa7ff;
  background: #eef5ff;
}
.cover-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-placeholder {
  color: #8a94a6;
}
.cover-actions { display: flex; gap: 8px; margin-top: 10px; }
.hidden-file-input { display: none; }
.page-footer { display: flex; justify-content: flex-end; gap: 14px; margin-top: 28px; }
.page-btn { min-width: 112px; height: 44px; border-radius: 12px; }
@media (max-width: 768px) {
  .project-edit-page { padding: 12px; }
  .page-card { border-radius: 14px; }
}
</style>
