<template>
  <el-container class="app-shell">
    <AppSidebar :items="navItems" :active-key="activeModule" @change="navigate" />
    <el-main class="main">
      <DashboardHeader
        :filters="filters"
        :title="moduleMeta.title"
        :description="moduleMeta.description"
        :show-channel-filter="false"
        :show-period-filter="false"
        :platforms="platforms"
        :platform="selectedPlatform"
        :loading-platforms="loadingPlatforms"
        @update:filters="handleFiltersChange"
        @platform-change="handlePlatformChange"
        @refresh="handleHeaderRefresh"
      />

      <div class="export-toolbar">
        <div>
          <strong>{{ exportMeta.title }}</strong>
          <span>{{ exportMeta.description }}</span>
        </div>
        <el-button
          class="export-button"
          type="success"
          :icon="Download"
          :disabled="!exportMeta.enabled"
          @click="handleExportReport"
        >
          {{ exportMeta.buttonText }}
        </el-button>
      </div>

      <MetricGrid :metrics="moduleMetrics" />
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import DashboardHeader from '@/components/layout/DashboardHeader.vue'
import MetricGrid from '@/components/metrics/MetricGrid.vue'
import { navItems } from '@/data/dashboard'
import { refreshTasks } from '@/api/tasks'
import { createReportHistory, fetchAnomalyRules, fetchDataSourceStatuses, fetchMetricConfigs, fetchPlatformConfigs, fetchReportHistory } from '@/api/platform'
import { fetchAccountPerformance, fetchContent, fetchMetricComparison, fetchMetricDefinitions, fetchPagedContents } from '@/api/content'
import { exportExcelWorkbook } from '@/utils/exportExcel'
import { usePlatformContext } from '@/composables/usePlatformContext'
import { useUnifiedAnalytics } from '@/composables/useUnifiedAnalytics'
import { useCommentInsights } from '@/composables/useCommentInsights'

const router = useRouter()
const route = useRoute()
const filters = reactive({ channel: 'all', period: '30d' })
const { platforms, selectedPlatform, loadingPlatforms, loadPlatforms } = usePlatformContext()
const { loadUnifiedAnalytics, loadAccountPerformance } = useUnifiedAnalytics()
const { summary: commentSummary } = useCommentInsights()
loadPlatforms().catch(() => {})

const activeModule = computed(() => ['contentDetail', 'contentDanmaku'].includes(route.name) ? 'contents' : route.name || 'contents')
const platformModules = new Set(['contents', 'metrics', 'comment', 'creator', 'collector', 'dataSource'])

watch(() => route.query.platform, (platform) => {
  const next = typeof platform === 'string' && platform.trim() ? platform : 'all'
  if (selectedPlatform.value !== next) selectedPlatform.value = next
}, { immediate: true })

const moduleCopy = {
  overview: ['运营仪表盘', '汇总核心表现、待处理风险和数据健康状态，作为日常运营的决策入口。'],
  contents: ['内容分析', '查看已接入视频平台的内容表现与互动数据。'],
  metrics: ['指标对比', '基于统一指标字典对比各平台最新内容快照。'],
  comment: ['互动洞察', '分析评论、回复、剧评及其情感覆盖情况。'],
  creator: ['账号表现', '比较创作者、频道和发行方的内容表现。'],
  task: ['任务中心', '跟踪由分析结果生成的运营工作。'],
  collector: ['数据采集', '通过已接入的爬虫平台创建和管理网页采集任务。'],
  dataSource: ['数据监控', '监控 v2 数据覆盖率、新鲜度和采集健康状态。'],
  platformConfig: ['平台管理', '管理平台连接器、启用状态和数据能力。'],
  metricConfig: ['指标管理', '管理指标中文名称、口径、单位和跨平台可比性。'],
  reportCenter: ['报表中心', '查看已导出的报表历史。'],
  anomalyRule: ['规则管理', '配置分析预警阈值。'],
  aiAssistant: ['AI 助手', '使用已配置的助手分析平台数据。'],
}

const exportCopy = {
  overview: ['运营仪表盘', '仪表盘以实时查看和跳转处置为主，不单独导出。', '暂不导出', false],
  contents: ['内容分析报表', '导出当前筛选范围内的内容快照。', '导出报表', true],
  metrics: ['指标对比报表', '导出统一指标定义与当前对比结果。', '导出报表', true],
  comment: ['互动洞察报表', '导出当前 v2 互动分析结果。', '导出报表', true],
  creator: ['创作者画像报表', '导出当前平台范围内的账号表现。', '导出报表', true],
  task: ['任务报表', '导出当前运营任务。', '导出报表', true],
  collector: ['采集任务报表', '爬虫平台未提供任务列表接口，暂不支持导出。', '暂不可导出', false],
  dataSource: ['数据监控报表', '导出 v2 数据源与平台采集状态。', '导出报表', true],
  reportCenter: ['报表历史', '导出报表历史记录。', '导出报表', true],
  anomalyRule: ['规则配置', '导出预警规则。', '导出报表', true],
  platformConfig: ['平台配置报表', '导出已配置的平台、连接器和能力。', '导出报表', true],
  metricConfig: ['指标配置报表', '导出统一指标定义和可比性配置。', '导出报表', true],
  default: ['导出', '当前页面暂不提供报表导出。', '暂不可导出', false],
}

