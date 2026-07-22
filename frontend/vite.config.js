import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true,
      },
      '/actuator': {
        target: apiProxyTarget,
        changeOrigin: true,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('echarts')) return 'vendor-echarts'
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'vendor-element'
          return 'vendor'
        },
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
