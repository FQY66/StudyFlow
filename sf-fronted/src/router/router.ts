import { createWebHashHistory, createRouter } from 'vue-router'
import axios from 'axios'
import { API_BASE_URL } from '@/config/api'
import MainLayout from '@/layouts/MainLayout.vue'
import Index from '@/pages/index.vue'
import NotFound from '@/pages/common/NotFound.vue'
import Square from '@/pages/forum/Square.vue'
import ChatWindow from '@/pages/forum/ChatWindow.vue'
import Users from '@/pages/system/Users.vue'
import Management from '@/pages/projects/Management.vue'
import News from '@/pages/projects/News.vue'
import NewsDetail from '@/pages/projects/NewsDetail.vue'
import Courses from '@/pages/projects/Courses.vue'
import CoursesDetail from '@/pages/projects/CoursesDetail.vue'
import Login from '@/pages/login/Login.vue'
import Register from '@/pages/login/Register.vue'
import ArticleDetail from '@/pages/projects/ArticleDetail.vue'
import NewProject from '@/pages/projects/NewProject.vue'
import ProjectDetail from '@/pages/projects/ProjectDetail.vue'
import UploadTest from '@/pages/common/UploadTest.vue'

const routes = [
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', component: Index },
      { path: 'square', component: Square },
      { path: 'square/forum', component: Square },
      { path: 'square/chat', component: ChatWindow },
      { path: 'system/users', component: Users },
      { path: 'projects/management', component: Management },
      { path: 'projects/new', component: NewProject },
      { path: 'projects/detail/:id', component: ProjectDetail },
      { path: 'projects/news', component: News },
      { path: 'projects/news/detail/:type/:id', component: NewsDetail },
      { path: 'projects/courses', component: Courses },
      { path: 'projects/courses/:id', component: CoursesDetail },
      { path: 'projects/courses/article/:id', component: ArticleDetail },
      { path: 'common/upload-test', component: UploadTest }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: NotFound }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(async (to) => {
  // 登录页和注册页允许直接访问
  if (to.path === '/login' || to.path === '/register') {
    return true
  }

  // 1. 从本地存储读取 token，判断是否已登录
  const token = localStorage.getItem('token')
  if (!token) {
    return '/login'
  }

  // 2. 调用后端校验接口，确认 token 是否有效
  try {
    const response = await axios.get(`${API_BASE_URL}/admin/check`, {
      headers: {
        // 后端配置的 token 请求头名为 token
        token
      }
    })

    if (response.data?.code !== 1) {
      localStorage.removeItem('token')
      return '/login'
    }
    return true
  } catch (error) {
    // 校验接口异常时，视为未登录
    localStorage.removeItem('token')
    return '/login'
  }
})

export default router
