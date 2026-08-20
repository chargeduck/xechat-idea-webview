<template>
  <el-container class="app-container" :style="{ opacity: pluginOpacity }">
    <el-aside width="48px" class="app-sidebar">
      <div class="nav-buttons">
        <div
            class="nav-btn"
            :class="{ active: currentRoute === 'chat' }"
            @click="state.currentRoute = 'chat'"
            title="聊天"
        >
          <el-icon :size="20">
            <ChatDotRound/>
          </el-icon>
        </div>
        <div
            class="nav-btn"
            :class="{ active: currentRoute === 'tools' }"
            @click="state.currentRoute = 'tools'"
            title="工具"
        >
          <el-icon :size="20">
            <Tools/>
          </el-icon>
        </div>
        <div
            class="nav-btn"
            :class="{ active: currentRoute === 'games' || currentRoute === 'room' }"
            @click="state.currentRoute = 'games'"
            title="游戏"
        >
          <el-icon :size="20">
            <Trophy/>
          </el-icon>
        </div>
        <div
            class="nav-btn"
            :class="{ active: currentRoute === 'users' }"
            @click="state.currentRoute = 'users'"
            title="在线列表"
        >
          <el-icon :size="20">
            <UserFilled/>
          </el-icon>
        </div>
      </div>
      <div class="nav-bottom">
        <div class="nav-btn" :class="{ active: currentRoute === 'settings' }" @click="state.currentRoute = 'settings'" title="设置">
          <el-icon :size="18">
            <Setting/>
          </el-icon>
        </div>
      </div>
    </el-aside>

    <el-main class="app-main">
      <ChatPanel v-if="currentRoute === 'chat'"/>
      <ToolPanel v-else-if="currentRoute === 'tools'"/>
      <GamePanel v-else-if="currentRoute === 'games'"/>
      <RoomPanel v-else-if="currentRoute === 'room'"/>
      <OnlineUsersPanel v-else-if="currentRoute === 'users'"/>
      <StyleEditor v-else-if="currentRoute === 'style-editor'"/>
      <SettingsPanel v-else-if="currentRoute === 'settings'"/>
    </el-main>
  </el-container>
</template>

<script setup>
import {computed, onBeforeUnmount, onMounted} from 'vue'
import {state} from './store.js'
import {isDark, toggleTheme} from '@/composables/useTheme.js'
import { useSettingsStore, matchShortcut } from '@/stores/settingsStore.js'
import ChatPanel from '@/views/chat/ChatPanel.vue'
import ToolPanel from '@/views/tools/ToolPanel.vue'
import GamePanel from '@/views/game/GamePanel.vue'
import RoomPanel from '@/views/chat/RoomPanel.vue'
import OnlineUsersPanel from '@/views/chat/OnlineUsersPanel.vue'
import StyleEditor from '@/views/settings/StyleEditor.vue'
import SettingsPanel from '@/views/settings/SettingsPanel.vue'

const currentRoute = computed(() => state.currentRoute)
const settingsStore = useSettingsStore()
const pluginOpacity = computed(() => settingsStore.pluginOpacity)

/** 全局快捷键：长按连续调节插件透明度（每次 ±0.05，节流 60ms） */
const SHORTCUT_STEP = 0.05
const SHORTCUT_INTERVAL = 60
let lastShortcutAdjustTime = 0

function clampOpacity(value) {
  return Math.min(1, Math.max(0.1, Math.round(value * 100) / 100))
}

function isEditableTarget(el) {
  if (!el) return false
  const tag = el.tagName
  return tag === 'INPUT' || tag === 'TEXTAREA' || el.isContentEditable === true
}

/** 一键隐藏插件：按平台桥接分发（JCEF/IDEA 或 VSCode webview） */
function hidePlugin() {
  // JCEF / IDEA 桥接
  if (window.xechat && typeof window.xechat.execCommand === 'function') {
    window.xechat.execCommand('#hideToolWindow')
    console.log('[shortcut] 已调用 execCommand("#hideToolWindow")')
    return
  }
  // VSCode webview
  if (typeof window.acquireVsCodeApi === 'function') {
    const vscodeApi = window.acquireVsCodeApi()
    vscodeApi.postMessage({ type: 'hidePanel' })
    console.log('[shortcut] 已向 VSCode 发送 hidePanel 消息')
    return
  }
  // DEV 环境兜底提示
  console.log('[shortcut] 当前环境不支持一键隐藏插件（无 JCEF/VSCode 桥接）')
}

function onGlobalKeydown(e) {
  // 快捷键录制期间（设置面板）不响应
  if (window.__xechatShortcutRecording) return
  // 输入/可编辑区域聚焦时不触发
  if (isEditableTarget(document.activeElement)) return

  // 一键切换主题：直接响应一次，不节流
  if (matchShortcut(settingsStore.shortcutTheme, e)) {
    e.preventDefault()
    toggleTheme()
    return
  }

  // 一键隐藏插件：直接响应一次
  if (matchShortcut(settingsStore.shortcutHide, e)) {
    e.preventDefault()
    hidePlugin()
    return
  }

  let delta = 0
  if (matchShortcut(settingsStore.shortcutMinus, e)) delta = -SHORTCUT_STEP
  else if (matchShortcut(settingsStore.shortcutPlus, e)) delta = SHORTCUT_STEP
  if (!delta) return

  // 节流：模拟 Windows 音量键的长按连续调节
  const now = Date.now()
  if (now - lastShortcutAdjustTime < SHORTCUT_INTERVAL) return
  lastShortcutAdjustTime = now

  e.preventDefault()
  settingsStore.setPluginOpacity(clampOpacity(settingsStore.pluginOpacity + delta))
}

onMounted(() => {
  window.addEventListener('keydown', onGlobalKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onGlobalKeydown)
})
</script>

<style lang="scss" scoped>
.app-container {
  height: 100%;
  background: var(--bg-primary);
}

.app-sidebar {
  width: 48px !important;
  min-width: 48px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 8px 0;
  overflow: hidden;
}

.nav-buttons {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.nav-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.nav-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.15s;

  &:hover {
    background: var(--bg-tertiary);
    color: var(--text-primary);
  }

  &.active {
    background: var(--bg-tertiary);
    color: var(--accent-color);
  }
}

.app-main {
  flex: 1;
  padding: 0;
  overflow: hidden;
}
</style>
