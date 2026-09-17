<template>
  <div v-if="auth.user" class="home container-narrow">
    <!-- 问候区 -->
    <div class="greet pin-in">
      <div class="greet-line">{{ greeting }}，{{ auth.user.nickname }}。</div>
      <div class="greet-sub">{{ auth.user.studentNoMasked }} · 信用 {{ auth.user.creditScore }} 分 · 楼下的布告栏有人看</div>
    </div>

    <!-- 搜索 -->
    <div class="search">
      <input v-model.trim="keyword" placeholder="搜二手、失物、拼单…" aria-label="搜索布告" />
    </div>

    <!-- 快捷入口 -->
    <nav class="quick" aria-label="快捷入口">
      <button class="q q-hot" @click="filterKind('二手集市')"><i>🧺</i>二手集市</button>
      <button class="q" @click="filterKind('失物招领')"><i>📣</i>失物招领</button>
      <button class="q" @click="filterKind('跑腿拼单')"><i>🛵</i>跑腿拼单</button>
      <button class="q" @click="filterKind('')"><i>🤖</i>看全部</button>
    </nav>

    <!-- 楼下新帖 -->
    <div class="sec-title">
      <b>{{ activeKind || '楼下新帖' }}</b>
      <span class="demo-tag">演示数据</span>
    </div>
    <div class="feed">
      <article
        v-for="(p, i) in feed"
        :key="i"
        class="post pin-in"
        :class="{ sticky: p.urgent }"
        :style="{ animationDelay: i * 0.08 + 's' }"
      >
        <span v-if="p.urgent" class="stamp">急</span>
        <span class="kind">{{ p.kind }}</span>
        <h4>{{ p.title }}</h4>
        <div class="row">
          <span :class="{ price: p.price }">{{ p.price ? '¥' + p.price : p.left }}</span>
          <span>{{ p.right }}</span>
        </div>
      </article>
      <p v-if="feed.length === 0" class="empty">这一栏暂时空着，等第一张布告贴上来。</p>
    </div>

    <!-- 我的（楼栋信用） -->
    <div class="mine">
      <div class="sec-title"><b>我的</b></div>
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
      <p class="tip">W1 最小闭环演示：注册 → 登录 → 主页。布告流为静态演示，商品 / 跑腿 / AI 模块按 spec 依次开发中。</p>
      <button class="btn btn-ghost" @click="logout">退出登录</button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { api } from '../api'

const router = useRouter()
const auth = useAuthStore()

const keyword = ref('')
const activeKind = ref('')

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const creditClass = computed(() => {
  const s = auth.user?.creditScore ?? 0
  return s >= 150 ? 'good' : s >= 80 ? 'mid' : 'low'
})

// 演示布告流：静态数据，W2+ 接入真实接口后替换
const posts = [
  { kind: '失物招领', title: '捡到一张校园卡（尾号 2107）', left: '二食堂门口', right: '8 分钟前', urgent: true },
  { kind: '二手集市', title: '《数据结构与算法》第九版，9 成新', price: 15, left: '3 号楼 · 可自取', right: '刚发布' },
  { kind: '跑腿拼单', title: '周六 10:00 拼车去高铁站，还差 2 人', left: '人均 ¥18 · 东门集合', right: '2 小时前' },
  { kind: '二手集市', title: '小米台灯 Pro，用了半年', price: 59, left: '5 号楼 · 可小刀', right: '今天' },
  { kind: '失物招领', title: '一食堂三楼捡到蓝牙耳机，白色充电盒', left: '一食堂三楼', right: '昨天', urgent: true },
  { kind: '跑腿拼单', title: '今晚 6 点拼奶茶，满 4 杯免配送费', left: '还差 1 人', right: '昨天' },
]

const feed = computed(() =>
  posts.filter((p) => {
    const okKind = !activeKind.value || p.kind === activeKind.value
    const okText = !keyword.value || p.title.includes(keyword.value)
    return okKind && okText
  })
)

function filterKind(kind) {
  activeKind.value = kind === activeKind.value ? '' : kind
}

async function logout() {
  try { await api('/api/v1/auth/logout', 'POST') } catch { /* 已失效则忽略 */ }
  auth.clear()
  router.replace('/')
}
</script>

<style scoped>
.greet { margin-bottom: 4px; }
.greet-line { font-family: var(--font-hand); font-size: 22px; font-weight: 700; }
.greet-sub { font-size: 12px; color: var(--muted); margin-top: 2px; }

.search { margin: 14px 0; }
.search input {
  width: 100%;
  padding: 10px 14px;
  background: #fff;
  border: 1.5px solid var(--line);
  border-radius: 10px;
  font-size: 13.5px;
  font-family: var(--font-body);
  color: var(--charcoal);
}
.search input:focus { outline: none; border-color: var(--ink); box-shadow: 0 0 0 3px rgba(36, 65, 143, .12); }
.search input::placeholder { color: #B4AFA0; }

.quick { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; margin-bottom: 18px; }
.q {
  padding: 10px 0 8px;
  border-radius: 10px;
  background: #fff;
  border: 1.5px solid var(--line);
  font-size: 12px;
  color: var(--charcoal);
  font-family: var(--font-body);
  cursor: pointer;
  transition: transform .12s;
}
.q:hover { transform: translateY(-1px); }
.q i { display: block; font-style: normal; font-size: 19px; margin-bottom: 3px; }
.q-hot { background: var(--highlight); border-color: var(--highlight-deep); font-weight: 600; }

.sec-title { display: flex; align-items: baseline; gap: 8px; padding: 2px 0 10px; }
.sec-title b { font-family: var(--font-hand); font-size: 17px; }
.demo-tag { font-size: 11px; color: var(--muted); border: 1px dashed var(--line); border-radius: 8px; padding: 0 6px; }

.feed { display: flex; flex-direction: column; gap: 12px; margin-bottom: 24px; }
.empty { font-size: 13px; color: var(--muted); text-align: center; padding: 18px 0; }

.mine { border-top: 2px dashed var(--line); padding-top: 14px; }
.stats { display: flex; gap: 8px; margin-bottom: 14px; }
.stat {
  flex: 1;
  background: #fff;
  border: 1.5px solid var(--line);
  border-radius: 10px;
  padding: 12px;
  text-align: center;
}
.num { font-family: var(--font-hand); font-size: 22px; font-weight: 700; }
.num.good { color: var(--board); }
.num.mid { color: #C9A916; }
.num.low { color: var(--pin); }
.lab { font-size: 12px; color: var(--muted); margin-top: 2px; }
.tip { font-size: 12px; color: var(--muted); margin-bottom: 14px; }
</style>
