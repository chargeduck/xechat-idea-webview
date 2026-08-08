/**
 * JSBridge Transport — wraps window.xechat (JCEF CefMessageRouter).
 * Zero-breaking: delegates execCommand/sendMessage to Java bridge,
 * forwards all existing CustomEvents to the transport event bus.
 */
export class JSBridgeTransport {
  constructor() {
    this.name = 'jsbridge'
    this._handlers = {}
  }

  async connect() {
    if (!window.xechat) {
      console.warn('[JSBridge] window.xechat not available')
      return
    }
    var events = [
      'console', 'toolOpen', 'toolClose', 'gameStart', 'gameOver',
      'gameState', 'gameRoom', 'gameRoomCreated', 'message', 'stateChange',
      'onlineUsers', 'history', 'userState', 'statusUpdate', 'system', 'error', 'connected'
    ]
    var self = this
    events.forEach(function(evt) {
      if (typeof window.xechat.on === 'function') {
        window.xechat.on(evt, function(data) { self._emit(evt, data) })
      }
    })
    this._emit('connected', {})
  }

  execCommand(cmd) {
    if (window.xechat && window.xechat.execCommand) {
      window.xechat.execCommand(cmd)
    }
  }

  /**
   * JSBridge 模式：构造 #login 命令并转发给 Java 处理。
   * Java 端 LoginCommandHandler 负责 NettyClient 连接。
   */
  loginToServer(host, port, loginPayload) {
    var username = (loginPayload && loginPayload.body && loginPayload.body.username) || ''
    var cmd = '#login ' + username + ' -h ' + host + ' -p ' + port
    this.execCommand(cmd)
  }

  sendMessage(text) {
    if (window.xechat && window.xechat.sendMessage) {
      window.xechat.sendMessage(text)
    }
  }

  disconnect() { /* JSBridge lifecycle managed by JCEF */ }

  _emit(type, data) {
    var h = this._handlers[type]
    if (h) { h.forEach(function(fn) { try { fn(data) } catch(e) {} }) }
    var w = this._handlers['*']
    if (w && type !== '*') { w.forEach(function(fn) { try { fn(type, data) } catch(e) {} }) }
  }

  on(type, handler) {
    if (!this._handlers[type]) this._handlers[type] = []
    this._handlers[type].push(handler)
  }

  off(type, handler) {
    var h = this._handlers[type]
    if (h) { this._handlers[type] = h.filter(function(fn) { return fn !== handler }) }
  }
}
