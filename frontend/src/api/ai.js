import { requestJson } from './http'

export function chatWithAssistant(payload) {
  return requestJson('/api/ai/chat', {
    method: 'POST',
    body: payload,
  })
}

export function chatWithAssistantStream(payload) {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  return fetch(`${baseUrl}/api/ai/chat-stream`, {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
    },
    credentials: 'same-origin',
    body: JSON.stringify(payload),
  })
}

export async function consumeAssistantStream(payload, handlers = {}) {
  const response = await chatWithAssistantStream(payload)
  if (!response.ok) {
    throw new Error(`AI请求失败：HTTP ${response.status}`)
  }
  if (!response.body) {
    throw new Error('浏览器不支持流式响应读取')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let eventName = 'message'
  let dataLines = []

  function flushEvent() {
    if (dataLines.length === 0) return
    const data = dataLines.join('\n')
    if (eventName === 'message') {
      handlers.onMessage?.(data)
    } else if (eventName === 'done') {
      handlers.onDone?.(safeJsonParse(data))
    } else if (eventName === 'error') {
      handlers.onError?.(data)
    } else if (eventName === 'file') {
      handlers.onFile?.(safeJsonParse(data) ?? data)
    }
    eventName = 'message'
    dataLines = []
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split(/\r?\n/)
    buffer = lines.pop() ?? ''

    for (const line of lines) {
      if (line === '') {
        flushEvent()
      } else if (line.startsWith('event:')) {
        eventName = line.slice(6).trim() || 'message'
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).replace(/^ /, ''))
      }
    }
  }

  if (buffer || dataLines.length) {
    if (buffer.startsWith('data:')) {
      dataLines.push(buffer.slice(5).replace(/^ /, ''))
    }
    flushEvent()
  }
}

export function askPageInsight({ query, mode, context, conversationId }) {
  return chatWithAssistant({
    query,
    mode,
    context,
    conversationId,
    user: 'bililens-dashboard',
  })
}

function safeJsonParse(value) {
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}
