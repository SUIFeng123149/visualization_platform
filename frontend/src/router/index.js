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
    path: '/video/:bvid',
    name: 'videoDetail',
    component: () => import('@/views/VideoDetailPage.vue'),
  },
  {
    path: '/contents',
    name: 'contents',
    component: () => import('@/views/ContentPage.vue'),
  },
  {
    path: '/contents/:contentId',
    name: 'contentDetail',
    component: () => import('@/views/ContentDetailPage.vue'),
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
  {
    path: '/collector',
    name: 'collector',
    component: () => import('@/views/DataCollectorPage.vue'),
  },
  {
    path: '/data-sources',
    name: 'dataSource',
    component: () => import('@/views/DataSourcePage.vue'),
  },
  {
    path: '/reports',
    name: 'reportCenter',
    component: () => import('@/views/ReportCenterPage.vue'),
  },
  {
    path: '/anomaly-rules',
    name: 'anomalyRule',
    component: () => import('@/views/AnomalyRulePage.vue'),
  },
  {
    path: '/ai-assistant',
    name: 'aiAssistant',
    component: () => import('@/views/AiAssistantPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
