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
          <el-input v-model="form.password" placeholder="••••••••" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirm" placeholder="••••••••" show-password autocomplete="new-password" />
        </el-form-item>

        <el-button type="primary" class="submit" round @click="onSubmit" :loading="loading" style="width: 100%;">
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

const router = useRouter()

const form = reactive({
  email: '',
  password: '',
  confirm: '',
})

const loading = ref(false)

function onSubmit() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    router.push('/login')
  }, 800)
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
