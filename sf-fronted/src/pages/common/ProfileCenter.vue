<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface ProjectItem {
  id: number
  theme?: string
  category?: string
  signupStatus?: string
  signupTime?: string
  createTime?: string
  clickCount?: number
  likeCount?: number
}

interface UserProfile {
  id: number
  username: string
  name: string
  avatar: string
  sex: string
  phone: string
  email: string
  role: string
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const editVisible = ref(false)
const avatarUploading = ref(false)
const participatedProjects = ref<ProjectItem[]>([])

const role = computed(() => sessionStorage.getItem('userRole') || sessionStorage.getItem('role') || localStorage.getItem('userRole') || localStorage.getItem('role') || '')
const isTeacher = computed(() => role.value === '老师')

const profile = reactive<UserProfile>({
  id: Number(sessionStorage.getItem('id') || localStorage.getItem('id') || 0),
  username: sessionStorage.getItem('username') || localStorage.getItem('username') || '',
  name: sessionStorage.getItem('name') || localStorage.getItem('name') || '',
  avatar: sessionStorage.getItem('avatar') || localStorage.getItem('avatar') || '',
  sex: sessionStorage.getItem('sex') || localStorage.getItem('sex') || '男',
  phone: sessionStorage.getItem('phone') || localStorage.getItem('phone') || '',
  email: sessionStorage.getItem('email') || localStorage.getItem('email') || '',
  role: role.value
})

const editForm = reactive<UserProfile>({ ...profile })

const resolveAvatar = (avatar?: string) => {
  if (!avatar) return ''
  if (/^https?:\/\//i.test(avatar) || avatar.startsWith('data:')) return avatar
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return `${base}/${avatar.replace(/^\/+/, '')}`
}

const syncStorage = (data: Partial<UserProfile>) => {
  for (const [key, value] of Object.entries(data)) {
    if (value === undefined || value === null) continue
    sessionStorage.setItem(key, String(value))
    localStorage.setItem(key, String(value))
  }
  sessionStorage.setItem('userRole', profile.role)
  localStorage.setItem('userRole', profile.role)
}

const openEditDialog = () => {
  Object.assign(editForm, profile)
  editVisible.value = true
}

const handleAvatarUpload = async (file: File) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return false
  }

  avatarUploading.value = true
  try {
    const base64 = await new Promise<string>((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(String(reader.result || ''))
      reader.onerror = () => reject(new Error('read failed'))
      reader.readAsDataURL(file)
    })
    editForm.avatar = base64
    ElMessage.success('头像已选择，保存后生效')
  } catch {
    ElMessage.error('头像读取失败')
  } finally {
    avatarUploading.value = false
  }
  return false
}

const loadTeacherProjects = async () => {
  if (!isTeacher.value || !profile.id) {
    participatedProjects.value = []
    return
  }

  try {
    const { data } = await request.get('/project/mySignups', {
      params: { userId: profile.id }
    })
    participatedProjects.value = (data?.data || []) as ProjectItem[]
  } catch {
    participatedProjects.value = []
  }
}

const loadProfile = async () => {
  loading.value = true
  try {
    const username = profile.username || sessionStorage.getItem('username') || localStorage.getItem('username')
    if (username) {
      const { data } = await request.get('/admin/page', {
        params: { page: 1, pageSize: 1, username }
      })
      const record = data?.data?.records?.[0]
      if (record) {
        profile.id = Number(record.id || profile.id || 0)
        profile.username = record.username || profile.username
        profile.name = record.name || profile.name
        profile.avatar = record.avatar || profile.avatar
        profile.sex = record.sex || profile.sex
        profile.phone = record.phone || profile.phone
        profile.email = record.email || profile.email
        profile.role = record.role || profile.role
        syncStorage(profile)
      }
    }
    await loadTeacherProjects()
  } finally {
    loading.value = false
  }
}

const saveProfile = async () => {
  if (!profile.id) {
    ElMessage.warning('未获取到用户ID，无法保存')
    return
  }

  saving.value = true
  try {
    const payload = { ...editForm, id: profile.id }
    const { data } = await request.put('/admin/updateUser', payload)
    if (data?.code && data.code !== 1) {
      ElMessage.error(data?.msg || '保存失败')
      return
    }
    Object.assign(profile, payload)
    syncStorage(payload)
    editVisible.value = false
    ElMessage.success('个人信息已更新')
    await loadTeacherProjects()
  } catch {
    ElMessage.error('保存失败，请稍后再试')
  } finally {
    saving.value = false
  }
}

