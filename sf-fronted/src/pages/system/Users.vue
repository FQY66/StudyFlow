<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, DeleteFilled, CircleCloseFilled, CircleCheckFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface UserRow {
  id: number
  username: string
  avatar: string
  name: string
  gender: '男' | '女'
  phone: string
  email: string
  status: '正常' | '禁用' | '未激活'
  createTime: string
  role: '管理员' | '教师' | '学生'
}

interface UserApiRow {
  id: number
  username: string
  name: string
  sex: string
  phone: string
  email: string
  status: number
  createTime: string
  role: string
  avatar: string
}

interface UserUpdateDTO {
  username: string
  name?: string
  sex?: '男' | '女'
  phone?: string
  email?: string
  status?: 0 | 1
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

const searchForm = ref({
  keyword: '',
  gender: '',
  status: ''
})

const tableData = ref<UserRow[]>([])
const selectedRows = ref<UserRow[]>([])

const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(5)
const total = ref(0)

const editDialogVisible = ref(false)
const editLoading = ref(false)
const editForm = ref<UserUpdateDTO>({
  username: '',
  name: '',
  sex: '男',
  phone: '',
  email: '',
  status: 1
})

const mapRole = (role: string): UserRow['role'] => {
  if (role === 'ADMIN' || role === '管理员') return '管理员'
  if (role === 'TEACHER' || role === '教师') return '教师'
  return '学生'
}

const mapStatus = (status: number): UserRow['status'] => {
  if (status === 1) return '正常'
  if (status === 0) return '禁用'
  return '未激活'
}

const toStatusNumber = (status: UserRow['status']): 0 | 1 => (status === '正常' ? 1 : 0)

const formatDateTime = (time: string) => {
  if (!time) return ''
  return time.replace('T', ' ')
}

const formatAvatar = (avatar: string) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return `${import.meta.env.VITE_API_BASE_URL || ''}/${avatar.replace(/^\/+/, '')}`
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const statusMap: Record<string, string> = {
      正常: '1',
      禁用: '0',
      未激活: '2'
    }

    const { data } = await request.get<ApiResult<PageResult<UserApiRow>>>('/admin/page', {
      params: {
        name: searchForm.value.keyword || undefined,
        sex: searchForm.value.gender || undefined,
        status: searchForm.value.status ? statusMap[searchForm.value.status] : undefined,
        page: currentPage.value,
        pageSize: pageSize.value
      }
    })

    if (data.code !== 1) {
      ElMessage.error(data.msg || '获取用户列表失败')
      return
    }

    total.value = data.data.total
    tableData.value = data.data.records.map((item) => ({
      id: item.id,
      username: item.username,
      avatar: formatAvatar(item.avatar),
      name: item.name || item.username,
      gender: (item.sex === '女' ? '女' : '男') as UserRow['gender'],
      phone: item.phone,
      email: item.email || '',
      status: mapStatus(item.status),
      createTime: formatDateTime(item.createTime),
      role: mapRole(item.role)
    }))
  } catch {
    ElMessage.error('获取用户列表失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await fetchUsers()
}

const handleReset = async () => {
  searchForm.value = {
    keyword: '',
    gender: '',
    status: ''
  }
  currentPage.value = 1
  await fetchUsers()
}

const handleExport = () => {}

const handleEdit = (row: UserRow) => {
  editForm.value = {
    username: row.username,
    name: row.name,
    sex: row.gender,
    phone: row.phone,
    email: row.email,
    status: toStatusNumber(row.status)
  }
  editDialogVisible.value = true
}

const submitEdit = async () => {
  if (!editForm.value.username) {
    ElMessage.error('缺少用户名，无法更新')
    return
  }

  editLoading.value = true
  try {
    const { data } = await request.put<ApiResult<null>>('/admin/updateUser', editForm.value)
    if (data.code !== 1) {
      ElMessage.error(data.msg || '更新用户失败')
      return
    }
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    await fetchUsers()
  } catch {
    ElMessage.error('更新用户失败，请稍后再试')
  } finally {
    editLoading.value = false
  }
}

const handleDeleteById = async (id: number) => {
  const { data } = await request.delete<ApiResult<null>>('/admin/deleteUser', {
    params: { id }
  })
  return data
}

const handleDelete = async (row: UserRow) => {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.name}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })

    const data = await handleDeleteById(row.id)
    if (data.code !== 1) {
      ElMessage.error(data.msg || '删除用户失败')
      return
    }

    ElMessage.success('删除成功')
    await fetchUsers()
  } catch {
    // 用户取消或接口异常
  }
}

