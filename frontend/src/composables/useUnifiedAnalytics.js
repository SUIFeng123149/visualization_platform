import { computed, ref } from 'vue'
import { fetchAccountPerformance, fetchContents, fetchDashboardSummary, fetchUnifiedKeywords, fetchUnifiedTrends } from '@/api/content'
import { usePlatformContext } from '@/composables/usePlatformContext'

const contents = ref([])
const trends = ref([])
const keywords = ref([])
const accounts = ref([])
const loadingUnified = ref(false)
const loadingAccounts = ref(false)
const unifiedError = ref('')
const dashboardSummary = ref(null)

export function useUnifiedAnalytics() {
  const { selectedPlatform } = usePlatformContext()

  const queryPlatform = computed(() => selectedPlatform.value === 'all' ? undefined : selectedPlatform.value)
  const summary = computed(() => {
    if (dashboardSummary.value) return dashboardSummary.value
    const availableViews = contents.value.filter((item) => item.viewCount !== null && item.viewCount !== undefined)
    const availableHeat = contents.value.filter((item) => item.normalizedHeatScore !== null && item.normalizedHeatScore !== undefined)
    return {
      contentCount: contents.value.length,
      platformCount: new Set(contents.value.map((item) => item.platformCode)).size,
      totalViews: availableViews.reduce((sum, item) => sum + Number(item.viewCount), 0),
      averageNormalizedHeat: availableHeat.length
        ? availableHeat.reduce((sum, item) => sum + Number(item.normalizedHeatScore), 0) / availableHeat.length
        : null,
      interactionCount: trends.value.reduce((sum, item) => sum + Number(item.interactionCount), 0),
    }
  })

  async function loadUnifiedAnalytics() {
    loadingUnified.value = true
    unifiedError.value = ''
    try {
      const params = { platform: queryPlatform.value }
      const [summaryRow, contentRows, trendRows, keywordRows, accountRows] = await Promise.all([
        fetchDashboardSummary(params),
        fetchContents({ ...params, limit: 100 }),
        fetchUnifiedTrends(params),
        fetchUnifiedKeywords({ ...params, limit: 20 }),
        fetchAccountPerformance({ ...params, limit: 20 }),
      ])
      dashboardSummary.value = summaryRow
      contents.value = contentRows
      trends.value = trendRows
      keywords.value = keywordRows
      accounts.value = accountRows
    } catch (error) {
      unifiedError.value = error.message || '统一分析数据加载失败'
      throw error
    } finally {
      loadingUnified.value = false
    }
  }

  async function loadAccountPerformance() {
    loadingAccounts.value = true
    try {
      accounts.value = await fetchAccountPerformance({ platform: queryPlatform.value, limit: 20 })
    } finally {
      loadingAccounts.value = false
    }
  }

  return {
    contents,
    trends,
    keywords,
    accounts,
    loadingUnified,
    loadingAccounts,
    unifiedError,
    summary,
    loadUnifiedAnalytics,
    loadAccountPerformance,
  }
}
