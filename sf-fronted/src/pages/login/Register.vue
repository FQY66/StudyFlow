<template>
  <div class="register-page">
    <div class="card">
      <h1>创建账户</h1>
      <p class="desc">输入你的个人账号和密码</p>

      <el-form :model="form" label-position="top" class="form">
        <el-form-item label="个人账号">
          <el-input v-model="form.email" placeholder="账号" autocomplete="email" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" placeholder="请输入你的密码" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirm" placeholder="请确认你的密码" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="身份">
          <el-select v-model="form.role" placeholder="请选择身份">
            <el-option label="学生" value="学生" />
            <el-option label="教师" value="教师" />
            <el-option label="管理员" value="管理员" />
          </el-select>
        </el-form-item>

        <el-button type="primary" class="submit" round @click="register" :loading="loading" style="width: 100%;">
            注册
        </el-button>

        <div class="actions">
          <span>已经有账户了？</span>
          <router-link to="/login">登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { API_BASE_URL } from '@/config/api'

const router = useRouter()

const form = reactive({
  email: '',
  password: '',
  confirm: '',
  role: '',
})

const loading = ref(false)

async function register() {
  if (!form.email || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }

  if (form.password !== form.confirm) {
    ElMessage.warning('两次密码不一致')
    return
  }

  if (!form.role) {
    ElMessage.warning('请选择身份')
    return
  }

  loading.value = true
  try {
    // 1. 调用后端注册接口
    const response = await axios.post(`${API_BASE_URL}/admin/register`, {
      username: form.email,
      password: form.password,
      role: form.role
    })

    // 2. 判断是否注册成功
    if (response.data?.code !== 1) {
      ElMessage.error(response.data?.msg || '注册失败')
      return
    }

    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    ElMessage.error('注册接口请求失败，请检查后端服务是否已启动')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #f3f4ff, #ffffff);
  padding: 40px 16px;
}

.card {
  width: min(440px, 100%);
  padding: 40px;
  background: #ffffff;
  border-radius: 18px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.08);
}

h1 {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 700;
}

.desc {
  margin: 0 0 24px;
  color: rgba(0, 0, 0, 0.58);
}

.form {
  display: grid;
  gap: 18px;
}

.actions {
  margin-top: 18px;
  font-size: 13px;
  text-align: center;
  color: rgba(0, 0, 0, 0.6);
}

.actions a {
  margin-left: 6px;
  color: #4c5ef8;
  font-weight: 600;
}
</style>
