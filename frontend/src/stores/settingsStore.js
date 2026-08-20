import { defineStore } from 'pinia'

const SETTINGS_KEY = 'xechat_settings_v1'

function loadSettings() {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY)
    if (!raw) return null
    return JSON.parse(raw)
  } catch {
    return null
  }
}

function saveSettings(data) {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(data))
}

/** 提高透明度默认快捷键：Ctrl+Alt+Shift+'+' */
export const DEFAULT_SHORTCUT_PLUS = { ctrl: true, alt: true, shift: true, key: '+' }
/** 降低透明度默认快捷键：Ctrl+Alt+Shift+'-' */
export const DEFAULT_SHORTCUT_MINUS = { ctrl: true, alt: true, shift: true, key: '-' }
/** 一键切换主题默认快捷键：Ctrl+Alt+Shift+T */
export const DEFAULT_SHORTCUT_THEME = { ctrl: true, alt: true, shift: true, key: 'T' }
export const DEFAULT_SHORTCUT_OPACITY_0 = { ctrl: true, alt: true, shift: true, key: '0' }
export const DEFAULT_SHORTCUT_OPACITY_1 = { ctrl: true, alt: true, shift: true, key: '1' }
/** 紧急模式默认快捷键：Ctrl+Alt+Shift+P */
export const DEFAULT_SHORTCUT_PANIC = { ctrl: true, alt: true, shift: true, key: 'P' }

const defaults = {
  transportMode: 'auto',
  /** 插件整体透明度：0.1 ~ 1，默认 1（不透明） */
  pluginOpacity: 1,
  /** 游戏面板透明度：0.1 ~ 1，默认 1（不透明），GamePanel 播放浮层使用，与设置面板联动 */
  gamePanelOpacity: 1,
  /** 提高透明度快捷键：Ctrl+Alt+Shift+'+' */
  shortcutPlus: DEFAULT_SHORTCUT_PLUS,
  /** 降低透明度快捷键：Ctrl+Alt+Shift+'-' */
  shortcutMinus: DEFAULT_SHORTCUT_MINUS,
  /** 一键切换主题快捷键：Ctrl+Alt+Shift+T */
  shortcutTheme: DEFAULT_SHORTCUT_THEME,
  /** 透明度 0% 快捷键：Ctrl+Alt+Shift+0 */
  shortcutOpacity0: DEFAULT_SHORTCUT_OPACITY_0,
  /** 透明度 100% 快捷键：Ctrl+Alt+Shift+1 */
  shortcutOpacity1: DEFAULT_SHORTCUT_OPACITY_1,
  /** 紧急模式快捷键：Ctrl+Alt+Shift+9 */
  shortcutPanic: DEFAULT_SHORTCUT_PANIC,
  /** 紧急模式内容：图片 URL 或网页地址，空则不展示内容 */
  panicUrl: ''
}

/** 解析辅助：将存储值规整为 { ctrl, alt, shift, key }，非法返回 null */
export function normalizeShortcut(raw) {
  if (!raw || typeof raw !== 'object') return null
  return {
    ctrl: !!raw.ctrl,
    alt: !!raw.alt,
    shift: !!raw.shift,
    key: typeof raw.key === 'string' && raw.key ? raw.key : ''
  }
}

/** 解析辅助：格式化组合键用于展示，如 "Ctrl+Alt+Shift+-" */
export function formatShortcut(raw) {
  const s = normalizeShortcut(raw)
  if (!s || !s.key) return '未设置'
  const parts = []
  if (s.ctrl) parts.push('Ctrl')
  if (s.alt) parts.push('Alt')
  if (s.shift) parts.push('Shift')
  parts.push(s.key === ' ' ? 'Space' : s.key)
  return parts.join('+')
}

/** 解析辅助：判断 keydown 事件是否命中某组合键（修饰键严格匹配，主键忽略纯修饰键） */
export function matchShortcut(raw, event) {
  const s = normalizeShortcut(raw)
  if (!s || !s.key) return false
  if (s.key === 'Control' || s.key === 'Alt' || s.key === 'Shift' || s.key === 'Meta') return false
  if (!!event.ctrlKey !== s.ctrl) return false
  if (!!event.altKey !== s.alt) return false
  if (!!event.shiftKey !== s.shift) return false
  if (event.key === s.key) return true
  // 数字键兼容：Ctrl+Alt+Shift 组合下 event.key 可能变为符号（如 ')' 对应 0），用 e.code 兜底
  if (/^[0-9]$/.test(s.key) && event.code === 'Digit' + s.key) return true
  return false
}

export const useSettingsStore = defineStore('settings', {
  state: () => {
    const saved = loadSettings() || {}
    return {
      transportMode: saved.transportMode ?? defaults.transportMode,
      pluginOpacity: saved.pluginOpacity ?? defaults.pluginOpacity,
      gamePanelOpacity: saved.gamePanelOpacity ?? defaults.gamePanelOpacity,
      shortcutPlus: normalizeShortcut(saved.shortcutPlus) || defaults.shortcutPlus,
      shortcutMinus: normalizeShortcut(saved.shortcutMinus) || defaults.shortcutMinus,
      shortcutTheme: normalizeShortcut(saved.shortcutTheme) || defaults.shortcutTheme,
      shortcutOpacity0: normalizeShortcut(saved.shortcutOpacity0) || defaults.shortcutOpacity0,
      shortcutOpacity1: normalizeShortcut(saved.shortcutOpacity1) || defaults.shortcutOpacity1,
      shortcutPanic: normalizeShortcut(saved.shortcutPanic) || defaults.shortcutPanic,
      panicUrl: saved.panicUrl ?? defaults.panicUrl
    }
  },

  actions: {
    setTransportMode(mode) {
      this.transportMode = mode
      this.persist()
    },
    setPluginOpacity(value) {
      this.pluginOpacity = value
      this.persist()
    },
    setGamePanelOpacity(value) {
      this.gamePanelOpacity = value
      this.persist()
    },
    setShortcutPlus(value) {
      this.shortcutPlus = normalizeShortcut(value) || this.shortcutPlus
      this.persist()
    },
    setShortcutMinus(value) {
      this.shortcutMinus = normalizeShortcut(value) || this.shortcutMinus
      this.persist()
    },
    setShortcutTheme(value) {
      this.shortcutTheme = normalizeShortcut(value) || this.shortcutTheme
      this.persist()
    },
    setShortcutOpacity0(value) {
      this.shortcutOpacity0 = normalizeShortcut(value) || this.shortcutOpacity0
      this.persist()
    },
    setShortcutOpacity1(value) {
      this.shortcutOpacity1 = normalizeShortcut(value) || this.shortcutOpacity1
      this.persist()
    },
    setShortcutPanic(value) {
      this.shortcutPanic = normalizeShortcut(value) || this.shortcutPanic
      this.persist()
    },
    setPanicUrl(value) {
      this.panicUrl = (value || '').trim()
      this.persist()
    },
    persist() {
      saveSettings({
        transportMode: this.transportMode,
        pluginOpacity: this.pluginOpacity,
        gamePanelOpacity: this.gamePanelOpacity,
        shortcutPlus: this.shortcutPlus,
        shortcutMinus: this.shortcutMinus,
        shortcutTheme: this.shortcutTheme,
        shortcutOpacity0: this.shortcutOpacity0,
        shortcutOpacity1: this.shortcutOpacity1,
        shortcutPanic: this.shortcutPanic,
        panicUrl: this.panicUrl
      })
    }
  }
})
