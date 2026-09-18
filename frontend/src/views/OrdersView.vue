<template>
  <div class="orders-page">
    <header class="page-head pin-in">
      <div class="head-row">
        <h1 class="hand">我经手的单子</h1>
        <span class="count">{{ list.length }} 条</span>
      </div>
      <p class="page-desc">下单即冻结余额（模拟支付），楼下交割完点「确认取货」才算成交；卖家入账，双方各得 1% 积分。</p>
    </header>

    <div class="tabs" role="group">
      <button class="chip" :class="{ on: role === 'buyer' }" @click="switchRole('buyer')">我买的</button>
      <button class="chip" :class="{ on: role === 'seller' }" @click="switchRole('seller')">我卖的</button>
      <span class="spacer"></span>
      <button class="chip" :class="{ on: filter === 'FROZEN' }" @click="filter = 'FROZEN'">进行中</button>
      <button class="chip" :class="{ on: filter === 'DONE' }" @click="filter = 'DONE'">已完结</button>
    </div>

    <p v-if="loading" class="hint">正在取单子…</p>
    <p v-else-if="err" class="hint err">{{ err }}</p>
    <p v-else-if="list.length === 0" class="hint">{{ role === 'buyer' ? '还没买过东西。去集市逛逛？' : '还没人下单你的闲置。' }}</p>

    <div class="feed">
      <article v-for="o in list" :key="o.id" class="order-card pin-in">
        <span class="stamp" :class="stampClass(o.status)">{{ statusLabel[o.status] }}</span>
        <div class="o-head">
          <h4>{{ o.goodsTitle }}</h4>
          <span class="price">¥{{ money(o.goodsPrice) }}</span>
        </div>
        <div class="meta">
          <span class="ono">{{ o.orderNo }}</span>
          <span>{{ role === 'buyer' ? '卖家' : '买家' }} {{ role === 'buyer' ? '同学' + o.sellerId : o.buyerNickname }}</span>
          <span>{{ relTime(o.createdTime) }}</span>
        </div>
        <p v-if="o.remark" class="remark">留言：{{ o.remark }}</p>

        <div class="actions">
          <button v-if="o.status === 'FROZEN'" class="btn yellow sm" @click="act(o, 'complete')">确认取货</button>
          <button v-if="o.status === 'FROZEN'" class="btn-ghost sm" @click="act(o, 'cancel')">取消并退款</button>
          <button v-if="o.status === 'COMPLETED' && !o.reviewedByMe" class="btn yellow sm" @click="openReview(o)">
            {{ role === 'buyer' ? '评价卖家' : '评价买家' }}
          </button>
          <span v-if="o.status === 'COMPLETED' && o.reviewedByMe" class="done-tip">已评价</span>
          <span v-if="o.status === 'CANCELLED'" class="done-tip">已退款回余额</span>
        </div>
      </article>
    </div>

    <p class="tip">模拟环境：本页所有资金变动均为站内余额记账，不涉及真实支付。</p>

    <!-- 评价弹层 -->
    <div v-if="reviewTarget" class="mask" @click.self="reviewTarget = null">
      <form class="rv-card pin-in" @submit.prevent="submitReview">
        <h3 class="hand">给这单打个分</h3>
        <p class="rv-sub">{{ reviewTarget.goodsTitle }}</p>
        <div class="stars">
          <button
            v-for="n in 5" :key="n" type="button"
            class="star" :class="{ on: n <= rv.rating }"
            @click="rv.rating = n"
          >★</button>
          <span class="star-tip">{{ starTip[rv.rating] }}</span>
        </div>
        <label class="field">
          <span>评语（≤ 200 字，可不填）</span>
          <textarea v-model.trim="rv.content" maxlength="200" rows="3" placeholder="东西怎么样？人好不好说话？"></textarea>
        </label>
        <p v-if="rvErr" class="pub-err">{{ rvErr }}</p>
        <p class="rv-note">评分会累积到对方信用分：好评加分，差评减分。</p>
        <div class="pub-actions">
          <button type="button" class="btn-ghost" @click="reviewTarget = null">先不评</button>
          <button type="submit" class="btn yellow" :disabled="rvSubmitting">{{ rvSubmitting ? '提交中…' : '提交评价' }}</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { api } from '../api'

const role = ref('buyer')
const filter = ref('FROZEN')
const orders = ref([])
const loading = ref(false)
const err = ref('')

const statusLabel = { FROZEN: '待取货', COMPLETED: '已完成', CANCELLED: '已取消' }
const starTip = { 0: '', 1: '很差', 2: '一般', 3: '还行', 4: '不错', 5: '很棒' }

const list = computed(() =>
  orders.value.filter((o) =>
    filter.value === 'DONE' ? o.status !== 'FROZEN' : o.status === 'FROZEN'
  )
)

function money(v) {
  const n = Number(v ?? 0)
  return n % 1 === 0 ? String(n) : n.toFixed(2)
}

