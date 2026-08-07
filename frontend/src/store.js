import { reactive, nextTick } from 'vue'

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

// DEV 环境 mock：浏览器中没有 JCEF 桥接层时提供空壳，避免报错
if (!window.xechat) {
    window.xechat = {
        on() {},
        getState() { return '{}' },
        getTools() { return '[]' },
        getGameList() { return '[]' }
    }
}

// ---- 消息处理 ----
console.log('[store.js] 脚本加载，准备注册 console 监听器')
window.xechat.on('console', (msgs) => {
    console.log('[store.js] xechat:console 回调触发, count=' + msgs.length)
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

window.xechat.on('toolOpen', () => {
    state.toolOpen = true
    refreshState()
})
window.xechat.on('toolClose', () => {
    state.toolOpen = false
    refreshState()
})
window.xechat.on('gameStart', (data) => {
    state.gamePlaying = true
    state.currentGameIndex = data.gameIndex
    navigate('game')
})
window.xechat.on('gameOver', () => {
    state.gamePlaying = false
    state.currentGameIndex = -1
    resetRoomState()
    navigate('chat')
})
window.xechat.on('gameState', (data) => {
    state.gameStateData = data
})
window.xechat.on('gameRoom', (data) => {
    handleRoomEvent(data)
})
window.xechat.on('message', (data) => {
    state.messages.push({ text: data.text, time: new Date().toLocaleTimeString(), type: 'system' })
    nextTick(() => {
        const el = document.querySelector('.message-list')
        if (el) el.scrollTop = el.scrollHeight
    })
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
