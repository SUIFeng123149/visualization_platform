import { computed, reactive, ref, shallowRef, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  fetchDanmakuTimeline,
  fetchHeatRank,
  fetchKeywords,
  fetchNegativeComments,
  fetchSentimentTrend,
  fetchUpPerformance,
  fetchVideoSentiment,
  fetchVideoSentimentByBvids,
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

let initialized = false
let loadingPromise = null
const HEAT_RANK_POOL_LIMIT = 100

const visibleHeatRank = computed(() => {
  if (filters.channel === 'all') return heatRank.value
  return heatRank.value.filter((item) => getVideoCategory(item) === filters.channel)
})

const mappedVideos = computed(() =>
  visibleHeatRank.value.map((item) => {
    const sentiment = videoSentiments.value.find((entry) => entry.bvid === item.bvid)
    const interactionRate = item.viewCount > 0 ? (item.likeCount + item.favoriteCount) / item.viewCount : 0
    return {
      bvid: item.bvid,
      title: item.title,
      up: item.upName,
      category: getVideoCategory(item),
      views: formatCompact(item.viewCount),
      interaction: `${(interactionRate * 100).toFixed(1)}%`,
      sentiment: sentiment ? `${(sentiment.positiveRatio * 100).toFixed(1)}%` : '暂无样本',
      status: item.rankNo <= 2 ? '爆发中' : '观察',
      statusType: item.rankNo <= 2 ? 'success' : 'warning',
    }
  }),
)

const allMappedVideos = computed(() =>
  heatRank.value.map((item) => {
    const sentiment = videoSentiments.value.find((entry) => entry.bvid === item.bvid)
    const interactionRate = item.viewCount > 0 ? (item.likeCount + item.favoriteCount) / item.viewCount : 0
    return {
      bvid: item.bvid,
      title: item.title,
      up: item.upName,
      category: getVideoCategory(item),
      views: formatCompact(item.viewCount),
      interaction: `${(interactionRate * 100).toFixed(1)}%`,
      sentiment: sentiment ? `${(sentiment.positiveRatio * 100).toFixed(1)}%` : '暂无样本',
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

const allFilteredVideos = computed(() => {
  const term = keyword.value.trim().toLowerCase()
  if (!term) return allMappedVideos.value
  return allMappedVideos.value.filter((item) =>
    [item.title, item.up, item.category || ''].some((value) => value.toLowerCase().includes(term)),
  )
})

const videoCards = computed(() =>
  visibleHeatRank.value.map((item) => ({
    ...item,
    category: getVideoCategory(item),
    interactions: getInteractions(item),
  })),
)

const allVideoCards = computed(() =>
  heatRank.value.map((item) => ({
    ...item,
    category: getVideoCategory(item),
    interactions: getInteractions(item),
  })),
)

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

const globalInsights = computed(() => {
  const insights = []
  const topVideo = heatRank.value[0]
  const avgNegativeRatio = average(videoSentiments.value.map((item) => item.negativeRatio))
  const highestNegative = [...videoSentiments.value].sort((a, b) => b.negativeRatio - a.negativeRatio)[0]
  const hotspot = danmakuHotspots.value[0]
  const topUp = upPerformance.value[0]

  if (topVideo) {
    insights.push({
      title: '优先复盘热度榜首',
      level: '高优先级',
      type: 'warning',
      text: `${topVideo.title} 当前热度最高，建议进入详情页拆解互动、弹幕和评论结构。`,
      bvid: topVideo.bvid,
    })
  }

  if (highestNegative && highestNegative.negativeRatio >= 0.25) {
    insights.push({
      title: '负向情绪异常',
      level: '风险',
      type: 'danger',
      text: `${highestNegative.title} 负向占比 ${(highestNegative.negativeRatio * 100).toFixed(1)}%，需要优先查看高赞负面评论。`,
      bvid: highestNegative.bvid,
    })
  } else if (avgNegativeRatio > 0) {
    insights.push({
      title: '舆情整体可控',
      level: '观察',
      type: 'success',
      text: `当前平均负向占比 ${(avgNegativeRatio * 100).toFixed(1)}%，可重点跟踪新增评论波动。`,
      bvid: topVideo?.bvid,
    })
  }

  if (hotspot) {
    insights.push({
      title: '高能切片机会',
      level: '可执行',
      type: 'primary',
      text: `弹幕峰值出现在 ${hotspot.timeText}，建议回看前后 15 秒制作二次传播素材。`,
      bvid: hotspot.bvid,
    })
  }

  if (topUp) {
    insights.push({
      title: 'UP主合作线索',
      level: '增长',
      type: 'success',
      text: `${topUp.upName} 平均热度最高，可作为内容合作或选题复盘样本。`,
      bvid: topVideo?.bvid,
    })
  }

  return insights
})

async function loadDashboardData() {
  if (loadingPromise) return loadingPromise

  loading.value = true
  loadingPromise = (async () => {
    try {
      const [heatRankData, trendData, keywordData, upData] = await Promise.all([
        fetchHeatRank(HEAT_RANK_POOL_LIMIT),
        fetchSentimentTrend(getDateRange(filters.period)),
        fetchKeywords({ dimensionType: 'global', dimensionValue: 'all', limit: 10 }),
        fetchUpPerformance(10),
      ])
      const heatRankBvids = heatRankData.map((item) => item.bvid)
      const matchedSentimentData = heatRankBvids.length > 0 ? await fetchVideoSentimentByBvids(heatRankBvids) : []
      const fallbackSentimentData = matchedSentimentData.length > 0 ? matchedSentimentData : await fetchVideoSentiment(100)
      heatRank.value = heatRankData
      videoSentiments.value = fallbackSentimentData
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
      loadingPromise = null
    }
  })()

  return loadingPromise
}

async function handleRefresh() {
  await loadDashboardData()
  ElMessage.success('数据已刷新')
}

async function handleFiltersChange(nextFilters) {
  Object.assign(filters, nextFilters)
  await loadDashboardData()
}

function ensureDashboardData() {
  if (initialized) return
  initialized = true

  loadDashboardData()

  watch(selectedBvid, async (bvid) => {
    danmakuTimeline.value = bvid ? await fetchDanmakuTimeline(bvid) : []
  })
}

export function useDashboardData() {
  ensureDashboardData()

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
    allMappedVideos,
    filteredVideos,
    allFilteredVideos,
    videoCards,
    allVideoCards,
    danmakuHotspots,
    globalInsights,
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

export function getVideoCategory(item) {
  if (item?.category) return item.category

  const text = `${item?.title ?? ''} ${item?.upName ?? item?.up ?? ''}`.toLowerCase()
  const rules = [
    ['科技', ['科技', '数码', '电脑', '主机', '代码', 'ai', 'rog', '算法', '生物鉴定']],
    ['知识', ['科普', '知识', '讲解', '原理', '教程', '鉴定', '毕导']],
    ['游戏', ['游戏', '鸣潮', '战神', '街霸', '三国杀', '火柴人', '忍法帖']],
    ['动画', ['动画', '番', '纸牌屋', '火柴人', 'alanbecker']],
    ['音乐', ['音乐', '歌', '曲', 'ep', '演唱', '日推']],
    ['生活', ['外卖', '猫', '篮球馆', '街头', '酒店', '美食', '旅行', '吃']],
    ['娱乐', ['挑战', '特工', '买瓜', '妖术', '搞笑', '主持人']],
  ]

  return rules.find(([, keywords]) => keywords.some((keyword) => text.includes(keyword)))?.[0] ?? '综合'
}
