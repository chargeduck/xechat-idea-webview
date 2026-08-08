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

    var useWS = false
    if (this._mode === 'websocket') {
      useWS = true
    } else if (this._mode === 'jsbridge') {
      useWS = false
    } else {
      useWS = (typeof window.xechat === 'undefined' || !window.xechat || !window.xechat.execCommand)
    }

    if (useWS) {
      this._transport = new WebSocketTransport(config.wsUrl || 'ws://127.0.0.1:1025/xechat')
    } else {
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
