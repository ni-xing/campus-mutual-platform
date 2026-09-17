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
        </div>

        <div class="feed">
          <article
            v-for="(p, i) in list"
            :key="i"
            class="post pin-in"
            :class="{ sticky: p.urgent, finished: p.done }"
            :style="{ animationDelay: Math.min(i, 6) * 0.06 + 's' }"
          >
            <span v-if="p.urgent" class="stamp">急</span>
            <span v-if="p.done" class="stamp done-stamp">{{ doneLabel[secKey] }}</span>
            <span class="kind">{{ p.kind }}</span>
            <h4 :class="{ strike: p.done }">{{ p.title }}</h4>
            <div class="row">
              <span :class="{ price: p.price }">{{ p.price ? '¥' + p.price : p.left }}</span>
              <span>{{ p.right }}</span>
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
          <router-link class="btn yellow cta-btn" to="/login?mode=register">贴一张布告</router-link>
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

    <p class="tip">演示数据 · W2+ 接入真实接口后替换</p>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { secs, posts, sideNotes } from '../data/posts'

const props = defineProps({ secKey: { type: String, required: true } })

const info = computed(() => secs[props.secKey])
const kindName = computed(() => secs[props.secKey].name)

const chips = [
  { key: 'all', label: '全部' },
  { key: 'open', label: '进行中' },
  { key: 'done', label: '已完结' },
]
const filter = ref('all')

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

const activeList = computed(() => posts.filter((p) => p.kind === kindName.value && !p.done))
const list = computed(() =>
  posts.filter((p) => {
    if (p.kind !== kindName.value) return false
    if (filter.value === 'open') return !p.done
    if (filter.value === 'done') return p.done
    return true
  })
)
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
.chips { display: flex; gap: 8px; margin-bottom: 16px; }
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

.feed { display: flex; flex-direction: column; gap: 12px; }
.post.finished { opacity: .68; }
.post.finished h4.strike { text-decoration: line-through; text-decoration-thickness: 1.5px; }
.done-stamp {
  border-color: var(--board); color: var(--board);
  transform: rotate(-6deg);
}
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
.cta-btn { width: auto; padding: 8px 20px; font-size: 14px; }

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

/* ---------- responsive ---------- */
@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
  .side { order: 2; }
}
</style>
