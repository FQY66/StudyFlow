<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { View, Star, User, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type { ECharts, EChartsOption } from 'echarts'

interface StatCard {
  label: string
  value: number | string
  trend: string
  trendType: 'up' | 'down'
}

interface UserData {
  id: number
  name: string
  isonline: number
  gender: string
  progress: number
}

interface HotProject {
  id: number
  title: string
  views: number
  likes: number
  category: string
}

const statCards = ref<StatCard[]>([
  { label: '总项目数', value: 52, trend: '+20%', trendType: 'up' },
  { label: '在线访客数', value: 182, trend: '-10%', trendType: 'down' },
  { label: '点击量', value: 9520, trend: '-12%', trendType: 'down' },
  { label: '参与人数', value: 156, trend: '+30%', trendType: 'up' }
])

const users = ref<UserData[]>([
  { id: 1, name: '张飞', isonline: 0, gender: '女', progress: 60 },
  { id: 2, name: '诸葛亮', isonline: 1, gender: '男', progress: 20 },
  { id: 3, name: '刘备', isonline: 1,  gender: '男', progress: 60 }
])

// 将在线用户排在前面
const sortedUsers = computed(() => {
  return users.value.slice().sort((a, b) => b.isonline - a.isonline)
})

const hotProjects = ref<HotProject[]>([
  { id: 1, title: '大数据技术在高校教学中的应用', views: 2150, likes: 650, category: '大数据' },
  { id: 2, title: '人工智能伦理与研究方法论', views: 1980, likes: 520, category: 'AI' },
  { id: 3, title: '云计算平台构建与实践', views: 2340, likes: 720, category: '云计算' },
  { id: 4, title: '高校智能管理系统开发', views: 1680, likes: 410, category: '系统开发' },
  { id: 5, title: '教育数据挖掘与分析', views: 1520, likes: 385, category: '数据分析' }
])

interface TodoItem {
  id: number
  title: string
  time: string
  completed: boolean
}

const todoItems = ref<TodoItem[]>([
  { id: 1, title: '查看今天工作内容', time: '上午 09:30', completed: false },
  { id: 2, title: '回复邮件', time: '上午 10:00', completed: false },
  { id: 3, title: '工作日报表管理', time: '上午 11:00', completed: false },
  { id: 4, title: '整理会议内容', time: '下午 02:00', completed: false },
  { id: 5, title: '明天工作计划', time: '下午 06:30', completed: false }
])

const chartData = {
  months: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月'],
  values: [150, 90, 140, 70, 170, 90, 160, 100, 150]
}

const userOverviewChartRef = ref<HTMLElement | null>(null)
const visitsChartRef = ref<HTMLElement | null>(null)

let userOverviewChart: ECharts | null = null
let visitsChart: ECharts | null = null

const initUserOverviewChart = () => {
  if (!userOverviewChartRef.value) return
  if (userOverviewChart) {
    userOverviewChart.dispose()
  }
  userOverviewChart = echarts.init(userOverviewChartRef.value)

  const option: EChartsOption = {
    animationDuration: 800,
    animationEasing: 'cubicOut',
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['新用户', '活跃用户', '回访用户'],
      top: 0
    },
    grid: {
      top: 40,
      left: '3%',
      right: '4%',
      bottom: 40,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: chartData.months
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '新用户',
        type: 'bar',
        barMaxWidth: 24,
        emphasis: { focus: 'series' },
        data: [40, 32, 51, 34, 60, 42, 55, 48, 62]
      },
      {
        name: '活跃用户',
        type: 'bar',
        barMaxWidth: 24,
        emphasis: { focus: 'series' },
        data: [60, 45, 50, 30, 72, 48, 70, 52, 68]
      },
      {
        name: '回访用户',
        type: 'bar',
        barMaxWidth: 24,
        emphasis: { focus: 'series' },
        data: [20, 13, 39, 6, 38, 0, 35, 20, 20]
      }
    ]
  }

  userOverviewChart.setOption(option)
}

const initVisitsChart = () => {
  if (!visitsChartRef.value) return
  if (visitsChart) {
    visitsChart.dispose()
  }
  visitsChart = echarts.init(visitsChartRef.value)

  const option: EChartsOption = {
    animationDuration: 800,
    animationEasing: 'cubicOut',
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '6%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: chartData.months
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '访问量',
        type: 'line',
        smooth: true,
        areaStyle: {},
        data: chartData.values
      }
    ]
  }

  visitsChart.setOption(option)
}

const handleResize = () => {
  userOverviewChart?.resize()
  visitsChart?.resize()
}

onMounted(() => {
  initUserOverviewChart()
  initVisitsChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  userOverviewChart?.dispose()
  visitsChart?.dispose()
})
</script>

