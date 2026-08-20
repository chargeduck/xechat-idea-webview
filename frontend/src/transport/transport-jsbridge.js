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

  /**
   * 判断 window.xechat 是否为真实 Java 桥接。
   * DEV mock 的 getState() 返回 '{}'，真实注入桥接返回完整 StateInfo JSON。
   */
  _isRealBridge() {
    var x = window.xechat
    if (!x || typeof x.execCommand !== 'function') return false
    try {
      var s = typeof x.getState === 'function' ? x.getState() : ''
      return s !== '{}'
    } catch (e) {
      return false
    }
  }

  /**
   * cefQuery 直连 Java（CefMessageRouter 在 loadURL 前注册，页面加载即就绪）。
   * 即使 register() 注入 window.xechat 失败，命令仍能到达 Java handleQuery。
   */
  _cefCall(method, args) {
    if (typeof window.cefQuery !== 'function') {
      console.warn('[JSBridge][cefQuery] cefQuery 不可用，丢弃调用: ' + method)
      return
    }
    window.cefQuery({
      request: JSON.stringify({ method: method, args: args || [] }),
      onSuccess: function(resp) {
        console.log('[JSBridge][cefQuery] ' + method + ' 成功: ' + resp)
      },
      onFailure: function(code, msg) {
        console.error('[JSBridge][cefQuery] ' + method + ' 失败: code=' + code + ', msg=' + msg)
      }
    })
  }

  execCommand(cmd) {
    if (this._isRealBridge()) {
      console.log('[JSBridge][execCommand] 转发到 Java: ' + cmd)
      window.xechat.execCommand(cmd)
      return
    }
    console.log('[JSBridge][execCommand] window.xechat 非真实桥接（注入未生效），改走 cefQuery 直连: ' + cmd)
    this._cefCall('execCommand', [cmd])
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
    if (this._isRealBridge() && window.xechat.sendMessage) {
      window.xechat.sendMessage(text)
      return
    }
    console.log('[JSBridge][sendMessage] 非真实桥接，改走 cefQuery 直连: ' + text)
    this._cefCall('sendMessage', [text])
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
