// API 封装：Bearer Token + 统一错误处理
const TOKEN_KEY = 'cc_token'
const USER_KEY = 'cc_user'

export function getToken() { return localStorage.getItem(TOKEN_KEY) }
export function getUser() {
  try { return JSON.parse(localStorage.getItem(USER_KEY)) } catch { return null }
}
export function setSession(token, user) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  window.dispatchEvent(new Event('auth-changed'))
}
export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  window.dispatchEvent(new Event('auth-changed'))
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`
  const resp = await fetch(path, { ...options, headers })
  if (!resp.ok) {
    let msg = `请求失败 (${resp.status})`
    try { msg = (await resp.json()).error || msg } catch { /* ignore */ }
    // 401 时清除会话，由 App 的 auth-changed 监听响应式跳回登录页（无需整页刷新）
    if (resp.status === 401) clearSession()
    throw new Error(msg)
  }
  return resp.json()
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) }),
}
