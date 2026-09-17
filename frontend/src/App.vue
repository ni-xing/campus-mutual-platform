<template>
  <div class="site">
    <header class="topbar">
      <div class="bar">
        <router-link class="me" :to="auth.token ? '/me' : '/login'" title="个人中心">
          <span class="me-ava">{{ auth.token ? (auth.user?.nickname || '我')[0] : '生' }}</span>
          <span class="me-label">{{ auth.token ? (auth.user?.nickname || '个人中心') : '个人中心' }}</span>
        </router-link>
        <router-link class="brand" to="/">搭把手<em>。</em></router-link>
        <nav class="nav-links" aria-label="板块导航">
          <router-link to="/market">二手集市</router-link>
          <router-link to="/lost">失物招领</router-link>
          <router-link to="/errand">跑腿拼单</router-link>
          <router-link to="/ai">AI 助手</router-link>
        </nav>
        <span class="env-tag">模拟环境</span>
        <router-link v-if="!auth.token" class="bar-btn" to="/login">登录 / 注册</router-link>
      </div>
    </header>
    <router-view />
  </div>
</template>

<script setup>
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
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

/* 左上角个人中心 */
.me {
  display: flex; align-items: center; gap: 7px;
  padding: 4px 10px 4px 4px;
  border-radius: 20px;
  border: 1.5px solid var(--line);
  background: #fff;
}
.me:hover { border-color: var(--ink); text-decoration: none; }
.me-ava {
  width: 26px; height: 26px; border-radius: 50%;
  background: var(--board); color: #F2F0E8;
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-hand); font-size: 13px; font-weight: 700;
}
.me-label { font-size: 13px; color: var(--charcoal); font-weight: 600; max-width: 72px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.brand { font-family: var(--font-hand); font-weight: 700; font-size: 23px; color: var(--charcoal); }
.brand em { font-style: normal; color: var(--ink); }

.nav-links { display: flex; gap: 20px; margin-left: 6px; font-size: 14.5px; }
.nav-links a { color: var(--charcoal); }
.nav-links a:hover { color: var(--ink); text-decoration: none; }
.nav-links a.router-link-active { color: var(--ink); font-weight: 700; }

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
  .nav-links, .me-label, .brand-sub { display: none; }
}
</style>
