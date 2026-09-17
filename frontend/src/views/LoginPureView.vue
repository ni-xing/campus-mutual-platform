<template>
  <div class="card">
    <h2 class="title">登录</h2>
    <p v-if="err" class="err">{{ err }}</p>
    <div class="field">
      <label>学号或校园邮箱</label>
      <input v-model.trim="account" placeholder="如 2025010101" />
    </div>
    <div class="field">
      <label>密码</label>
      <input v-model="password" type="password" placeholder="至少 8 位" @keyup.enter="submit" />
    </div>
    <button class="btn" :disabled="loading" @click="submit">{{ loading ? '登录中…' : '登录' }}</button>
    <p class="foot">没有账号？<router-link class="link" to="/login?mode=register">去注册</router-link></p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const account = ref('')
const password = ref('')
const err = ref('')
const loading = ref(false)

async function submit() {
  err.value = ''
  if (!account.value || !password.value) { err.value = '请填写学号和密码'; return }
  loading.value = true
  try {
    const res = await api('/api/v1/auth/login', 'POST', { account: account.value, password: password.value })
    auth.setSession(res.data.token, res.data.user)
    router.replace('/')
  } catch (e) {
    err.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.title { font-size: 18px; margin-bottom: 16px; }
.foot { margin-top: 14px; font-size: 13px; color: #666; }
</style>
