<template>
  <section v-loading="loadingCommentInsights || loadingInteractionTypes" class="comment-insight-page">
    <div class="comment-insight-toolbar" aria-label="评论洞察筛选">
      <div class="comment-filter-group">
        <span>互动类型</span>
        <el-segmented
          v-model="interactionType"
          :options="interactionTypeOptions"
          :disabled="loadingInteractionTypes"
          aria-label="互动类型筛选"
          @change="handleInteractionTypeChange"
        />
      </div>
      <el-date-picker
        v-model="dateRange"
        class="comment-date-range"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        aria-label="评论日期范围"
        :disabled-date="disableFutureDate"
        :shortcuts="dateShortcuts"
        unlink-panels
      />
      <el-button type="primary" :loading="loadingCommentInsights || loadingInteractionTypes" @click="applyFilters">查询</el-button>
      <el-button plain @click="resetFilters">重置</el-button>
    </div>

    <el-alert
      v-if="selectedPlatform !== 'all' && activePlatform"
      :title="`${activePlatform.displayName} 可用互动：${capabilitySummary}`"
      :type="supportsCommentInsight ? 'info' : 'warning'"
      :closable="false"
      show-icon
      class="capability-alert"
    />

    <el-alert
      v-if="commentInsightFallback"
      title="当前日期范围没有互动数据，已展示该平台最近可用的历史记录。"
      type="warning"
      :closable="false"
      show-icon
      class="capability-alert"
    />

    <section v-if="supportsCommentInsight" class="analysis-grid">
      <ChartPanel title="互动情感占比" description="统计评论、回复或剧评中已完成情感分析的正向、中性和负向样本。">
        <SentimentPieChart :data="sentimentRows" />
      </ChartPanel>
      <ChartPanel title="互动量与情感风险趋势" description="对照互动数量、平均情感和负向占比，识别讨论增长是否伴随情绪风险。">
        <template #actions>
          <el-segmented v-model="trendMode" :options="['日', '周', '月']" aria-label="趋势聚合周期" />
        </template>
        <CommentSentimentTrendChart :mode="trendMode" :data="trends" />
      </ChartPanel>
    </section>

    <section v-if="supportsCommentInsight" class="panel video-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">讨论主题</div>
          <div class="panel-desc">从已分析互动的关键词中提取高频主题；负面关联数用于识别需要优先处理的话题。</div>
        </div>
      </div>
      <el-table :data="topics" empty-text="当前筛选范围暂无可用主题词" style="width: 100%">
        <el-table-column prop="topic" label="主题" min-width="180" />
        <el-table-column prop="interactionCount" label="关联互动" width="140" align="right" header-align="right"><template #default="{ row }">{{ nullableCount(row.interactionCount) }}</template></el-table-column>
        <el-table-column prop="negativeCount" label="负面关联" width="140" align="right" header-align="right"><template #default="{ row }"><el-tag :type="row.negativeCount ? 'warning' : 'success'" effect="plain">{{ nullableCount(row.negativeCount) }}</el-tag></template></el-table-column>
      </el-table>
    </section>

    <section v-if="supportsCommentInsight" class="panel video-panel comment-negative-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">负向互动样本</div>
          <div class="panel-desc">按点赞数优先展示负向评论、回复和剧评，可进入对应内容继续复盘。</div>
        </div>
      </div>
      <el-table :data="negativeItems" empty-text="当前筛选范围暂无负向互动样本" style="width: 100%">
        <el-table-column label="平台" width="100" align="center" header-align="center">
          <template #default="{ row }">{{ platformName(row.platformCode) }}</template>
        </el-table-column>
        <el-table-column label="内容" min-width="220">
          <template #default="{ row }">
            <div class="content-title-cell">
              <strong class="content-title-text" :title="row.contentTitle">{{ row.contentTitle }}</strong>
              <span>{{ row.externalContentId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center" header-align="center">
          <template #default="{ row }">{{ interactionTypeName(row.interactionType) }}</template>
        </el-table-column>
        <el-table-column prop="userName" label="用户" width="120" show-overflow-tooltip />
        <el-table-column prop="text" label="互动内容" min-width="280" show-overflow-tooltip />
        <el-table-column label="点赞" width="90" align="right" header-align="right">
          <template #default="{ row }">{{ nullableCount(row.likeCount) }}</template>
        </el-table-column>
        <el-table-column label="情感" width="90" align="right" header-align="right">
          <template #default="{ row }">{{ sentimentScore(row.sentimentScore) }}</template>
        </el-table-column>
        <el-table-column label="发生时间" width="170" align="center" header-align="center">
          <template #default="{ row }">{{ formatDateTime(row.occurredAt || row.capturedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" header-align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openContent(row.contentId)">查看内容</el-button>
              <el-button link type="danger" :loading="creatingTaskId === row.interactionId" @click="createFollowUpTask(row)">纳入处置</el-button>
            </template>
          </el-table-column>
      </el-table>
      <div class="content-analysis-footer">
        <span>共 {{ negativeTotal }} 条负向样本</span>
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="negativeTotal"
          layout="prev, pager, next, sizes"
          :page-sizes="[10, 20, 50]"
          background
          @size-change="handlePageSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <div v-else class="panel empty-state comment-capability-empty">
      当前平台未声明评论或剧评能力，请切换平台或检查平台能力配置。
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import ChartPanel from '@/components/charts/ChartPanel.vue'
import CommentSentimentTrendChart from '@/components/charts/CommentSentimentTrendChart.vue'
import SentimentPieChart from '@/components/charts/SentimentPieChart.vue'
import { useCommentInsights } from '@/composables/useCommentInsights'
import { usePlatformContext } from '@/composables/usePlatformContext'
import { createNegativeInteractionTask } from '@/api/tasks'

const route = useRoute()
const router = useRouter()
const defaultRange = recentDateRange(30)
const initialStartDate = validSelectableDate(route.query.startDate) || defaultRange[0]
const initialEndDate = validSelectableDate(route.query.endDate) || defaultRange[1]
const dateRange = ref(initialStartDate <= initialEndDate ? [initialStartDate, initialEndDate] : defaultRange)
const interactionType = ref(validInteractionType(route.query.type) || 'all')
const trendMode = ref('日')
const page = ref(Math.max(1, Number(route.query.page) || 1))
const pageSize = ref([10, 20, 50].includes(Number(route.query.pageSize)) ? Number(route.query.pageSize) : 10)
const creatingTaskId = ref(null)

const { platforms, selectedPlatform, activePlatform, activeCapabilities, loadPlatforms } = usePlatformContext()
const {
  sentimentRows,
  trends,
  negativeItems,
  negativeTotal,
  topics,
  loadingCommentInsights,
  commentInsightFallback,
  availableInteractionTypes,
  loadingInteractionTypes,
  loadCommentInteractionTypes,
  loadCommentInsights,
  clearCommentInsights,
} = useCommentInsights()

const supportsCommentInsight = computed(() => selectedPlatform.value === 'all'
  || availableInteractionTypes.value.length > 0
  || activeCapabilities.value.comments
  || activeCapabilities.value.reviews)

const interactionTypeOptions = computed(() => {
  const total = availableInteractionTypes.value.reduce((sum, item) => sum + Number(item.interactionCount), 0)
  return [
    { label: total > 0 ? `全部互动 (${total.toLocaleString('zh-CN')})` : '全部互动', value: 'all' },
    ...availableInteractionTypes.value.map((item) => ({
      label: `${interactionTypeName(item.interactionType)} (${Number(item.interactionCount).toLocaleString('zh-CN')})`,
      value: item.interactionType,
    })),
  ]
})

const capabilitySummary = computed(() => {
  if (!availableInteractionTypes.value.length) return '当前没有已采集的评论类互动'
  return availableInteractionTypes.value
    .map((item) => `${interactionTypeName(item.interactionType)} ${Number(item.interactionCount).toLocaleString('zh-CN')} 条`)
    .join('、')
})

const dateShortcuts = [
  { text: '最近 7 天', value: () => recentDateObjects(7) },
  { text: '最近 30 天', value: () => recentDateObjects(30) },
  { text: '最近 90 天', value: () => recentDateObjects(90) },
]

onMounted(async () => {
  window.addEventListener('video-analytics:refresh-comments', refreshData)
  try {
    await loadPlatforms()
    await refreshInteractionTypes()
    normalizeInteractionType()
    syncQuery()
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || '评论洞察数据加载失败')
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('video-analytics:refresh-comments', refreshData)
})

watch(selectedPlatform, async () => {
  page.value = 1
  try {
    await refreshInteractionTypes()
    normalizeInteractionType()
    syncQuery()
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || '平台评论数据加载失败')
  }
})

watch(
  () => [route.query.type, route.query.startDate, route.query.endDate, route.query.page, route.query.pageSize],
  async ([nextType, nextStartDate, nextEndDate, nextPage, nextPageSize]) => {
    const nextInteractionType = validInteractionType(nextType) || 'all'
    const normalizedDateRange = routeDateRange(nextStartDate, nextEndDate)
    const normalizedPage = Math.max(1, Number(nextPage) || 1)
    const normalizedPageSize = [10, 20, 50].includes(Number(nextPageSize)) ? Number(nextPageSize) : 10
    const changed = interactionType.value !== nextInteractionType
      || dateRange.value?.[0] !== normalizedDateRange?.[0]
      || dateRange.value?.[1] !== normalizedDateRange?.[1]
      || page.value !== normalizedPage
      || pageSize.value !== normalizedPageSize

    if (!changed) return
    interactionType.value = nextInteractionType
    dateRange.value = normalizedDateRange
    page.value = normalizedPage
    pageSize.value = normalizedPageSize
    try {
      await refreshInteractionTypes()
      normalizeInteractionType()
      await loadData()
    } catch (error) {
      ElMessage.error(error.message || '评论筛选状态恢复失败')
    }
  },
)

function normalizeInteractionType() {
  if (!interactionTypeOptions.value.some((item) => item.value === interactionType.value)) {
    interactionType.value = 'all'
  }
}

async function refreshInteractionTypes() {
  await loadCommentInteractionTypes({
    platform: selectedPlatform.value === 'all' ? undefined : selectedPlatform.value,
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1],
  })
}

