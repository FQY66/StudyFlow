<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'

interface AnnouncementItem {
  id: number
  title: string
  category: string
  summary: string
  source: string
  status: string
  createTime: string
  clickCount: number
}

interface ApiResult<T> {
  code: number
  msg?: string
  data: T
}

interface PageResult<T> {
  total: number
  records: T[]
}

const props = withDefaults(defineProps<{
  title?: string
  apiPath?: string
  limit?: number
  showFooter?: boolean
}>(), {
  title: '课程公告',
  apiPath: '/researchNews/page',
  limit: 8,
  showFooter: true
})

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const announcements = ref<AnnouncementItem[]>([])
const total = ref(0)

const prefix = computed(() => (route.path.startsWith('/student') ? '/student' : ''))

const fetchAnnouncements = async () => {
  loading.value = true
  try {
    const { data } = await request.get<ApiResult<PageResult<AnnouncementItem>>>(props.apiPath, {
      params: {
        page: 1,
        pageSize: props.limit
      }
    })
    if (data.code === 1) {
      total.value = data.data.total || 0
      announcements.value = (data.data.records || []).map((item) => ({
        ...item,
        summary: item.summary || '',
        source: item.source || '课程公告',
        status: item.status || '已发布',
        clickCount: item.clickCount || 0
      }))
    }
  } finally {
    loading.value = false
  }
}

const goDetail = (item: AnnouncementItem) => {
  router.push(`${prefix.value}/projects/news/detail/study/${item.id}`)
}

watch(() => props.apiPath, () => {
  void fetchAnnouncements()
})

onMounted(() => {
  void fetchAnnouncements()
})
</script>

<template>
  <div class="announcement-panel" v-loading="loading">
    <div class="panel-title">{{ title }}</div>
    <div class="announcement-list" v-if="announcements.length">
      <a
        v-for="(item, idx) in announcements"
        :key="item.id"
        class="announcement-item"
        href="javascript:void(0)"
        @click.prevent="goDetail(item)"
      >
        <div class="announcement-rank">{{ String(idx + 1).padStart(2, '0') }}</div>
        <div class="announcement-info">
          <div class="announcement-text">{{ item.title }}</div>
          <div class="announcement-meta">
            <span>{{ item.category }}</span>
            <span>{{ item.createTime }}</span>
          </div>
        </div>
      </a>
    </div>
    <el-empty v-else description="暂无公告" />
    <div v-if="showFooter" class="panel-footer">共 {{ total }} 条公告</div>
  </div>
</template>

<style scoped>
.announcement-panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  padding: 14px 16px 12px;
}

.panel-title {
  font-weight: 700;
  color: #303133;
  font-size: 16px;
  margin-bottom: 10px;
}

.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.announcement-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 6px 8px;
  border-radius: 10px;
  text-decoration: none;
  color: inherit;
  transition: background 0.18s ease;
}

.announcement-item:hover {
  background: rgba(64, 158, 255, 0.08);
}

.announcement-rank {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: rgba(64, 158, 255, 0.12);
  color: #409eff;
  font-weight: 700;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.announcement-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.4;
}

.announcement-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.panel-footer {
  margin-top: 10px;
  font-size: 13px;
  color: #909399;
}
</style>
