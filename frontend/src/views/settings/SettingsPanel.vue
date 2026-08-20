<template>
  <div class="settings-panel">
    <div class="panel-header">
      <span class="panel-title">设置</span>
    </div>
    <div class="settings-body">
      <div class="setting-row">
        <span>主题</span>
        <el-switch
          :model-value="isDark"
          @change="toggleTheme()"
          active-text="暗色"
          inactive-text="亮色"
        />
      </div>
      <div class="setting-row">
        <span>插件透明度</span>
        <div class="slider-wrap">
          <el-slider
            v-model="pluginOpacity"
            :min="0"
            :max="1"
            :step="0.05"
            :format-tooltip="(v) => Math.round(v * 100) + '%'"
            style="width: 130px"
          />
          <span class="slider-value">{{ Math.round(pluginOpacity * 100) }}%</span>
        </div>
      </div>
      <div class="setting-row">
        <span>游戏面板透明度</span>
        <div class="slider-wrap">
          <el-slider
            v-model="gamePanelOpacity"
            :min="0.1"
            :max="1"
            :step="0.05"
            :format-tooltip="(v) => Math.round(v * 100) + '%'"
            style="width: 130px"
          />
          <span class="slider-value">{{ Math.round(gamePanelOpacity * 100) }}%</span>
        </div>
        <div style="font-size: 11px; color: var(--el-text-color-secondary); margin-top: 4px; width: 100%; text-align: right">
          游戏播放时面板透明度
        </div>
      </div>
      <div class="setting-block">
        <div class="block-title">快捷键</div>
        <div v-for="item in shortcutItems" :key="item.type" class="shortcut-row">
          <span>{{ item.label }}</span>
          <div class="shortcut-actions">
            <button
              class="shortcut-btn"
              :class="{ recording: recordingShortcut === item.type }"
              @click="startRecording(item.type)"
            >
              {{ recordingShortcut === item.type ? '按下新组合键...' : formatShortcut(settingsStore[item.getter]) }}
            </button>
            <button class="shortcut-reset" title="恢复默认" @click="settingsStore[item.setter](DEFAULT_SHORTCUTS[item.type])">默认</button>
          </div>
        </div>
        <div v-if="recordingShortcut" class="shortcut-hint">
          请按下新的组合键（支持 Ctrl/Alt/Shift 组合），按 Esc 取消
        </div>
      </div>
      <div class="setting-block">
        <div class="block-title">紧急模式</div>
        <div class="setting-row">
          <span style="color: var(--text-secondary); font-size: 12px">按下快捷键全屏显示伪装内容（图片或网页），再按一次恢复摸鱼页面</span>
        </div>
        <div class="setting-row">
          <span>紧急内容（图片 URL / 网页地址）</span>
          <el-input
            v-model="localPanicUrl"
            placeholder="例如 https://example.com/xx.png 或 https://example.com"
            clearable
            style="width: 100%; margin-top: 4px"
            @change="onPanicUrlChange"
          />
        </div>
        <div style="font-size: 11px; color: var(--el-text-color-secondary)">
          以 .png/.jpg/.gif/.webp 等图片扩展名结尾的地址按图片展示，其余按网页加载
        </div>
      </div>
      <el-form-item label="通信方式">
        <el-radio-group v-model="localTransportMode" @change="onTransportModeChange" size="small">
          <el-radio value="auto">自动检测</el-radio>
          <el-radio value="jsbridge">JSBridge</el-radio>
          <el-radio value="websocket">WebSocket</el-radio>
        </el-radio-group>
        <div style="font-size: 11px; color: var(--el-text-color-secondary); margin-top: 4px">
          当前：{{ currentTransportMode }} | 切换后需刷新页面生效
        </div>
      </el-form-item>
      <div class="setting-row">
        <span>在线状态</span>
        <el-tag :type="state.online ? 'success' : 'info'" size="small">
          {{ state.online ? '已连接' : '未连接' }}
        </el-tag>
      </div>
      <div v-if="state.username" class="setting-row">
        <span>用户名</span>
        <span style="color:var(--text-secondary)">{{ state.username }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { state } from '@/store.js'
import { isDark, toggleTheme } from '@/composables/useTheme.js'
import { getConfiguredMode } from '@/api.js'
import {
  useSettingsStore,
  formatShortcut,
  normalizeShortcut,
  DEFAULT_SHORTCUT_PLUS,
  DEFAULT_SHORTCUT_MINUS,
  DEFAULT_SHORTCUT_THEME,
  DEFAULT_SHORTCUT_OPACITY_0,
  DEFAULT_SHORTCUT_OPACITY_1,
  DEFAULT_SHORTCUT_PANIC
} from '@/stores/settingsStore.js'

const settingsStore = useSettingsStore()
const currentTransportMode = ref(getConfiguredMode())

const pluginOpacity = computed({
  get: () => settingsStore.pluginOpacity,
  set: (val) => settingsStore.setPluginOpacity(val)
})

const gamePanelOpacity = computed({
  get: () => settingsStore.gamePanelOpacity,
  set: (val) => settingsStore.setGamePanelOpacity(val)
})

/** 快捷键配置驱动表：类型数组驱动录制 UI 与默认值，避免复制粘贴 */
const DEFAULT_SHORTCUTS = {
  minus: DEFAULT_SHORTCUT_MINUS,
  plus: DEFAULT_SHORTCUT_PLUS,
  theme: DEFAULT_SHORTCUT_THEME,
  opacity0: DEFAULT_SHORTCUT_OPACITY_0,
  opacity1: DEFAULT_SHORTCUT_OPACITY_1,
  panic: DEFAULT_SHORTCUT_PANIC
}

const shortcutItems = [
  { type: 'minus', label: '降低透明度', getter: 'shortcutMinus', setter: 'setShortcutMinus' },
  { type: 'plus', label: '提高透明度', getter: 'shortcutPlus', setter: 'setShortcutPlus' },
  { type: 'opacity0', label: '透明度 0%（全透明）', getter: 'shortcutOpacity0', setter: 'setShortcutOpacity0' },
  { type: 'opacity1', label: '透明度 100%', getter: 'shortcutOpacity1', setter: 'setShortcutOpacity1' },
  { type: 'theme', label: '切换主题', getter: 'shortcutTheme', setter: 'setShortcutTheme' },
  { type: 'panic', label: '紧急模式（老板键）', getter: 'shortcutPanic', setter: 'setShortcutPanic' }
]

/** 快捷键录制态：null | 'minus' | 'plus' | 'opacity0' | 'opacity1' | 'theme' | 'panic' */
const recordingShortcut = ref(null)

function startRecording(type) {
  if (recordingShortcut.value === type) {
    stopRecording()
    return
  }
  recordingShortcut.value = type
  window.__xechatShortcutRecording = true
}

function stopRecording() {
  recordingShortcut.value = null
  window.__xechatShortcutRecording = false
}

function onShortcutKeydown(e) {
  if (!recordingShortcut.value) return
  e.preventDefault()
  e.stopPropagation()
  if (e.key === 'Escape') {
    stopRecording()
    return
  }
  // 忽略纯修饰键（等待用户按下主键）
  if (e.key === 'Control' || e.key === 'Alt' || e.key === 'Shift' || e.key === 'Meta') return
  const shortcut = {
    ctrl: e.ctrlKey,
    alt: e.altKey,
    shift: e.shiftKey,
    key: e.key
  }
  // 至少保留一个修饰键，避免覆盖系统默认按键
  if (!shortcut.ctrl && !shortcut.alt && !shortcut.shift) return
  const normalized = normalizeShortcut(shortcut)
  if (!normalized || !normalized.key) return
  const item = shortcutItems.find(i => i.type === recordingShortcut.value)
  if (item) settingsStore[item.setter](normalized)
  stopRecording()
}

onMounted(() => {
  window.addEventListener('keydown', onShortcutKeydown, true)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onShortcutKeydown, true)
  window.__xechatShortcutRecording = false
})