const moduleMeta = computed(() => {
  const [title, description] = moduleCopy[activeModule.value] ?? moduleCopy.contents
  return { title, description }
})

const exportMeta = computed(() => {
  const [title, description, buttonText, enabled] = exportCopy[activeModule.value] ?? exportCopy.default
  return { title, description, buttonText, enabled }
})

const moduleMetrics = computed(() => {
  if (activeModule.value !== 'comment') return []
  const total = commentSummary.value.interactionCount || 0
  const analyzed = commentSummary.value.analyzedCount || 0
  const coverage = total ? analyzed / total : 0
  return [
    metric('互动总量', total.toLocaleString('zh-CN'), '评论 / 回复 / 剧评', '当前筛选范围总量', 'up'),
    metric('已分析样本', analyzed.toLocaleString('zh-CN'), `${(coverage * 100).toFixed(1)}%`, '情感分析覆盖率', coverage >= 0.8 ? 'up' : 'warn'),
    metric('平均情感', commentSummary.value.averageSentiment == null ? '--' : Number(commentSummary.value.averageSentiment).toFixed(3), '', '统一情感分数', 'up'),
    metric('负向样本', (commentSummary.value.negativeCount || 0).toLocaleString('zh-CN'), '', '待重点复盘', commentSummary.value.negativeCount ? 'warn' : 'up'),
  ]
})

function metric(label, value, delta, note, status) {
  return { label, value, delta, note, status, description: note }
}

function navigate(key) {
  const query = platformModules.has(key) && selectedPlatform.value !== 'all' ? { platform: selectedPlatform.value } : undefined
  router.push({ name: key, query })
}

async function handleHeaderRefresh() {
  if (activeModule.value === 'task') {
    window.dispatchEvent(new CustomEvent('video-analytics:refresh-tasks'))
    return
  }
  if (activeModule.value === 'comment') {
    window.dispatchEvent(new CustomEvent('video-analytics:refresh-comments'))
    return
  }
  if (activeModule.value === 'creator') {
    await loadAccountPerformance()
    return
  }
  await loadUnifiedAnalytics()
  window.dispatchEvent(new CustomEvent('video-analytics:refresh-unified'))
}

function handleFiltersChange(nextFilters) {
  Object.assign(filters, nextFilters)
}

function handlePlatformChange(value) {
  selectedPlatform.value = value
  if (value !== 'all' && !platformModules.has(activeModule.value)) {
    router.push({ name: 'contents', query: { platform: value } })
    return
  }
  router.replace({ query: { ...route.query, platform: value === 'all' ? undefined : value } })
}

async function handleExportReport() {
  try {
    const sheets = await buildReportSheets(activeModule.value)
    const rows = sheets.reduce((sum, sheet) => sum + sheet.rows.length, 0)
    if (!rows) {
      ElMessage.warning('暂无可导出的数据')
      return
    }
    const filename = `视频数据平台-${activeModule.value}-${new Date().toISOString().slice(0, 10)}.xlsx`
    exportExcelWorkbook(filename, sheets)
    createReportHistory({ reportName: exportMeta.value.title, reportType: activeModule.value, fileName: filename, rowCount: rows, remark: '由 v2 统一界面导出。' }).catch(() => {})
    ElMessage.success('报表已导出')
  } catch (error) {
    ElMessage.error(error.message || '报表导出失败')
  }
}

