<template>
  <div class="login-page">
    <div class="login-side">
      <div class="login-side-bg"></div>
      <div class="login-side-noise"></div>

      <div class="login-side-top">
        <div class="brand">
          <div class="brand-mark" aria-hidden="true">
            <div class="brand-mark-inner"></div>
          </div>
          <div class="brand-text">
            <div class="brand-name">StudyFlow</div>
            <div class="brand-tagline">高校研学交流平台</div>
          </div>
        </div>
      </div>

      <div class="login-side-description">
        <h3 class="desc-title">一款面向高校研学的 StudyFlow 平台</h3>
        <p class="desc-subtitle">整合项目申报、研学课程与交流社区，帮助老师与学生高效协作。</p>
      </div>
    </div>

    <div class="form-side">
      <div class="form-inner">
        <div class="form-header-meta">
          <span class="meta-link">帮助中心</span>
          <span class="meta-dot">·</span>
          <span class="meta-link">联系管理员</span>
        </div>

        <div class="form-wrapper">
          <h1 class="title">欢迎回来</h1>
          <p class="subtitle">输入您的账号信息，先选择身份再进入对应角色的工作台</p>

          <div class="role-switcher">
            <span class="role-switcher-label">选择身份</span>
            <div class="role-switcher-buttons">
              <el-button :type="form.role === '学生' ? 'primary' : 'default'" round @click="form.role = '学生'">
                学生
              </el-button>
              <el-button :type="form.role === '老师' ? 'primary' : 'default'" round @click="form.role = '老师'">
                老师
              </el-button>
              <el-button :type="form.role === '管理员' ? 'primary' : 'default'" round @click="form.role = '管理员'">
                管理员
              </el-button>
            </div>
          </div>

          <el-form :model="form" label-position="top" class="login-form">
            <el-form-item label="个人账号" prop="email">
              <el-input v-model="form.user" placeholder="输入你的账号" autocomplete="email" />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" placeholder="请输入密码" show-password autocomplete="current-password" />
            </el-form-item>

            <div class="form-actions">
              <el-checkbox v-model="form.remember">记住账号</el-checkbox>
              <a href="#" class="forgot-link">忘记密码？</a>
            </div>

            <el-button type="primary" class="submit" round @click="onLogin" :loading="loading" style="width: 100%;">
              登录
            </el-button>

            <div class="signup">
              <span>没有账号？</span>
              <router-link to="/register">注册</router-link>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_BASE_URL } from '@/config/api'
import { connectChatSocket } from '@/utils/chatSocket'

const router = useRouter()

const form = reactive({
  user: '',
  password: '',
  remember: false,
  role: '学生',
})

const loading = ref(false)

onMounted(() => {
  const username = localStorage.getItem('username') || sessionStorage.getItem('username')
  if (username) {
    form.user = username
  }
})

