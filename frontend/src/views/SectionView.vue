<template>
  <div class="container-narrow">
    <header class="sec-head pin-in">
      <h1 class="hand">{{ info.name }}</h1>
      <p class="sec-desc">{{ info.desc }}</p>
    </header>

    <div class="feed">
      <article
        v-for="(p, i) in list"
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
      <p v-if="list.length === 0" class="empty">这一栏暂时空着，等第一张布告贴上来。</p>
    </div>

    <p class="tip">演示数据 · W2+ 接入真实接口后替换</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { secs, posts } from '../data/posts'

const props = defineProps({ secKey: { type: String, required: true } })

const info = computed(() => secs[props.secKey])
const kindName = computed(() => secs[props.secKey].name)
const list = computed(() => posts.filter((p) => p.kind === kindName.value))
</script>

<style scoped>
.sec-head { margin-bottom: 18px; }
.sec-head h1 { font-family: var(--font-hand); font-size: 26px; font-weight: 700; }
.sec-desc { font-size: 13.5px; color: var(--muted); margin-top: 4px; }

.feed { display: flex; flex-direction: column; gap: 12px; }
.empty { font-size: 13px; color: var(--muted); text-align: center; padding: 18px 0; }
.tip { font-size: 12px; color: var(--muted); margin-top: 20px; }
</style>
