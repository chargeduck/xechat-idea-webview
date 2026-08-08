import { reactive, nextTick } from 'vue'
import { on, getMode } from './api.js'
import { ElMessage } from 'element-plus'

export const state = reactive({
    online: false,
    username: '',
    onlineCount: 0,
    toolOpen: false,
    currentTool: 0,
    currentRoute: 'chat',
    messages: [],
    tools: [],
    games: [],
    gamePlaying: false,
    currentGameIndex: -1,
    gameStateData: null,
    settingsOpen: false,
    inRoom: false,
    roomId: '',
    roomGameName: '',
    roomGameIndex: -1,
    roomNums: 0,
    isHomeowner: false,
    roomPlayers: [],
    roomReady: false
})

window.addEventListener('hashchange', () => {
    state.currentRoute = location.hash.slice(2) || 'chat'
})
state.currentRoute = location.hash.slice(2) || 'chat'

// ---- 消息处理 ----
// 桥接器：set 后同时写入 Pinia chatStore
export let _chatStoreBridge = null
// 消息缓冲：桥接未建立时缓存 console 消息
let __pendingConsoleMessages = []

export function setChatStoreBridge(store) {
    _chatStoreBridge = store
    // 排空缓冲队列——解决 ChatPanel 挂载前 Java 已推送消息的时序竞态
    if (__pendingConsoleMessages.length > 0) {
        console.log('[store.js] 桥接已建立，排空 ' + __pendingConsoleMessages.length + ' 条缓存消息')
        _chatStoreBridge.addMessages(__pendingConsoleMessages)
        __pendingConsoleMessages = []
    }
}

console.log('[store.js] 脚本加载，准备注册 console 监听器')
on('console', (msgs) => {
    console.log('[store.js] console 回调触发, count=' + msgs.length)

    // 写入 Pinia chatStore（若已桥接）；未就绪时先缓存，setChatStoreBridge 时自动排空
    if (_chatStoreBridge) {
        _chatStoreBridge.addMessages(msgs)
    } else {
        console.log('[store.js] 桥接未就绪，缓存 ' + msgs.length + ' 条消息到队列')
        __pendingConsoleMessages.push(...msgs)
    }

    // 兼容旧 state.messages（其他组件可能引用）
    msgs.forEach(function (m) {
        console.log('[store.js] 追加消息: ' + (typeof m === 'string' ? m.substring(0, 80) : JSON.stringify(m).substring(0, 80)))
        state.messages.push({ text: m, time: new Date().toLocaleTimeString() })
    })
    if (state.messages.length > 1000) {
        state.messages.splice(0, state.messages.length - 1000)
    }
    nextTick(() => {
        const el = document.querySelector('.message-list')
        if (el) el.scrollTop = el.scrollHeight
    })
})

on('toolOpen', () => {
    state.toolOpen = true
    refreshState()
})
on('toolClose', () => {
    state.toolOpen = false
    refreshState()
})
on('gameStart', (data) => {
    state.gamePlaying = true
    state.currentGameIndex = data.gameIndex
    navigate('game')
})
on('gameOver', () => {
    state.gamePlaying = false
    state.currentGameIndex = -1
    resetRoomState()
    navigate('chat')
})
on('gameState', (data) => {
    state.gameStateData = data
})
on('gameRoom', (data) => {
    handleRoomEvent(data)
})
on('message', (data) => {
    const redText = '<font color=red>' + data.text + '</font>'
    if (_chatStoreBridge) {
        _chatStoreBridge.addMessage(redText, 'system')
    }
    state.messages.push({ text: redText, time: new Date().toLocaleTimeString(), type: 'system' })
    nextTick(() => {
        const el = document.querySelector('.message-list')
        if (el) el.scrollTop = el.scrollHeight
    })
})

// 监听 Java NotifyUtils 推送的系统通知事件，弹出 ElMessage
window.addEventListener('xechat:notify', (e) => {
    const { title, content, level } = e.detail || {}
    const msg = title && content ? title + '：' + content : (title || content || '')
    const elLevel = level === 'warn' ? 'warning' : (level === 'error' ? 'error' : 'info')
    ElMessage({ message: msg, type: elLevel, duration: 5000, showClose: true, grouping: true })
})

export function refreshState() {
    try {
        const json = window.xechat.getState()
        const s = JSON.parse(json)
        state.online = s.online
        state.username = s.username
        state.onlineCount = s.onlineCount
        state.toolOpen = s.toolOpen
        state.currentTool = s.currentTool
    } catch (e) { /* ignore */}
}

export function loadTools() {
    try {
        state.tools = JSON.parse(window.xechat.getTools())
    } catch (e) { /* ignore */}
}

export function loadGames() {
    try {
        state.games = JSON.parse(window.xechat.getGameList())
    } catch (e) { /* ignore */}
}

function handleRoomEvent(data) {
    const type = data.type
    switch (type) {
        case 'roomCreated':
        case 'roomOpened': {
            const room = data.data
            state.inRoom = true
            state.roomId = room.id
            state.roomGameName = room.game ? room.game.name : ''
            state.roomNums = room.nums
            state.isHomeowner = room.homeowner && room.homeowner.username === state.username
            state.roomReady = false
            const players = []
            const users = room.users || {}
            Object.keys(users).forEach(key => {
                const p = users[key]
                players.push({ id: p.id, username: p.username, readied: p.readied })
            })
            state.roomPlayers = players
            navigate('room')
            break
        }
        case 'roomClosed':
        case 'gameEnded':
            resetRoomState()
            navigate('chat')
            break
        case 'playerJoined': {
            const exists = state.roomPlayers.find(p => p.username === data.data.username)
            if (!exists) {
                state.roomPlayers.push({ id: data.data.id, username: data.data.username, readied: false })
            }
            break
        }
        case 'playerLeft':
            state.roomPlayers = state.roomPlayers.filter(p => p.username !== data.data.username)
            break
        case 'playerReadied': {
            const rp = state.roomPlayers.find(p => p.username === data.data.username)
            if (rp) rp.readied = true
            if (data.data.username === state.username) state.roomReady = true
            break
        }
    }
}

export function resetRoomState() {
    state.inRoom = false
    state.roomId = ''
    state.roomGameName = ''
    state.roomGameIndex = -1
    state.roomNums = 0
    state.isHomeowner = false
    state.roomPlayers = []
    state.roomReady = false
}

export function navigate(route) {
    state.currentRoute = route
}
