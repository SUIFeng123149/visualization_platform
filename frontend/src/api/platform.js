import { request, requestJson } from './http'

export function fetchDataSourceStatuses() {
  return request('/api/platform/data-sources/status')
}

export function fetchReportHistory() {
  return request('/api/platform/reports')
}

export function createReportHistory(payload) {
  return requestJson('/api/platform/reports', {
    method: 'POST',
    body: payload,
  })
}

export function fetchAnomalyRules() {
  return request('/api/platform/anomaly-rules')
}

export function updateAnomalyRule(payload) {
  return requestJson('/api/platform/anomaly-rules', {
    method: 'PUT',
    body: payload,
  })
}
