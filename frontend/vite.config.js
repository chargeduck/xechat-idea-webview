import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import ElementPlus from 'unplugin-element-plus/vite'
import path from 'path'

const __dirname = path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'))

export default defineConfig({
  plugins: [
    vue(),
    ElementPlus(),
    // 移除 HTML 中的 type="module"，兼容 file:/// 协议
    {
      name: 'remove-module-type',
      transformIndexHtml(html) {
        return html.replace(/\s*type="module"/g, '')
      }
    }
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  base: './',
  css: {
    preprocessorOptions: {
      scss: { api: 'modern-compiler' }
    }
  },
  build: {
    outDir: path.resolve(__dirname, '../xechat-webview-plugin/src/main/resources/web'),
    emptyOutDir: true,
    assetsInlineLimit: 0,
    // IIFE 格式绕过 file:/// 协议的 ES module CORS 限制
    target: 'es2015',
    modulePreload: false,
    rollupOptions: {
      output: {
        format: 'iife',
        inlineDynamicImports: true,
        entryFileNames: 'js/[name].js',
        chunkFileNames: 'js/[name].js',
        assetFileNames: (assetInfo) => {
          if (assetInfo.name?.endsWith('.css')) return 'css/[name].[ext]'
          return 'assets/[name].[ext]'
        }
      }
    }
  },
  server: {
    port: 5173
  }
})
