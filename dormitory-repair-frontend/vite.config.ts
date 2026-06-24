import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    // Increase chunk size warning limit for a project of this size
    chunkSizeWarningLimit: 400,
    rollupOptions: {
      output: {
        // Split vendor dependencies for better caching
        manualChunks(id: string) {
          if (id.includes('node_modules/vue') || id.includes('node_modules/pinia') || id.includes('node_modules/vue-router')) {
            return 'vendor-vue'
          }
          if (id.includes('node_modules/axios') || id.includes('node_modules/@stomp') || id.includes('node_modules/sockjs')) {
            return 'vendor-http'
          }
          if (id.includes('node_modules/@capacitor')) {
            return 'vendor-ui'
          }
        }
      }
    }
  }
})
