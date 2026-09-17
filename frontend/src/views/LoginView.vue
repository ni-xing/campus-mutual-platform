<template>
  <RegisterView v-if="isRegister" />
  <LoginView v-else />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import LoginView from './LoginPureView.vue'
import RegisterView from './RegisterView.vue'

const route = useRoute()
const isRegister = computed(() => route.query.mode === 'register')
</script>
