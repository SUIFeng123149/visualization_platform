<template>
  <section class="panel content-panel" v-loading="loading">
    <div class="panel-header content-analysis-toolbar">
      <div>
        <div class="panel-title">跨平台内容表现</div>
        <div class="panel-desc">默认查看昨天的指标快照，可按平台、内容类型、日期和关键词筛选并进入复盘。</div>
      </div>
      <div class="content-analysis-actions">
        <el-input v-model="keyword" class="content-search" clearable placeholder="搜索标题、内容 ID、账号" @keyup.enter="applyFilters" />
        <el-date-picker
          v-model="dateRange"
          class="content-date-range"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          aria-label="指标日期范围"
          :disabled-date="disableFutureDate"
          :shortcuts="dateShortcuts"
          unlink-panels
        />
        <el-button type="primary" :loading="loading" @click="applyFilters">查询</el-button>
        <el-button plain @click="resetFilters">重置</el-button>
      </div>
    </div>

    <div class="content-type-filter">
      <span>内容类型</span>
      <el-segmented v-model="contentType" :options="contentTypeOptions" aria-label="内容类型筛选" />
      <el-button text type="primary" @click="useAllDates">查看全部日期</el-button>
    </div>

    <el-alert
      v-if="selectedPlatform !== 'all' && activePlatform"
      :title="`${activePlatform.displayName} 数据能力：${capabilitySummary}`"
      type="info"
      :closable="false"
      show-icon
      class="capability-alert"
    />

    <el-table class="content-analysis-table" :data="contents" empty-text="当前日期范围没有内容快照，请调整日期或查看全部日期" style="width: 100%">
      <el-table-column label="内容" min-width="320">
        <template #default="{ row }">
          <div class="content-title-cell">
            <strong class="content-title-text" :title="row.title">{{ row.title }}</strong>
            <span>{{ row.externalContentId }} · {{ row.accountName || '未知发布账号' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="平台" width="112" align="center" header-align="center">
        <template #default="{ row }">{{ platformName(row.platformCode) }}</template>
      </el-table-column>
      <el-table-column label="类型" width="112" align="center" header-align="center">
        <template #default="{ row }">{{ contentTypeName(row.contentType) }}</template>
      </el-table-column>
      <el-table-column label="播放量" width="138" align="right" header-align="right" sortable>
        <template #default="{ row }">{{ nullableCount(row.viewCount) }}</template>
      </el-table-column>
      <el-table-column label="归一化热度" width="148" align="right" header-align="right" sortable>
        <template #default="{ row }">{{ percentile(row.normalizedHeatScore) }}</template>
      </el-table-column>
      <el-table-column label="指标时间" width="178" align="center" header-align="center">
        <template #default="{ row }">{{ formatDateTime(row.metricsCapturedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="112" align="center" header-align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openContent(row.contentId)">进入复盘</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="content-analysis-footer">
      <div class="content-compare-picker">
        <el-select v-model="comparisonIds" multiple collapse-tags collapse-tags-tooltip placeholder="选择 2 至 5 条内容对比" aria-label="选择内容横向对比" style="width: min(420px, 100%)">
          <el-option v-for="item in contents" :key="item.contentId" :label="item.title" :value="item.contentId" :disabled="comparisonIds.length >= 5 && !comparisonIds.includes(item.contentId)" />
        </el-select>
        <el-button type="primary" :disabled="comparisonIds.length < 2 || comparisonIds.length > 5" @click="compareSelected">横向对比</el-button>
      </div>
      <span>共 {{ total }} 条内容</span>
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next, sizes" :page-sizes="[10, 20, 50]" background @size-change="handlePageSizeChange" @current-change="handlePageChange" />
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { fetchPagedContents } from '@/api/content'
import { usePlatformContext } from '@/composables/usePlatformContext'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const contents = ref([])
const comparisonIds = ref([])
const total = ref(0)
const page = ref(Math.max(1, Number(route.query.page) || 1))
const pageSize = ref([10, 20, 50].includes(Number(route.query.pageSize)) ? Number(route.query.pageSize) : 20)
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const yesterdayValue = yesterday()
const allDatesSelected = route.query.allDates === '1'
const initialStartDate = validSelectableDate(route.query.startDate) || yesterdayValue
const initialEndDate = validSelectableDate(route.query.endDate) || yesterdayValue
const dateRange = ref(allDatesSelected ? null : (initialStartDate <= initialEndDate
  ? [initialStartDate, initialEndDate]
  : [yesterdayValue, yesterdayValue]))
const contentType = ref(['all', 'short_video', 'video', 'series', 'episode'].includes(route.query.contentType) ? route.query.contentType : 'all')
const contentTypeOptions = [
  { label: '全部', value: 'all' },
  { label: '短视频', value: 'short_video' },
  { label: '视频', value: 'video' },
  { label: '剧集', value: 'series' },
  { label: '单集', value: 'episode' },
]
const dateShortcuts = [
  { text: '昨天', value: () => sameDayRange(-1) },
  { text: '最近 7 天', value: () => recentDayRange(7) },
  { text: '最近 30 天', value: () => recentDayRange(30) },
]
const { platforms, selectedPlatform, activePlatform, activeCapabilities, loadPlatforms } = usePlatformContext()

const capabilitySummary = computed(() => {
  const labels = { comments: '评论', danmaku: '弹幕', reviews: '评分', series: '剧集', completion_rate: '完播率' }
  const supported = Object.entries(activeCapabilities.value)
    .filter(([key, value]) => value && labels[key])
    .map(([key]) => labels[key])
  return supported.length ? supported.join('、') : '仅基础内容指标'
})

onMounted(async () => {
  try {
    await loadPlatforms()
    await loadContents()
  } catch (error) {
    ElMessage.error(error.message || '平台数据加载失败')
  }
})

onMounted(() => window.addEventListener('video-analytics:refresh-unified', loadContents))
onBeforeUnmount(() => window.removeEventListener('video-analytics:refresh-unified', loadContents))

watch(selectedPlatform, () => {
  const nextType = selectedPlatform.value === 'all' ? contentType.value : contentType.value
  const queryType = nextType === 'all' ? undefined : nextType
  if (route.query.platform !== (selectedPlatform.value === 'all' ? undefined : selectedPlatform.value) || route.query.contentType !== queryType) {
    router.replace({ query: { ...route.query, platform: selectedPlatform.value === 'all' ? undefined : selectedPlatform.value, contentType: queryType } })
  }
  page.value = 1
  loadContents()
})

watch(contentType, (next) => {
  router.replace({ query: { ...route.query, contentType: next === 'all' ? undefined : next } })
  page.value = 1
  loadContents()
})

watch(() => route.query.contentType, (value) => {
  const next = ['all', 'short_video', 'video', 'series', 'episode'].includes(value) ? value : 'all'
  if (contentType.value !== next) contentType.value = next
})

watch(() => [route.query.keyword, route.query.startDate, route.query.endDate, route.query.allDates], ([nextKeyword, nextStartDate, nextEndDate, nextAllDates]) => {
  const normalizedKeyword = typeof nextKeyword === 'string' ? nextKeyword : ''
  if (keyword.value !== normalizedKeyword) keyword.value = normalizedKeyword
  if (nextAllDates === '1') {
    if (dateRange.value !== null) dateRange.value = null
    return
  }
  const normalizedStartDate = validSelectableDate(nextStartDate) || dateRange.value?.[0]
  const normalizedEndDate = validSelectableDate(nextEndDate) || dateRange.value?.[1]
  if (normalizedStartDate && normalizedEndDate
      && (dateRange.value?.[0] !== normalizedStartDate || dateRange.value?.[1] !== normalizedEndDate)) {
    dateRange.value = [normalizedStartDate, normalizedEndDate]
  }
})

async function loadContents() {
  loading.value = true
  try {
    const response = await fetchPagedContents({
      platform: selectedPlatform.value === 'all' ? undefined : selectedPlatform.value,
      contentType: contentType.value === 'all' ? undefined : contentType.value,
      keyword: keyword.value.trim() || undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    contents.value = response.items
    comparisonIds.value = comparisonIds.value.filter((id) => response.items.some((item) => item.contentId === id))
    total.value = response.total
  } catch (error) {
    ElMessage.error(error.message || '内容数据加载失败')
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 1
  syncListQuery()
  loadContents()
}

function resetFilters() {
  keyword.value = ''
  contentType.value = 'all'
  const defaultDate = yesterday()
  dateRange.value = [defaultDate, defaultDate]
  page.value = 1
  applyFilters()
}

function useAllDates() {
  dateRange.value = null
  applyFilters()
}

function handlePageSizeChange(size) {
  pageSize.value = size
  page.value = 1
  syncListQuery()
  loadContents()
}

function handlePageChange() {
  syncListQuery()
  loadContents()
}

function syncListQuery() {
  router.replace({
    query: {
      ...route.query,
      keyword: keyword.value.trim() || undefined,
      capturedDate: undefined,
      allDates: dateRange.value === null ? '1' : undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
      page: page.value > 1 ? String(page.value) : undefined,
      pageSize: pageSize.value !== 20 ? String(pageSize.value) : undefined,
    },
  })
}

function yesterday() {
  return dateValue(dateWithOffset(-1))
}

function today() {
  return dateValue(new Date())
}

function dateValue(date) {
  return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-')
}

function dateWithOffset(offset) {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  date.setDate(date.getDate() + offset)
  return date
}

function sameDayRange(offset) {
  const date = dateWithOffset(offset)
  return [date, date]
}

function recentDayRange(days) {
  return [dateWithOffset(-(days - 1)), dateWithOffset(0)]
}

function disableFutureDate(date) {
  const endOfToday = new Date()
  endOfToday.setHours(23, 59, 59, 999)
  return date.getTime() > endOfToday.getTime()
}

function validSelectableDate(value) {
  return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value) && value <= today() ? value : null
}

function platformName(code) {
  return platforms.value.find((item) => item.platformCode === code)?.displayName ?? code
}
function contentTypeName(type) {
  return { short_video: '短视频', video: '视频', series: '剧集', episode: '单集', movie: '电影' }[type] ?? type
}
function nullableCount(value) {
  return value === null || value === undefined ? '不适用' : Number(value).toLocaleString('zh-CN')
}
function percentile(value) {
  return value === null || value === undefined ? '--' : `P${Math.round(Number(value) * 100)}`
}
function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '--'
}
function openContent(contentId) {
  router.push({
    name: 'contentDetail',
    params: { contentId },
    query: {
      from: 'contents',
      platform: selectedPlatform.value === 'all' ? undefined : selectedPlatform.value,
      contentType: contentType.value === 'all' ? undefined : contentType.value,
      keyword: keyword.value.trim() || undefined,
      allDates: dateRange.value === null ? '1' : undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
      page: page.value > 1 ? String(page.value) : undefined,
      pageSize: pageSize.value !== 20 ? String(pageSize.value) : undefined,
    },
  })
}

function compareSelected() {
  if (comparisonIds.value.length < 2 || comparisonIds.value.length > 5) return
  router.push({ name: 'contentCompare', query: { ids: comparisonIds.value.join(','), ...route.query } })
}
</script>
