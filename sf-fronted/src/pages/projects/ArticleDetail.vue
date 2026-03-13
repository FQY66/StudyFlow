<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const id = computed(() => String(route.params.id ?? ''))
</script>

<template>
  <div class="course-detail">
    <el-card shadow="never" class="detail-card">
      <div class="detail-header">
        <el-button link type="primary" @click="router.back()">返回</el-button>
        <div class="detail-breadcrumb">热门文章</div>
      </div>

      <h2 class="detail-title">文章详情 #{{ id }}</h2>
      <p class="detail-desc">
        这里是占位文章详情页。后续你接入后端接口后，可以根据课程 id 拉取真实内容并渲染。
      </p>
    </el-card>
  </div>
</template>

<style scoped>
.course-detail {
  padding: 16px 12px 12px;
}

.detail-card {
  background: #ffffff;
  border-radius: 12px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.detail-breadcrumb {
  font-size: 13px; /* 字体大小（可调整） */
  color: #909399;
}

.detail-title {
  margin: 0 0 10px;
  font-size: 20px; /* 字体大小（可调整） */
  font-weight: 800;
  color: #303133;
}

.detail-desc {
  margin: 0;
  font-size: 14px; /* 字体大小（可调整） */
  color: #606266;
  line-height: 1.8;
}
</style>

