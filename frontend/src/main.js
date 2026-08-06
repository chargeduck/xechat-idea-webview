import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import './styles/global.scss'
import './api.js'

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

const app = createApp(App)
app.use(ElementPlus, { locale: zhCn })

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
