<template>
  <div class="sec-page">
    <header class="sec-head pin-in">
      <div class="head-row">
        <h1 class="hand">{{ info.name }}</h1>
        <span class="count">{{ activeList.length }} 条进行中</span>
      </div>
      <p class="sec-desc">{{ info.desc }}</p>
    </header>

    <div class="layout">
      <!-- 主栏：筛选 + 布告流 -->
      <div class="main">
        <div class="chips" role="group" aria-label="筛选布告状态">
          <button
            v-for="c in chips"
            :key="c.key"
            class="chip"
            :class="{ on: filter === c.key }"
            @click="filter = c.key"
          >{{ c.label }}</button>
          <button
            v-if="secKey === 'market' && categoryChips.length"
            v-for="c in categoryChips" :key="'cat-' + c.key"
            class="chip cat"
            :class="{ on: catFilter === c.key }"
            @click="catFilter = c.key"
          >{{ c.label }}</button>
        </div>

        <div class="feed">
          <article
            v-for="(p, i) in list"
            :key="(p.demo ? 'demo-' : 'real-') + i"
            class="post pin-in"
            :class="{ sticky: p.urgent, finished: p.done, demo: p.demo }"
            :style="{ animationDelay: Math.min(i, 6) * 0.06 + 's' }"
          >
            <span v-if="p.urgent" class="stamp">急</span>
            <span v-if="p.done" class="stamp done-stamp">{{ doneLabel[secKey] }}</span>
            <span class="kind">{{ p.kind }}</span>
            <h4 :class="{ strike: p.done }">{{ p.title }}</h4>
            <p v-if="p.desc" class="desc">{{ p.desc }}</p>
            <div class="row">
              <span :class="{ price: p.price }">{{ p.price ? '¥' + p.price : p.left }}</span>
              <span>{{ p.right }}</span>
            </div>
            <div v-if="!p.demo && !p.done" class="post-actions">
              <button class="btn yellow sm" :disabled="buyingId === p.id" @click="buy(p)">
                {{ buyingId === p.id ? '下单中…' : '我要了（模拟支付）' }}
              </button>
              <span class="bal-hint">余额 ¥{{ balanceText }}</span>
            </div>
          </article>
          <p v-if="list.length === 0" class="empty">{{ emptyText[filter] }}</p>
        </div>
      </div>

      <!-- 侧栏 -->
      <aside class="side">
        <section class="cta-panel">
          <h3>{{ ctaCopy[secKey] }}</h3>
          <p>贴上去，对面楼的人就能看到。留言和交割都在楼里完成，不抽成。</p>
          <button v-if="secKey === 'market' && auth.token" class="btn yellow cta-btn" @click="openPublish">贴一张布告</button>
          <router-link v-else class="btn yellow cta-btn" to="/login?mode=register">贴一张布告</router-link>
        </section>

        <section class="side-card">
          <h4 class="hand">今日速报</h4>
          <ul class="speed">
            <li v-for="(n, i) in sideNotes[secKey]" :key="i">{{ n }}</li>
          </ul>
        </section>

        <section class="side-card">
          <h4 class="hand">楼栋信用榜</h4>
          <div v-for="(r, i) in creditBoard" :key="i" class="credit-row">
            <span class="ava">{{ r.name[0] }}</span>
            <span class="credit-info"><b>{{ r.name }}</b><small>{{ r.building }} · 交易 {{ r.deals }} 次 · 好评 {{ r.rate }}</small></span>
          </div>
          <p class="credit-note">楼栋就是信用，交易越多越靠谱。</p>
        </section>

        <section class="side-card">
          <h4 class="hand">贴布告三步</h4>
          <ol class="howto">
            <li>拍张照，写两行</li>
            <li>等人搭把手</li>
            <li>楼下交割</li>
          </ol>
        </section>
      </aside>
    </div>

    <p class="tip">{{ tipText }}</p>

    <!-- 发布布告弹层（仅 market） -->
    <div v-if="showPublish" class="mask" @click.self="showPublish = false">
      <form class="pub-card pin-in" @submit.prevent="submitPublish">
        <h3 class="hand">贴一张布告</h3>
        <p class="pub-sub">写清楚是什么、几成新、多少钱，等人来搭把手。</p>
        <label class="field">
          <span>标题（≤ 50 字）</span>
          <input v-model.trim="form.title" maxlength="50" required placeholder="例：《数据结构与算法》第九版，9 成新" />
        </label>
        <div class="pub-row">
          <label class="field">
            <span>价格（¥）</span>
            <input v-model.number="form.price" type="number" min="0.01" max="9999" step="0.01" required placeholder="15" />
          </label>
          <label class="field">
            <span>分类</span>
            <select v-model="form.category">
              <option v-for="c in categoryChips" :key="c.key" :value="c.key">{{ c.label }}</option>
            </select>
          </label>
        </div>
        <label class="field">
          <span>描述（≤ 500 字，可不填）</span>
          <textarea v-model.trim="form.description" maxlength="500" rows="3" placeholder="成色、自取地点、能不能小刀……"></textarea>
        </label>
        <p v-if="pubErr" class="pub-err">{{ pubErr }}</p>
        <div class="pub-actions">
          <button type="button" class="btn-ghost" @click="showPublish = false">先不贴</button>
          <button type="submit" class="btn yellow" :disabled="publishing">{{ publishing ? '贴上去…' : '贴上去' }}</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { secs, posts, sideNotes } from '../data/posts'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'

