import { JSBridgeTransport } from './transport-jsbridge.js'
import { WebSocketTransport } from './transport-websocket.js'

/**
 * Transport Manager — auto-selects JSBridge or WebSocket.
 * Mode priority: explicit config > settingsStore > auto-detect.
 */
class TransportManager {
  constructor() {
    this._transport = null
    this._handlers = {}
    this._mode = 'auto'
  }

  async init(config) {
    config = config || {}
    var storedMode = this._readStoredMode()
    this._mode = config.mode || storedMode || 'auto'
    console.log('[Transport][init] config.mode=' + JSON.stringify(config.mode) + ', storedMode=' + JSON.stringify(storedMode) + ', final mode=' + this._mode)

    var useWS = false
    if (this._mode === 'websocket') {
      useWS = true
      console.log('[Transport][init] 显式配置 websocket 模式，直接使用 WS')
    } else if (this._mode === 'jsbridge') {
      useWS = false
      console.log('[Transport][init] 显式配置 jsbridge 模式，直接使用 JSBridge')
    } else {
      // cefQuery 是 JCEF C++ 层异步注入的 JavaScript 绑定，可能晚于页面首个脚本执行。
      // 等待其就绪再判定，避免时序竞态导致 IDEA 插件误判为 WebSocket。
      if (typeof window.cefQuery !== 'function') {
        console.log('[Transport] cefQuery 未就绪，等待中...')
        await new Promise(function (resolve) {
          var attempts = 0
          var timer = setInterval(function () {
            if (typeof window.cefQuery === 'function' || ++attempts > 10) {
              console.log('[Transport] cefQuery 等待结束, attempts=' + attempts + ', typeof=' + typeof window.cefQuery)
              clearInterval(timer)
              resolve()
            }
          }, 100)
        })
      }

      var isJCef = typeof window.cefQuery === 'function'
      var hasXechatNS = !!window.xechat
      var hasGetState = !!(window.xechat && typeof window.xechat.getState === 'function')
      var getStateRaw = ''
      try {
        getStateRaw = hasGetState ? window.xechat.getState() : '(no getState)'
      } catch (e) {
        getStateRaw = '(getState threw: ' + e.message + ')'
      }
      var isRealJSBridge = hasXechatNS && hasGetState && (getStateRaw !== '{}' || isJCef)
      console.log('[Transport][init] 自动检测: isJCef=' + isJCef + ', hasXechatNS=' + hasXechatNS + ', hasGetState=' + hasGetState + ', getState()=' + getStateRaw + ', isRealJSBridge=' + isRealJSBridge)
      useWS = !isRealJSBridge
      console.log('[Transport][init] 自动检测结果: useWS=' + useWS)
    }

    if (useWS) {
      var wsUrl = config.wsUrl || 'ws://127.0.0.1:1025/xechat'
      console.log('[Transport][init] 创建 WebSocketTransport, url=' + wsUrl)
      this._transport = new WebSocketTransport(wsUrl)
    } else {
      console.log('[Transport][init] 创建 JSBridgeTransport')
      this._transport = new JSBridgeTransport()
    }

    var self = this
    this._transport.on('*', function(type, data) {
      if (self._handlers[type]) {
        self._handlers[type].forEach(function(fn) { try { fn(data) } catch(e) {} })
      }
      if (self._handlers['*']) {
        self._handlers['*'].forEach(function(fn) { try { fn(type, data) } catch(e) {} })
      }
    })

    await this._transport.connect()
    console.log('[Transport] initialized, mode:', this._transport.name)
  }

  get mode() { return this._transport ? this._transport.name : 'none' }
  get configuredMode() { return this._mode }

  execCommand(cmd) {
    if (this._transport) this._transport.execCommand(cmd)
  }

  loginToServer(host, port, loginPayload) {
    if (this._transport && this._transport.loginToServer) {
      return this._transport.loginToServer(host, port, loginPayload)
    }
  }

  sendMessage(text) {
    if (this._transport) this._transport.sendMessage(text)
  }

  /** 服务器列表回传 Java（JSBridge 模式写入 DataCache.serverList） */
  updateServerList(json) {
    if (this._transport && this._transport.updateServerList) {
      this._transport.updateServerList(json)
    }
  }

  on(type, handler) {
    if (!this._handlers[type]) this._handlers[type] = []
    this._handlers[type].push(handler)
  }

  off(type, handler) {
    var h = this._handlers[type]
    if (h) { this._handlers[type] = h.filter(function(fn) { return fn !== handler }) }
  }

  disconnect() {
    if (this._transport) { this._transport.disconnect(); this._transport = null }
  }

  /** 从持久化存储读取传输模式，同时兼容新旧 storage key */
  _readStoredMode() {
    if (typeof localStorage === 'undefined') return null
    // 优先读 Pinia settingsStore 用的新键，兼容旧键 'xechat_transport_mode'
    try {
      var raw = localStorage.getItem('xechat_settings_v1')
      if (raw) return JSON.parse(raw).transportMode
    } catch (e) {}
    return localStorage.getItem('xechat_transport_mode')
  }
}

export var transport = new TransportManager()
