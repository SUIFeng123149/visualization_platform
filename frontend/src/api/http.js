const API_PREFIX = ''

export async function request(path, params = {}) {
  const url = new URL(`${API_PREFIX}${path}`, window.location.origin)

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, value)
    }
  })

  const response = await fetch(url)

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`)
  }

  const payload = await response.json()

  if (payload.code !== 200) {
    throw new Error(payload.message || '接口请求失败')
  }

  return payload.data
}
