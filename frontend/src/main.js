import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import './styles/global.scss'
import './api.js'
import { init as initTransport, getMode } from './api.js'
import { state } from './store.js'

// 游戏/工具渲染器注册表
import './games/chess.js'
import './games/pushbox.js'
import './games/tetris.js'
import './games/minesweeper.js'
import './games/gobang.js'
import './games/snake.js'
import './games/sudoku.js'
import './games/game2048.js'
import './games/uno.js'
import './games/monopoly.js'
import './tools/browser.js'
import './tools/reader.js'

;(async function() {
  try {
    await initTransport({ mode: 'auto' })

    const app = createApp(App)
    const pinia = createPinia()
    app.use(pinia)
    app.use(ElementPlus, { locale: zhCn })

    // 注册所有 Element Plus 图标
    for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
      app.component(key, component)
    }

    app.mount('#app')
    console.log('[main.js] Vue 应用已挂载到 #app')
  } catch (e) {
    console.error('[main.js] 初始化失败', e)
    var appEl = document.getElementById('app')
    if (appEl) {
      appEl.innerHTML = '<div style="color:red;padding:20px;white-space:pre-wrap">[main.js] 初始化失败:\n' + (e && e.stack ? e.stack : String(e)) + '</div>'
    }
    // 不 throw，让后续 setTimeout 的 ready 信号仍能发出
  }

  // ready 信号：仅在 JSBridge 模式下发送（WebSocket 模式由 transport 层自行管理连接）
  if (getMode() === 'jsbridge') {
    // setTimeout（宏任务）确保在 onLoadEnd→JSBridge.register()→executeJS 注入 ready() 之后才调用。
    // nextTick 是微任务，在 inline 脚本执行完后立即触发，此时 JSBridge 尚未注入，会静默失败。
    setTimeout(function() {
      try {
        state.currentRoute = state.currentRoute || 'chat'
        console.log('[main.js] 路由已强制刷新为: ' + state.currentRoute)
        if (window.xechat && typeof window.xechat.ready === 'function') {
          window.xechat.ready()
          window.__xechatReadySent = true
          console.log('[main.js] 已发送 ready 信号')
        } else {
          console.log('[main.js] 警告: window.xechat.ready 不可用')
        }
      } catch (e) {
        console.error('[main.js] setTimeout 回调异常', e)
      }
    }, 200)

    // 备用 ready 信号：如果 200ms 时 xechat 尚未注入，1000ms 时再试一次
    setTimeout(function() {
      try {
        if (window.xechat && typeof window.xechat.ready === 'function') {
          if (!window.__xechatReadySent) {
            window.xechat.ready()
            window.__xechatReadySent = true
            console.log('[main.js] 备用 ready 信号已发送')
          }
        } else {
          console.log('[main.js] 警告（1000ms）: window.xechat.ready 仍不可用')
        }
      } catch (e) {
        console.error('[main.js] 备用 setTimeout 回调异常', e)
      }
    }, 1000)
  } else {
    console.log('[main.js] 当前传输模式为 ' + getMode() + '，跳过 JSBridge ready 信号')
  }
})()
