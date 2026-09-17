import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../views/LoginView.vue') },
    { path: '/privacy', component: () => import('../views/PrivacyView.vue') },
    { path: '/', component: () => import('../views/HomeView.vue'), meta: { requireAuth: true } },
  ],
})

// 全局路由守卫（系统设计 §3.5.5）：未登录访问受保护页 → 跳登录
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requireAuth && !auth.token) return '/login'
  if (to.path === '/login' && auth.token) return '/'
})

export default router
