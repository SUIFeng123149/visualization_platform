import { request } from './http'

export function fetchPlatforms() {
  return request('/api/v2/platforms')
}

export function fetchContents(params = {}) {
  return request('/api/v2/contents', params)
}

export function fetchPagedContents(params = {}) {
  return request('/api/v2/contents/page', params)
}

export function fetchContent(contentId) {
  return request(`/api/v2/contents/${encodeURIComponent(contentId)}`)
}

export function fetchContentChildren(contentId, limit = 100) {
  return request(`/api/v2/contents/${encodeURIComponent(contentId)}/children`, { limit })
}

export function fetchContentInteractions(contentId, params = {}) {
  return request(`/api/v2/contents/${encodeURIComponent(contentId)}/interactions`, params)
}

export function fetchContentSentiment(contentId) {
  return request(`/api/v2/contents/${encodeURIComponent(contentId)}/sentiment`)
}

export function fetchContentTimeline(contentId, type = 'danmaku') {
  return request(`/api/v2/contents/${encodeURIComponent(contentId)}/timeline`, { type })
}

export function fetchContentMetricHistory(contentId) {
  return request(`/api/v2/contents/${encodeURIComponent(contentId)}/metric-history`)
}

export function fetchUnifiedTrends(params = {}) {
  return request('/api/v2/analytics/trends', params)
}

export function fetchUnifiedKeywords(params = {}) {
  return request('/api/v2/analytics/keywords', params)
}

export function fetchMetricDefinitions() {
  return request('/api/v2/analytics/metric-definitions')
}

export function fetchMetricComparison(params = {}) {
  return request('/api/v2/analytics/metric-comparison', params)
}

export function fetchAccountPerformance(params = {}) {
  return request('/api/v2/accounts/performance', params)
}

export function fetchCommentInsightSummary(params = {}) {
  return request('/api/v2/analytics/comments/summary', params)
}

export function fetchCommentInteractionTypes(params = {}) {
  return request('/api/v2/analytics/comments/types', params)
}

export function fetchCommentInsightTrends(params = {}) {
  return request('/api/v2/analytics/comments/trends', params)
}

export function fetchNegativeInteractions(params = {}) {
  return request('/api/v2/analytics/comments/negative', params)
}
