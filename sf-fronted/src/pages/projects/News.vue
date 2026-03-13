<template>
  <div class="projects-info">
    <section class="banner">
      <el-carousel height="360px" indicator-position="outside" arrow="always">
        <el-carousel-item v-for="(src, index) in slides" :key="index">
          <img :src="src" class="carousel-img" alt="研学项目广告" />
        </el-carousel-item>
      </el-carousel>
    </section>

    <section class="info-grid">
      <div class="info-panel">
        <div class="panel-header">
          <div class="panel-title">研学资讯</div>
          <div class="panel-more">MORE&gt;&gt;</div>
        </div>
        <ul class="panel-list">
          <li v-for="item in studyNews" :key="item.id" class="panel-item">
            <router-link
              class="panel-link"
              :to="`/projects/news/detail/study/${item.id}`"
            >
              <span class="item-dot" />
              <span class="item-text">{{ item.title }}</span>
            </router-link>
          </li>
        </ul>
      </div>

      <div class="info-panel">
        <div class="panel-header">
          <div class="panel-title">政府声音</div>
          <div class="panel-more">MORE&gt;&gt;</div>
        </div>
        <ul class="panel-list">
          <li v-for="item in govVoice" :key="item.id" class="panel-item">
            <router-link class="panel-link" :to="`/projects/news/detail/gov/${item.id}`">
              <span class="item-dot" />
              <span class="item-text">{{ item.title }}</span>
            </router-link>
          </li>
        </ul>
      </div>

      <div class="info-panel">
        <div class="panel-header">
          <div class="panel-title">专家观点</div>
          <div class="panel-more">MORE&gt;&gt;</div>
        </div>
        <ul class="panel-list">
          <li v-for="item in expertView" :key="item.id" class="panel-item">
            <router-link
              class="panel-link"
              :to="`/projects/news/detail/expert/${item.id}`"
            >
              <span class="item-dot" />
              <span class="item-text">{{ item.title }}</span>
            </router-link>
          </li>
        </ul>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import project1 from '@/assets/images/project/project1.jpg'
import project2 from '@/assets/images/project/project2.jpg'
import project3 from '@/assets/images/project/project3.jpg'

const slides = [project1, project2, project3]

const studyNews = [
  { id: 1, title: '关于研学旅行课程建设的若干建议' },
  { id: 2, title: '研学项目安全管理要点与案例分析' },
  { id: 3, title: '高校研学活动组织流程与实践经验' },
  { id: 4, title: '研学主题如何与专业课程深度融合' },
  { id: 5, title: '研学成果展示与评价体系设计' }
]

const govVoice = [
  { id: 1, title: '教育部门：强化研学旅行规范管理' },
  { id: 2, title: '文化和旅游：推进文旅研学高质量发展' },
  { id: 3, title: '关于开展研学实践基地建设的指导意见' },
  { id: 4, title: '加强中小学研学旅行安全保障工作通知' },
  { id: 5, title: '推动校外实践教育资源开放共享' }
]

const expertView = [
  { id: 1, title: '研学旅行的价值与育人路径探讨' },
  { id: 2, title: '如何设计高质量的研学课程与任务单' },
  { id: 3, title: '研学活动评价：从过程到成果的量化方法' },
  { id: 4, title: '研学与跨学科融合：真实问题驱动学习' },
  { id: 5, title: '研学项目运营：资源整合与持续迭代' }
]
</script>

<style scoped>
.projects-info {
  padding: 16px 12px 12px;
}

.banner {
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.carousel-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-grid {
  margin-top: 100px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  
}

.info-panel {
  background: #ffffff;
  border-radius: 12px;
  padding: 14px 16px 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  min-height: 220px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.panel-more {
  font-size: 12px;
  color: #909399;
  cursor: pointer;
  user-select: none;
}

.panel-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.panel-item {
  font-size: 15px;
  color: #606266;
  line-height: 1.8;
}

.panel-link {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  text-decoration: none;
  color: inherit;
  border-radius: 8px;
  padding: 2px 4px;
}

.panel-link:hover {
  background: rgba(64, 158, 255, 0.08);
  color: #409eff;
}

.item-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #409eff;
  margin-top: 6px;
  flex-shrink: 0;
}

.item-text {
  flex: 1;
}

@media (max-width: 1024px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>