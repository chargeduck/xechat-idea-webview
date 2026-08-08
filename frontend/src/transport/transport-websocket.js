/**
 * WebSocket Transport — connects to XEChat Netty server.
 * Protocol: ws://127.0.0.1:1025/xechat
 *
 * Command → WS action mapping:
 *   #login → LOGIN    #setStatus → SET_STATUS
 *   #weather → WEATHER   #over → GAME_OVER
 *   #exit → disconnect    plain text → CHAT
 *   #help/#clean/#moyu/#showStatus/#showMode/#showGame/#showServer
 *   #mode/#notify/#alive/#admin/#mask/#backgroundImage/#play/#join/#open
 *   → client-side only (not sent via WebSocket)
 */
export class WebSocketTransport {
  constructor(url) {
    this.name = 'websocket'
    this._url = url || 'ws://127.0.0.1:1025/xechat'
    this._ws = null
    this._handlers = {}
    this._heartbeatTimer = null
    this._reconnectTimer = null
    this._reconnectDelay = 3000
    this._intentionalClose = false
  }

  async connect() {
    if (this._ws && this._ws.readyState === WebSocket.OPEN) return
    this._intentionalClose = false
    var self = this

    return new Promise(function(resolve, reject) {
      var ws = new WebSocket(self._url)
      self._ws = ws

      ws.onopen = function() {
        console.log('[WS] connected to', self._url)
        self._startHeartbeat()
        self._emit('connected', {})
        resolve()
      }

      ws.onmessage = function(event) {
        try {
          var msg = JSON.parse(event.data)
          self._dispatch(msg)
        } catch(e) {
          console.error('[WS] JSON parse error:', e)
        }
      }

      ws.onclose = function(event) {
        console.log('[WS] disconnected, code:', event.code)
        self._stopHeartbeat()
        self._emit('disconnected', { code: event.code })
        if (!self._intentionalClose) self._scheduleReconnect()
      }

      ws.onerror = function(err) {
        console.error('[WS] connection error:', err)
        reject(err)
      }
    })
  }

  _dispatch(msg) {
    var type = msg.type
    switch (type) {
      case 'USER':
        this._emit('message', {
          user: msg.user, content: msg.body && msg.body.content,
          msgType: msg.body && msg.body.msgType, toUsers: msg.body && msg.body.toUsers, time: msg.time
        })
        break
      case 'SYSTEM':
        this._emit('message', { type: 'system', content: msg.body, time: msg.time })
        this._emit('system', { content: msg.body, time: msg.time })
        break
      case 'ONLINE_USERS':
        this._emit('onlineUsers', { userList: (msg.body && msg.body.userList) || [], time: msg.time })
        break
      case 'USER_STATE':
        this._emit('userState', { user: msg.body && msg.body.user, state: msg.body && msg.body.state, time: msg.time })
        break
      case 'HISTORY_MSG':
        this._emit('history', { msgList: (msg.body && msg.body.msgList) || [], time: msg.time })
        break
      case 'STATUS_UPDATE':
        this._emit('statusUpdate', { user: msg.user, time: msg.time })
        break
      case 'GAME':
        this._emit('gameState', msg.body)
        break
      case 'GAME_OVER':
        this._emit('gameOver', msg.body)
        break
      case 'GAME_ROOM':
        this._emit('gameRoom', msg.body)
        break
      case 'GAME_ROOM_CREATED':
        this._emit('gameRoomCreated', msg.body)
        break
      case 'HEARTBEAT':
        break
      default:
        console.log('[WS] unknown message type:', type, msg)
    }
  }

  execCommand(cmdStr) {
    var parsed = this._parseCommand(cmdStr)
    if (parsed) this._send(parsed)
    return !!parsed
  }

