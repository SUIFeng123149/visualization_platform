import { request, requestJson } from './http'

export function fetchTasks() {
  return request('/api/tasks')
}

export function refreshTasks() {
  return requestJson('/api/tasks/refresh', {
    method: 'POST',
  })
}

export function fetchTaskStatuses() {
  return request('/api/tasks/statuses')
}

export function updateTaskStatus(taskId, status) {
  return requestJson(`/api/tasks/statuses/${encodeURIComponent(taskId)}`, {
    method: 'PUT',
    body: { status },
  })
}
