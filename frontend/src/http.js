/**
 * manager 后端 HTTP 请求封装（fetch 版）。
 *
 * - baseURL 固定为 https://dld.lesscoding.net/api（nginx 反代至 /xeManager/**）
 * - 自动携带 satoken header（token 从 localStorage 读取，兼容 key: satoken / token）
 * - 响应按 {code, message, data} 解包，code !== 200 时抛错
 *
 * 供 GamePanel 登录/注册/游戏大厅/云存档使用，不依赖 axios。
 */

const BASE_URL = 'https://dld.lesscoding.net/api'
const DEFAULT_TIMEOUT = 15000

/**
 * 读取本地 token。
 * 优先 'satoken' key，兼容 manager-ui 的 'token' key（同环境共享 localStorage 时）。
 */
export function getToken() {
  return localStorage.getItem('satoken') || localStorage.getItem('token') || ''
}

/**
 * 保存 token（key 固定 'satoken'，与后端 header 名一致）。
 */
export function setToken(token) {
  if (token) {
    localStorage.setItem('satoken', token)
  } else {
    localStorage.removeItem('satoken')
  }
}

/**
 * 清除本地 token。
 */
export function clearToken() {
  localStorage.removeItem('satoken')
  localStorage.removeItem('token')
}

/**
 * 核心 request：发起 HTTP 请求并解包 {code, message, data}。
 * @param {string} url 相对路径（如 '/user/login'）
 * @param {object} options { method, params, body, headers, timeout }
 * @returns {Promise<any>} 解包后的 data
 */
export async function request(url, options = {}) {
  const {
    method = 'GET',
    params = null,
    body = null,
    headers = {},
    timeout = DEFAULT_TIMEOUT
  } = options

  let fullUrl = BASE_URL + url
  if (params) {
    const qs = new URLSearchParams(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== null)
    ).toString()
    if (qs) fullUrl += (fullUrl.includes('?') ? '&' : '?') + qs
  }

  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeout)

  const finalHeaders = { 'Content-Type': 'application/json', ...headers }
  const token = getToken()
  if (token) finalHeaders['satoken'] = token

  let resp
  try {
    resp = await fetch(fullUrl, {
      method,
      headers: finalHeaders,
      body: body != null ? JSON.stringify(body) : undefined,
      signal: controller.signal
    })
  } catch (e) {
    clearTimeout(timer)
    if (e && e.name === 'AbortError') {
      throw new Error(`请求超时（${timeout}ms）：${url}`)
    }
    throw new Error(`网络请求失败：${url}（${e && e.message ? e.message : e}）`)
  }
  clearTimeout(timer)

  let json
  try {
    json = await resp.json()
  } catch (e) {
    throw new Error(`响应解析失败：${url}，HTTP ${resp.status}`)
  }

  if (json && json.code !== undefined && json.code !== 200) {
    const err = new Error(json.message || `请求失败（code=${json.code}）：${url}`)
    err.code = json.code
    throw err
  }
  return json && json.data !== undefined ? json.data : json
}

/* ==================== 认证 ==================== */

/** 登录：{username, password} → data.token */
export function login(data) {
  return request('/user/login', { method: 'POST', body: data })
}

/** 注册：{username, password, nickname?} → data.token */
export function register(data) {
  return request('/user/register', { method: 'POST', body: data })
}

/** 当前用户信息（需登录） */
export function getUserInfo() {
  return request('/user/userInfo', { method: 'GET' })
}

/* ==================== 游戏大厅 ==================== */

/**
 * 游戏列表。默认取全部已上线游戏。
 * @param {object} data { pageSize, status, gameType }
 * @returns {Promise<{records: Array}>}
 */
export function getGameList(data = {}) {
  const { pageSize = 500, status = 1, gameType, page = 1 } = data
  return request('/gameInfo/list', {
    method: 'POST',
    body: { pageSize, status, gameType, page }
  })
}

/** 启用的游戏分类列表 */
export function getCategoryList() {
  return request('/category/enabled', { method: 'POST' })
}

/* ==================== 云存档 ==================== */

/** 保存存档：{gameInfoId, saveName, saveData, version?} */
export function storageSave(dto) {
  return request('/storage/save', { method: 'POST', body: dto })
}

/** 读取存档：{gameInfoId, saveName} → saveData 字符串 */
export function storageLoad(query) {
  return request('/storage/load', { method: 'POST', body: query })
}

/** 存档槽位列表：{gameInfoId} → List<SaveSlotVO> */
export function storageSlots(gameInfoId) {
  return request(`/storage/slots/${gameInfoId}`, { method: 'GET' })
}

/** 删除存档：{gameInfoId, saveName} */
export function storageDelete(query) {
  return request('/storage/delete', { method: 'POST', body: query })
}

export default { request, login, register, getUserInfo, getGameList, getCategoryList, storageSave, storageLoad, storageSlots, storageDelete, getToken, setToken, clearToken }