  /**
   * 断开当前连接，重连到指定 host:port 的 WebSocket（port+1），
   * 连接成功后自动发送 LOGIN。
   * @param {string} host
   * @param {number} port
   * @param {object} loginPayload - LOGIN action body
   */
  async loginToServer(host, port, loginPayload) {
    var wsPort = parseInt(port) + 1
    var url = 'ws://' + host + ':' + wsPort + '/xechat'
    this._url = url

    // 断开旧连接
    this.disconnect()
    this._intentionalClose = false

    var self = this
    return new Promise(function(resolve, reject) {
      var ws = new WebSocket(url)
      self._ws = ws

      ws.onopen = function() {
        console.log('[WS] connected to', url)
        self._startHeartbeat()
        self._emit('connected', {})

        // 连接成功后立即发送 LOGIN
        if (loginPayload) {
          self._send(loginPayload)
        }
        resolve()
      }

      ws.onmessage = function(event) {
        try {
          var msg = JSON.parse(event.data)
          self._dispatch(msg)
        } catch(e) {
          console.error('[WS] JSON parse error:', e)
        }
      }

      ws.onclose = function(event) {
        console.log('[WS] disconnected, code:', event.code)
        self._stopHeartbeat()
        self._emit('disconnected', { code: event.code })
        if (!self._intentionalClose) self._scheduleReconnect()
      }

      ws.onerror = function(err) {
        console.error('[WS] connection error:', err)
        reject(err)
      }
    })
  }

  sendMessage(text) {
    this._send({ action: 'CHAT', body: { content: text, msgType: 'TEXT' } })
  }

  _parseCommand(cmdStr) {
    var trimmed = cmdStr.trim()
    if (!trimmed || trimmed.charAt(0) !== '#') {
      return { action: 'CHAT', body: { content: trimmed, msgType: 'TEXT' } }
    }
    var parts = trimmed.split(/\s+/)
    var cmd = parts[0].slice(1).toLowerCase()

    switch (cmd) {
      case 'login':
        var username = ''
        var args = { s: 0, h: '', p: '', c: false }
        for (var i = 1; i < parts.length; i++) {
          var p = parts[i]
          if (p.charAt(0) === '-' && i + 1 < parts.length) {
            var key = p.slice(1)
            if (key === 'c') { args.c = true; continue }
            args[key] = parts[++i]
          } else if (p && p.charAt(0) !== '-') {
            username = p
          }
        }
        if (!username) username = 'User-' + Date.now().toString(36)
        return {
          action: 'LOGIN',
          body: { username: username, status: 'FISHING', platform: 'WEB', uuid: this._uuid(), pluginVersion: '', reconnected: false }
        }
      case 'setstatus':
        var raw = parts[1]
        var map = { working: 'WORKING', fishing: 'FISHING', playing: 'PLAYING' }
        var val = map[raw && raw.toLowerCase()] || (raw && raw.toUpperCase()) || 'FISHING'
        return { action: 'SET_STATUS', body: val }
      case 'weather':
        var location = '', type = ''
        for (var j = 1; j < parts.length; j++) {
          if (parts[j] === '-d' && j + 1 < parts.length) type = parts[++j]
          else if (parts[j].charAt(0) !== '-') location = parts[j]
        }
        return { action: 'WEATHER', body: { location: location || '', type: type } }
      case 'over':
        return { action: 'GAME_OVER', body: {} }
      case 'exit':
        this.disconnect()
        return null
      case 'help': case 'clean': case 'moyu': case 'showstatus':
      case 'showmode': case 'showgame': case 'showserver':
      case 'mode': case 'notify': case 'alive': case 'admin':
      case 'mask': case 'backgroundimage': case 'play': case 'join':
      case 'open':
        return null
      default:
        return null
    }
  }

  _send(data) {
    if (this._ws && this._ws.readyState === WebSocket.OPEN) {
      this._ws.send(JSON.stringify(data))
    }
  }

  _startHeartbeat() {
    this._stopHeartbeat()
    var self = this
    this._heartbeatTimer = setInterval(function() { self._send({ action: 'HEARTBEAT' }) }, 15000)
  }

  _stopHeartbeat() {
    if (this._heartbeatTimer) { clearInterval(this._heartbeatTimer); this._heartbeatTimer = null }
  }

  _scheduleReconnect() {
    if (this._reconnectTimer) return
    var self = this
    console.log('[WS] reconnecting in', this._reconnectDelay, 'ms')
    this._reconnectTimer = setTimeout(function() { self._reconnectTimer = null; self.connect().catch(function(){}) }, this._reconnectDelay)
  }

  _uuid() {
    var uuid = localStorage.getItem('xechat_uuid')
    if (!uuid) {
      uuid = 'web-' + Math.random().toString(36).substring(2) + Date.now().toString(36)
      localStorage.setItem('xechat_uuid', uuid)
    }
    return uuid
  }

  disconnect() {
    this._intentionalClose = true
    this._stopHeartbeat()
    if (this._reconnectTimer) { clearTimeout(this._reconnectTimer); this._reconnectTimer = null }
    if (this._ws) { this._ws.close(); this._ws = null }
  }

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
