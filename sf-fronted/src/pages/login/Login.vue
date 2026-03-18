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

      <div class="login-side-visual" aria-hidden="true">
        <div class="hero-illustration">
          <div class="hero-bg-circle hero-bg-circle-lg"></div>
          <div class="hero-bg-circle hero-bg-circle-sm"></div>

          <div class="hero-device">
            <div class="hero-device-screen">
              <div class="hero-folder hero-folder-main"></div>
              <div class="hero-folder hero-folder-back"></div>
              <div class="hero-check"></div>
            </div>
            <div class="hero-device-base"></div>
          </div>

          <div class="hero-paper hero-paper-left"></div>
          <div class="hero-paper hero-paper-top"></div>
        </div>
      </div>

      <div class="login-side-description">
        <h3 class="desc-title">一款面向高校研学的 StudyFlow 平台</h3>
        <p class="desc-subtitle">整合项目申报、研学课程与交流社区，帮助老师与学生高效协作。</p>
      </div>

      <div class="footer-links">
        <span class="footer-year">© {{ new Date().getFullYear() }} StudyFlow</span>
        <div class="footer-links-right">
          <a href="#" class="footer-link">隐私政策</a>
          <span class="footer-dot">•</span>
          <a href="#" class="footer-link">服务条款</a>
        </div>
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
        <p class="subtitle">输入您的账号信息，进入高校研学交流平台</p>

        <el-form :model="form" label-position="top" class="login-form">
          <el-form-item label="个人账号" prop="email">
            <el-input
              v-model="form.user"
              placeholder="输入你的账号"
              autocomplete="email"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              placeholder="请输入密码"
              show-password
              autocomplete="current-password"
            />
          </el-form-item>

          <div class="form-actions">
            <el-checkbox v-model="form.remember">记住账号</el-checkbox>
            <a href="#" class="forgot-link">忘记密码？</a>
          </div>

          <el-button type="primary" class="submit" round @click="onLogin" :loading="loading" style="width: 100%;">
            登录
          </el-button>

          <!-- <el-button type="outline" class="google" round @click="onLoginWithGoogle" style="width: 100%;">
            使用其他方式登录
          </el-button> -->

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

const router = useRouter()

const form = reactive({
  user: '',
  password: '',
  remember: false,
})

const loading = ref(false)

onMounted(() => {
  const username = localStorage.getItem('username')
  if(username){
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
    // 1. 调用后端登录接口
    const response = await axios.post('http://localhost:8080/admin/login', {
      // 后端要求的字段名是 username / password
      username: form.user,
      password: form.password,
    })

    // 2. 判断后端返回是否成功（Result.code === 1 表示成功）
    if (response.data?.code !== 1) {
      ElMessage.error(response.data?.msg || '登录失败')
      return
    }

    // 3. 取出 token 并存到本地（路由守卫会用它判断是否登录）
    const token = response.data?.data?.token
    if (!token) {
      ElMessage.error('登录成功，但未获取到 token')
      return
    }

    localStorage.setItem('token', token)
    ElMessage.success('登录成功')

    // 4. 跳转到首页
    router.push('/')
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

.illustration {
  position: relative;
  flex: 1;
  display: grid;
  place-items: center;
  padding: 20px 0;
}

.headline-block {
  max-width: 460px;
}

.headline-title {
  font-size: 32px;
  line-height: 1.2;
  letter-spacing: 0.04em;
  margin: 0 0 14px;
  color: #1f2f56;
}

.headline-desc {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.6;
  color: #7b88a6;
}

.stats-row {
  display: flex;
  gap: 14px;
}

.stat-card {
  flex: 1;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 14px;
  padding: 12px 14px;
  box-shadow:
    0 8px 22px rgba(31, 56, 104, 0.12),
    0 0 0 1px rgba(210, 220, 255, 0.8);
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #1f2f56;
}

.stat-label {
  margin-top: 2px;
  font-size: 12px;
  color: #8a95b2;
}

.login-side-visual {
  position: relative;
  flex: 1;
  margin-top: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-illustration {
  position: relative;
  width: 420px;
  max-width: 100%;
  height: 260px;
}

.hero-bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(120, 150, 255, 0.18);
}

.hero-bg-circle-lg {
  width: 260px;
  height: 260px;
  right: -40px;
  top: -40px;
}

.hero-bg-circle-sm {
  width: 80px;
  height: 80px;
  left: -20px;
  bottom: 10px;
  background: rgba(153, 185, 255, 0.18);
}

.hero-device {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 260px;
}

.hero-device-screen {
  position: relative;
  width: 100%;
  height: 150px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow:
    0 14px 32px rgba(31, 56, 104, 0.18),
    0 0 0 1px rgba(216, 224, 255, 0.9);
  overflow: hidden;
}

.hero-folder {
  position: absolute;
  border-radius: 14px;
}

.hero-folder-main {
  width: 110px;
  height: 70px;
  left: 40px;
  top: 54px;
  background: linear-gradient(135deg, #4b7fff, #6ea5ff);
}

.hero-folder-back {
  width: 78px;
  height: 54px;
  right: 32px;
  top: 62px;
  background: rgba(102, 153, 255, 0.16);
}

.hero-check {
  position: absolute;
  width: 22px;
  height: 22px;
  right: 50px;
  top: 76px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 4px 10px rgba(32, 73, 166, 0.25);
}

.hero-check::before {
  content: '';
  position: absolute;
  inset: 6px;
  border-radius: inherit;
  border-bottom: 2px solid #4b7fff;
  border-right: 2px solid #4b7fff;
  transform: rotate(40deg);
}

.hero-device-base {
  margin: 10px auto 0;
  width: 210px;
  height: 22px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(144, 172, 255, 0.3), rgba(199, 213, 255, 0.12));
  opacity: 0.9;
}

.hero-paper {
  position: absolute;
  width: 54px;
  height: 38px;
  border-radius: 10px;
  background: rgba(129, 164, 255, 0.25);
  border: 1px solid rgba(160, 191, 255, 0.35);
}

.hero-paper-left {
  left: 40px;
  top: 40px;
  transform: rotate(-12deg);
}

.hero-paper-top {
  left: 150px;
  top: 10px;
  transform: rotate(8deg);
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

.footer-links {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: rgba(216, 229, 255, 0.76);
  margin-top: 24px;
}

.footer-year {
  opacity: 0.9;
}

.footer-links-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.footer-dot {
  opacity: 0.6;
}

.footer-link {
  color: inherit;
  text-decoration: none;
  transition: color 0.2s;
}

.footer-link:hover {
  color: rgba(255, 255, 255, 0.95);
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
