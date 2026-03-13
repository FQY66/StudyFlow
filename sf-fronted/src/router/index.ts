import { createWebHashHistory, createRouter } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import Index from '@/pages/index.vue'
import NotFound from '@/pages/common/NotFound.vue'
import Square from '@/pages/forum/Square.vue'
import Users from '@/pages/system/Users.vue'
import Management from '@/pages/projects/Management.vue'
import News from '@/pages/projects/News.vue'
import NewsDetail from '@/pages/projects/NewsDetail.vue'
import Courses from '@/pages/projects/Courses.vue'
import CoursesDetail from '@/pages/projects/CoursesDetail.vue'
import Login from '@/pages/login/Login.vue'
import Register from '@/pages/login/Register.vue'
import ArticleDetail from '@/pages/projects/ArticleDetail.vue'

const routes = [
  {path: '/login', component: Login},
  { path: '/register', component: Register },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', component: Index },
      { path: 'square', component: Square },
      { path: 'system/users', component: Users },
      { path: 'projects/management', component: Management },
      { path: 'projects/news', component: News },
      { path: 'projects/news/detail/:type/:id', component: NewsDetail },
      { path: 'projects/courses', component: Courses },
      { path: 'projects/courses/:id', component: CoursesDetail },
      { path: 'projects/courses/article/:id', component: ArticleDetail },
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: NotFound }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router