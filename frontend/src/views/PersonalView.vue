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
        <div class="num">¥{{ balanceText }}</div>
        <div class="lab">可用余额</div>
      </div>
      <div class="stat">
        <div class="num">{{ balance.points ?? 0 }}</div>
        <div class="lab">积分</div>
      </div>
      <div class="stat">
        <div class="num">{{ sellingCount }}</div>
        <div class="lab">在售商品</div>
      </div>
    </div>
    <p v-if="balance.frozen > 0" class="frozen-tip">另有 ¥{{ balance.frozen }} 在交易冻结中（下单后未完成/未取消）</p>

    <!-- 模拟充值（方案 B：点击即到账） -->
    <section class="recharge">
      <h4 class="hand">余额中心 <span class="env-tag">模拟环境</span></h4>
      <div class="quick">
        <button v-for="a in [10, 50, 100]" :key="a" class="chip" :disabled="recharging" @click="recharge(a)">充 ¥{{ a }}</button>
      </div>
      <p v-if="reMsg" class="re-msg" :class="{ err: reErr }">{{ reMsg }}</p>
      <p class="re-note">MVP 模拟支付：点击即到账，不接入真实微信/支付宝，不产生任何真实资金。</p>
    </section>

    <p class="tip">布告栏登录态演示：注册 → 登录 → 个人中心 → 集市发布。订单管理 / 跑腿 / AI 模块按 spec 依次开发中。</p>
    <button class="btn btn-ghost" @click="logout">退出登录</button>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
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

// ---------- 余额 / 积分 / 在售 ----------
const balance = ref({})
const sellingCount = ref(0)
const recharging = ref(false)
const reMsg = ref('')
const reErr = ref(false)

const balanceText = computed(() => {
  const b = Number(balance.value.balance ?? 0)
  return b % 1 === 0 ? String(b) : b.toFixed(2)
})

async function loadWallet() {
  try {
    const res = await api('/api/v1/trade/balance')
    balance.value = res.data || {}
  } catch { /* 服务未起则静默 */ }
  try {
    const res = await api('/api/v1/goods/mine?pageNo=1&pageSize=1')
    sellingCount.value = res.data?.total ?? 0
  } catch { /* 静默 */ }
}

onMounted(loadWallet)

async function recharge(amount) {
  recharging.value = true
  reMsg.value = ''
  reErr.value = false
  try {
    const res = await api('/api/v1/trade/balance/recharge', 'POST', { amount }, {
      'X-Idempotency-Key': 'web-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8),
    })
    balance.value = res.data || {}
    reMsg.value = `已到账 ¥${amount}（模拟）`
  } catch (e) {
    reMsg.value = e.message
    reErr.value = true
  } finally {
    recharging.value = false
  }
}

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
.frozen-tip { font-size: 12px; color: #C9A916; margin: -6px 0 12px; }

.recharge {
  background: #fff;
  border: 1.5px solid var(--line);
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 14px;
}
.recharge h4 { font-size: 16px; margin-bottom: 10px; }
.env-tag {
  font-size: 11px; color: var(--pin);
  border: 1.5px solid var(--pin);
  border-radius: 6px; padding: 0 6px;
  transform: rotate(-4deg); display: inline-block; margin-left: 6px;
}
.quick { display: flex; gap: 8px; margin-bottom: 8px; }
.re-msg { font-size: 12.5px; color: var(--board); margin-bottom: 4px; }
.re-msg.err { color: var(--pin); }
.re-note { font-size: 11.5px; color: var(--muted); border-top: 1px dashed var(--line); padding-top: 8px; }

.tip { font-size: 12px; color: var(--muted); margin-bottom: 14px; }
</style>
