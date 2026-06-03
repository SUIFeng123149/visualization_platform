const API_PREFIX = import.meta.env.VITE_API_BASE_URL ?? ''
const DEFAULT_TIMEOUT = Number(import.meta.env.VITE_API_TIMEOUT_MS ?? 15000)

export async function request(path, params = {}) {
  const url = new URL(`${API_PREFIX}${path}`, window.location.origin)
  const controller = new AbortController()
  const timeoutId = window.setTimeout(() => controller.abort(), DEFAULT_TIMEOUT)

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, value)
    }
  })

  let response
  try {
    response = await fetch(url, {
      headers: {
        Accept: 'application/json',
      },
      signal: controller.signal,
    })
  } catch (error) {
    if (error.name === 'AbortError') {
      throw new Error('接口请求超时，请稍后重试')
    }
    throw new Error(`网络请求失败: ${error.message || '请检查网络连接'}`)
  } finally {
    window.clearTimeout(timeoutId)
  }

  if (!response.ok) {
    let serverMessage = response.statusText
    try {
      const errorBody = await response.json()
      serverMessage = errorBody.message || serverMessage
    } catch {
      // ignore parse error
    }
    throw new Error(serverMessage || `HTTP ${response.status}`)
  }

  const payload = await response.json()

  if (payload.code !== 200) {
    throw new Error(payload.message || '接口请求失败')
  }

  return payload.data
}