const goToProject = (id: number) => {
  if (!id) return
  const prefix = route.path.startsWith('/teacher') ? '/teacher' : ''
  router.push(`${prefix}/projects/detail/${id}`)
}

watch(
  () => role.value,
  () => {
    loadTeacherProjects()
  }
)

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <div class="profile-page">
    <el-card class="profile-card" shadow="never" v-loading="loading">
      <div class="header-row">
        <div>
          <div class="page-title">个人中心</div>
          <div class="page-subtitle">可查看并修改当前账号资料</div>
        </div>
        <div class="header-actions">
          <el-button @click="openEditDialog">编辑资料</el-button>
          <el-button link type="primary" @click="router.back()">返回</el-button>
        </div>
      </div>

      <div class="profile-layout">
        <div class="avatar-panel">
          <el-avatar :size="92" :src="resolveAvatar(profile.avatar)">
            {{ (profile.name || profile.username || 'U').slice(0, 1) }}
          </el-avatar>
          <div class="role-badge">{{ profile.role || '未设置角色' }}</div>
          <el-button type="primary" plain @click="openEditDialog">修改个人信息</el-button>
        </div>

        <div class="profile-summary">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="账号">{{ profile.username || '-' }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ profile.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ profile.sex || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ profile.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ profile.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ profile.role || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <section v-if="isTeacher" class="projects-section">
        <div class="section-header">
          <h3>参与的项目</h3>
          <span class="section-tip">{{ participatedProjects.length }} 个项目</span>
        </div>
        <div v-if="participatedProjects.length" class="project-list">
          <div v-for="item in participatedProjects" :key="item.id" class="project-row" @click="goToProject(item.id)">
            <div class="project-main">
              <div class="project-title">{{ item.theme || '未命名项目' }}</div>
              <div class="project-meta">{{ item.category || '未分类' }}</div>
              <div class="project-meta status-line">报名状态：{{ item.signupStatus || '未知' }}</div>
              <div class="project-meta status-line">报名时间：{{ item.signupTime || '-' }}</div>
            </div>
            <div class="project-counts">
              <span>浏览 {{ item.clickCount ?? 0 }}</span>
              <span>点赞 {{ item.likeCount ?? 0 }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无参与项目" />
      </section>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑个人信息" width="560px">
      <el-form label-width="88px">
        <el-form-item label="账号">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="editForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="头像">
          <div class="avatar-upload-row">
            <el-avatar :size="64" :src="resolveAvatar(editForm.avatar)">
              {{ (editForm.name || editForm.username || 'U').slice(0, 1) }}
            </el-avatar>
            <el-upload :show-file-list="false" :before-upload="handleAvatarUpload" accept="image/*">
              <el-button :loading="avatarUploading" :icon="UploadFilled">上传头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.sex">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色">
          <el-input :model-value="editForm.role" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-page {
  padding: 20px 16px 24px;
}

.profile-card {
  max-width: 1040px;
  margin: 0 auto;
  border-radius: 14px;
}

.header-row,
.header-actions,
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-title {
  font-size: 20px;
  font-weight: 800;
  color: #303133;
}

.page-subtitle,
.section-tip,
.project-meta {
  color: #909399;
  font-size: 12px;
}

.profile-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 24px;
  margin-top: 18px;
}

.avatar-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 18px;
  background: #f8fafc;
  border-radius: 14px;
}

.role-badge {
  padding: 6px 12px;
  border-radius: 999px;
  background: #ecf5ff;
  color: #1677ff;
  font-size: 12px;
}

.profile-summary {
  display: flex;
  align-items: flex-start;
}

.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.projects-section {
  margin-top: 28px;
}

.project-list {
  margin-top: 12px;
  display: grid;
  gap: 12px;
}

.project-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f8fafc;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.project-row:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}

.project-title {
  font-weight: 700;
  color: #303133;
}

.project-counts {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  color: #606266;
  font-size: 12px;
}

.status-line {
  margin-top: 2px;
}

@media (max-width: 960px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}
</style>
