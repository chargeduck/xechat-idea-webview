import { reactive, nextTick } from 'vue'
import { on, getMode } from './api.js'
import { ElMessage } from 'element-plus'
import { useOnlineUsersStore } from './stores/onlineUsersStore.js'

/**
 * 检测 JSBridge console 消息是否为系统消息。
 * console 消息是纯文本，需按关键词特征判定：
 * - "SYSTEM_MSG::" 前缀 → 系统消息
 * - "[xxx]进入了鱼塘" / "[xxx]离开了鱼塘" → 进出提示
 * - 含 "系统消息"/"系统公告"/"系统提示"/"系统通知" → 系统消息
 * - 服务端公告类："当前服务端版本" / "公告" / "版本过低" / "版本更新"
 * 其余视为普通用户消息。
 */
function _isConsoleSystemMsg(text) {
    if (typeof text !== 'string') return false
    // 系统消息前缀（ConsoleAction.formatSystemMsg 使用）
    if (text.indexOf('SYSTEM_MSG::') === 0) return true
    // 进出鱼塘提示
    if (/^\[.+\]进入/.test(text) || /^\[.+\]离开/.test(text)) return true
    // 显式系统关键词
    if (/系统消息|系统公告|系统提示|系统通知/.test(text)) return true
    // 服务端版本类公告
    if (/当前服务端版本|服务端版本过低|服务端版本更新|公告/.test(text)) return true
    return false
}

// 状态映射
function _statusText(s) {
    switch (s) {
        case 'FISHING': return '鱼'
        case 'WORKING': return '忙'
        case 'PLAYING': return '玩'
        default: return s || ''
    }
}

// 平台图标
function _platformIcon(p) {
    switch (p) {
        case 'IDEA': return '☕'
        case 'WEB':
        case 'VSCODE': return '🌐'
        default: return ''
    }
}

/**
 * 格式化用户消息头：[时间][地区] 昵称 (状态) 平台：
 * 例如：[08/09 00:28][鲁] test (鱼) 🌐： 123
 */
function _formatUserMessageHead(data) {
    var time = data.time || ''
    var region = (data.user && data.user.shortRegion) || ''
    var username = (data.user && data.user.username) || ''
    var status = (data.user && data.user.status) ? _statusText(data.user.status) : ''
    var platform = (data.user && data.user.platform) ? _platformIcon(data.user.platform) : ''

    var parts = []
    if (time) parts.push('[' + time + ']')
    if (region) parts.push('[' + region + ']')
    var userInfo = username
    if (status) userInfo += ' (' + status + ')'
    if (platform) userInfo += ' ' + platform
    parts.push(userInfo + '：')
    return parts.join(' ')
}

/**
 * 合并 Java ConsoleAction 拆成两条推送的用户消息。
 * "USER_NAME::[08/09 00:36][鲁] test1(鱼) ♠:" + "123"
 *   → "USER_NAME::[08/09 00:36][鲁] test1(鱼) ♠: 123"
 */
function _mergeConsoleMessages(msgs) {
    if (!msgs || msgs.length <= 1) return msgs || []
    var result = []
    for (var i = 0; i < msgs.length; i++) {
        var m = msgs[i]
        if (typeof m === 'string' && m.indexOf('USER_NAME::') === 0 && i + 1 < msgs.length) {
            // 下一项是此行对应的消息内容，合并为一行，并去掉 USER_NAME:: 前缀
            var next = msgs[i + 1]
            if (typeof next === 'string' && next.indexOf('USER_NAME::') !== 0) {
                result.push(m.replace('USER_NAME::', '') + ' ' + next)
                i++ // 跳过已合并的内容行
                continue
            }
        }
        // 单独出现的 USER_NAME:: 头（无后续内容）也去掉前缀
        if (typeof m === 'string' && m.indexOf('USER_NAME::') === 0) {
            result.push(m.replace('USER_NAME::', ''))
        } else {
            result.push(m)
        }
    }
    return result
}

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
        _chatStoreBridge.addMessagesRaw(__pendingConsoleMessages)
        __pendingConsoleMessages = []
    }
}