async function buildReportSheets(module) {
  const platform = selectedPlatform.value === 'all' ? undefined : selectedPlatform.value
  if (module === 'contents') {
    if (route.name === 'contentDetail' || route.name === 'contentDanmaku') {
      const content = await fetchContent(route.params.contentId)
      return [sheet('内容复盘', ['contentId', 'platformCode', 'externalContentId', 'contentType', 'title', 'accountName', 'category', 'publishedAt', 'viewCount', 'likeCount', 'commentCount', 'danmakuCount', 'normalizedHeatScore', 'metricsCapturedAt'], [content])]
    }
    const result = await fetchPagedContents({
      platform,
      contentType: route.query.contentType || undefined,
      keyword: route.query.keyword || undefined,
      startDate: route.query.startDate || undefined,
      endDate: route.query.endDate || undefined,
      page: 1,
      pageSize: 5000,
    })
    return [sheet('内容快照', ['contentId', 'platformCode', 'externalContentId', 'contentType', 'title', 'accountName', 'category', 'publishedAt', 'viewCount', 'likeCount', 'commentCount', 'danmakuCount', 'normalizedHeatScore', 'metricsCapturedAt'], result.items || [])]
  }
  if (module === 'metrics') {
    const definitions = await fetchMetricDefinitions()
    const comparisonSheets = await Promise.all(definitions.map(async (definition) => sheet(
      definition.displayName || definition.metricKey,
      ['platformCode', 'contentCount', 'availableCount', 'averageValue', 'minValue', 'maxValue'],
      await fetchMetricComparison({ metricKey: definition.metricKey, platform }),
    )))
    return [sheet('指标定义', ['metricKey', 'displayName', 'definition', 'unit', 'comparable'], definitions), ...comparisonSheets]
  }
  if (module === 'comment') {
    return [sheet('互动汇总', Object.keys(commentSummary.value), [commentSummary.value])]
  }
  if (module === 'creator') return [sheet('账号表现', ['accountId', 'platformCode', 'displayName', 'accountType', 'contentCount', 'totalViewCount', 'totalLikeCount', 'averageNormalizedHeat'], await fetchAccountPerformance({ platform }))]
  if (module === 'task') return [sheet('运营任务', ['taskId', 'title', 'level', 'type', 'status', 'text', 'contentId', 'platformCode', 'externalContentId', 'source', 'sortNo', 'updatedAt'], await refreshTasks())]
  if (module === 'dataSource') return [sheet('数据源状态', ['tableName', 'displayName', 'layer', 'rowCount', 'latestAt', 'status', 'message'], await fetchDataSourceStatuses())]
  if (module === 'platformConfig') return [sheet('平台配置', ['platformCode', 'displayName', 'connectorName', 'capabilities', 'enabled', 'updatedAt'], await fetchPlatformConfigs())]
  if (module === 'metricConfig') return [sheet('指标配置', ['metricKey', 'displayName', 'definition', 'unit', 'comparable', 'enabled', 'updatedAt'], await fetchMetricConfigs())]
  if (module === 'reportCenter') return [sheet('报表历史', ['id', 'reportName', 'reportType', 'status', 'rowCount', 'fileName', 'remark', 'createdAt'], await fetchReportHistory())]
  if (module === 'anomalyRule') return [sheet('预警规则', ['ruleKey', 'name', 'metric', 'operator', 'threshold', 'level', 'enabled', 'description', 'updatedAt'], await fetchAnomalyRules())]
  return []
}

function sheet(name, keys, rows) {
  return { name: String(name).replace(/[\\/:?*\[\]]/g, '_').slice(0, 31) || '数据', columns: keys.map((key) => ({ key, label: columnLabel(key) })), rows: Array.isArray(rows) ? rows : [] }
}

function columnLabel(key) {
  return {
    contentId: '内容 ID', externalContentId: '外部内容 ID', contentType: '内容类型', category: '分类', publishedAt: '发布时间',
    accountId: '账号 ID', accountName: '发布账号', displayName: '显示名称', accountType: '账号类型',
    viewCount: '播放量', likeCount: '点赞量', commentCount: '评论量', danmakuCount: '弹幕量', normalizedHeatScore: '归一化热度', metricsCapturedAt: '指标时间',
    totalViewCount: '累计播放量', totalLikeCount: '累计点赞量', averageNormalizedHeat: '平均归一化热度', contentCount: '内容数', availableCount: '有指标内容数', averageValue: '平均值', minValue: '最小值', maxValue: '最大值',
    metricKey: '指标代码', displayName: '指标名称', definition: '指标口径', unit: '单位', comparable: '可跨平台比较',
    taskId: '任务 ID', taskName: '任务名称', connectorName: '连接器', targetType: '目标类型', progress: '进度', batchId: '批次号', capabilities: '可用能力',
    taskId: '任务 ID', title: '标题', level: '优先级', type: '类型', status: '状态', text: '说明',
    contentId: '内容 ID', platformCode: '平台', externalContentId: '外部内容 ID', source: '来源',
    sortNo: '排序', updatedAt: '更新时间', tableName: '数据表', displayName: '显示名称',
    layer: '层级', rowCount: '数据量', latestAt: '最近时间', message: '说明', id: 'ID',
    reportName: '报表名称', reportType: '报表类型', fileName: '文件名', remark: '备注',
    createdAt: '创建时间', ruleKey: '规则键', name: '名称', metric: '指标', operator: '条件',
    threshold: '阈值', enabled: '启用', description: '说明', interactionCount: '互动总量',
    analyzedCount: '已分析数量', positiveCount: '正向数量', neutralCount: '中性数量',
    negativeCount: '负向数量', averageSentiment: '平均情感', positiveRatio: '正向占比',
    neutralRatio: '中性占比', negativeRatio: '负向占比',
  }[key] ?? key
}
</script>
