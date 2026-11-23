import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 开发环境：前端 5173，后端 8080，通过代理避免跨域（生产环境由 Nginx 或 Spring Boot 静态资源统一处理）
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    open: false,
    // Windows 下某些编辑器/工具用"临时目录+原子写入"保存文件，
    // Vite 的文件监听会因 EBUSY 直接崩溃，这里忽略这些临时目录
    watch: {
      ignored: ['**/.*.tmpdir/**', '**/*.tmp', '**/.git/**']
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 商品图片由后端静态资源映射提供
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500
  }
})
