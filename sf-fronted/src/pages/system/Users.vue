<script setup lang="ts">
import { ref } from 'vue'

interface UserRow {
  id: number
  avatar: string
  name: string
  gender: '男' | '女'
  phone: string
  status: '正常' | '禁用' | '未激活'
  createTime: string
}

const searchForm = ref({
  keyword: '',
  gender: '',
  status: ''
})

const tableData = ref<UserRow[]>([
  {
    id: 1,
    avatar: 'https://via.placeholder.com/40',
    name: 'Scott',
    gender: '男',
    phone: '18300000001',
    status: '正常',
    createTime: '2008-11-07 07:37:12'
  },
  {
    id: 2,
    avatar: 'https://via.placeholder.com/40',
    name: 'Kevin',
    gender: '男',
    phone: '18300000002',
    status: '禁用',
    createTime: '2011-08-07 08:36:33'
  }
])

const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(200)

const handleSearch = () => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 500)
}

const handleReset = () => {
  searchForm.value = {
    keyword: '',
    gender: '',
    status: ''
  }
  handleSearch()
}

const handleAdd = () => {}

const handleExport = () => {}

const handleEdit = (row: UserRow) => {
  console.log('edit', row)
}

const handleDelete = (row: UserRow) => {
  console.log('delete', row)
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  handleSearch()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  handleSearch()
}
</script>

<template>
  <div class="user-page">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" label-width="80px">
        <el-form-item label="用户名称">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入用户名 / 手机号"
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
            <el-option label="未激活" value="未激活" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" class="btn-post">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <div>
          <el-button type="primary" @click="handleAdd" plain class="btn-post">新建用户</el-button>
          <el-button @click="handleExport" class="btn-post" plain>导出</el-button>
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
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="序号" width="80" />
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :src="row.avatar" size="small" />
              <div class="user-info">
                <div class="user-name">{{ row.name }}</div>
                <div class="user-phone">{{ row.phone }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="150" />
        <el-table-column prop="phone" label="手机号" width="250" />
        <el-table-column prop="status" label="状态" width="250">
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
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
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
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
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

.user-phone {
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
</style>

