import { request, requestJson } from './http'

export function fetchDataSourceStatuses() {
  return request('/api/platform/data-sources/status')
}

export function fetchPlatformConfigs() { return request('/api/platform/platforms') }
export function savePlatformConfig(payload) { return requestJson('/api/platform/platforms', { method: 'PUT', body: payload }) }
export function deletePlatformConfig(platformCode, password) { return requestJson(`/api/platform/platforms/${encodeURIComponent(platformCode)}`, { method: 'DELETE', body: { password } }) }
export function fetchMetricConfigs() { return request('/api/platform/metrics') }
export function saveMetricConfig(metricKey, payload) { return requestJson(`/api/platform/metrics/${encodeURIComponent(metricKey)}`, { method: 'PUT', body: payload }) }

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