const props = defineProps({ secKey: { type: String, required: true } })
const auth = useAuthStore()

const info = computed(() => secs[props.secKey])
const kindName = computed(() => secs[props.secKey].name)

const chips = [
  { key: 'all', label: '全部' },
  { key: 'open', label: '进行中' },
  { key: 'done', label: '已完结' },
]
const filter = ref('all')
const catFilter = ref('all')

const categoryChips = [
  { key: 'all', label: '全部分类' },
  { key: 'BOOK', label: '教材书籍' },
  { key: 'DIGITAL', label: '数码电器' },
  { key: 'DAILY', label: '生活日用' },
  { key: 'SPORT', label: '运动健身' },
  { key: 'OTHER', label: '其他' },
]

const doneLabel = {
  market: '已出',
  lost: '已找到',
  errand: '已完成',
  ai: '已归档',
}
const emptyText = {
  all: '这一栏暂时空着，等第一张布告贴上来。',
  open: '暂时没有进行中的布告，你来贴第一张？',
  done: '还没有完结的布告。',
}
const ctaCopy = {
  market: '有闲置要出？贴上来',
  lost: '丢了东西或捡到东西？贴上来',
  errand: '顺路捎一个，或喊人搭把手',
  ai: '让 AI 帮你盯着布告栏',
}

const creditBoard = [
  { name: '林同学', building: '3 号楼', deals: 12, rate: '100%' },
  { name: '王同学', building: '5 号楼', deals: 9, rate: '100%' },
  { name: '李同学', building: '2 号楼', deals: 7, rate: '96%' },
]

// ---------- market 真实数据（W2）：失败/未登录自动回落演示数据 ----------
const realGoods = ref([])
const realLoaded = ref(false)

function relTime(iso) {
  if (!iso) return ''
  const diff = Date.now() - new Date(iso).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return m + ' 分钟前'
  if (m < 60 * 24) return Math.floor(m / 60) + ' 小时前'
  if (m < 60 * 24 * 2) return '昨天'
  return Math.floor(m / 60 / 24) + ' 天前'
}

async function loadGoods() {
  if (props.secKey !== 'market' || !auth.token) return
  try {
    const res = await api('/api/v1/goods?pageNo=1&pageSize=20')
    realGoods.value = (res.data?.records || []).map((g) => ({
      id: g.id,
      title: g.title,
      desc: g.description || '',
      price: Number(g.price),
      kind: '二手集市',
      cat: g.category,
      sellerId: g.sellerId,
      left: '布告栏在售 · 可下单',
      right: relTime(g.createdTime),
    }))
    realLoaded.value = true
  } catch { /* 服务未起/网络异常 → 演示数据兜底 */ }
  await loadBalance()
}

