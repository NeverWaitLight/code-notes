import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', component: () => import('../pages/VideoListPage.vue') },
  { path: '/upload', component: () => import('../pages/UploadPage.vue') },
  { path: '/play/:id', component: () => import('../pages/PlayerPage.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 无鉴权需求，所有路由直接放行
// 如需添加鉴权，可在此处实现路由守卫逻辑

export default router