/** 紧急模式内容 URL：本地输入框绑定，change 时落库 */
const localPanicUrl = ref(settingsStore.panicUrl)

function onPanicUrlChange(val) {
  settingsStore.setPanicUrl(val || '')
}

const localTransportMode = computed({
  get: () => settingsStore.transportMode,
  set: (val) => settingsStore.setTransportMode(val)
})

function onTransportModeChange(val) {
  currentTransportMode.value = val
}
</script>

<style scoped>
.settings-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-color);
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.settings-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  overflow-y: auto;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  flex-wrap: wrap;
  gap: 4px;
}

.slider-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.slider-value {
  min-width: 36px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: right;
}

.setting-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.block-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.shortcut-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  gap: 8px;
}

.shortcut-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.shortcut-btn {
  min-width: 130px;
  padding: 4px 10px;
  font-size: 12px;
  color: var(--text-primary, #e8eaed);
  background: var(--bg-tertiary, #2a2e35);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s;
  text-align: center;
}

.shortcut-btn:hover {
  border-color: var(--accent-color);
  color: var(--accent-color);
}

.shortcut-btn.recording {
  border-color: #e6a23c;
  color: #e6a23c;
  animation: shortcut-blink 1s infinite;
}

.shortcut-reset {
  padding: 4px 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s;
}

.shortcut-reset:hover {
  color: var(--accent-color);
  border-color: var(--accent-color);
}

.shortcut-hint {
  font-size: 11px;
  color: #e6a23c;
}

@keyframes shortcut-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