async function loadData() {
  if (!supportsCommentInsight.value) {
    clearCommentInsights()
    return
  }
  await loadCommentInsights({
    platform: selectedPlatform.value === 'all' ? undefined : selectedPlatform.value,
    type: interactionType.value === 'all' ? undefined : interactionType.value,
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1],
    page: page.value,
    pageSize: pageSize.value,
  })
}

async function applyFilters() {
  page.value = 1
  try {
    await refreshInteractionTypes()
    normalizeInteractionType()
    syncQuery()
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || '筛选评论数据失败')
  }
}

async function handleInteractionTypeChange() {
  page.value = 1
  syncQuery()
  try {
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || '互动类型筛选失败')
  }
}

async function refreshData() {
  try {
    await refreshInteractionTypes()
    normalizeInteractionType()
    await loadData()
    ElMessage.success('评论洞察数据已刷新')
  } catch (error) {
    ElMessage.error(error.message || '评论洞察数据刷新失败')
  }
}

function resetFilters() {
  interactionType.value = 'all'
  dateRange.value = recentDateRange(30)
  applyFilters()
}

function handlePageSizeChange(size) {
  pageSize.value = size
  page.value = 1
  syncQuery()
  loadData().catch((error) => ElMessage.error(error.message || '负向样本加载失败'))
}

