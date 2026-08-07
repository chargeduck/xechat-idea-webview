import { defineStore } from 'pinia'
import { nextTick } from 'vue'

/**
 * chatStore - 消息列表状态管理（Pinia）
 * 替代旧 store.js 中的 messages 相关逻辑。
 */
export const useChatStore = defineStore('chat', {
  state: () => ({
    messages: []
  }),

  getters: {
    isEmpty(state) {
      return state.messages.length === 0
    }
  },

  actions: {
    /**
     * 添加单条消息
     */
    addMessage(text, type) {
      this.messages.push({
        text,
        time: new Date().toLocaleTimeString(),
        type: type || undefined
      })
      if (this.messages.length > 1000) {
        this.messages.splice(0, this.messages.length - 1000)
      }
    },

    /**
     * 批量添加消息
     */
    addMessages(texts) {
      texts.forEach(t => {
        this.messages.push({
          text: t,
          time: new Date().toLocaleTimeString()
        })
      })
      if (this.messages.length > 1000) {
        this.messages.splice(0, this.messages.length - 1000)
      }
    },

    /**
     * 清空消息
     */
    clear() {
      this.messages = []
    }
  }
})
