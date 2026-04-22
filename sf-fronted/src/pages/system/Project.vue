<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, ArrowDown } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { Editor2Icon, UsersIcon, Delete2Icon } from '@/components/icons'

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
  pendingSignupCount: number
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
  pendingSignupCount?: number
  projectSignupList?: SignupUser[]
}

interface SignupUser {
  id?: number
  userId?: number
  avatar?: string
  name?: string
  status?: string
  role?: string
  createTime?: string
  userName?: string
  username?: string
  nickname?: string
  realName?: string
  fullName?: string
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

const router = useRouter()
const loading = ref(false)
const tableData = ref<ProjectRow[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(8)
const dialogVisible = ref(false)
const categoryOptions = ref<string[]>([])
const filteredTableData = computed(() => {
  const themeKeyword = searchForm.theme.trim().toLowerCase()
  const categoryKeyword = searchForm.category.trim().toLowerCase()
  const pendingFilter = searchForm.pendingFilter

  return tableData.value
    .filter((item) => {
      const themeMatch = !themeKeyword || item.theme.toLowerCase().includes(themeKeyword)
      const categoryMatch = !categoryKeyword || item.category.toLowerCase().includes(categoryKeyword)
      const statusPendingMatch = pendingFilter !== 'statusPending' || item.status === '待审核'
      const usersPendingMatch = pendingFilter !== 'hasPendingUsers' || item.pendingSignupCount > 0
      const publishedMatch = pendingFilter !== 'published' || item.status === '已发布'
      return themeMatch && categoryMatch && statusPendingMatch && usersPendingMatch && publishedMatch
    })
    .sort((a, b) => {
      const pendingDiff = (b.pendingSignupCount || 0) - (a.pendingSignupCount || 0)
      if (pendingDiff !== 0) return pendingDiff
      return new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime()
    })
})
const signupDialogVisible = ref(false)
const signupUsers = ref<SignupUser[]>([])
const signupProjectTheme = ref('')
const signupProjectId = ref<number | null>(null)
const submitting = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const searchForm = reactive({
  theme: '',
  category: '',
  pendingFilter: 'all'
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

const normalizeProject = (item: ProjectApiRow): ProjectRow => {
  const pendingSignupCount = item.pendingSignupCount ?? (item.projectSignupList || []).filter((user) => user.status !== '已通过').length

  return {
    ...item,
    coverUrl: formatAvatar(item.coverPath),
    introduction: item.introduction || '',
    content: item.content || '',
    category: item.category || '',
    status: item.status || '审核中',
    likeCount: item.likeCount || 0,
    clickCount: item.clickCount || 0,
    pendingSignupCount
  }
}

const fetchProjects = async () => {
  loading.value = true
  try {
    const { data } = await request.get<ApiResult<PageResult<ProjectApiRow>>>('/project/page', {
      params: {
        theme: searchForm.theme || undefined,
        category: searchForm.category || undefined,
        pendingFilter: searchForm.pendingFilter === 'all' ? undefined : searchForm.pendingFilter,
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
  router.push('/projects/new')
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
      cancelButtonText: '取消',
      customClass: 'sf-confirm-box sf-confirm-box--danger'
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

      const isCreate = dialogMode.value === 'create'
      const url = isCreate ? '/project/save' : '/project/update'
      const { data } = isCreate
        ? await request.post<ApiResult<null>>(url, payload)
        : await request.put<ApiResult<null>>(url, payload)

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

const handleAutoSearch = async () => {
  currentPage.value = 1
  await fetchProjects()
}

watch(
  () => [searchForm.theme, searchForm.category, searchForm.pendingFilter],
  () => {
    void handleAutoSearch()
  }
)

const resetSearch = async () => {
  searchForm.theme = ''
  searchForm.category = ''
  searchForm.pendingFilter = 'all'
  currentPage.value = 1
  await fetchProjects()
}

const goToDetail = (row: ProjectRow) => {
  router.push(`/projects/detail/${row.id}`)
}

const openSignupUsers = async (row: ProjectRow) => {
  try {
    const { data } = await request.get<ApiResult<any>>('/project/detail', {
      params: { id: row.id }
    })

    if (data.code !== 1) {
      ElMessage.error(data.msg || '获取报名用户失败')
      return
    }

    const detail = data.data || {}
    signupUsers.value = (detail.projectSignupList || []).slice().sort((a: SignupUser, b: SignupUser) => {
      const aPending = a.status !== '已通过'
      const bPending = b.status !== '已通过'
      if (aPending === bPending) {
        return String(b.createTime || '').localeCompare(String(a.createTime || ''))
      }
      return Number(bPending) - Number(aPending)
    })
    signupProjectTheme.value = row.theme
    signupProjectId.value = row.id
    signupDialogVisible.value = true
  } catch {
    ElMessage.error('获取报名用户失败，请稍后再试')
  }
}

const getUserDisplayName = (user: any) => {
  return user.name || user.realName || user.nickname || user.userName || user.username || user.trueName || user.fullName || '未命名用户'
}

const openUserDetail = (user: SignupUser) => {
  router.push({
    path: '/system/users/detail',
    query: {
      id: user.id || user.userId || '',
      username: user.userName || user.username || '',
      name: getUserDisplayName(user),
      avatar: user.avatar || '',
      gender: (user as any).gender || (user as any).sex || '',
      phone: (user as any).phone || '',
      email: (user as any).email || '',
      role: user.role || '学生',
      status: user.status || '',
      createTime: user.createTime || '',
      signupTime: (user as any).signupTime || user.createTime || '',
      projectTheme: signupProjectTheme.value
    }
  })
}

const handleSignupAudit = async (user: SignupUser, approved: boolean) => {
  if (!signupProjectId.value) return
  const actionLabel = approved ? '通过' : '不通过'
  const confirmLabel = approved ? '通过' : '不通过'
  const requestUrl = approved ? '/project/approveSignup' : '/project/cancelSignup'

  try {
    await ElMessageBox.confirm(`确认将「${getUserDisplayName(user)}」的报名审核${actionLabel}吗？`, '审核确认', {
      type: 'warning',
      confirmButtonText: confirmLabel,
      cancelButtonText: '取消'
    })

    const requestConfig = {
      params: { projectId: signupProjectId.value, userId: user.userId || user.id }
    }
    const { data } = await (approved
      ? request.post<ApiResult<null>>(requestUrl, null, requestConfig)
      : request.delete<ApiResult<null>>(requestUrl, requestConfig))

    if (data.code !== 1) {
      ElMessage.error(data.msg || `审核${actionLabel}失败`)
      return
    }

    ElMessage.success(`审核${actionLabel}成功`)
    await openSignupUsers({ id: signupProjectId.value, theme: signupProjectTheme.value } as ProjectRow)
  } catch {
    // cancelled
  }
}

const handleSignupAuditCommand = (user: SignupUser, command: 'approve' | 'reject') => {
  void handleSignupAudit(user, command === 'approve')
}

const handleSearch = async () => {
  await handleAutoSearch()
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
  fetchCategoryOptions()
  fetchProjects()
})
</script>

<template>
  <div class="project-page">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form" label-position="left">
        <el-form-item label="项目主题">
          <el-input v-model="searchForm.theme" placeholder="请输入主题关键词" clearable @change="handleAutoSearch" @clear="handleAutoSearch" />
        </el-form-item>
        <el-form-item label="项目类别">
          <el-select v-model="searchForm.category" placeholder="请选择项目类别" clearable filterable class="category-select" @change="handleAutoSearch" @clear="handleAutoSearch">
            <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目状态">
          <el-select v-model="searchForm.pendingFilter" placeholder="请选择" clearable class="pending-select" @change="handleAutoSearch" @clear="handleAutoSearch">
            <el-option value="all" label="全部显示" />
            <el-option value="statusPending" label="只显示待审核" />
            <el-option value="published" label="只显示已发布" />
            <el-option value="hasPendingUsers" label="只显示有待审核用户" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" class="sf-btn" :icon="Plus" @click="openCreate">新增项目</el-button>
        </div>
        <div class="toolbar-right">{{ tableSummary }}</div>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="filteredTableData" border>
        <el-table-column label="封面" width="110">
          <template #default="{ row }">
            <el-avatar shape="square" :size="64" :src="row.coverUrl">{{ row.theme.slice(0, 1) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column label="主题" min-width="180">
          <template #default="{ row }">
            <el-button link type="primary" class="theme-link" @click="goToDetail(row)">{{ row.theme }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="类别" width="120" />
        <el-table-column prop="capacity" label="上限" width="100" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="likeCount" label="点赞" width="90" />
        <el-table-column prop="clickCount" label="点击" width="90" />
        <el-table-column prop="updateTime" label="更新时间" min-width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-tooltip content="编辑项目" placement="top" show-after="300">
                <el-button text type="primary" class="action-btn" @click="openEdit(row)">
                  <el-icon size="25">
                    <Editor2Icon />
                  </el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip :content="row.pendingSignupCount > 0 ? `查看报名用户（${row.pendingSignupCount}个待审核）` : '查看报名用户'" placement="top" show-after="300">
                <el-badge :value="row.pendingSignupCount" :hidden="row.pendingSignupCount === 0" type="danger" :offset="[2, 8]">
                  <el-button text type="success" class="action-btn" @click="openSignupUsers(row)">
                    <el-icon size="25">
                      <UsersIcon />
                    </el-icon>
                  </el-button>
                </el-badge>
              </el-tooltip>
              <el-tooltip content="删除项目" placement="top" show-after="300">
                <el-button text type="danger" class="action-btn" @click="handleDelete(row)">
                  <el-icon size="25">
                    <Delete2Icon />
                  </el-icon>
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

    <el-dialog v-model="signupDialogVisible" :title="`${signupProjectTheme} - 报名用户`" width="820px">
      <el-empty v-if="!signupUsers.length" description="暂无用户报名" />
      <div v-else class="signup-list">
        <el-card v-for="user in signupUsers" :key="user.id || user.userId || user.userName || user.nickname || user.realName" class="signup-user-card" shadow="never">
          <div class="signup-user-row clickable" @click="openUserDetail(user)">
            <el-avatar :size="44" :src="formatAvatar(user.avatar || '')">{{ getUserDisplayName(user).slice(0, 1) }}</el-avatar>
            <div class="signup-user-info">
              <div class="signup-user-name">
                <span>{{ getUserDisplayName(user) }}</span>
                <el-tag size="small" type="warning" effect="light" class="signup-status-tag">{{ user.status || '待审核' }}</el-tag>
              </div>
              <div class="signup-user-meta">
                <span v-if="user.userName">账号：{{ user.userName }}</span>
                <span v-if="user.createTime">报名时间：{{ user.createTime }}</span>
              </div>
            </div>
          </div>
          <div class="signup-user-actions">
            <el-dropdown trigger="click" @command="(command: 'approve' | 'reject') => handleSignupAuditCommand(user, command)">
              <el-button size="small" type="primary" class="sf-btn" @click.stop>
                审核操作
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="approve" :disabled="user.status === '已通过'">通过</el-dropdown-item>
                  <el-dropdown-item command="reject">拒绝</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-card>
      </div>
    </el-dialog>

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
              <el-input v-model="formModel.content" type="textarea" :rows="6" placeholder="请输入项目内容" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button type="primary" class="sf-btn is-plain" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" class="sf-btn" :loading="submitting" @click="submitForm">保存</el-button>
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

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
}

.category-select,
.pending-select {
  width: 240px;
}

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

.toolbar-right {
  color: #909399;
}

.theme-link {
  padding: 0;
  font-weight: 600;
}

.clickable {
  cursor: pointer;
}

.table-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.signup-list {
  display: grid;
  gap: 12px;
}

.signup-user-card {
  border-radius: 12px;
}

.signup-user-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-row {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.action-btn {
  margin: 0;
  padding: 0;
  font-size: 18px;
  min-height: 34px;
  line-height: 1;
}

.action-btn :deep(.el-icon) {
  font-size: 18px;
}

.signup-user-row.clickable {
  cursor: pointer;
}

.signup-user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.signup-user-name {
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.signup-status-tag {
  margin-left: 0;
}

.signup-user-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.signup-user-meta {
  color: #909399;
  font-size: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.w-100 {
  width: 100%;
}

:deep(.signup-users-message-box) {
  white-space: pre-line;
}
</style>