<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div
        v-for="(card, index) in statCards"
        :key="index"
        class="stat-card"
      >
        <div class="stat-header">
          <span class="stat-label">{{ card.label }}</span>
          <el-icon class="stat-icon"><TrendCharts /></el-icon>
        </div>
        <div class="stat-value">{{ card.value }}</div>
        <div class="stat-trend" :class="card.trendType">
          较上周 {{ card.trend }}
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-container">
      <div class="chart-card">
        <div class="chart-header">
          <h3>用户概述</h3>
          <span class="chart-subtitle">比上周 +23%</span>
        </div>
        <div class="chart-content">
          <div class="chart-stats">
            <div class="stat-item">
              <span class="stat-name">总用户量</span>
              <span class="stat-number">32k</span>
            </div>
            <div class="stat-item">
              <span class="stat-name">总访问量</span>
              <span class="stat-number">128k</span>
            </div>
            <div class="stat-item">
              <span class="stat-name">日活跃量</span>
              <span class="stat-number">1.2k</span>
            </div>
            <div class="stat-item">
              <span class="stat-name">周增比</span>
              <span class="stat-number">+5%</span>
            </div>
          </div>
          <div ref="userOverviewChartRef" class="chart-echarts"></div>
        </div>
      </div>

      <div class="chart-card">
        <div class="chart-header">
          <h3>访问量</h3>
          <span class="chart-subtitle">今年截比 +15%</span>
        </div>
        <div ref="visitsChartRef" class="chart-echarts"></div>
      </div>
    </div>

    <!-- 热点项目和待办事项区域 -->
    <div class="projects-container">
      <div class="card projects-card">
        <div class="card-header">
          <h3>热点项目</h3>
          <span class="tag">本周热门 <el-icon><Star /></el-icon></span>
        </div>
        <div class="projects-list">
          <div
            v-for="(project, index) in hotProjects"
            :key="project.id"
            class="project-item"
          >
            <div class="project-rank">{{ index + 1 }}</div>
            <div class="project-info">
              <p class="project-title">{{ project.title }}</p>
              <div class="project-meta">
                <span class="category-badge">{{ project.category }}</span>
              </div>
            </div>
            <div class="project-stats">
              <div class="stat">
                <el-icon><View /></el-icon>
                <span>{{ project.views }}</span>
              </div>
              <div class="stat">
                <el-icon><Star /></el-icon>
                <span>{{ project.likes }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="card tasks-card">
        <div class="card-header">
          <h3>代办事项</h3>
          <span class="tag">待办 {{ todoItems.length }}</span>
        </div>
        <div class="tasks-list">
          <div
            v-for="task in todoItems"
            :key="task.id"
            class="task-item"
          >
            <el-checkbox v-model="task.completed" />
            <div class="task-content">
              <p :class="{ 'task-completed': task.completed }">{{ task.title }}</p>
              <span class="task-time">{{ task.time }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

/* 统计卡片 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

.stat-icon {
  font-size: 20px;
  color: #909399;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.stat-trend {
  font-size: 12px;
}

.stat-trend.up {
  color: #67c23a;
}

.stat-trend.down {
  color: #f56c6c;
}

/* 图表区域 */
.charts-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.chart-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.chart-subtitle {
  font-size: 12px;
  color: #909399;
}

.chart-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.stat-name {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-number {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.chart-echarts {
  margin-top: 24px;
  min-height: 260px;
  width: 100%;
}

/* 热点项目容器 */
.projects-container {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 16px;
}

.card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.trend {
  font-size: 12px;
  color: #909399;
}

.trend.up {
  color: #67c23a;
}

.tag {
  font-size: 12px;
  background: #e6f7ff;
  color: #409eff;
  padding: 4px 8px;
  border-radius: 4px;
}

/* 用户表格 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

  /* 在线状态小点 */
  .status-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    margin: 0 auto;
  }

  .status-dot.online {
    background-color: #67c23a; /* 绿色 */
  }

  .status-dot.offline {
    background-color: #c0c4cc; /* 灰色 */
  }

.dynamics-item {
  display: flex;
  gap: 12px;
  font-size: 12px;
}

.item-dot {
  width: 6px;
  height: 6px;
  background: #409eff;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.item-content p {
  margin: 0;
  color: #606266;
}

.item-title {
  margin-bottom: 4px !important;
}

/* 任务列表 */
.tasks-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.task-content {
  flex: 1;
}

.task-content p {
  margin: 0;
  font-size: 13px;
  color: #303133;
}

.task-time {
  font-size: 11px;
  color: #909399;
}

.task-completed {
  text-decoration: line-through;
  color: #c0c4cc;
}

/* 热点项目列表 */
.projects-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.project-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.project-item:hover {
  background: #f0f2f5;
  transform: translateX(4px);
}

.project-rank {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: white;
  border-radius: 50%;
  font-weight: 600;
  font-size: 12px;
  flex-shrink: 0;
}

.project-info {
  flex: 1;
  min-width: 0;
}

.project-title {
  margin: 0 0 6px 0;
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.project-meta {
  display: flex;
  gap: 8px;
}

.category-badge {
  font-size: 11px;
  background: #ecf5ff;
  color: #409eff;
  padding: 2px 6px;
  border-radius: 2px;
}

.project-stats {
  display: flex;
  gap: 16px;
  flex-shrink: 0;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.stat :deep(.el-icon) {
  font-size: 14px;
}


@media (max-width: 1024px) {
  .charts-container {
    grid-template-columns: 1fr;
  }

  .projects-container {
    grid-template-columns: 1fr;
  }

  .chart-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>