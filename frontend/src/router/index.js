import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/overview',
  },
  {
    path: '/overview',
    name: 'overview',
    component: () => import('@/views/OverviewPage.vue'),
  },
  {
    path: '/video',
    name: 'video',
    component: () => import('@/views/VideoPage.vue'),
  },
  {
    path: '/danmaku',
    name: 'danmaku',
    component: () => import('@/views/DanmakuPage.vue'),
  },
  {
    path: '/comment',
    name: 'comment',
    component: () => import('@/views/CommentPage.vue'),
  },
  {
    path: '/creator',
    name: 'creator',
    component: () => import('@/views/CreatorPage.vue'),
  },
  {
    path: '/task',
    name: 'task',
    component: () => import('@/views/TaskPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