async function onLogin() {
  if (!form.user || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  if(form.remember){
    localStorage.setItem('username', form.user)
  }
  loading.value = true
  try {
    const response = await axios.post(`${API_BASE_URL}/admin/login`, {
      username: form.user,
      password: form.password,
      role: form.role,
    })

    if (response.data?.code !== 1) {
      ElMessage.error(response.data?.msg || '登录失败')
      return
    }

    const loginData = response.data?.data || {}
    const token = loginData?.token
    if (!token) {
      ElMessage.error('登录成功，但未获取到 token')
      return
    }

    sessionStorage.setItem('token', token)
    sessionStorage.setItem('role', loginData?.role || form.role)
    sessionStorage.setItem('userRole', loginData?.role || form.role)

    const userFields = ['id', 'username', 'name', 'nickname', 'avatar', 'avatarUrl', 'headImg', 'headimg', 'photo', 'picture']
    userFields.forEach((key) => {
      const value = loginData?.[key]
      if (value !== undefined && value !== null && value !== '') {
        sessionStorage.setItem(key, String(value))
      }
    })

    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('userRole')
    localStorage.removeItem('avatar')
    localStorage.removeItem('avatarUrl')
    localStorage.removeItem('headImg')
    localStorage.removeItem('headimg')
    localStorage.removeItem('photo')
    localStorage.removeItem('picture')

    try {
      await connectChatSocket(token, Number(loginData?.id || 0))
    } catch {
      ElMessage.warning('登录成功，但消息连接尚未建立，稍后会自动重试')
    }

    ElMessage.success('登录成功')

    const roleHomeMap: Record<string, string> = {
      学生: '/student/projects/news',
      老师: '/teacher',
      管理员: '/',
    }
    router.push(roleHomeMap[form.role] || '/')
  } catch (error) {
    ElMessage.error('登录接口请求失败，请检查后端服务是否已启动')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 3fr minmax(280px, 1fr);
  background: radial-gradient(circle at 0 0, #eef3ff 0, #f7f9ff 40%, #f4f6fb 100%);
}

.login-side {
  position: relative;
  color: #324157;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 32px 56px 28px 56px;
  overflow: hidden;
}

.login-side-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 0% 0%, rgba(84, 132, 255, 0.16) 0, transparent 55%),
    radial-gradient(circle at 90% 100%, rgba(72, 192, 255, 0.12) 0, transparent 52%),
    linear-gradient(135deg, #f5f7ff, #f7f9ff 40%, #f2f5ff 100%);
  opacity: 1;
  z-index: -3;
}

.login-side-noise {
  position: absolute;
  inset: -40px;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 160 160' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='3' stitchTiles='noStitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.12'/%3E%3C/svg%3E");
  mix-blend-mode: soft-light;
  pointer-events: none;
  z-index: -2;
}

.login-side-top {
  display: flex;
  flex-direction: column;
  gap: 36px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  font-weight: 600;
}

.brand-mark {
  position: relative;
  width: 44px;
  height: 44px;
  border-radius: 999px;
  background: radial-gradient(circle at 30% 20%, #ffffff 0, #e5f1ff 40%, #a2b4ff 80%);
  box-shadow:
    0 6px 16px rgba(47, 84, 235, 0.25),
    inset 0 0 0 1px rgba(255, 255, 255, 0.9);
}

.brand-mark-inner {
  position: absolute;
  inset: 11px;
  border-radius: inherit;
  background: linear-gradient(135deg, #4b7fff, #6ca4ff);
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-name {
  font-size: 20px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #1f2f56;
}

.brand-tagline {
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: none;
  color: #8a95b2;
}

.login-side-description {
  margin-top: 32px;
}

.desc-title {
  font-size: 18px;
  color: #1f2f56;
  margin: 0 0 8px;
}

.desc-subtitle {
  margin: 0;
  font-size: 13px;
  color: #8791aa;
}

.form-side {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 56px 0 32px;
  background: #fdfefe;
}

.form-inner {
  width: 100%;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  height: 100%;
  justify-content: center;
}

.form-header-meta {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #a0a6b8;
  padding-top: 24px;
  padding-bottom: 32px;
}

.meta-link {
  cursor: pointer;
}

.meta-link:hover {
  color: #4b7fff;
}

.meta-dot {
  opacity: 0.6;
}

.form-wrapper {
  width: 100%;
  background: transparent;
  padding: 16px 0 32px;
}

.title {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.55);
  margin-bottom: 28px;
}

.role-switcher {
  margin-bottom: 20px;
  padding: 14px;
  border: 1px solid #e7ecf5;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff, #f8faff);
}

.role-switcher-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 10px;
}

.role-switcher-buttons {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.login-form {
  display: grid;
  gap: 18px;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  margin-bottom: 8px;
}

.forgot-link {
  color: #4c5ef8;
  text-decoration: none;
}

.forgot-link:hover {
  text-decoration: underline;
}

.signup {
  margin-top: 18px;
  font-size: 13px;
  text-align: center;
  color: rgba(0, 0, 0, 0.58);
}

.signup a {
  color: #4c5ef8;
  font-weight: 600;
  margin-left: 4px;
}

@media (max-width: 960px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-side {
    display: none;
  }

  .form-side {
    padding: 48px 24px;
  }
}
</style>
