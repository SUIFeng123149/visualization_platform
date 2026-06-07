import { requestJson } from './http'

export function chatWithAssistant(payload) {
  return requestJson('/api/ai/chat', {
    method: 'POST',
    body: payload,
  })
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
