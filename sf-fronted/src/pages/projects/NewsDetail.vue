<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const type = computed(() => String(route.params.type ?? ''))
const id = computed(() => String(route.params.id ?? ''))

const typeLabel = computed(() => {
  if (type.value === 'study') return '研学资讯'
  if (type.value === 'gov') return '政府声音'
  if (type.value === 'expert') return '专家观点'
  return '资讯'
})
</script>

<template>
  <div class="info-detail">
    <el-card shadow="never" class="detail-card">
      <div class="detail-header">
        <el-button link type="primary" @click="router.back()">返回</el-button>
        <div class="detail-breadcrumb">{{ typeLabel }}</div>
      </div>

      <h2 class="detail-title">{{ typeLabel }} #{{ id }}</h2>
      <p class="detail-desc">
        这里是占位详情页：你点击列表项后会跳转到这里。后续接入后端接口后，可以根据
        `type` + `id` 拉取真实内容并渲染。
      </p>
    </el-card>
  </div>
</template>

<style scoped>
.info-detail {
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
  font-size: 12px;
  color: #909399;
}

.detail-title {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.detail-desc {
  margin: 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}
</style>

