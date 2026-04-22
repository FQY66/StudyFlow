import { createWebHashHistory, createRouter } from 'vue-router'
import axios from 'axios'
import { API_BASE_URL } from '@/config/api'
import AdminLayout from '@/layouts/Admin.vue'
import StudentLayout from '@/layouts/Student.vue'
import Index from '@/pages/index.vue'
import NotFound from '@/pages/common/NotFound.vue'
import Square from '@/pages/forum/Square.vue'
import ChatWindow from '@/pages/forum/ChatWindow.vue'
import Users from '@/pages/system/Users.vue'
import UserDetail from '@/pages/system/UserDetail.vue'
import SystemProject from '@/pages/system/Project.vue'
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
    component: AdminLayout,
    children: [
      { path: '', component: Index },
      { path: 'square', component: Square },
      { path: 'square/forum', component: Square },
      { path: 'square/chat', component: ChatWindow },
      { path: 'system/users', component: Users },
      { path: 'system/users/detail', component: UserDetail },
      { path: 'system/project', component: SystemProject },
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
  {
    path: '/student',
    component: StudentLayout,
    children: [
      { path: '', redirect: '/student/projects/news' },
      { path: 'projects/management', component: Management },
      { path: 'projects/new', component: NewProject },
      { path: 'square/forum', component: Square },
      { path: 'square/chat', component: ChatWindow },
      { path: 'projects/news', component: News },
      { path: 'projects/news/detail/:type/:id', component: NewsDetail },
      { path: 'projects/courses', component: Courses },
      { path: 'projects/courses/:id', component: CoursesDetail },
      { path: 'projects/courses/article/:id', component: ArticleDetail },
      { path: 'projects/detail/:id', component: ProjectDetail },
    ]
  },
  {
    path: '/student/projects/detail/:id',
    component: StudentLayout,
    children: [
      { path: '', component: ProjectDetail }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: NotFound }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

const adminOnlyPaths = [
  '/system',
  '/projects/management',
  '/projects/new',
  '/projects/detail',
  '/common/upload-test',
]

const studentAllowedPaths = [
  '/student',
  '/student/projects/management',
  '/student/projects/new',
  '/student/square/forum',
  '/student/square/chat',
  '/student/projects/news',
  '/student/projects/news/detail',
  '/student/projects/courses',
  '/student/projects/courses/',
  '/student/projects/courses/article/',
  '/student/projects/detail/',
  '/student/sf/ai',
]

function isPathMatched(path: string, rules: string[]) {
  return rules.some((rule) => path === rule || path.startsWith(rule))
}

function isAllowedPathByRole(path: string, role: string) {
  if (role === '学生') {
    return isPathMatched(path, studentAllowedPaths)
  }

  if (role === '老师') {
    return !isPathMatched(path, adminOnlyPaths)
  }

  return true
}

function isAdminOnlyPath(path: string) {
  return isPathMatched(path, adminOnlyPaths)
}

router.beforeEach(async (to) => {
  if (to.path === '/login' || to.path === '/register') {
    return true
  }

  const token = sessionStorage.getItem('token') || localStorage.getItem('token')
  if (!token) {
    return '/login'
  }

  const role = sessionStorage.getItem('userRole') || sessionStorage.getItem('role') || localStorage.getItem('userRole') || localStorage.getItem('role') || '学生'

  if (!isAllowedPathByRole(to.path, role)) {
    return role === '学生' ? '/student/projects/news' : '/'
  }

  if (role === '学生' && isAdminOnlyPath(to.path)) {
    return '/student/projects/news'
  }

  try {
    const response = await axios.get(`${API_BASE_URL}/admin/check`, {
      headers: {
        token
      }
    })

    if (response.data?.code !== 1) {
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('userRole')
      return '/login'
    }
    return true
  } catch (error) {
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('userRole')
    return '/login'
  }
})

export default router
