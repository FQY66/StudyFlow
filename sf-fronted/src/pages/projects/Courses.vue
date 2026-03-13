<script setup lang="ts">
import { computed, ref } from 'vue'

interface CourseItem {
  id: number
  title: string
  summary: string
  cover: string
  tags: string[]
  createdAt: string
}

interface HotItem {
  id: number
  title: string
  date: string
}

const keyword = ref('')

const courses = ref<CourseItem[]>([
  {
    id: 1,
    title: '研学旅行是一种什么体验',
    summary:
      '从“看风景”到“做研究”，用任务单和情境问题把研学变成可复盘的学习过程。',
    cover: 'https://picsum.photos/seed/sf-course-1/560/320',
    tags: ['研学', '课程设计', '体验'],
    createdAt: '2025-03-05'
  },
  {
    id: 2,
    title: '带你做高质量研学路线：如何组织与安全管理',
    summary:
      '出行前的风险评估、现场的秩序组织、过程记录与评价闭环，让研学更专业更安心。',
    cover: 'https://picsum.photos/seed/sf-course-2/560/320',
    tags: ['安全', '组织', '流程'],
    createdAt: '2025-02-19'
  },
  {
    id: 3,
    title: '传统研学形式不够新颖，如何为研学“突出重围”？',
    summary:
      '用跨学科项目制学习（PBL）重构研学任务，让学生带着问题出发、带着成果归来。',
    cover: 'https://picsum.photos/seed/sf-course-3/560/320',
    tags: ['PBL', '跨学科', '任务单'],
    createdAt: '2025-01-12'
  },
  {
    id: 4,
    title: '研学课程设计：从理论到实践',
    summary:
      '如何设计一个既有趣又有教育意义的研学课程？本课程将介绍研学课程设计的基本原则和方法，帮助教师打造高质量的研学体验。',
    cover: 'https://picsum.photos/seed/sf-course-4/560/320',
    tags: ['PBL', '跨学科', '任务单'],
    createdAt: '2025-01-12'
  }
])

const hotList = ref<HotItem[]>([
  { id: 1, title: '研学旅行纳入课程体系的若干建议', date: '2025-03-07' },
  { id: 2, title: '地方研学基地建设与评价指标解读', date: '2025-03-01' },
  { id: 3, title: '如何避免“到此一游式”研学', date: '2025-02-26' },
  { id: 4, title: '12 条研学安全底线与应急清单', date: '2025-02-18' },
  { id: 5, title: '研学任务单怎么写：从目标到评价的模板', date: '2025-02-10' },
  { id: 6, title: '研学活动如何拍摄与记录：素材清单', date: '2025-02-02' },
  { id: 7, title: '校企共建研学项目：合作流程与注意事项', date: '2025-01-26' },
  { id: 8, title: '研学课程的跨学科融合：案例拆解', date: '2025-01-18' },
  { id: 9, title: '研学成果如何展示：海报/视频/报告指南', date: '2025-01-12' },
  { id: 10, title: '研学安全：天气、交通与场地三类风险预案', date: '2025-01-05' }
])

const filteredCourses = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return courses.value
  return courses.value.filter(c => {
    return (
      c.title.toLowerCase().includes(kw) ||
      c.summary.toLowerCase().includes(kw) ||
      c.tags.some(t => t.toLowerCase().includes(kw))
    )
  })
})
</script>

<template>
  <div class="courses-page">
    <section class="top-bar">
      <div class="top-left">
        <h2 class="page-title">精品课程</h2>
        <div class="page-subtitle">精选优质研学课程内容，快速找到你想学的主题。</div>
      </div>
      <el-input
        v-model="keyword"
        class="search"
        placeholder="搜索课程标题 / 标签"
        clearable
      />
    </section>

    <section class="layout">
      <main class="main">
        <router-link
          v-for="item in filteredCourses"
          :key="item.id"
          class="course-link"
          :to="`/projects/courses/${item.id}`"
        >
          <article class="course">
            <div class="course-cover">
              <img :src="item.cover" alt="" />
            </div>
            <div class="course-body">
              <div class="course-title">{{ item.title }}</div>
              <div class="course-summary">{{ item.summary }}</div>
              <div class="course-tags">
                <span v-for="t in item.tags" :key="t" class="tag">{{ t }}</span>
              </div>
              <div class="course-meta">
                <span class="meta-date">{{ item.createdAt }}</span>
                <span class="meta-dot">·</span>
                <span class="meta-action">查看详情</span>
              </div>
            </div>
          </article>
        </router-link>
      </main>

      <aside class="aside">
        <div class="side-card">
          <div class="side-title">热门文章</div>
          <div class="hot-list">
            <router-link
              v-for="(h, idx) in hotList"
              :key="h.id"
              class="hot-link"
              :to="`/projects/courses/article/${h.id}`"
            >
              <div class="hot-item">
                <div class="hot-rank">
                  {{ String(idx + 1).padStart(2, '0') }}
                </div>
                <div class="hot-info">
                  <div class="hot-text">{{ h.title }}</div>
                  <div class="hot-date">{{ h.date }}</div>
                </div>
              </div>
            </router-link>
          </div>
        </div>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.courses-page {
  padding: 16px 12px 12px;
}

.top-bar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-title {
  margin: 0 0 4px;
  font-size: 24px; /* 字体大小（可调整） */
  font-weight: 700;
  color: #303133;
}

.page-subtitle {
  font-size: 14px; /* 字体大小（可调整） */
  color: #909399;
}

.search {
  width: 320px;
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
}

.course {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.course-link {
  display: block;
  text-decoration: none;
  color: inherit;
  margin-bottom: 16px;
}

.course-link:hover .course {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.08);
}

.course-cover {
  height: 100%;
}

.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.course-body {
  padding: 14px 16px 12px;
}

.course-title {
  font-size: 18px; /* 字体大小（可调整） */
  font-weight: 700;
  color: #303133;
  margin-bottom: 6px;
}

.course-summary {
  font-size: 15px; /* 字体大小（可调整） */
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
}

.course-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.tag {
  font-size: 13px; /* 字体大小（可调整） */
  padding: 2px 8px;
  border-radius: 999px;
  background: #ecf5ff;
  color: #409eff;
}

.course-meta {
  font-size: 13px; /* 字体大小（可调整） */
  color: #909399;
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-action {
  color: #409eff;
  cursor: pointer;
}

.aside {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  padding: 14px 16px 12px;
}

.side-title {
  font-weight: 700;
  color: #303133;
  font-size: 16px; /* 字体大小（可调整） */
  margin-bottom: 10px;
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hot-link {
  text-decoration: none;
  color: inherit;
  border-radius: 10px;
  display: block;
}

.hot-link:hover {
  background: rgba(64, 158, 255, 0.08);
}

.hot-link:hover .hot-text {
  color: #409eff;
}

.hot-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 6px 8px;
}

.hot-rank {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: rgba(64, 158, 255, 0.12);
  color: #409eff;
  font-weight: 700;
  font-size: 13px; /* 字体大小（可调整） */
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hot-text {
  font-size: 14px; /* 字体大小（可调整） */
  color: #303133;
  line-height: 1.4;
}

.hot-date {
  font-size: 13px; /* 字体大小（可调整） */
  color: #909399;
  margin-top: 2px;
}

@media (max-width: 1024px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .search {
    width: 100%;
  }

  .course {
    grid-template-columns: 1fr;
  }
}
</style>

