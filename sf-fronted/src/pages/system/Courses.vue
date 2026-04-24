<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ApproveIcon, Delete3Icon, Editor3Icon, UnderReviewIcon } from '@/components/icons'

interface CourseRow {
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
  updateTime: string
  clickCount: number
  sortOrder: number
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

interface PageResult<T> {
  total: number
  records: T[]
}

interface ApiResult<T> {
  code: number
  msg?: string
  data: T
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
  createTime?: string
  updateTime?: string
  clickCount: number | ''
  sortOrder: number | ''
}

const router = useRouter()
const loading = ref(false)
const tableData = ref<CourseRow[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(8)
const dialogVisible = ref(false)
const submitting = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')

const searchForm = reactive({
  keyword: '',
  category: '',
  status: ''
})

const formRef = ref()
const coverInputRef = ref<HTMLInputElement | null>(null)
const coverFile = ref<File | null>(null)
const videoInputRef = ref<HTMLInputElement | null>(null)
const videoFile = ref<File | null>(null)
const coverPreview = ref('')
const videoName = ref('')

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

const categoryOptions = ['课程设计', '安全管理', '案例实操', '师资培训', '综合实践']
const statusOptions = ['已发布', '待审核', '已下架']

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

const openDetail = (id: number) => {
  const prefix = router.currentRoute.value.path.startsWith('/student') ? '/student' : ''
  router.push(`${prefix}/projects/courses/detail/${id}`)
}

const normalizeCourse = (item: CourseApiRow): CourseRow => ({
  ...item,
  summary: item.summary || '',
  content: item.content || '',
  coverUrl: formatUrl(item.coverPath),
  videoUrl: formatUrl(item.videoPath),
  teacherName: item.teacherName || '',
  duration: item.duration || '',
  status: item.status || '已发布',
  clickCount: item.clickCount || 0,
  sortOrder: item.sortOrder || 0
})

const fetchCourses = async () => {
  loading.value = true
  try {
    const { data } = await request.get<ApiResult<PageResult<CourseApiRow>>>('/premiumCourse/page', {
      params: {
        keyword: searchForm.keyword || undefined,
        category: searchForm.category || undefined,
        status: searchForm.status || undefined,
        page: currentPage.value,
        pageSize: pageSize.value
      }
    })

    if (data.code !== 1) {
      ElMessage.error(data.msg || '获取课程列表失败')
      return
    }

    total.value = data.data.total
    tableData.value = (data.data.records || []).map(normalizeCourse)
  } catch {
    ElMessage.error('获取课程列表失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const resetSearch = async () => {
  searchForm.keyword = ''
  searchForm.category = ''
  searchForm.status = ''
  currentPage.value = 1
  await fetchCourses()
}

const clearFileState = () => {
  coverFile.value = null
  videoFile.value = null
  coverPreview.value = ''
  videoName.value = ''
  if (coverInputRef.value) coverInputRef.value.value = ''
  if (videoInputRef.value) videoInputRef.value.value = ''
}

const openCreate = () => {
  router.push('/projects/courses/create')
}

const openEdit = (row: CourseRow) => {
  router.push(`/projects/courses/edit/${row.id}`)
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

      const isCreate = dialogMode.value === 'create'
      const { data } = isCreate
        ? await request.post<ApiResult<null>>('/premiumCourse/save', payload)
        : await request.put<ApiResult<null>>('/premiumCourse/update', payload)

      if (data.code !== 1) {
        ElMessage.error(data.msg || (isCreate ? '新增课程失败' : '更新课程失败'))
        return
      }

      ElMessage.success(isCreate ? '新增成功' : '更新成功')
      dialogVisible.value = false
      clearFileState()
      await fetchCourses()
    } catch {
      ElMessage.error(dialogMode.value === 'create' ? '新增失败，请稍后再试' : '更新失败，请稍后再试')
    } finally {
      submitting.value = false
    }
  })
}

const handleApprove = async (row: CourseRow) => {
  try {
    const { data } = await request.put<ApiResult<null>>('/premiumCourse/approve', null, {
      params: { id: row.id }
    })
    if (data.code !== 1) {
      ElMessage.error(data.msg || '审核通过失败')
      return
    }
    ElMessage.success('已审核通过')
    await fetchCourses()
  } catch {
    ElMessage.error('审核通过失败，请稍后再试')
  }
}

const handleDelete = async (row: CourseRow) => {
  try {
    await ElMessageBox.confirm(`确认删除课程「${row.title}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      customClass: 'sf-confirm-box sf-confirm-box--danger'
    })

    const { data } = await request.delete<ApiResult<null>>('/premiumCourse/delete', {
      params: { id: row.id }
    })

    if (data.code !== 1) {
      ElMessage.error(data.msg || '删除课程失败')
      return
    }

    ElMessage.success('删除成功')
    await fetchCourses()
  } catch {
    // cancelled
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await fetchCourses()
}

const handleCurrentChange = async (page: number) => {
  currentPage.value = page
  await fetchCourses()
}

const handleSizeChange = async (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  await fetchCourses()
}

watch(
  () => [searchForm.keyword, searchForm.category, searchForm.status],
  () => {
    void handleSearch()
  }
)

onMounted(() => {
  void fetchCourses()
})
</script>

<template>
  <div class="admin-page">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form" label-position="left">
        <el-form-item label="课程标题">
          <el-input v-model="searchForm.keyword" placeholder="请输入标题关键词" clearable>
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="课程分类">
          <el-select v-model="searchForm.category" placeholder="请选择分类" clearable filterable class="category-select">
            <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable class="status-select">
            <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="sf-btn" @click="handleSearch">查询</el-button>
          <el-button class="sf-btn is-plain" :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" class="sf-btn" :icon="Plus" @click="openCreate">新增课程</el-button>
        </div>
        <div class="toolbar-right">共 {{ total }} 条课程</div>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column label="封面" width="110">
          <template #default="{ row }">
            <el-avatar shape="square" :size="64" :src="row.coverUrl">{{ row.title.slice(0, 1) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="openDetail(row.id)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="teacherName" label="讲师" width="120" />
        <el-table-column prop="duration" label="时长" width="100" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column prop="clickCount" label="点击" width="90" />
        <el-table-column prop="updateTime" label="更新时间" min-width="170">
          <template #default="{ row }">{{ row.updateTime?.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tooltip :content="row.status === '待审核' ? '点击审核通过' : '已通过'" placement="top" :show-after="150">
              <el-button
                text
                class="status-button"
                :disabled="row.status !== '待审核'"
                @click="row.status === '待审核' && handleApprove(row)"
              >
                <el-icon :size="24" :class="row.status === '待审核' ? 'status-icon status-icon--pending' : 'status-icon status-icon--approved'">
                  <UnderReviewIcon v-if="row.status === '待审核'" />
                  <ApproveIcon v-else />
                </el-icon>
              </el-button>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="96" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-tooltip content="编辑" placement="top" :show-after="150">
                <el-button text type="primary" class="action-btn" @click="openEdit(row)">
                  <el-icon size="18px"><Editor3Icon /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="删除" placement="top" :show-after="150">
                <el-button text type="danger" class="action-btn" @click="handleDelete(row)">
                  <el-icon size="18px"><Delete3Icon /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
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

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增课程' : '编辑课程'" width="820px">
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
        <el-button class="sf-btn is-plain" @click="dialogVisible = false">取消</el-button>
        <el-button class="sf-btn" type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-page { display: flex; flex-direction: column; gap: 12px; }
.search-form { display: flex; flex-wrap: wrap; gap: 6px 12px; }
.category-select, .status-select { width: 220px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.toolbar-right { color: #909399; }
.table-footer { margin-top: 16px; display: flex; justify-content: flex-end; }
.action-row { display: flex; align-items: center; justify-content: center; gap: 4px; flex-wrap: nowrap; }
.status-button { min-width: auto; padding: 0; }
.action-btn { min-width: 28px; width: 28px; height: 28px; padding: 0; }
.status-icon { vertical-align: middle; }
.status-icon--pending { color: #e6a23c; }
.status-icon--approved { color: #67c23a; }
.w-100 { width: 100%; }
.upload-box { width: 100%; height: 180px; border: 2px dashed #dcdfe6; border-radius: 12px; display: grid; place-items: center; overflow: hidden; background: #fafafa; cursor: pointer; }
.upload-box--video { padding: 16px; }
.preview-img { width: 100%; height: 100%; object-fit: cover; }
.upload-placeholder { color: #909399; }
.upload-file-name { color: #303133; word-break: break-all; padding: 0 16px; text-align: center; }
.hidden-input { display: none; }

:deep(.el-table),
:deep(.el-table__inner-wrapper),
:deep(.el-table__body-wrapper),
:deep(.el-table__header-wrapper),
:deep(.el-table td),
:deep(.el-table th.is-leaf) {
  border: none !important;
}

:deep(.el-table::before),
:deep(.el-table__inner-wrapper::before),
:deep(.el-table__border-left-patch) {
  display: none !important;
}

:deep(.el-table__row > td) {
  border-bottom: 1px solid #ebeef5 !important;
  padding-top: 18px !important;
  padding-bottom: 18px !important;
}

:deep(.el-table .cell) {
  line-height: 1.6;
  font-size: 15px;
}
:deep(.el-table__row) {
  height: 84px;
}
</style>
