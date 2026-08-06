/**
 * xechat JSBridge 前端封装。
 * 通过 window.xechat 与 Java 端通信，事件通过 CustomEvent 接收。
 */
const xechat = window.xechat || {}

xechat._handlers = {}
xechat.on = function (event, handler) {
    if (!this._handlers[event]) {
        this._handlers[event] = []
        window.addEventListener('xechat:' + event, function (e) {
            (xechat._handlers[event] || []).forEach(function (fn) { fn(e.detail) })
        })
    }
    this._handlers[event].push(handler)
}
xechat.off = function (event, handler) {
    if (this._handlers[event]) {
        this._handlers[event] = this._handlers[event].filter(function (fn) { return fn !== handler })
    }
}

xechat.gameAction = function (gameIndex, action) {
    window.dispatchEvent(new CustomEvent('xechat:gameAction', {
        detail: { gameIndex: gameIndex, action: action }
    }))
}

xechat.openTool = function (toolIndex) {
    window.dispatchEvent(new CustomEvent('xechat:openTool', { detail: { toolIndex: toolIndex } }))
}
xechat.toolClose = function () {
    window.dispatchEvent(new CustomEvent('xechat:toolClose', {}))
}

xechat.roomReady = function () {
    window.dispatchEvent(new CustomEvent('xechat:roomReady', {}))
}
xechat.roomUnready = function () {
    window.dispatchEvent(new CustomEvent('xechat:roomUnready', {}))
}
xechat.roomInvite = function (username) {
    window.dispatchEvent(new CustomEvent('xechat:roomInvite', { detail: { username: username } }))
}
xechat.roomLeave = function () {
    window.dispatchEvent(new CustomEvent('xechat:roomLeave', {}))
}

window.xechat = xechat
