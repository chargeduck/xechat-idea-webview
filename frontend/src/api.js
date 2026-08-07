/**
 * xechat JSBridge 前端封装 — 支持 JSBridge + WebSocket 双传输模式。
 */

import { transport } from './transport/transport-manager.js'

/**
 * 保证 window.xechat 命名空间存在，提供 DEV mock 与事件系统。
 * 保留原有 index.html 内联脚本的兼容逻辑。
 */
export function ensureXechatNS() {
  var xechat = window.xechat || {}

  // DEV 环境 mock：浏览器中没有 JCEF 桥接层时提供空壳，避免报错
  if (!xechat.getState) {
    xechat.getState = function () { return '{}' }
    xechat.getTools = function () { return '[]' }
    xechat.getGameList = function () { return '[]' }
    xechat.execCommand = function (cmd) { console.log('[xechat mock] execCommand: ' + cmd) }
    xechat.sendMessage = function (msg) { console.log('[xechat mock] sendMessage: ' + msg) }
    xechat.ready = function () { console.log('[xechat mock] ready') }
  }

  // 事件系统：不覆盖 index.html 内联脚本已建立的 _handlers / on / off
  xechat._handlers = xechat._handlers || {}
  xechat.on = xechat.on || function (event, handler) {
    if (!this._handlers[event]) {
      this._handlers[event] = []
      window.addEventListener('xechat:' + event, function (e) {
        console.log('[api.js] 收到 xechat:' + event + ' 事件, detail:', e.detail)
        ;(xechat._handlers[event] || []).forEach(function (fn) { fn(e.detail) })
      })
    }
    console.log('[api.js] 注册 xechat:' + event + ' 监听器')
    this._handlers[event].push(handler)
  }
  xechat.off = xechat.off || function (event, handler) {
    if (this._handlers[event]) {
      this._handlers[event] = this._handlers[event].filter(function (fn) { return fn !== handler })
    }
  }

  window.xechat = xechat
}

// 模块加载时初始化 window.xechat 命名空间
ensureXechatNS()

// === 原有导出函数 ===
export function gameAction(gameIndex, action) {
  window.dispatchEvent(new CustomEvent('xechat:gameAction', {
    detail: { gameIndex: gameIndex, action: action }
  }))
}

export function openTool(toolIndex) {
  window.dispatchEvent(new CustomEvent('xechat:openTool', { detail: { toolIndex: toolIndex } }))
}

export function toolClose() {
  window.dispatchEvent(new CustomEvent('xechat:toolClose', {}))
}

export function roomReady() {
  window.dispatchEvent(new CustomEvent('xechat:roomReady', {}))
}

export function roomUnready() {
  window.dispatchEvent(new CustomEvent('xechat:roomUnready', {}))
}

export function roomInvite(username) {
  window.dispatchEvent(new CustomEvent('xechat:roomInvite', { detail: { username: username } }))
}

export function roomLeave() {
  window.dispatchEvent(new CustomEvent('xechat:roomLeave', {}))
}

// === Transport 集成 API ===
export async function init(config) {
  return transport.init(config)
}

export function getMode() {
  return transport.mode
}

export function on(type, handler) {
  transport.on(type, handler)
}

export function off(type, handler) {
  transport.off(type, handler)
}

export function execCommand(cmd) {
  transport.execCommand(cmd)
}

export function sendMessage(text) {
  transport.sendMessage(text)
}

export function disconnect() {
  transport.disconnect()
}