function relTime(iso) {
  if (!iso) return ''
  const m = Math.floor((Date.now() - new Date(iso).getTime()) / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return m + ' 分钟前'
  if (m < 1440) return Math.floor(m / 60) + ' 小时前'
  if (m < 2880) return '昨天'
  return Math.floor(m / 1440) + ' 天前'
}

function stampClass(status) {
  if (status === 'COMPLETED') return 'ok'
  if (status === 'CANCELLED') return 'gray'
  return ''
}

async function load() {
  loading.value = true
  err.value = ''
  try {
    const res = await api(`/api/v1/orders/mine?role=${role.value}&pageNo=1&pageSize=30`)
    orders.value = res.data?.records || []
  } catch (e) {
    err.value = e.message
    orders.value = []
  } finally {
    loading.value = false
  }
}

function switchRole(r) {
  role.value = r
  load()
}

watch(filter, () => { /* 纯本地筛选 */ })

onMounted(load)

async function act(order, action) {
  const verb = action === 'complete' ? '确认取货' : '取消并退款'
  if (!window.confirm(`确定要${verb}吗？`)) return
  try {
    await api(`/api/v1/orders/${order.id}/${action}`, 'POST')
    await load()
  } catch (e) {
    err.value = e.message
  }
}

// ---------- 评价 ----------
const reviewTarget = ref(null)
const rv = reactive({ rating: 5, content: '' })
const rvErr = ref('')
const rvSubmitting = ref(false)

function openReview(order) {
  reviewTarget.value = order
  rv.rating = 5
  rv.content = ''
  rvErr.value = ''
}

async function submitReview() {
  rvSubmitting.value = true
  rvErr.value = ''
  try {
    await api(`/api/v1/orders/${reviewTarget.value.id}/review`, 'POST', {
      rating: rv.rating,
      content: rv.content || null,
    })
    reviewTarget.value = null
    await load()
  } catch (e) {
    rvErr.value = e.message
  } finally {
    rvSubmitting.value = false
  }
}
</script>

<style scoped>
.orders-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 26px 24px 44px;
  width: 100%;
}
.page-head { margin-bottom: 16px; }
.head-row { display: flex; align-items: baseline; gap: 12px; }
.head-row h1 { font-family: var(--font-hand); font-size: 26px; font-weight: 700; }
.count {
  font-size: 12px; color: var(--ink);
  background: #EAF0FB; border: 1px dashed var(--ink);
  border-radius: 10px; padding: 1px 8px; white-space: nowrap;
}
.page-desc { font-size: 13.5px; color: var(--muted); margin-top: 4px; }

.tabs { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.spacer { flex: 1; }
.chip {
  padding: 5px 14px; border-radius: 16px;
  border: 1.5px solid var(--line); background: #fff;
  font-size: 12.5px; color: var(--muted);
  font-family: var(--font-body); cursor: pointer;
}
.chip:hover { border-color: var(--ink); color: var(--ink); }
.chip.on { background: var(--highlight); border-color: var(--highlight-deep); color: var(--charcoal); font-weight: 700; }

.hint { font-size: 13px; color: var(--muted); text-align: center; padding: 20px 0; }
.hint.err { color: var(--pin); }

.feed { display: flex; flex-direction: column; gap: 12px; }
.order-card {
  position: relative;
  background: #fff;
  border: 1.5px solid var(--line);
  border-radius: 12px;
  padding: 16px 18px;
}
.order-card .stamp {
  position: absolute; top: 12px; right: 14px;
  font-family: var(--font-hand); font-size: 12px; font-weight: 700;
  color: var(--pin); border: 1.5px solid var(--pin);
  border-radius: 6px; padding: 0 6px;
  transform: rotate(-6deg);
}
.order-card .stamp.ok { color: var(--board); border-color: var(--board); }
.order-card .stamp.gray { color: var(--muted); border-color: var(--muted); }
.o-head { display: flex; align-items: baseline; gap: 10px; padding-right: 64px; }
.o-head h4 { font-size: 15.5px; font-weight: 700; }
.o-head .price { font-family: var(--font-hand); font-size: 17px; font-weight: 700; color: var(--pin); }
.meta { display: flex; gap: 14px; font-size: 12px; color: var(--muted); margin-top: 6px; flex-wrap: wrap; }
.meta .ono { font-family: monospace; }
.remark { font-size: 12.5px; color: var(--charcoal); margin-top: 6px; }
.actions { display: flex; gap: 10px; align-items: center; margin-top: 12px; }
.sm { padding: 6px 14px; font-size: 13px; }
.done-tip { font-size: 12.5px; color: var(--muted); }
.tip { font-size: 12px; color: var(--muted); margin-top: 22px; }

/* 评价弹层 */
.mask {
  position: fixed; inset: 0; z-index: 50;
  background: rgba(30, 30, 26, .45);
  display: flex; align-items: center; justify-content: center; padding: 20px;
}
.rv-card {
  background: #fff; border-radius: 14px;
  border: 1.5px solid var(--line);
  box-shadow: 0 10px 30px rgba(30, 30, 26, .18);
  width: 420px; max-width: 100%; padding: 22px 22px 18px;
}
.rv-card h3 { font-size: 20px; margin-bottom: 4px; }
.rv-sub { font-size: 12.5px; color: var(--muted); margin-bottom: 12px; }
.stars { display: flex; align-items: center; gap: 4px; margin-bottom: 12px; }
.star {
  background: none; border: none; cursor: pointer;
  font-size: 26px; line-height: 1; color: #DDD8C8; padding: 0 2px;
}
.star.on { color: var(--highlight-deep); }
.star-tip { font-size: 13px; color: var(--charcoal); margin-left: 8px; font-family: var(--font-hand); }
.pub-err { font-size: 12.5px; color: var(--pin); margin: 8px 0 0; }
.rv-note { font-size: 11.5px; color: var(--muted); border-top: 1px dashed var(--line); padding-top: 8px; margin-top: 10px; }
.pub-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; }
</style>
