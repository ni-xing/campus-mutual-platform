<template>
  <div class="site">
    <header class="topbar">
      <div class="bar">
        <router-link class="brand" to="/">搭把手<em>。</em></router-link>
        <span class="brand-sub">校园互助布告栏</span>
        <nav v-if="isLanding" class="nav-links" aria-label="主导航">
          <a href="#market">二手集市</a>
          <a href="#lost">失物招领</a>
          <a href="#errand">跑腿拼单</a>
          <a href="#ai">AI 助手</a>
        </nav>
        <span class="env-tag">模拟环境</span>
        <router-link v-if="!auth.token" class="bar-btn" to="/login">登录 / 注册</router-link>
        <router-link v-else class="bar-btn" to="/board">进入布告栏</router-link>
      </div>
    </header>
    <router-view />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const auth = useAuthStore()
const isLanding = computed(() => route.path === '/')
</script>

<style>
.site { min-height: 100vh; display: flex; flex-direction: column; }
.topbar {
  position: sticky; top: 0; z-index: 50;
  background: rgba(251, 249, 243, .92);
  backdrop-filter: blur(6px);
  border-bottom: 2px solid var(--charcoal);
}
.bar {
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 14px;
  height: 60px;
}
.brand { font-family: var(--font-hand); font-weight: 700; font-size: 23px; color: var(--charcoal); }
.brand em { font-style: normal; color: var(--ink); }
.brand-sub { font-size: 12.5px; color: var(--muted); }
.nav-links { display: flex; gap: 20px; margin-left: 18px; font-size: 14.5px; }
.nav-links a { color: var(--charcoal); }
.nav-links a:hover { color: var(--ink); text-decoration: none; }
.env-tag {
  margin-left: auto;
  font-size: 12px;
  color: var(--ink);
  background: #EAF0FB;
  border: 1px dashed var(--ink);
  padding: 1px 8px;
  border-radius: 10px;
  white-space: nowrap;
}
.bar-btn {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: var(--ink);
  border: 1.5px solid var(--ink-deep);
  box-shadow: 2px 2px 0 var(--ink-deep);
  padding: 6px 14px;
  border-radius: 7px;
  white-space: nowrap;
}
.bar-btn:hover { background: var(--ink-deep); text-decoration: none; }

/* 内页窄容器 */
.container-narrow {
  max-width: 520px;
  margin: 0 auto;
  padding: 26px 16px 44px;
  width: 100%;
}

@media (max-width: 760px) {
  .nav-links, .brand-sub { display: none; }
}
</style>
