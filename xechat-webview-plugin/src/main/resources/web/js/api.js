/**
 * xechat JSBridge 前端封装。
 * 通过 window.xechat 与 Java 端通信，事件通过 CustomEvent 接收。
 */
const xechat = window.xechat || {};

/** 事件监听：xechat.on('console', data => ...) */
xechat._handlers = {};
xechat.on = function (event, handler) {
    if (!this._handlers[event]) {
        this._handlers[event] = [];
        window.addEventListener('xechat:' + event, function (e) {
            (xechat._handlers[event] || []).forEach(function (fn) { fn(e.detail); });
        });
    }
    this._handlers[event].push(handler);
};
xechat.off = function (event, handler) {
    if (this._handlers[event]) {
        this._handlers[event] = this._handlers[event].filter(function (fn) { return fn !== handler; });
    }
};

/** 游戏操作：发送到 Java 端 gameAction(index, action) */
xechat.gameAction = function (gameIndex, action) {
    // 通过 CustomEvent 发送给 Java 端 JSBridge
    window.dispatchEvent(new CustomEvent('xechat:gameAction', {
        detail: { gameIndex: gameIndex, action: action }
    }));
};

/** 工具操作 */
xechat.openTool = function (toolIndex) {
    window.dispatchEvent(new CustomEvent('xechat:openTool', {
        detail: { toolIndex: toolIndex }
    }));
};
xechat.toolClose = function () {
    window.dispatchEvent(new CustomEvent('xechat:toolClose', {}));
};

/** 房间操作 */
xechat.roomReady = function () {
    window.dispatchEvent(new CustomEvent('xechat:roomReady', {}));
};
xechat.roomUnready = function () {
    window.dispatchEvent(new CustomEvent('xechat:roomUnready', {}));
};
xechat.roomInvite = function (username) {
    window.dispatchEvent(new CustomEvent('xechat:roomInvite', {
        detail: { username: username }
    }));
};
xechat.roomLeave = function () {
    window.dispatchEvent(new CustomEvent('xechat:roomLeave', {}));
};

// 重新暴露，保持 API 一致
window.xechat = xechat;