function handlePageChange() {
  syncQuery()
  loadData().catch((error) => ElMessage.error(error.message || '负向样本加载失败'))
}

function syncQuery() {
  router.replace({
    query: {
      ...route.query,
      type: interactionType.value === 'all' ? undefined : interactionType.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      page: page.value > 1 ? String(page.value) : undefined,
      pageSize: pageSize.value !== 10 ? String(pageSize.value) : undefined,
    },
  })
}

function openContent(contentId) {
  router.push({
    name: 'contentDetail',
    params: { contentId },
    query: { ...route.query, from: 'comment' },
  })
}

async function createFollowUpTask(row) {
  creatingTaskId.value = row.interactionId
  try {
    await createNegativeInteractionTask(row.interactionId)
    ElMessage.success('已纳入处置任务，可在任务中心跟踪状态')
  } catch (error) {
    ElMessage.error(error.message || '创建处置任务失败')
  } finally {
    creatingTaskId.value = null
  }
}

function validInteractionType(value) {
  return typeof value === 'string' && ['comment', 'reply', 'review'].includes(value) ? value : null
}

function recentDateRange(days) {
  return [dateValue(dateWithOffset(-(days - 1))), dateValue(dateWithOffset(0))]
}

function recentDateObjects(days) {
  return [dateWithOffset(-(days - 1)), dateWithOffset(0)]
}

function dateWithOffset(offset) {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  date.setDate(date.getDate() + offset)
  return date
}

function dateValue(date) {
  return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-')
}

function disableFutureDate(date) {
  const endOfToday = new Date()
  endOfToday.setHours(23, 59, 59, 999)
  return date.getTime() > endOfToday.getTime()
}

function validSelectableDate(value) {
  return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value) && value <= dateValue(new Date()) ? value : null
}

function routeDateRange(startDate, endDate) {
  const normalizedStartDate = validSelectableDate(startDate)
  const normalizedEndDate = validSelectableDate(endDate)
  if (!normalizedStartDate && !normalizedEndDate) return null
  return normalizedStartDate && normalizedEndDate && normalizedStartDate <= normalizedEndDate
    ? [normalizedStartDate, normalizedEndDate]
    : null
}

function platformName(code) {
  return platforms.value.find((item) => item.platformCode === code)?.displayName ?? code
}

function interactionTypeName(type) {
  return { comment: '评论', reply: '回复', review: '剧评/短评' }[type] ?? type
}

function nullableCount(value) {
  return value === null || value === undefined ? '--' : Number(value).toLocaleString('zh-CN')
}

function sentimentScore(value) {
  return value === null || value === undefined ? '--' : Number(value).toFixed(2)
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '--'
}
</script>
