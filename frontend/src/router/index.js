import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import SectionView from '../views/SectionView.vue'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior() { return { top: 0 } },
  routes: [
    { path: '/', component: () => import('../views/LandingView.vue') },
    { path: '/market', component: SectionView, props: { secKey: 'market' } },
    { path: '/lost', component: SectionView, props: { secKey: 'lost' } },
    { path: '/errand', component: SectionView, props: { secKey: 'errand' } },
    { path: '/ai', component: SectionView, props: { secKey: 'ai' } },
    { path: '/login', component: () => import('../views/LoginView.vue') },
    { path: '/privacy', component: () => import('../views/PrivacyView.vue') },
    { path: '/me', component: () => import('../views/PersonalView.vue'), meta: { requireAuth: true } },
    { path: '/orders', component: () => import('../views/OrdersView.vue'), meta: { requireAuth: true } },
  ],
})

// 全局路由守卫（系统设计 §3.5.5）：未登录访问受保护页 → 跳登录
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requireAuth && !auth.token) return '/login'
  if (to.path === '/login' && auth.token) return '/'
})

export default router
