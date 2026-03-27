<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { API_BASE_URL } from '@/config/api'

const fileList = ref<File[]>([])
const uploading = ref(false)
const uploadedUrl = ref('')
const responseText = ref('')

const onFileChange = (uploadFile: any) => {
  if (uploadFile?.raw) {
    fileList.value = [uploadFile.raw as File]
  }
}

const onUpload = async () => {
  if (!fileList.value.length) {
    ElMessage.warning('请先选择文件')
    return
  }

  const file = fileList.value[0]
  if (!file) {
    ElMessage.warning('请选择有效文件')
    return
  }

  const formData = new FormData()
  formData.append('file', file, file.name)

  uploading.value = true
  uploadedUrl.value = ''
  responseText.value = ''

  try {
    const { data } = await axios.post(`${API_BASE_URL}/common/file/upload`, formData, {
      headers: {
        token: localStorage.getItem('token') || ''
      }
    })

    responseText.value = JSON.stringify(data, null, 2)

    if (data?.code === 1 && data?.data?.url) {
      uploadedUrl.value = `${API_BASE_URL}/${data.data.url}`
      ElMessage.success('上传成功')
    } else {
      ElMessage.error(data?.msg || '上传失败')
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || '上传失败，请检查后端服务')
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="upload-test-page">
    <section class="card">
      <h2 class="title">文件上传测试</h2>
      <p class="desc">用于测试后端 <code>/common/file/upload</code> 接口是否可用</p>

      <el-upload
        class="uploader"
        :auto-upload="false"
        :limit="1"
        :show-file-list="true"
        :on-change="onFileChange"
      >
        <el-button type="primary" plain>选择文件</el-button>
      </el-upload>

      <div class="actions">
        <el-button type="primary" :loading="uploading" @click="onUpload">上传测试</el-button>
      </div>

      <div v-if="uploadedUrl" class="result-block">
        <p class="label">上传后访问地址：</p>
        <a :href="uploadedUrl" target="_blank" rel="noopener noreferrer">{{ uploadedUrl }}</a>
        <img v-if="uploadedUrl" :src="uploadedUrl" alt="preview" class="preview" />
      </div>

      <div v-if="responseText" class="result-block">
        <p class="label">接口返回：</p>
        <pre>{{ responseText }}</pre>
      </div>
    </section>
  </div>
</template>

<style scoped>
.upload-test-page {
  padding: 8px;
}

.card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 20px;
  max-width: 760px;
}

.title {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.desc {
  margin: 8px 0 16px;
  color: #606266;
}

.actions {
  margin-top: 14px;
}

.result-block {
  margin-top: 18px;
  padding: 12px;
  border-radius: 8px;
  background: #f8f9fb;
}

.label {
  margin: 0 0 8px;
  color: #303133;
  font-weight: 600;
}

.preview {
  display: block;
  margin-top: 10px;
  max-width: 300px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