// ---------- 下单（模拟支付：下单即冻结余额） ----------
const buyingId = ref(null)
const balance = ref(0)
const balanceText = computed(() => {
  const b = Number(balance.value ?? 0)
  return b % 1 === 0 ? String(b) : b.toFixed(2)
})

async function loadBalance() {
  if (!auth.token) return
  try {
    const res = await api('/api/v1/trade/balance')
    balance.value = Number(res.data?.balance ?? 0)
  } catch { /* 静默 */ }
}

async function buy(goods) {
  if (!window.confirm(`确认买下「${goods.title}」？¥${goods.price} 将从余额冻结（模拟支付）`)) return
  buyingId.value = goods.id
  try {
    await api('/api/v1/orders', 'POST', { goodsId: goods.id }, {
      'X-Idempotency-Key': 'web-order-' + goods.id + '-' + Date.now(),
    })
    await loadGoods()
  } catch (e) {
    window.alert(e.message)
  } finally {
    buyingId.value = null
  }
}

onMounted(loadGoods)

// market 的 demo 帖默认视为已出，避免和真实在售混淆
const demoPosts = computed(() =>
  posts
    .filter((p) => p.kind === kindName.value)
    .map((p) => ({ ...p, demo: true, done: props.secKey === 'market' ? true : p.done }))
)

const activeList = computed(() => list.value.filter((p) => !p.done))

const list = computed(() => {
  const base = props.secKey === 'market' && realLoaded.value ? [...realGoods.value, ...demoPosts.value] : posts.filter((p) => p.kind === kindName.value)
  return base.filter((p) => {
    if (props.secKey === 'market' && realLoaded.value && p.cat && catFilter.value !== 'all' && p.cat !== catFilter.value) return false
    if (filter.value === 'open') return !p.done
    if (filter.value === 'done') return p.done
    return true
  })
})

const tipText = computed(() => {
  if (props.secKey === 'market' && realLoaded.value) return '上面的帖子来自布告栏实时数据 · 带灰底的为演示数据'
  return '演示数据 · 登录后 market 板块展示真实商品'
})

// ---------- 发布布告（market） ----------
const showPublish = ref(false)
const publishing = ref(false)
const pubErr = ref('')
const form = reactive({ title: '', price: null, category: 'BOOK', description: '' })

function openPublish() {
  pubErr.value = ''
  showPublish.value = true
}

async function submitPublish() {
  if (!form.title || !form.price || form.price <= 0) {
    pubErr.value = '标题和价格得填好（价格要大于 0）'
    return
  }
  publishing.value = true
  pubErr.value = ''
  try {
    await api('/api/v1/goods', 'POST', {
      title: form.title,
      price: Number(form.price),
      category: form.category,
      description: form.description || null,
    })
    showPublish.value = false
    form.title = ''
    form.price = null
    form.description = ''
    await loadGoods()
  } catch (e) {
    pubErr.value = e.message
  } finally {
    publishing.value = false
  }
}
</script>

<style scoped>
.sec-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 26px 24px 44px;
  width: 100%;
}

.sec-head { margin-bottom: 18px; }
.head-row { display: flex; align-items: baseline; gap: 12px; }
.head-row h1 { font-family: var(--font-hand); font-size: 26px; font-weight: 700; }
.count {
  font-size: 12px; color: var(--ink);
  background: #EAF0FB; border: 1px dashed var(--ink);
  border-radius: 10px; padding: 1px 8px; white-space: nowrap;
}
.sec-desc { font-size: 13.5px; color: var(--muted); margin-top: 4px; }

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 28px;
  align-items: start;
}