console.log('[store.js] 脚本加载，准备注册 console 监听器')
on('console', (msgs) => {
    console.log('[store.js] console 回调触发, count=' + msgs.length)

    // 合并相邻的 USER_NAME::头 + 内容 为单条消息（Java ConsoleAction 拆成两条推送）
    var merged = _mergeConsoleMessages(msgs)

    // 检测系统消息：JSBridge 模式下消息为纯文本，需按特征判定类型；系统消息去除 SYSTEM_MSG:: 前缀
    var parsed = merged.map(function(m) {
        var isSystem = _isConsoleSystemMsg(m)
        var text = isSystem ? m.replace(/^SYSTEM_MSG::/, '') : m
        return { text: text, type: isSystem ? 'system' : undefined }
    })

    // 写入 Pinia chatStore（若已桥接）；未就绪时先缓存，setChatStoreBridge 时自动排空
    if (_chatStoreBridge) {
        _chatStoreBridge.addMessagesRaw(parsed)
    } else {
        console.log('[store.js] 桥接未就绪，缓存 ' + parsed.length + ' 条消息到队列')
        __pendingConsoleMessages.push(...parsed)
    }

    // 兼容旧 state.messages（其他组件可能引用）
    parsed.forEach(function (m) {
        console.log('[store.js] 追加消息: ' + (typeof m.text === 'string' ? m.text.substring(0, 80) : JSON.stringify(m).substring(0, 80)))
        state.messages.push({
            text: m.text,
            time: new Date().toLocaleTimeString(),
            type: m.type
        })
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
    // JSBridge 模式下消息由 on('console') 处理，避免重复
    if (getMode() !== 'websocket') return

    var isSystem = data.type === 'system'
    // 系统消息直接展示原文；用户消息格式化：[时间][地区] 昵称 (状态) 平台： 内容
    var displayText = isSystem
        ? data.text
        : _formatUserMessageHead(data) + (data.text || '')
    var displayType = isSystem ? 'system' : undefined

    if (_chatStoreBridge) {
        _chatStoreBridge.addMessage(displayText, displayType)
    }
    state.messages.push({ text: displayText, time: new Date().toLocaleTimeString(), type: displayType })
    nextTick(() => {
        const el = document.querySelector('.message-list')
        if (el) el.scrollTop = el.scrollHeight
    })
})

on('history', (data) => {
    var msgList = data.msgList || []
    if (!msgList.length) return
    var parsed = []
    for (var i = 0; i < msgList.length; i++) {
        var item = msgList[i]
        if (item.type === 'SYSTEM') {
            parsed.push({ text: item.body || '', type: 'system' })
        } else if (item.type === 'USER') {
            var head = _formatUserMessageHead({
                time: item.time,
                user: item.body && item.body.user
            })
            parsed.push({ text: head + (item.body && item.body.content || ''), type: 'user' })
        }
    }
    if (_chatStoreBridge) {
        _chatStoreBridge.addMessagesRaw(parsed)
    } else {
        __pendingConsoleMessages.push(...parsed)
    }
    parsed.forEach(function (m) {
        state.messages.push({ text: m.text, time: new Date().toLocaleTimeString(), type: m.type })
    })
})

on('onlineUsers', (data) => {
    var userList = data.userList || []
    state.onlineCount = userList.length
    state.online = true

    var onlineStore = useOnlineUsersStore()
    onlineStore.setUsers(userList)

    if (!onlineStore.welcomeShown) {
        onlineStore.markWelcomeShown()
        var welcomeMsg = '系统公告：亲爱的鱼友，欢迎你来到鱼塘~ 倡导文明摸鱼、理性摸鱼，做个德才兼备的顶级摸鱼选手！\n本项目为开源项目，开源地址：[xechat-idea-webview](https://gitee.com/chargeduck/xechat-idea-webview)，[原项目地址xechat-idea](https://github.com/anlingyi/xechat-idea)\n插件使用有问题请进群反馈或是直接去GitHub提交issues，摸鱼技术交流群：754126966。'
        if (_chatStoreBridge) {
            _chatStoreBridge.addMessage(welcomeMsg, 'system')
        }
        state.messages.push({ text: welcomeMsg, time: new Date().toLocaleTimeString(), type: 'system' })
    }
})

on('userState', (data) => {
    var user = data.user
    if (!user || !user.username) return
    var onlineStore = useOnlineUsersStore()
    onlineStore.upsertUser(user)
    state.onlineCount = onlineStore.count
})

on('disconnected', () => {
    state.online = false
    state.onlineCount = 0
    useOnlineUsersStore().clear()
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
