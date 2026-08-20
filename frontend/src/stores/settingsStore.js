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
/** 一键隐藏插件默认快捷键：Ctrl+Alt+Shift+H */
export const DEFAULT_SHORTCUT_HIDE = { ctrl: true, alt: true, shift: true, key: 'H' }

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
  /** 一键隐藏插件快捷键：Ctrl+Alt+Shift+H */
  shortcutHide: DEFAULT_SHORTCUT_HIDE
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
  return event.key === s.key
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
      shortcutHide: normalizeShortcut(saved.shortcutHide) || defaults.shortcutHide
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
    setShortcutHide(value) {
      this.shortcutHide = normalizeShortcut(value) || this.shortcutHide
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
        shortcutHide: this.shortcutHide
      })
    }
  }
})
