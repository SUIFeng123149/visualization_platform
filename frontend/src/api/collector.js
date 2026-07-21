import { request, requestJson } from './http'

export function createCollectorTask(payload) {
  return requestJson('/api/collector/tasks', {
    method: 'POST',
    body: payload,
  })
}

export function fetchCollectorTasks() {
  return request('/api/collector/tasks')
}

export function fetchCollectorTask(taskId) {
  return request(`/api/collector/tasks/${taskId}`)
}

export function cancelCollectorTask(taskId) {
  return requestJson(`/api/collector/tasks/${taskId}/cancel`, {
    method: 'POST',
  })
}
