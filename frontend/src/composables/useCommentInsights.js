import { computed, ref } from 'vue'
import {
  fetchCommentInteractionTypes,
  fetchCommentInsightSummary,
  fetchCommentInsightTrends,
  fetchCommentTopics,
  fetchNegativeInteractions,
} from '@/api/content'

const emptySummary = () => ({
  interactionCount: 0,
  analyzedCount: 0,
  positiveCount: 0,
  neutralCount: 0,
  negativeCount: 0,
  averageSentiment: null,
  positiveRatio: 0,
  neutralRatio: 0,
  negativeRatio: 0,
})

const summary = ref(emptySummary())
const trends = ref([])
const negativeItems = ref([])
const negativeTotal = ref(0)
const topics = ref([])
const loadingCommentInsights = ref(false)
const commentInsightFallback = ref(false)
const commentInsightError = ref('')
const availableInteractionTypes = ref([])
const loadingInteractionTypes = ref(false)
let commentRequestId = 0
let typeRequestId = 0

const sentimentRows = computed(() => [{
  ...summary.value,
  totalCount: summary.value.analyzedCount,
}])

export function useCommentInsights() {
  async function loadCommentInteractionTypes(filters = {}) {
    const requestId = ++typeRequestId
    loadingInteractionTypes.value = true
    try {
      const requestedParams = compactParams({
        platform: filters.platform,
        startDate: filters.startDate,
        endDate: filters.endDate,
      })
      let rows = await fetchCommentInteractionTypes(requestedParams)
      if (!rows.length && (requestedParams.startDate || requestedParams.endDate)) {
        rows = await fetchCommentInteractionTypes({ platform: requestedParams.platform })
      }
      if (requestId === typeRequestId) availableInteractionTypes.value = rows
      return rows
    } finally {
      if (requestId === typeRequestId) loadingInteractionTypes.value = false
    }
  }

  async function loadCommentInsights(filters = {}) {
    const requestId = ++commentRequestId
    loadingCommentInsights.value = true
    commentInsightError.value = ''
    try {
      const requestedParams = compactParams(filters)
      let result = await requestInsightBundle(requestedParams)
      const hasRequestedData = result.summary.interactionCount > 0 || result.trends.length > 0

      const shouldTryFallback = !hasRequestedData
        && Boolean(requestedParams.startDate || requestedParams.endDate)

      if (shouldTryFallback) {
        const fallbackParams = { ...requestedParams, startDate: undefined, endDate: undefined }
        result = await requestInsightBundle(fallbackParams)
      }

      if (requestId !== commentRequestId) return result
      commentInsightFallback.value = shouldTryFallback
        && (result.summary.interactionCount > 0 || result.trends.length > 0)
      summary.value = result.summary ?? emptySummary()
      trends.value = result.trends ?? []
      negativeItems.value = result.negativePage?.items ?? []
      negativeTotal.value = result.negativePage?.total ?? 0
      topics.value = result.topics ?? []
      return result
    } catch (error) {
      if (requestId === commentRequestId) {
        commentInsightError.value = error.message || '评论洞察数据加载失败'
      }
      throw error
    } finally {
      if (requestId === commentRequestId) loadingCommentInsights.value = false
    }
  }

  function clearCommentInsights() {
    commentRequestId += 1
    typeRequestId += 1
    summary.value = emptySummary()
    trends.value = []
    negativeItems.value = []
    negativeTotal.value = 0
    topics.value = []
    commentInsightFallback.value = false
    commentInsightError.value = ''
    loadingCommentInsights.value = false
    availableInteractionTypes.value = []
    loadingInteractionTypes.value = false
  }

  return {
    summary,
    sentimentRows,
    trends,
    negativeItems,
    negativeTotal,
    topics,
    loadingCommentInsights,
    commentInsightFallback,
    commentInsightError,
    availableInteractionTypes,
    loadingInteractionTypes,
    loadCommentInteractionTypes,
    loadCommentInsights,
    clearCommentInsights,
  }
}

async function requestInsightBundle(params) {
  const [summaryData, trendData, negativePage, topicData] = await Promise.all([
    fetchCommentInsightSummary(params),
    fetchCommentInsightTrends(params),
    fetchNegativeInteractions(params),
    fetchCommentTopics(params),
  ])
  return { summary: summaryData ?? emptySummary(), trends: trendData ?? [], negativePage, topics: topicData ?? [] }
}

function compactParams(params) {
  return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''))
}
