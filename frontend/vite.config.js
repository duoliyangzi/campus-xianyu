import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
        // SSE 流式：关闭代理超时缓冲，避免整包到达才转发给浏览器
        timeout: 0,
        proxyTimeout: 0,
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes, req, res) => {
            const url = req.url || ''
            if (url.includes('/ai/chat/stream')) {
              res.setHeader('Cache-Control', 'no-cache, no-transform')
              res.setHeader('X-Accel-Buffering', 'no')
              // 防止压缩导致缓冲
              if (proxyRes.headers['content-encoding']) {
                delete proxyRes.headers['content-encoding']
              }
            }
          })
        },
      },
      '/uploads': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
      },
    },
  },
})
