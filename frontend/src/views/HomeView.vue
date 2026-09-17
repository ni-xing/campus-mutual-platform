<template>
  <div class="card" v-if="auth.user">
    <div class="head">
      <div class="avatar">{{ initial }}</div>
      <div>
        <div class="nick">{{ auth.user.nickname }}</div>
        <div class="sub">学号 {{ auth.user.studentNoMasked }}</div>
      </div>
    </div>
    <div class="stats">
      <div class="stat">
        <div class="num" :class="creditClass">{{ auth.user.creditScore }}</div>
        <div class="lab">信用分</div>
      </div>
      <div class="stat">
        <div class="num">0</div>
        <div class="lab">在售商品</div>
      </div>
      <div class="stat">
        <div class="num">0</div>
        <div class="lab">跑腿单</div>
      </div>
    </div>
    <p class="tip">W1 最小闭环演示：注册 → 登录 → 主页。商品/跑腿/AI 模块按 spec 依次开发中。</p>
    <button class="btn btn-ghost" @click="logout">退出登录</button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { api } from '../api'

const router = useRouter()
const auth = useAuthStore()

const initial = computed(() => (auth.user?.nickname || '同')[0])
const creditClass = computed(() => {
  const s = auth.user?.creditScore ?? 0
  return s >= 150 ? 'good' : s >= 80 ? 'mid' : 'low'
})

async function logout() {
  try { await api('/api/v1/auth/logout', 'POST') } catch { /* 已失效则忽略 */ }
  auth.clear()
  router.replace('/login')
}
</script>

<style scoped>
.head { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.avatar { width: 48px; height: 48px; border-radius: 50%; background: #2563eb; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 600; }
.nick { font-size: 16px; font-weight: 600; }
.sub { font-size: 13px; color: #888; margin-top: 2px; }
.stats { display: flex; gap: 8px; margin-bottom: 16px; }
.stat { flex: 1; background: #f8fafc; border: 1px solid #eef0f3; border-radius: 10px; padding: 12px; text-align: center; }
.num { font-size: 20px; font-weight: 700; }
.num.good { color: #16a34a; }
.num.mid { color: #d97706; }
.num.low { color: #dc2626; }
.lab { font-size: 12px; color: #888; margin-top: 2px; }
.tip { font-size: 12px; color: #94a3b8; margin-bottom: 14px; }
</style>
