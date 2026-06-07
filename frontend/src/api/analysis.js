import { request } from './http'

export function fetchHeatRank(limit = 10) {
  return request('/api/analysis/videos/heat-rank', { limit })
}

export function fetchHistoricalVideoSamples(limit = 20) {
  return request('/api/analysis/videos/historical-samples', { limit })
}

export function fetchVideoSentiment(limit = 100) {
  return request('/api/analysis/videos/sentiment', { limit })
}

export function fetchVideoSentimentByBvids(bvids = []) {
  return request('/api/analysis/videos/sentiment/by-bvids', { bvids: bvids.join(',') })
}

export function fetchVideoDetail(bvid) {
  return request(`/api/analysis/videos/${encodeURIComponent(bvid)}/detail`)
}

export function fetchSentimentTrend(params = {}) {
  return request('/api/analysis/sentiment/trend', params)
}

export function fetchDanmakuTimeline(bvid) {
  return request('/api/analysis/danmaku/timeline', { bvid })
}

export function fetchKeywords(params = {}) {
  return request('/api/analysis/keywords', {
    dimensionType: 'global',
    dimensionValue: 'all',
    limit: 30,
    ...params,
  })
}

export function fetchUpPerformance(limit = 10) {
  return request('/api/analysis/ups/performance', { limit })
}

export function fetchNegativeComments(params = {}) {
  return request('/api/analysis/comments/negative', params)
}
