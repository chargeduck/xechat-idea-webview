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

const defaults = {
  transportMode: 'auto'
}

export const useSettingsStore = defineStore('settings', {
  state: () => {
    const saved = loadSettings()
    return {
      transportMode: (saved && saved.transportMode) || defaults.transportMode
    }
  },

  actions: {
    setTransportMode(mode) {
      this.transportMode = mode
      saveSettings({ transportMode: this.transportMode })
    }
  }
})
