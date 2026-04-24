<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ApproveIcon, Delete3Icon, Editor3Icon, UnderReviewIcon } from '@/components/icons'

interface NewsRow {
  id: number
  title: string
  category: string
  summary: string
  source: string
  status: string
  createTime: string
  updateTime: string
  clickCount: number
}

interface NewsApiRow {
  id: number
  title: string
  category: string
  summary: string
  source: string
  status: string
  createTime: string
  updateTime: string
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

interface NewsForm {
  id?: number
  title: string
  category: string
  summary: string
  source: string
  status: string
  clickCount: number | ''
  content: string
}

const router = useRouter()
const loading = ref(false)
const tableData = ref<NewsRow[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const submitting = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')

const searchForm = reactive({
  keyword: '',
  category: '',
  status: ''
})

const formRef = ref()
const formModel = reactive<NewsForm>({
  title: '',
  category: '',
  summary: '',
  source: '',
  status: '待审核',
  clickCount: 0,
  content: ''
})

const categoryOptions = ['研学动态', '政策解读', '案例分享', '活动通知', '专家观点']
const statusOptions = ['已发布', '待审核', '已下架']

const rules = {
  title: [{ required: true, message: '请输入资讯标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择资讯分类', trigger: 'change' }],
  summary: [{ required: true, message: '请输入资讯摘要', trigger: 'blur' }],
  content: [{ required: true, message: '请输入资讯内容', trigger: 'blur' }],
  status: [{ required: true, message: '请选择资讯状态', trigger: 'change' }]
}

const normalizeNews = (item: NewsApiRow): NewsRow => ({
  ...item,
  summary: item.summary || '',
  source: item.source || '研学资讯',
  status: item.status || '已发布',
  clickCount: item.clickCount || 0
})

const openDetail = (row: NewsRow) => {
  const prefix = router.currentRoute.value.path.startsWith('/student') ? '/student' : ''
  const type = row.category === '政策解读' ? 'gov' : row.category === '专家观点' ? 'expert' : 'study'
  router.push(`${prefix}/projects/news/detail/${type}/${row.id}`)
}

const fetchNews = async () => {
  loading.value = true
  try {
    const { data } = await request.get<ApiResult<PageResult<NewsApiRow>>>('/researchNews/page', {
      params: {
        keyword: searchForm.keyword || undefined,
        category: searchForm.category || undefined,
        status: searchForm.status || undefined,
        page: currentPage.value,
        pageSize: pageSize.value
      }
    })

    if (data.code !== 1) {
      ElMessage.error(data.msg || '获取资讯列表失败')
      return
    }

    total.value = data.data.total
    tableData.value = (data.data.records || []).map(normalizeNews)
  } catch {
    ElMessage.error('获取资讯列表失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const resetSearch = async () => {
  searchForm.keyword = ''
  searchForm.category = ''
  searchForm.status = ''
  currentPage.value = 1
  await fetchNews()
}

const openCreate = () => {
  router.push('/projects/news/create')
}

const openEdit = (row: NewsRow) => {
  router.push(`/projects/news/edit/${row.id}`)
}

const submitForm = async () => {
  const form = formRef.value
  if (!form) return

  await form.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        ...formModel,
        status: '待审核',
        clickCount: Number(formModel.clickCount || 0)
      }

      const isCreate = dialogMode.value === 'create'
      const { data } = isCreate
        ? await request.post<ApiResult<null>>('/researchNews/save', payload)
        : await request.put<ApiResult<null>>('/researchNews/update', payload)

      if (data.code !== 1) {
        ElMessage.error(data.msg || (isCreate ? '新增资讯失败' : '更新资讯失败'))
        return
      }

      ElMessage.success(isCreate ? '新增成功' : '更新成功')
      dialogVisible.value = false
      await fetchNews()
    } catch {
      ElMessage.error(dialogMode.value === 'create' ? '新增失败，请稍后再试' : '更新失败，请稍后再试')
    } finally {
      submitting.value = false
    }
  })
}

const handleApprove = async (row: NewsRow) => {
  try {
    const { data } = await request.put<ApiResult<null>>('/researchNews/approve', null, {
      params: { id: row.id }
    })
    if (data.code !== 1) {
      ElMessage.error(data.msg || '审核通过失败')
      return
    }
    ElMessage.success('已审核通过')
    await fetchNews()
  } catch {
    ElMessage.error('审核通过失败，请稍后再试')
  }
}

const handleDelete = async (row: NewsRow) => {
  try {
    await ElMessageBox.confirm(`确认删除资讯「${row.title}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      customClass: 'sf-confirm-box sf-confirm-box--danger'
    })

    const { data } = await request.delete<ApiResult<null>>('/researchNews/delete', {
      params: { id: row.id }
    })

    if (data.code !== 1) {
      ElMessage.error(data.msg || '删除资讯失败')
      return
    }

    ElMessage.success('删除成功')
    await fetchNews()
  } catch {
    // cancelled
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await fetchNews()
}

const handleCurrentChange = async (page: number) => {
  currentPage.value = page
  await fetchNews()
}

const handleSizeChange = async (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  await fetchNews()
}

watch(
  () => [searchForm.keyword, searchForm.category, searchForm.status],
  () => {
    void handleSearch()
  }
)

onMounted(() => {
  void fetchNews()
})
</script>

<template>
  <div class="admin-page">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form" label-position="left">
        <el-form-item label="资讯标题">
          <el-input v-model="searchForm.keyword" placeholder="请输入标题关键词" clearable>
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="资讯分类">
          <el-select v-model="searchForm.category" placeholder="请选择分类" clearable filterable class="category-select">
            <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="资讯状态">
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
          <el-button type="primary" class="sf-btn" :icon="Plus" @click="openCreate">新增资讯</el-button>
        </div>
        <div class="toolbar-right">共 {{ total }} 条资讯</div>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="title" label="标题" min-width="220">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="openDetail(row)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="130" />
        <el-table-column prop="source" label="来源" width="160" />
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
        <el-table-column prop="clickCount" label="点击" width="90" />
        <el-table-column prop="updateTime" label="更新时间" min-width="170">
          <template #default="{ row }">{{ row.updateTime?.replace('T', ' ') }}</template>
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
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增资讯' : '编辑资讯'" width="760px">
      <el-form ref="formRef" :model="formModel" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="资讯标题" prop="title">
              <el-input v-model="formModel.title" placeholder="请输入资讯标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资讯分类" prop="category">
              <el-select v-model="formModel.category" class="w-100" filterable allow-create default-first-option placeholder="请选择或输入分类">
                <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源">
              <el-input v-model="formModel.source" placeholder="请输入来源" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资讯状态" prop="status">
              <el-select v-model="formModel.status" class="w-100">
                <el-option v-for="item in statusOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="点击数">
              <el-input-number v-model="formModel.clickCount" :min="0" class="w-100" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="资讯摘要" prop="summary">
              <el-input v-model="formModel.summary" type="textarea" :rows="3" placeholder="请输入资讯摘要" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="资讯内容" prop="content">
              <el-input v-model="formModel.content" type="textarea" :rows="6" placeholder="请输入资讯内容" />
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
.status-icon--pending { color: #f59e0b; filter: drop-shadow(0 0 3px rgba(245, 158, 11, 0.35)); }
.status-icon--approved { color: #22c55e; }
.w-100 { width: 100%; }

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
