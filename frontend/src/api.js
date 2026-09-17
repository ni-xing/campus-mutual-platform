import { useAuthStore } from './stores/auth'

// 统一请求封装：走 nginx(8088) 代理到 gateway，响应体 {code,msg,data,traceId}
export async function api(path, method = 'GET', body = null) {
  const auth = useAuthStore()
  const headers = { 'Content-Type': 'application/json' }
  if (auth.token) headers.Authorization = 'Bearer ' + auth.token
  let res
  try {
    res = await fetch(path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  } catch {
    throw new Error('网络异常，请确认服务已启动')
  }
  if (res.status === 401) {
    auth.clear()
    throw new Error('登录已失效，请重新登录')
  }
  const json = await res.json().catch(() => ({}))
  if (json.code !== '000000') throw new Error(json.msg || '请求失败')
  return json
}
