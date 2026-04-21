<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface ProjectRow {
  id: number
  coverPath: string
  coverUrl: string
  theme: string
  introduction: string
  content: string
  category: string
  capacity: number
  status: string
  createTime: string
  updateTime: string
  likeCount: number
  clickCount: number
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
  createTime: string
  updateTime: string
  likeCount: number
  clickCount: number
}

interface PageResult<T> {
  total: number
  records: T[]
}

interface ApiResult<T> {
  code: number
  msg?: string
  data: T
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

const loading = ref(false)
const tableData = ref<ProjectRow[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(8)
const dialogVisible = ref(false)
const submitting = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const searchForm = reactive({
  theme: '',
  category: ''
})

const formRef = ref()
const coverFile = ref<File | null>(null)
const coverPreview = ref('')
const coverInputRef = ref<HTMLInputElement | null>(null)
const coverDragOver = ref(false)
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

const formatAvatar = (coverPath: string) => {
  if (!coverPath) return ''
  if (coverPath.startsWith('http://') || coverPath.startsWith('https://') || coverPath.startsWith('data:')) {
    return coverPath
  }
  return `${import.meta.env.VITE_API_BASE_URL || ''}/${coverPath.replace(/^\/+/, '')}`
}

const resetCover = () => {
  coverFile.value = null
  coverPreview.value = ''
  coverDragOver.value = false
  formModel.coverPath = ''
  if (coverInputRef.value) {
    coverInputRef.value.value = ''
  }
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

const normalizeProject = (item: ProjectApiRow): ProjectRow => ({
  ...item,
  coverUrl: formatAvatar(item.coverPath),
  introduction: item.introduction || '',
  content: item.content || '',
  category: item.category || '',
  status: item.status || '审核中',
  likeCount: item.likeCount || 0,
  clickCount: item.clickCount || 0
})

const fetchProjects = async () => {
  loading.value = true
  try {
    const { data } = await request.get<ApiResult<PageResult<ProjectApiRow>>>('/project/page', {
      params: {
        theme: searchForm.theme || undefined,
        category: searchForm.category || undefined,
        page: currentPage.value,
        pageSize: pageSize.value
      }
    })

    if (data.code !== 1) {
      ElMessage.error(data.msg || '获取项目列表失败')
      return
    }

    total.value = data.data.total
    tableData.value = data.data.records.map(normalizeProject)
  } catch {
    ElMessage.error('获取项目列表失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  dialogMode.value = 'create'
  Object.assign(formModel, {
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
  resetCover()
  dialogVisible.value = true
}

const openEdit = (row: ProjectRow) => {
  dialogMode.value = 'edit'
  Object.assign(formModel, {
    id: row.id,
    coverPath: row.coverPath,
    theme: row.theme,
    introduction: row.introduction,
    content: row.content,
    category: row.category,
    capacity: row.capacity,
    status: row.status,
    likeCount: row.likeCount,
    clickCount: row.clickCount
  })
  coverFile.value = null
  coverPreview.value = row.coverUrl
  if (coverInputRef.value) {
    coverInputRef.value.value = ''
  }
  dialogVisible.value = true
}

const handleDelete = async (row: ProjectRow) => {
  try {
    await ElMessageBox.confirm(`确认删除项目「${row.theme}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })

    const { data } = await request.delete<ApiResult<null>>('/project/delete', {
      params: { id: row.id }
    })

    if (data.code !== 1) {
      ElMessage.error(data.msg || '删除失败')
      return
    }

    ElMessage.success('删除成功')
    await fetchProjects()
  } catch {
    // cancelled
  }
}

const uploadCover = async () => {
  if (!coverFile.value) return formModel.coverPath || ''

  const formData = new FormData()
  formData.append('file', coverFile.value)
  const { data } = await request.post<ApiResult<Record<string, string>>>('/common/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })

  if (data.code !== 1) {
    throw new Error(data.msg || '封面上传失败')
  }

  return data.data?.path || data.data?.url || ''
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

      const url = dialogMode.value === 'create' ? '/project/save' : '/project/update'
      const method = dialogMode.value === 'create' ? 'post' : 'put'
      const { data } = await request[method]<ApiResult<null>>(url, payload)

      if (data.code !== 1) {
        ElMessage.error(data.msg || (dialogMode.value === 'create' ? '新增失败' : '更新失败'))
        return
      }

      ElMessage.success(dialogMode.value === 'create' ? '新增成功' : '更新成功')
      dialogVisible.value = false
      resetCover()
      await fetchProjects()
    } catch {
      ElMessage.error(dialogMode.value === 'create' ? '新增失败，请稍后再试' : '更新失败，请稍后再试')
    } finally {
      submitting.value = false
    }
  })
}

const resetSearch = async () => {
  searchForm.theme = ''
  searchForm.category = ''
  currentPage.value = 1
  await fetchProjects()
}

const handleSearch = async () => {
  currentPage.value = 1
  await fetchProjects()
}

const handleCurrentChange = async (page: number) => {
  currentPage.value = page
  await fetchProjects()
}

const handleSizeChange = async (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  await fetchProjects()
}

const tableSummary = computed(() => `共 ${total.value} 条项目`)

onMounted(() => {
  fetchProjects()
})
</script>

<template>
  <div class="project-page">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="项目主题">
          <el-input v-model="searchForm.theme" placeholder="请输入主题关键词" clearable />
        </el-form-item>
        <el-form-item label="项目类别">
          <el-input v-model="searchForm.category" placeholder="请输入类别" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" :icon="Plus" @click="openCreate">新增项目</el-button>
        </div>
        <div class="toolbar-right">{{ tableSummary }}</div>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column label="封面" width="110">
          <template #default="{ row }">
            <el-avatar shape="square" :size="64" :src="row.coverUrl">{{ row.theme.slice(0, 1) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="theme" label="主题" min-width="180" />
        <el-table-column prop="category" label="类别" width="120" />
        <el-table-column prop="capacity" label="上限" width="100" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="likeCount" label="点赞" width="90" />
        <el-table-column prop="clickCount" label="点击" width="90" />
        <el-table-column prop="updateTime" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[8, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增项目' : '编辑项目'" width="760px">
      <el-form ref="formRef" :model="formModel" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目主题" prop="theme">
              <el-input v-model="formModel.theme" placeholder="请输入项目主题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目类别" prop="category">
              <el-input v-model="formModel.category" placeholder="请输入项目类别" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报名上限" prop="capacity">
              <el-input-number v-model="formModel.capacity" :min="1" class="w-100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
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
                    <el-icon><Plus /></el-icon>
                    <span>点击或拖拽上传封面</span>
                  </div>
                </div>
                <div class="cover-actions">
                  <el-button size="small" @click="coverInputRef?.click()">选择图片</el-button>
                  <el-button size="small" type="danger" plain @click="resetCover">清空</el-button>
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
              <el-input v-model="formModel.content" type="textarea" :rows="6" placeholder="请输入项目内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.project-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cover-uploader-wrap {
  width: 100%;
}

.cover-uploader {
  width: 100%;
  height: 220px;
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #fafafa;
  cursor: pointer;
}

.cover-uploader.is-drag-over {
  border-color: #409eff;
  background: #f0f7ff;
}

.cover-preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  color: #909399;
}

.cover-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.hidden-file-input {
  display: none;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-right {
  color: #909399;
}

.table-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.w-100 {
  width: 100%;
}
</style>
