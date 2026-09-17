<template>
  <div v-if="auth.user" class="container-narrow">
    <div class="me-head pin-in">
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

    <p class="tip">W1 最小闭环演示：注册 → 登录 → 个人中心。商品 / 跑腿 / AI 模块按 spec 依次开发中。</p>
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
  router.replace('/')
}
</script>

<style scoped>
.me-head { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.avatar {
  width: 52px; height: 52px; border-radius: 50%;
  background: var(--board); color: #F2F0E8;
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-hand); font-size: 22px; font-weight: 700;
}
.nick { font-family: var(--font-hand); font-size: 20px; font-weight: 700; }
.sub { font-size: 13px; color: var(--muted); margin-top: 2px; }
.stats { display: flex; gap: 8px; margin-bottom: 14px; }
.stat {
  flex: 1; background: #fff;
  border: 1.5px solid var(--line);
  border-radius: 10px; padding: 14px 12px; text-align: center;
}
.num { font-family: var(--font-hand); font-size: 24px; font-weight: 700; }
.num.good { color: var(--board); }
.num.mid { color: #C9A916; }
.num.low { color: var(--pin); }
.lab { font-size: 12px; color: var(--muted); margin-top: 2px; }
.tip { font-size: 12px; color: var(--muted); margin-bottom: 14px; }
</style>
