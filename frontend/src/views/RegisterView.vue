<template>
  <div class="card">
    <h2 class="title">注册</h2>
    <p v-if="err" class="err">{{ err }}</p>
    <div class="field">
      <label>学号（8~12 位数字）</label>
      <input v-model.trim="form.studentNo" placeholder="如 2025010101" />
    </div>
    <div class="field">
      <label>校园邮箱（仅支持 @stu.campus.edu.cn）</label>
      <input v-model.trim="form.email" placeholder="name@stu.campus.edu.cn" />
    </div>
    <div class="field">
      <label>密码（≥8 位，非纯数字）</label>
      <input v-model="form.password" type="password" placeholder="如 Passw0rd123" />
    </div>
    <div class="field">
      <label>昵称（选填）</label>
      <input v-model.trim="form.nickname" placeholder="留空默认取学号后 4 位" />
    </div>
    <label class="agree">
      <input v-model="agree" type="checkbox" />
      我已阅读并同意
      <router-link class="link" to="/privacy" target="_blank">《用户协议与隐私政策》</router-link>
    </label>
    <button class="btn" :disabled="loading" @click="submit">{{ loading ? '注册中…' : '注册并登录' }}</button>
    <p class="foot">已有账号？<router-link class="link" to="/login">去登录</router-link></p>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({ studentNo: '', email: '', password: '', nickname: '' })
const agree = ref(false)
const err = ref('')
const loading = ref(false)

async function submit() {
  err.value = ''
  if (!agree.value) { err.value = '请先阅读并勾选《用户协议与隐私政策》'; return }
  loading.value = true
  try {
    const res = await api('/api/v1/auth/register', 'POST', { ...form, privacyAgreed: agree.value })
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
.agree { display: flex; align-items: center; gap: 6px; font-size: 13px; margin-bottom: 14px; }
.foot { margin-top: 14px; font-size: 13px; color: #666; }
</style>
