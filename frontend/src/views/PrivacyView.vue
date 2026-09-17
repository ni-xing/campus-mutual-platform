<template>
  <div class="container-narrow">
    <div class="card-note pin-in">
    <h2 class="page-title">用户协议与隐私政策<span class="tag">模拟</span></h2>
    <div class="doc">
      <p><strong>1. 信息收集</strong>：本平台（校园模拟环境）仅收集注册所必需的学号、校园邮箱与昵称，用于身份标识与账户安全。</p>
      <p><strong>2. 信息使用</strong>：你的学号将脱敏展示（如 2025****01）；邮箱仅用于登录与找回，不对外公开。</p>
      <p><strong>3. 信息存储</strong>：所有数据存储于本地 Docker 环境，仅用于课程演示与学习，不做任何商业用途。</p>
      <p><strong>4. 你的权利</strong>：你可以随时联系管理员注销账户并删除数据。</p>
      <p class="note">本页面为静态模拟文本，用于满足个人信息保护法（PIPL）知情同意要求的最小实现。</p>
    </div>
    <button class="btn" @click="done">我已阅读并返回</button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
function done() { history.length > 1 ? router.back() : router.replace('/login?mode=register') }
</script>

<style scoped>
.tag {
  display: inline-block;
  vertical-align: 4px;
  margin-left: 8px;
  font-size: 12px;
  color: var(--pin);
  border: 2px solid var(--pin);
  border-radius: 4px;
  padding: 0 6px;
  transform: rotate(8deg);
  opacity: .85;
}
.doc { margin-bottom: 18px; }
.doc p {
  font-size: 13.5px;
  line-height: 1.8;
  margin-bottom: 10px;
  color: var(--charcoal);
  padding-left: 12px;
  border-left: 3px solid var(--line);
}
.doc .note {
  color: #8A6A10;
  border-left-color: var(--highlight-deep);
  background: var(--highlight);
  padding: 6px 10px;
  border-radius: 0 8px 8px 0;
  border-left: 3px solid var(--highlight-deep);
}
</style>
