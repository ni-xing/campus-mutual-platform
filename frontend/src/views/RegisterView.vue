<template>
  <div class="container-narrow">
    <div class="card-note pin-in">
    <h2 class="page-title">新同学，<span class="mark">留个名</span>吧。</h2>
    <p class="page-sub">校园邮箱验证身份，楼栋就是信用。</p>
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
      <span>我已阅读并同意
        <router-link class="link" to="/privacy" target="_blank">《用户协议与隐私政策》</router-link>
      </span>
    </label>
    <button class="btn yellow" :disabled="loading" @click="submit">{{ loading ? '注册中…' : '注册并登录' }}</button>
    <p class="foot">已经有账号了？<router-link class="link" to="/login">去登录</router-link></p>
    </div>
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
    router.replace('/board')
  } catch (e) {
    err.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.mark {
  background: var(--highlight);
  padding: 0 8px;
  display: inline-block;
  transform: rotate(-1.2deg);
  box-shadow: 2px 3px 0 rgba(43, 42, 36, .18);
}
.agree {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 14px;
  color: var(--charcoal);
}
.agree input { margin-top: 4px; accent-color: var(--ink); }
.foot { margin-top: 14px; font-size: 13px; color: var(--muted); }
</style>
