import { defineStore } from 'pinia'

/**
 * onlineUsersStore - 在线用户列表状态管理
 * 由 store.js 的 on('onlineUsers') / on('userState') 事件驱动更新
 */
export const useOnlineUsersStore = defineStore('onlineUsers', {
  state: () => ({
    users: [],
    welcomeShown: false
  }),

  getters: {
    count(state) {
      return state.users.length
    }
  },

  actions: {
    /** 全量替换用户列表（ONLINE_USERS） */
    setUsers(userList) {
      this.users = (userList || []).map(u => ({
        uuid: u.uuid || '',
        username: u.username || '',
        status: u.status || '',
        platform: u.platform || '',
        shortRegion: u.shortRegion || ''
      }))
    },

    /** 添加或更新一个用户（USER_STATE） */
    upsertUser(user) {
      if (!user || !user.username) return
      const entry = {
        uuid: user.uuid || '',
        username: user.username,
        status: user.status || '',
        platform: user.platform || '',
        shortRegion: user.shortRegion || ''
      }
      const idx = this.users.findIndex(u => u.username === entry.username)
      if (idx >= 0) {
        this.users[idx] = entry
      } else {
        this.users.push(entry)
      }
    },

    /** 移除一个用户（USER_STATE 下线） */
    removeUser(username) {
      if (!username) return
      this.users = this.users.filter(u => u.username !== username)
    },

    markWelcomeShown() {
      this.welcomeShown = true
    },

    clear() {
      this.users = []
      this.welcomeShown = false
    }
  }
})
