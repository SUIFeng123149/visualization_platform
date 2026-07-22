import { request, requestJson } from './http'

export function fetchCrawlerStatus() { return request('/api/crawler/status') }
export function fetchAuthProfiles() { return request('/api/crawler/auth-profiles') }
export function createAuthProfile(payload) { return requestJson('/api/crawler/auth-profiles', { method: 'POST', body: payload }) }
export function verifyAuthProfile(profileId) { return requestJson(`/api/crawler/auth-profiles/${encodeURIComponent(profileId)}/verify`, { method: 'POST' }) }
export function setAuthProfileEnabled(profileId, enabled) { return requestJson(`/api/crawler/auth-profiles/${encodeURIComponent(profileId)}/${enabled ? 'enable' : 'disable'}`, { method: 'POST' }) }
export function createCrawlJob(payload) { return requestJson('/api/crawler/crawl-jobs', { method: 'POST', body: payload }) }
export function fetchCrawlJob(jobId) { return request(`/api/crawler/crawl-jobs/${encodeURIComponent(jobId)}`) }
export function cancelCrawlJob(jobId) { return requestJson(`/api/crawler/crawl-jobs/${encodeURIComponent(jobId)}/cancel`, { method: 'POST' }) }
export function resumeCrawlJob(jobId, strategy = {}) { return requestJson(`/api/crawler/crawl-jobs/${encodeURIComponent(jobId)}/resume`, { method: 'POST', body: { strategy } }) }
