<script setup lang="ts">
import { computed, ref } from 'vue'

interface ProjectCard {
  id: number
  title: string
  category: string
  year: number
  cover: string
  createdAt: string
  views: number
  likes: number
}

const keyword = ref('')
const activeYear = ref<'all' | number>('all')

const projects = ref<ProjectCard[]>([
  {
    id: 1,
    title: '古都文脉探寻',
    category: '古都文化',
    year: 2024,
    cover: 'https://picsum.photos/seed/sf-p1/600/360',
    createdAt: '2024-08-26',
    views: 56,
    likes: 96
  },
  {
    id: 2,
    title: '红色记忆研学',
    category: '红色探访',
    year: 2024,
    cover: 'https://picsum.photos/seed/sf-p2/600/360',
    createdAt: '2024-02-22',
    views: 109,
    likes: 80
  },
  {
    id: 3,
    title: '人工智能前沿',
    category: '科技创新',
    year: 2023,
    cover: 'https://picsum.photos/seed/sf-p3/600/360',
    createdAt: '2023-11-30',
    views: 88,
    likes: 72
  },
  {
    id: 4,
    title: '航天航空探秘',
    category: '探秘航天',
    year: 2023,
    cover: 'https://picsum.photos/seed/sf-p4/600/360',
    createdAt: '2023-04-10',
    views: 146,
    likes: 104
  }
])

const yearOptions = computed(() => {
  const years = Array.from(new Set(projects.value.map(p => p.year))).sort(
    (a, b) => b - a
  )
  return years
})

const filteredProjects = computed(() => {
  return projects.value.filter(p => {
    const matchYear =
      activeYear.value === 'all' ? true : p.year === activeYear.value
    const kw = keyword.value.trim().toLowerCase()
    const matchKeyword = !kw
      ? true
      : p.title.toLowerCase().includes(kw) ||
        p.category.toLowerCase().includes(kw)
    return matchYear && matchKeyword
  })
})

const handleCreate = () => {
  // TODO: 打开新建项目弹窗
}
</script>

<template>
  <div class="project-page">
    <header class="project-header">
      <el-input
        v-model="keyword"
        class="search-input"
        placeholder="输入项目名称进行筛选"
        clearable
      />

      <div class="header-right">
        <div class="year-tabs">
          <button
            class="year-tab"
            :class="{ active: activeYear === 'all' }"
            type="button"
            @click="activeYear = 'all'"
          >
            All
          </button>
          <button
            v-for="year in yearOptions"
            :key="year"
            class="year-tab"
            :class="{ active: activeYear === year }"
            type="button"
            @click="activeYear = year"
          >
            {{ year }}
          </button>
        </div>

        <el-button type="primary" @click="handleCreate" plain class="btn-post">
          新建项目
        </el-button>
      </div>
    </header>

    <main class="card-grid">
      <article
        v-for="project in filteredProjects"
        :key="project.id"
        class="project-card"
      >
        <div class="cover-wrapper">
          <img :src="project.cover" class="cover-img" alt="" />
          <span class="category-tag">
            {{ project.category }}
          </span>
        </div>

        <div class="card-body">
          <h3 class="card-title">
            {{ project.title }}
          </h3>
          <div class="card-footer">
            <div class="meta-left">
              <span class="date">{{ project.createdAt }}</span>
              <span class="meta-dot">·</span>
              <span class="meta-item">
                <el-icon><View /></el-icon>
                <span>{{ project.views }}</span>
              </span>
              <span class="meta-item">
                <el-icon><Star /></el-icon>
                <span>{{ project.likes }}</span>
              </span>
            </div>
          </div>
        </div>
      </article>
    </main>
  </div>
</template>

<style scoped>
.project-page {
  padding: 16px 8px 8px;
}

.project-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.search-input {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.year-tabs {
  display: flex;
  gap: 8px;
  background-color: #f5f7fa;
  border-radius: 999px;
  padding: 4px;
}

.year-tab {
  border: none;
  background: transparent;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
}

.year-tab.active {
  background-color: #2f43db7e;
  color: #fff;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.project-card {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
}
.project-card:hover {
  transform: scale(1.05);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.cover-wrapper {
  position: relative;
  width: 100%;
  padding-top: 60%;
  overflow: hidden;
}

.cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.category-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.36);
}

.card-body {
  padding: 12px 14px 10px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px;
  color: #303133;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.meta-dot {
  opacity: 0.6;
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

