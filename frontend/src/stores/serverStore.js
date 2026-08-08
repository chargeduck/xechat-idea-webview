import { defineStore } from 'pinia'

const STORAGE_KEY = 'xechat_server_cache'
const CACHE_VERSION = 2  // v2: 增加 wsAlive 字段
// gitee Open API（支持 CORS，IDEA 插件和 Web 环境通用）
const SERVER_LIST_API_URL = 'https://gitee.com/api/v5/repos/chargeduck/xechat-idea-webview/contents/server_list.json'

function loadCache() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const cached = JSON.parse(raw)
    // 版本不匹配则丢弃旧缓存，避免编码格式变更导致乱码残留
    if (cached._v !== CACHE_VERSION) return null
    return cached.data
  } catch {
    return null
  }
}

function saveCache(data) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ _v: CACHE_VERSION, data }))
}

/**
 * base64 → UTF-8 字符串。
 * 用 decodeURIComponent + percent-encoding 方案，不依赖 TextDecoder/Uint8Array，
 * 兼容性覆盖所有现代浏览器及 JCEF 内嵌 Chromium。
 */
function b64ToUtf8(b64) {
  const binary = atob(b64)
  var hex = ''
  for (var i = 0; i < binary.length; i++) {
    var h = binary.charCodeAt(i).toString(16)
    hex += '%' + (h.length === 1 ? '0' : '') + h
  }
  return decodeURIComponent(hex)
}

/**
 * 测试 TCP 连通性。JSBridge 环境走 Java Socket；Web 环境用 fetch no-cors 探针。
 * @returns {Promise<boolean>} true 存活 / false 不可达
 */
function testConnection(host, port, timeout) {
  // JSBridge：Java Socket（最可靠）
  if (window.xechat && typeof window.xechat.testConnection === 'function') {
    return new Promise(function (resolve) {
      window.xechat.testConnection(host, String(port), String(timeout), function (result) {
        resolve(result === 'true')
      })
    })
  }
  // Web：fetch no-cors 作为 TCP 探针，端口通则 resolve，拒绝/超时则 false
  var controller = new AbortController()
  var timer = setTimeout(function () { controller.abort() }, timeout)
  return fetch('http://' + host + ':' + port, {
    mode: 'no-cors',
    signal: controller.signal
  }).then(function () {
    clearTimeout(timer)
    return true
  }).catch(function () {
    clearTimeout(timer)
    return false
  })
}

/**
 * 测试 WebSocket 连通性：尝试连接 ws://host:(port+1)/xechat，2s 超时。
 * @returns {Promise<boolean>} true 握手成功 / false 不可达
 */
function testWsConnection(host, port) {
  return new Promise(function (resolve) {
    var wsPort = parseInt(port) + 1
    var wsUrl = 'ws://' + host + ':' + wsPort + '/xechat'
    var ws = null
    var done = false
    var timer = setTimeout(function () {
      if (done) return
      done = true
      if (ws) { try { ws.close() } catch (e) {} }
      resolve(false)
    }, 2000)
    try {
      ws = new WebSocket(wsUrl)
      ws.onopen = function () {
        if (done) return
        done = true
        clearTimeout(timer)
        ws.close()
        resolve(true)
      }
      ws.onerror = function () {
        if (done) return
        done = true
        clearTimeout(timer)
        resolve(false)
      }
      ws.onclose = function () {
        if (done) return
        done = true
        clearTimeout(timer)
        resolve(false)
      }
    } catch (e) {
      if (done) return
      done = true
      clearTimeout(timer)
      resolve(false)
    }
  })
}

/**
 * serverStore - 服务列表的获取与缓存。
 * 首次加载从 localStorage 恢复；#showServer 从 gitee API 拉取；-c 强制刷新。
 */
export const useServerStore = defineStore('server', {
  state: () => ({
    servers: loadCache() || [],
    loading: false
  }),

  getters: {
    /** 渲染为 Markdown 表格 */
    serverTable(state) {
      if (!state.servers.length) return '暂无鱼塘数据，输入 #showServer 获取。'
      let md = '| 编号 | 鱼塘 | 地址 | TCP | WS |\n|------|------|------|-----|-----|\n'
      state.servers.forEach((s, i) => {
        var tcp = s.alive === true ? '存活' : s.alive === false ? '离线' : '探测中'
        var ws = s.wsAlive === true ? '存活' : s.wsAlive === false ? '离线' : '探测中'
        md += '| ' + i + ' | ' + (s.name || s.ip) + ' | ' + s.ip + ':' + s.port + ' | ' + tcp + ' | ' + ws + ' |\n'
      })
      return md
    }
  },

  actions: {
    async fetchServers(forceRefresh) {
      if (!forceRefresh && this.servers.length > 0) {
        return
      }
      this.loading = true
      try {
        const resp = await fetch(SERVER_LIST_API_URL)
        if (!resp.ok) throw new Error('HTTP ' + resp.status)
        const apiData = await resp.json()
        if (!apiData.content) throw new Error('API 返回格式异常')
        const text = b64ToUtf8(apiData.content.replace(/\s/g, ''))
        const data = JSON.parse(text)
        this.servers = data
        saveCache(data)
        // 同步到 Java 端 DataCache.serverList（JSBridge 环境），供 #login -s 使用
        if (window.xechat && typeof window.xechat.updateServerList === 'function') {
          window.xechat.updateServerList(JSON.stringify(data))
        }
        // 等待 TCP 探测完成后再结束 loading
        await this.testServerConnections()
      } catch (e) {
        console.error('[serverStore] 获取服务列表失败', e)
        throw e
      } finally {
        this.loading = false
      }
    },

    /** 并行测试所有服务器 TCP + WebSocket 连通性，全部完成后一次性写回 */
    async testServerConnections() {
      var list = this.servers
      if (!list.length) return
      // TCP 探测
      var tcpTasks = list.map(function (s) {
        return testConnection(s.ip, s.port, 500)
      })
      // WS 探测（ws://host:(port+1)/xechat，2s 超时）
      var wsTasks = list.map(function (s) {
        return testWsConnection(s.ip, s.port)
      })
      var tcpResults = await Promise.allSettled(tcpTasks)
      var wsResults = await Promise.allSettled(wsTasks)
      // 全部探测完成后一次性替换
      var updated = list.map(function (s, idx) {
        var tcpR = tcpResults[idx]
        var wsR = wsResults[idx]
        return Object.assign({}, s, {
          alive: tcpR.status === 'fulfilled' ? tcpR.value : false,
          wsAlive: wsR.status === 'fulfilled' ? wsR.value : false
        })
      })
      this.servers = updated
      saveCache(updated)
    }
  }
})
