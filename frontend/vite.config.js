import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// dev 代理走 nginx(8088) -> gateway(8080) 全链路，与部署形态一致
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8088', changeOrigin: true },
    },
  },
})