/* ---------- 主栏 ---------- */
.chips { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.chip {
  padding: 5px 14px; border-radius: 16px;
  border: 1.5px solid var(--line); background: #fff;
  font-size: 12.5px; color: var(--muted);
  font-family: var(--font-body); cursor: pointer;
}
.chip:hover { border-color: var(--ink); color: var(--ink); }
.chip.on {
  background: var(--highlight); border-color: var(--highlight-deep);
  color: var(--charcoal); font-weight: 700;
}
.chip.cat { border-style: dashed; }

.feed { display: flex; flex-direction: column; gap: 12px; }
.post.finished { opacity: .68; }
.post.finished h4.strike { text-decoration: line-through; text-decoration-thickness: 1.5px; }
.done-stamp {
  border-color: var(--board); color: var(--board);
  transform: rotate(-6deg);
}
.post.demo { background-image: linear-gradient(rgba(0,0,0,.018) 1px, transparent 1px); background-size: 100% 3px; }
.post .desc { font-size: 12.5px; color: var(--muted); margin: 2px 0 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.post-actions { display: flex; align-items: center; gap: 10px; margin-top: 10px; }
.post-actions .sm { padding: 5px 13px; font-size: 12.5px; }
.bal-hint { font-size: 12px; color: var(--muted); }
.empty { font-size: 13px; color: var(--muted); text-align: center; padding: 18px 0; }

/* ---------- 侧栏 ---------- */
.side { display: flex; flex-direction: column; gap: 16px; }

.cta-panel {
  background: var(--board); color: #F2F0E8;
  border-radius: 14px; padding: 20px 18px;
  background-image:
    linear-gradient(rgba(255, 255, 255, .045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, .045) 1px, transparent 1px);
  background-size: 28px 28px;
}
.cta-panel h3 { font-family: var(--font-hand); font-size: 19px; margin-bottom: 6px; }
.cta-panel p { font-size: 12.5px; color: #C9D6CE; margin-bottom: 14px; }
.cta-btn { width: auto; padding: 8px 20px; font-size: 14px; border: none; cursor: pointer; }

.side-card {
  background: #fff;
  border: 1.5px solid var(--line);
  border-radius: 12px;
  padding: 14px 16px;
}
.side-card h4 { font-size: 16px; font-weight: 700; margin-bottom: 10px; }

.speed { list-style: none; }
.speed li {
  font-size: 12.5px; color: var(--charcoal);
  padding: 6px 0 6px 16px;
  position: relative;
  border-bottom: 1px dashed var(--line);
}
.speed li:last-child { border-bottom: none; }
.speed li::before {
  content: "●"; position: absolute; left: 0; top: 8px;
  color: var(--highlight-deep); font-size: 9px;
}

.credit-row { display: flex; align-items: center; gap: 10px; padding: 6px 0; }
.ava {
  width: 30px; height: 30px; border-radius: 50%;
  background: var(--board); color: #F2F0E8;
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-hand); font-size: 14px; flex-shrink: 0;
}
.credit-info b { font-size: 13px; display: block; }
.credit-info small { font-size: 11.5px; color: var(--muted); }
.credit-note { font-size: 11.5px; color: var(--muted); margin-top: 8px; border-top: 1px dashed var(--line); padding-top: 8px; }

.howto { margin: 0; padding-left: 20px; }
.howto li {
  font-size: 13px; color: var(--charcoal);
  padding: 4px 0;
  font-family: var(--font-hand);
}
.howto li::marker { color: var(--ink); font-weight: 700; }

.tip { font-size: 12px; color: var(--muted); margin-top: 22px; }

/* ---------- 发布弹层 ---------- */
.mask {
  position: fixed; inset: 0; z-index: 50;
  background: rgba(30, 30, 26, .45);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}
.pub-card {
  background: #fff; border-radius: 14px;
  border: 1.5px solid var(--line);
  box-shadow: 0 10px 30px rgba(30, 30, 26, .18);
  width: 420px; max-width: 100%;
  padding: 22px 22px 18px;
}
.pub-card h3 { font-size: 20px; margin-bottom: 4px; }
.pub-sub { font-size: 12.5px; color: var(--muted); margin-bottom: 14px; }
.pub-row { display: flex; gap: 10px; }
.pub-row .field { flex: 1; }
.pub-row input, .pub-row select { width: 100%; }
.pub-err { font-size: 12.5px; color: var(--pin); margin: 8px 0 0; }
.pub-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; }

/* ---------- responsive ---------- */
@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .side { order: 2; }
}
</style>
