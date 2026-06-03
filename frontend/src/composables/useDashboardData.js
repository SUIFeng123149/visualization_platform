import { computed, onMounted, reactive, ref, shallowRef, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  fetchDanmakuTimeline,
  fetchHeatRank,
  fetchKeywords,
  fetchNegativeComments,
  fetchSentimentTrend,
  fetchUpPerformance,
  fetchVideoSentiment,
} from '@/api/analysis'

const activeModule = ref('overview')
const trendMode = ref('日')
const keyword = ref('')
const loading = ref(false)
const selectedBvid = ref('')

const heatRank = shallowRef([])
const videoSentiments = shallowRef([])
const sentimentTrend = shallowRef([])
const danmakuTimeline = shallowRef([])
const keywords = shallowRef([])
const upPerformance = shallowRef([])
const negativeComments = shallowRef([])

const filters = reactive({ channel: 'all', period: '30d' })

export function useDashboardData() {
  const visibleHeatRank = computed(() => {
    if (filters.channel === 'all') return heatRank.value
    const matched = heatRank.value.filter((item) => item.category === filters.channel)
    return matched.length > 0 ? matched : heatRank.value
  })

  const mappedVideos = computed(() =>
    visibleHeatRank.value.map((item) => {
      const sentiment = videoSentiments.value.find((entry) => entry.bvid === item.bvid)
      const interactionRate = item.viewCount > 0 ? (item.likeCount + item.favoriteCount) / item.viewCount : 0
      return {
        bvid: item.bvid,
        title: item.title,
        up: item.upName,
        category: item.category,
        views: formatCompact(item.viewCount),
        interaction: `${(interactionRate * 100).toFixed(1)}%`,
        sentiment: sentiment ? `${(sentiment.positiveRatio * 100).toFixed(1)}%` : '--',
        status: item.rankNo <= 2 ? '爆发中' : '观察',
        statusType: item.rankNo <= 2 ? 'success' : 'warning',
      }
    }),
  )

  const filteredVideos = computed(() => {
    const term = keyword.value.trim().toLowerCase()
    if (!term) return mappedVideos.value
    return mappedVideos.value.filter((item) =>
      [item.title, item.up, item.category || ''].some((value) => value.toLowerCase().includes(term)),
    )
  })

  const videoCards = computed(() => visibleHeatRank.value.map((item) => ({ ...item, interactions: getInteractions(item) })))

  const danmakuHotspots = computed(() =>
    [...danmakuTimeline.value]
      .sort((a, b) => b.danmakuCount - a.danmakuCount)
      .slice(0, 8)
      .map((item) => ({
        ...item,
        timeText: formatVideoTime(item.timeBucket),
        avgSentimentText: `${(item.avgSentiment * 100).toFixed(1)}%`,
      })),
  )

  async function loadDashboardData() {
    loading.value = true
    try {
      const [heatRankData, sentimentData, trendData, keywordData, upData] = await Promise.all([
        fetchHeatRank(10),
        fetchVideoSentiment(),
        fetchSentimentTrend(getDateRange(filters.period)),
        fetchKeywords({ dimensionType: 'global', dimensionValue: 'all', limit: 10 }),
        fetchUpPerformance(10),
      ])
      heatRank.value = heatRankData
      videoSentiments.value = sentimentData
      sentimentTrend.value = trendData
      keywords.value = keywordData
      upPerformance.value = upData
      selectedBvid.value = heatRankData[0]?.bvid ?? ''
      negativeComments.value = selectedBvid.value
        ? await fetchNegativeComments({ bvid: selectedBvid.value, limit: 20 })
        : await fetchNegativeComments({ limit: 20 })
    } catch (error) {
      ElMessage.error(error.message || '数据加载失败')
    } finally {
      loading.value = false
    }
  }

  async function handleRefresh() {
    await loadDashboardData()
    ElMessage.success('数据已刷新')
  }

  async function handleFiltersChange(nextFilters) {
    Object.assign(filters, nextFilters)
    await loadDashboardData()
  }

  onMounted(loadDashboardData)

  watch(selectedBvid, async (bvid) => {
    if (!bvid) return
    danmakuTimeline.value = await fetchDanmakuTimeline(bvid)
  })

  return {
    activeModule,
    trendMode,
    keyword,
    loading,
    selectedBvid,
    heatRank,
    videoSentiments,
    sentimentTrend,
    danmakuTimeline,
    keywords,
    upPerformance,
    negativeComments,
    filters,
    visibleHeatRank,
    mappedVideos,
    filteredVideos,
    videoCards,
    danmakuHotspots,
    loadDashboardData,
    handleRefresh,
    handleFiltersChange,
  }
}

/* helper functions */

export function getInteractions(item) {
  return item.likeCount + item.coinCount + item.favoriteCount + item.replyCount + item.danmakuCount
}

export function average(values) {
  return values.length === 0 ? 0 : values.reduce((sum, value) => sum + value, 0) / values.length
}

export function formatCompact(value) {
  const number = Number(value) || 0
  if (number >= 10000) return `${(number / 10000).toFixed(1)}万`
  return Math.round(number).toLocaleString('zh-CN')
}

export function formatDate(date) {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-')
}

export function getDateRange(period) {
  const daysMap = { '7d': 7, '30d': 30, '90d': 90 }
  const days = daysMap[period] ?? 30
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - days + 1)
  return { startDate: formatDate(start), endDate: formatDate(end) }
}

export function formatVideoTime(value) {
  const minutes = Math.floor(value / 60)
  const seconds = String(value % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
}