const handleToggleStatus = async (row: UserRow) => {
  const targetStatus: 0 | 1 = row.status === '正常' ? 0 : 1
  const actionText = targetStatus === 1 ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(`确认${actionText}用户「${row.name}」吗？`, `${actionText}确认`, {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })

    const payload: UserUpdateDTO = {
      username: row.username,
      status: targetStatus
    }
    const { data } = await request.put<ApiResult<null>>('/admin/updateUser', payload)

    if (data.code !== 1) {
      ElMessage.error(data.msg || `${actionText}失败`)
      return
    }

    ElMessage.success(`${actionText}成功`)
    await fetchUsers()
  } catch {
    // 用户取消或接口异常
  }
}

const handleSelectionChange = (rows: UserRow[]) => {
  selectedRows.value = rows
}

const handleBatchDisable = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择要禁用的用户')
    return
  }

  try {
    await ElMessageBox.confirm(`确认禁用选中的 ${selectedRows.value.length} 个账号吗？`, '批量禁用确认', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })

    let successCount = 0
    for (const row of selectedRows.value) {
      const { data } = await request.put<ApiResult<null>>('/admin/updateUser', {
        username: row.username,
        status: 0
      } as UserUpdateDTO)
      if (data.code === 1) {
        successCount++
      }
    }

    ElMessage.success(`批量禁用完成，成功 ${successCount}/${selectedRows.value.length}`)
    await fetchUsers()
    selectedRows.value = []
  } catch {
    // 用户取消或接口异常
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择要删除的用户')
    return
  }

  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 个用户吗？此操作不可恢复`, '批量删除确认', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })

    let successCount = 0
    for (const row of selectedRows.value) {
      const data = await handleDeleteById(row.id)
      if (data.code === 1) {
        successCount++
      }
    }

    ElMessage.success(`批量删除完成，成功 ${successCount}/${selectedRows.value.length}`)
    await fetchUsers()
    selectedRows.value = []
  } catch {
    // 用户取消或接口异常
  }
}

const handlePageChange = async (page: number) => {
  currentPage.value = page
  await fetchUsers()
}

const handleSizeChange = async (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  await fetchUsers()
}

watch(
  () => [searchForm.value.keyword, searchForm.value.gender, searchForm.value.status],
  async () => {
    currentPage.value = 1
    await fetchUsers()
  }
)

onMounted(() => {
  fetchUsers()
})
</script>

<template>
  <div class="user-page">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" label-width="80px">
        <el-form-item label="用户名称">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入用户名"
            clearable
          />
        </el-form-item>
        <el-form-item label="性别">
          <el-select
            v-model="searchForm.gender"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option label="正常" value="正常" />
            <el-option label="禁用" value="禁用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" plain class="btn-post">查询</el-button>
          <el-button  @click="handleReset" plain class="btn-common">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <div class="toolbar-left">
          <!-- <el-button type="primary" @click="handleAdd" plain class="btn-post">新建用户</el-button> -->
          <el-button @click="handleExport" class="btn-common" plain>导出</el-button>
          <el-button
          type="danger"
          plain
            class="btn-post"
            :disabled="!selectedRows.length"
            @click="handleBatchDisable"
          >
            批量禁用
          </el-button>
          <el-button
          type="danger"
          plain
            class="btn-post"
            :disabled="!selectedRows.length"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </div>
        <div class="toolbar-right">
          <span>共 {{ total }} 条数据</span>
        </div>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        size="large"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :src="row.avatar" size="large" />
              <div class="user-info">
                <div class="user-name">{{ row.name }}</div>
                <div class="user-role">{{ row.role }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="120" />
        <el-table-column prop="phone" label="手机号" width="180" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag
              :type="
                row.status === '正常'
                  ? 'success'
                  : row.status === '禁用'
                    ? 'danger'
                    : 'info'
              "
              disable-transitions
            >
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建日期"
          min-width="180"
        />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="编辑" placement="top">
              <el-button
                class="action-btn"
                type="primary"
                link
                size="small"
                @click="handleEdit(row)"
              >
                <el-icon size="20">
                  <Edit />
                </el-icon>
              </el-button>
            </el-tooltip>

            <el-tooltip :content="row.status === '正常' ? '禁用' : '启用'" placement="top">
              <el-button
                class="action-btn"
                :type="row.status === '正常' ? 'warning' : 'success'"
                link
                size="small"
                @click="handleToggleStatus(row)"
              >
                <el-icon size="20">
                  <CircleCloseFilled v-if="row.status === '正常'" />
                  <CircleCheckFilled v-else />
                </el-icon>
              </el-button>
            </el-tooltip>

            <el-tooltip content="删除" placement="top">
              <el-button
                class="action-btn"
                type="danger"
                link
                size="small"
                @click="handleDelete(row)"
              >
                <el-icon size="20">
                  <DeleteFilled />
                </el-icon>
              </el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="editDialogVisible"
      width="640px"
      class="user-edit-dialog"
      :show-close="true"
    >
      <div class="edit-panel">
        <h2 class="edit-title">编辑用户信息</h2>
        <p class="edit-subtitle">请完善用户资料，保持信息准确</p>

        <el-form :model="editForm" label-position="top" class="edit-form">
          <el-form-item label="用户名">
            <el-input v-model="editForm.username" disabled class="edit-input" />
          </el-form-item>

          <div class="edit-grid">
            <el-form-item label="姓名">
              <el-input v-model="editForm.name" class="edit-input" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="editForm.sex" class="gender-group">
                <el-radio label="男">男</el-radio>
                <el-radio label="女">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </div>

          <div class="edit-grid">
            <el-form-item label="手机号">
              <el-input v-model="editForm.phone" class="edit-input" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="editForm.email" class="edit-input" placeholder="请输入邮箱" />
            </el-form-item>
          </div>
        </el-form>

        <div class="edit-footer">
          <el-button class="cancel-btn" @click="editDialogVisible = false">取消</el-button>
          <el-button class="save-btn" :loading="editLoading" @click="submitEdit">保存</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-card,
.toolbar-card,
.table-card {
  background-color: #ffffff;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.toolbar-right {
  color: #909399;
  font-size: 13px;
}

.user-cell {
  display: flex;
  align-items: center;
}

.user-cell .el-avatar {
  margin-right: 10px;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-weight: 500;
}

.user-role {
  font-size: 12px;
  color: #909399;
}

.table-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.action-btn + .action-btn {
  margin-left: 8px;
}

.btn-post {
  border-radius: 12px !important;
}
.btn-common{
  border-radius: 12px !important;
}
.btn-common:hover,
.btn-common.is-plain:hover {
  background-color: rgba(85, 45, 188, 0.409) !important;
  border-color: rgba(34, 36, 37, 0.2) !important;
}
.btn-post:hover,
.btn-post.is-plain:hover {
  background-color: rgba(85, 45, 188, 0.409) !important;
  border-color: rgba(34, 36, 37, 0.2) !important;
}

.btn-post:active,
.btn-post.is-plain:active {
  background-color: rgba(10, 118, 226, 0.2) !important;
  border-color: rgba(64, 158, 255, 0.25) !important;
}

.btn-post.is-disabled,
.btn-post.is-disabled:hover,
.btn-post.is-disabled:focus {
  color: #d46b6b !important;
  border-color: #e6b8b8 !important;
  background-color: #fff2f2 !important;
}

:deep(.user-edit-dialog .el-dialog) {
  border-radius: 24px;
  padding: 0;
  overflow: hidden;
}

:deep(.user-edit-dialog .el-dialog__header) {
  display: none;
}

:deep(.user-edit-dialog .el-dialog__body) {
  padding: 0;
}

.edit-panel {
  background: linear-gradient(180deg, #f8f9ff 0%, #ffffff 100%);
  padding: 28px;
}

.edit-title {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
  font-weight: 800;
  color: #1a2554;
}

.edit-subtitle {
  margin: 8px 0 20px;
  color: #6b7280;
  font-size: 14px;
}

.edit-form :deep(.el-form-item__label) {
  font-size: 14px;
  color: #1f2a56;
  font-weight: 600;
  padding-bottom: 8px;
}

.edit-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.edit-input :deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 14px;
  box-shadow: 0 0 0 1px #d5dbec inset;
  background-color: #ffffff;
}

.edit-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #7d8ae8 inset;
}

.gender-group {
  min-height: 46px;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid #d5dbec;
  display: flex;
  align-items: center;
  gap: 18px;
  background-color: #ffffff;
}

.edit-footer {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cancel-btn {
  height: 44px;
  border-radius: 14px;
  padding: 0 20px;
}

.save-btn {
  height: 50px;
  min-width: 160px;
  border-radius: 16px;
  border: none;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(90deg, #ff845f 0%, #ff6b4a 100%);
}

.save-btn:hover,
.save-btn:focus {
  color: #fff;
  transform: translateY(-1px);
  opacity: 0.95;
}
</style>
