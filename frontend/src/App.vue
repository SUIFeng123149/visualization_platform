<template>
  <el-container class="app-shell" v-loading="loading">
    <AppSidebar
      :items="navItems"
      :active-key="activeModule"
      @change="navigate"
    />

    <el-main class="main">
      <DashboardHeader
        :filters="filters"
        :title="moduleMeta.title"
        :description="moduleMeta.description"
        :show-channel-filter="moduleFilterConfig.showChannel"
        :show-period-filter="moduleFilterConfig.showPeriod"
        @update:filters="handleFiltersChange"
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
          :disabled="['aiAssistant', 'collector'].includes(activeModule)"
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
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import DashboardHeader from '@/components/layout/DashboardHeader.vue'
import MetricGrid from '@/components/metrics/MetricGrid.vue'
import { navItems } from '@/data/dashboard'
import { fetchVideoDetail } from '@/api/analysis'
import { refreshTasks } from '@/api/tasks'
import { createReportHistory, fetchAnomalyRules, fetchDataSourceStatuses, fetchReportHistory } from '@/api/platform'
import { exportExcelWorkbook } from '@/utils/exportExcel'
import {
  average,
  formatCompact,
  formatPercent,
  getInteractions,
  useDashboardData,
} from '@/composables/useDashboardData'

const {
  activeModule,
  loading,
  filters,
  heatRank,
  visibleHeatRank,
  videoSentiments,
  upPerformance,
  keywords,
  negativeComments,
  sentimentTrend,
  danmakuHotspots,
  dataQuality,
  handleRefresh: handleDashboardRefresh,
  handleFiltersChange,
} = useDashboardData()

const router = useRouter()
const route = useRoute()

const moduleCopy = {
  overview: ['数据总览', '聚合视频热度、弹幕、评论情感和 UP 主表现，快速判断整体增长状态。'],
  video: ['视频分析', '围绕单个视频拆解播放、互动、热度和口碑，定位爆款与待优化内容。'],
  danmaku: ['弹幕分析', '用时间轴识别观众集中反应片段，辅助剪辑、复盘和内容解释。'],
  comment: ['评论洞察', '分析评论情感结构和负面样本，服务舆情处理与用户反馈归因。'],
  creator: ['UP主画像', '横向比较创作者能力，识别高热度账号、口碑短板和合作优先级。'],
  task: ['任务中心', '把数据发现转成运营动作，沉淀可执行的分析工作流。'],
  collector: ['数据采集', '配置 B 站公开数据采集任务，按热门、首页、UP 主或关键词抓取视频、评论和弹幕。'],
  dataSource: ['数据监控', '监控各层数据表的数据量、更新时间和可访问状态，保证图表可信。'],
  reportCenter: ['报表中心', '沉淀报表生成历史，统一管理导出记录和后续下载链路。'],
  anomalyRule: ['规则管理', '配置异常检测阈值，为热度、情感、互动和弹幕预警提供规则基础。'],
  aiAssistant: ['AI助手', '接入 Dify 应用，辅助解释指标、生成复盘建议和排查数据问题。'],
}

const exportCopy = {
  overview: ['完整数据报表', '导出热度、情感、弹幕、关键词、UP主和负面评论全部工作表。', '导出完整报表'],
  video: ['视频专项报表', '导出视频热度排行和情感统计，用于内容复盘。', '导出视频专项'],
  videoDetail: ['单视频复盘报表', '导出当前视频的基础指标、情感、弹幕、关键词、负面评论和运营建议。', '导出当前视频'],
  danmaku: ['弹幕专项报表', '导出当前视频的弹幕高峰时间点、弹幕数、情感和关键词。', '导出弹幕专项'],
  comment: ['评论专项报表', '导出评论情感趋势、情感占比和负面评论样本。', '导出评论专项'],
  creator: ['UP主专项报表', '导出 UP 主表现排行和能力评估数据。', '导出 UP主专项'],
  task: ['运营任务报表', '导出基于数据库分析结果自动生成的运营动作建议。', '导出任务专项'],
  collector: ['数据采集任务', '采集任务产物为本地 CSV/JSONL 文件，暂不纳入 Excel 导出。', '无需导出'],
  dataSource: ['数据监控报表', '导出数据源状态、数据量和同步健康情况。', '导出监控报表'],
  reportCenter: ['报表历史报表', '导出当前报表中心记录。', '导出报表历史'],
  anomalyRule: ['异常规则报表', '导出当前异常检测规则配置。', '导出规则配置'],
  aiAssistant: ['AI助手对话', 'AI 对话内容暂不纳入 Excel 导出。', '无需导出'],
}

const metricDefinitions = [
  ['总播放量', '当前筛选视频播放量求和，来源 ads_video_heat_rank.view_count。'],
  ['互动总量', '点赞、投币、收藏、评论、弹幕五项互动求和。'],
  ['平均情感', '视频情感均值，来源 ads_video_sentiment.avg_sentiment。'],
  ['最高热度', '当前筛选范围内 heat_score 最高的视频热度分。'],
  ['正向占比', '评论情感正向数 / 评论样本总数；缺少样本时显示暂无。'],
]

watch(
  () => route.name,
  (name) => {
    const moduleName = name === 'videoDetail' ? 'video' : name
    if (moduleName && moduleName !== activeModule.value) {
      activeModule.value = moduleName
    }
  },
  { immediate: true },
)

function navigate(key) {
  activeModule.value = key
  router.push({ name: key })
}

async function handleHeaderRefresh() {
  if (activeModule.value === 'task') {
    window.dispatchEvent(new CustomEvent('bililens:refresh-tasks'))
    return
  }

  await handleDashboardRefresh()
}

const moduleMeta = computed(() => {
  const [title, description] = moduleCopy[activeModule.value] ?? moduleCopy.overview
  return { title, description }
})

const exportMeta = computed(() => {
  const key = route.name === 'videoDetail' ? 'videoDetail' : activeModule.value
  const [title, description, buttonText] = exportCopy[key] ?? exportCopy.overview
  return { title, description, buttonText }
})

const moduleFilterConfig = computed(() => {
  const config = {
    overview: { showChannel: true, showPeriod: true },
    video: { showChannel: false, showPeriod: false },
    danmaku: { showChannel: false, showPeriod: false },
    comment: { showChannel: false, showPeriod: true },
    creator: { showChannel: false, showPeriod: false },
    task: { showChannel: false, showPeriod: false },
    collector: { showChannel: false, showPeriod: false },
    dataSource: { showChannel: false, showPeriod: false },
    reportCenter: { showChannel: false, showPeriod: false },
    anomalyRule: { showChannel: false, showPeriod: false },
    aiAssistant: { showChannel: false, showPeriod: false },
  }
  return config[activeModule.value] ?? config.overview
})

const moduleMetrics = computed(() => {
  const totalViews = visibleHeatRank.value.reduce((sum, item) => sum + item.viewCount, 0)
  const totalInteraction = visibleHeatRank.value.reduce((sum, item) => sum + getInteractions(item), 0)
  const avgSentiment = average(videoSentiments.value.map((item) => item.avgSentiment))
  const highestHeat = visibleHeatRank.value[0]?.heatScore ?? 0
  const currentVideo = heatRank.value[0]
  const currentVideoSentiment = videoSentiments.value.find((item) => item.bvid === currentVideo?.bvid)
  const currentVideoInteractions = currentVideo ? getInteractions(currentVideo) : 0
  const currentVideoInteractionRate = currentVideo?.viewCount > 0 ? currentVideoInteractions / currentVideo.viewCount : 0

  if (activeModule.value === 'video') {
    return [
      metric('当前视频播放', currentVideo ? formatCompact(currentVideo.viewCount) : '--', currentVideo?.upName ?? '--', '当前热度榜首样本', 'up', '视频分析页默认展示热度榜首视频的单视频指标。'),
      metric('当前视频互动率', `${(currentVideoInteractionRate * 100).toFixed(1)}%`, formatCompact(currentVideoInteractions), '互动总量/播放量', 'up', '互动总量包含点赞、投币、收藏、评论、弹幕。'),
      metric('当前视频热度', currentVideo ? Math.round(currentVideo.heatScore).toLocaleString('zh-CN') : '--', currentVideo?.bvid ?? '--', '单视频热度分', 'up', '后端 heat_score 综合播放、互动和弹幕等指标生成。'),
      metric('当前视频口碑', currentVideoSentiment ? formatPercent(currentVideoSentiment.positiveRatio) : '--', currentVideoSentiment ? `${currentVideoSentiment.totalCount} 条样本` : '暂无样本', '正向占比', 'up', '正向占比来自评论情感统计；没有评论样本时不计算。'),
    ]
  }

  if (activeModule.value === 'comment') {
    const negativeCount = videoSentiments.value.reduce((sum, item) => sum + item.negativeCount, 0)
    return [
      metric('负向评论', formatCompact(negativeCount), `${negativeComments.value.length} 条样本`, '待排查', 'warn', '负向评论数量来自视频情感统计中的 negative_count。'),
      metric('平均情感', avgSentiment.toFixed(3), avgSentiment >= 0.55 ? '偏正向' : '需关注', '按视频均值', avgSentiment >= 0.55 ? 'up' : 'warn', '对当前情感样本求 avg_sentiment 均值。'),
      metric('评论总量', formatCompact(videoSentiments.value.reduce((sum, item) => sum + item.totalCount, 0)), '情感样本', '来自分析表', 'up', '情感模型参与统计的评论样本总量。'),
      metric('关键词数', keywords.value.length, 'TopN', '可用于归因', 'up', '关键词来自关键词聚合接口，优先使用全局维度。'),
    ]
  }

  if (activeModule.value === 'creator') {
    return [
      metric('UP主数量', upPerformance.value.length, '参与排行', '当前样本', 'up', '当前 UP 主表现排行返回的创作者数量。'),
      metric('最高平均热度', formatCompact(Math.max(0, ...upPerformance.value.map((item) => item.avgHeatScore))), 'Top 1', '按平均热度', 'up', '按 UP 主历史样本的 avg_heat_score 排序。'),
      metric('总点赞量', formatCompact(upPerformance.value.reduce((sum, item) => sum + item.totalLikeCount, 0)), '累计', '账号互动', 'up', '当前 UP 主样本的点赞数求和。'),
      metric('平均情感', avgSentiment.toFixed(3), '口碑参考', '跨视频均值', 'up', '用视频情感均值辅助判断创作者口碑。'),
    ]
  }

  if (['collector', 'dataSource', 'reportCenter', 'anomalyRule', 'aiAssistant'].includes(activeModule.value)) {
    return []
  }

  return [
    metric('总播放量', formatCompact(totalViews), `${visibleHeatRank.value.length} 个视频`, '来自热度排行', 'up', metricDefinitions[0][1]),
    metric('互动总量', formatCompact(totalInteraction), '点赞/投币/收藏', '评论弹幕合计', 'up', metricDefinitions[1][1]),
    metric('平均情感', avgSentiment.toFixed(3), avgSentiment >= 0.55 ? '偏正向' : '需关注', '按视频均值', avgSentiment >= 0.55 ? 'up' : 'warn', metricDefinitions[2][1]),
    metric('最高热度', Math.round(highestHeat).toLocaleString('zh-CN'), visibleHeatRank.value[0]?.bvid ?? '--', '当前榜首', 'up', metricDefinitions[3][1]),
  ]
})

function metric(label, value, delta, note, status, description) {
  return { label, value, delta, note, status, description }
}

async function handleExportReport() {
  try {
    const moduleKey = route.name === 'videoDetail' ? 'videoDetail' : activeModule.value
    const sheets = await buildReportSheets(moduleKey)
    const dataRows = sheets.filter((sheet) => sheet.name !== '报表说明').reduce((sum, sheet) => sum + sheet.rows.length, 0)
    if (dataRows === 0) {
      ElMessage.warning('暂无可导出的数据')
      return
    }
    const filename = `BiliLens_${exportMeta.value.title}_${new Date().toISOString().slice(0, 10)}.xlsx`
    exportExcelWorkbook(filename, sheets)
    createReportHistory({
      reportName: exportMeta.value.title,
      reportType: moduleKey,
      fileName: filename,
      rowCount: dataRows,
      remark: '由页面导出动作自动记录。',
    }).catch(() => {})
    ElMessage.success(`${exportMeta.value.title}已导出`)
  } catch (error) {
    ElMessage.error(error.message || '导出失败，请稍后重试')
  }
}

async function buildReportSheets(moduleKey) {
  if (moduleKey === 'videoDetail') {
    return withReportContext(await buildVideoDetailSheets(route.params.bvid), moduleKey)
  }

  if (moduleKey === 'task') {
    return withReportContext(await buildTaskSheets(), moduleKey)
  }

  if (moduleKey === 'collector') {
    return []
  }

  if (moduleKey === 'dataSource') {
    return withReportContext(await buildDataSourceSheets(), moduleKey)
  }

  if (moduleKey === 'reportCenter') {
    return withReportContext(await buildReportHistorySheets(), moduleKey)
  }

  if (moduleKey === 'anomalyRule') {
    return withReportContext(await buildAnomalyRuleSheets(), moduleKey)
  }

  if (moduleKey === 'aiAssistant') {
    return []
  }

  const videoRankRows = moduleKey === 'video' ? heatRank.value : visibleHeatRank.value
  const videoBvids = new Set(videoRankRows.map((item) => item.bvid))
  const videoSentimentRows = ['overview', 'video'].includes(moduleKey)
    ? videoSentiments.value.filter((item) => videoBvids.has(item.bvid))
    : videoSentiments.value

  const sheets = {
    videoRank: sheet('视频热度排行', [
      ['rankNo', '排名'], ['bvid', 'BVID'], ['title', '标题'], ['upName', 'UP主'], ['category', '分区'],
      ['viewCount', '播放量'], ['likeCount', '点赞数'], ['coinCount', '投币数'], ['favoriteCount', '收藏数'],
      ['replyCount', '评论数'], ['danmakuCount', '弹幕数'], ['heatScore', '热度分'], ['interactionCount', '互动总量'],
    ], videoRankRows.map((item) => ({ ...item, interactionCount: getInteractions(item) }))),
    videoSentiment: sheet('视频情感统计', [
      ['bvid', 'BVID'], ['title', '标题'], ['avgSentiment', '平均情感'], ['positiveCount', '正向数'],
      ['neutralCount', '中性数'], ['negativeCount', '负向数'], ['totalCount', '总数'],
      ['positiveRatio', '正向占比'], ['negativeRatio', '负向占比'],
    ], videoSentimentRows),
    trend: sheet('情感趋势', [
      ['statDate', '日期'], ['commentCount', '评论数'], ['danmakuCount', '弹幕数'],
      ['avgSentiment', '平均情感'], ['negativeRatio', '负向占比'],
    ], sentimentTrend.value),
    danmaku: sheet('弹幕高峰', [
      ['bvid', 'BVID'], ['timeText', '时间点'], ['timeBucket', '秒数'],
      ['danmakuCount', '弹幕数'], ['avgSentiment', '平均情感'], ['topWords', '关键词'],
    ], danmakuHotspots.value),
    keywords: sheet('关键词TopN', [
      ['rankNo', '排名'], ['word', '关键词'], ['wordCount', '出现次数'],
    ], keywords.value),
    creator: sheet('UP主表现', [
      ['upName', 'UP主'], ['videoCount', '视频数'], ['avgViewCount', '平均播放'],
      ['avgHeatScore', '平均热度'], ['avgSentiment', '平均情感'], ['totalLikeCount', '总点赞'],
    ], upPerformance.value),
    negative: sheet('负面评论样本', [
      ['bvid', 'BVID'], ['rpid', '评论ID'], ['userName', '用户'], ['cleanContent', '评论内容'],
      ['likeCount', '点赞数'], ['crawledAt', '采集时间'], ['sentimentScore', '情感分'],
    ], negativeComments.value),
  }

  const map = {
    overview: [sheets.videoRank, sheets.videoSentiment, sheets.trend, sheets.danmaku, sheets.keywords, sheets.creator, sheets.negative],
    video: [sheets.videoRank, sheets.videoSentiment],
    danmaku: [sheets.danmaku],
    comment: [sheets.videoSentiment, sheets.trend, sheets.negative],
    creator: [sheets.creator],
  }
  return withReportContext(map[moduleKey] ?? map.overview, moduleKey)
}

async function buildTaskSheets() {
  const tasks = await refreshTasks()
  return [
    sheet('运营任务', [
      ['taskId', '任务ID'], ['title', '任务'], ['level', '优先级'], ['type', '类型'],
      ['status', '状态'], ['text', '说明/触发规则'], ['bvid', '关联视频'], ['source', '来源'],
      ['sortNo', '排序'], ['statusUpdatedAt', '状态更新时间'], ['updatedAt', '任务更新时间'],
    ], tasks),
  ]
}

async function buildDataSourceSheets() {
  const rows = await fetchDataSourceStatuses()
  return [
    sheet('数据源状态', [
      ['tableName', '表名'], ['displayName', '显示名称'], ['layer', '层级'], ['rowCount', '行数'],
      ['latestAt', '最近时间'], ['status', '状态'], ['message', '说明'],
    ], rows),
  ]
}

async function buildReportHistorySheets() {
  const rows = await fetchReportHistory()
  return [
    sheet('报表历史', [
      ['id', 'ID'], ['reportName', '报表名称'], ['reportType', '类型'], ['status', '状态'],
      ['rowCount', '数据行数'], ['fileName', '文件名'], ['remark', '备注'], ['createdAt', '生成时间'],
    ], rows),
  ]
}

async function buildAnomalyRuleSheets() {
  const rows = await fetchAnomalyRules()
  return [
    sheet('异常规则', [
      ['ruleKey', '规则Key'], ['name', '规则名称'], ['metric', '指标'], ['operator', '条件'],
      ['threshold', '阈值'], ['level', '级别'], ['enabled', '启用'], ['description', '说明'], ['updatedAt', '更新时间'],
    ], rows),
  ]
}

async function buildVideoDetailSheets(bvid) {
  if (!bvid) return []

  const detail = await fetchVideoDetail(bvid)
  const video = detail.video
  if (!video) return []

  const interactionCount = getInteractions(video)
  const interactionRate = video.viewCount > 0 ? interactionCount / video.viewCount : 0
  const videoRows = [{ ...video, interactionCount, interactionRate }]

  return [
    sheet('视频基础指标', [
      ['rankNo', '排名'], ['bvid', 'BVID'], ['title', '标题'], ['upName', 'UP主'], ['category', '分区'],
      ['viewCount', '播放量'], ['likeCount', '点赞数'], ['coinCount', '投币数'], ['favoriteCount', '收藏数'],
      ['replyCount', '评论数'], ['danmakuCount', '弹幕数'], ['heatScore', '热度分'], ['interactionCount', '互动总量'],
      ['interactionRate', '互动率'],
    ], videoRows),
    sheet('情感统计', [
      ['bvid', 'BVID'], ['title', '标题'], ['avgSentiment', '平均情感'], ['positiveCount', '正向数'],
      ['neutralCount', '中性数'], ['negativeCount', '负向数'], ['totalCount', '评论样本数'],
      ['positiveRatio', '正向占比'], ['negativeRatio', '负向占比'],
    ], detail.sentiment ? [detail.sentiment] : []),
    sheet('弹幕时间轴', [
      ['bvid', 'BVID'], ['timeText', '时间点'], ['timeBucket', '秒数'], ['danmakuCount', '弹幕数'],
      ['avgSentiment', '平均情感'], ['topWords', '关键词'],
    ], (detail.danmakuTimeline ?? []).map((item) => ({ ...item, timeText: formatSeconds(item.timeBucket) }))),
    sheet('主题关键词', [
      ['rankNo', '排名'], ['word', '关键词'], ['wordCount', '出现次数'],
    ], detail.keywords ?? []),
    sheet('负面评论样本', [
      ['bvid', 'BVID'], ['rpid', '评论ID'], ['userName', '用户'], ['cleanContent', '评论内容'],
      ['likeCount', '点赞数'], ['crawledAt', '采集时间'], ['sentimentScore', '情感分'],
    ], detail.negativeComments ?? []),
    sheet('运营建议', [
      ['title', '建议'], ['level', '优先级'], ['type', '类型'], ['reason', '原因'], ['action', '动作'],
    ], detail.insights ?? []),
  ]
}

function withReportContext(sheets, moduleKey) {
  return [buildContextSheet(moduleKey), ...sheets]
}

function buildContextSheet(moduleKey) {
  const [title] = exportCopy[moduleKey] ?? exportCopy.overview
  const rows = [
    { item: '报表名称', value: title },
    { item: '生成时间', value: new Date().toLocaleString('zh-CN', { hour12: false }) },
    { item: '页面模块', value: moduleCopy[moduleKey === 'videoDetail' ? 'video' : moduleKey]?.[0] ?? moduleCopy.overview[0] },
    { item: '分区筛选', value: filters.channel === 'all' ? '全部类型' : filters.channel },
    { item: '趋势周期', value: filters.period },
    { item: '当前视频数', value: dataQuality.value.visibleVideoCount },
    { item: '数据质量', value: dataQuality.value.warnings.length ? dataQuality.value.warnings.join('；') : '当前筛选范围内未发现明显缺口' },
    { item: '任务来源说明', value: moduleKey === 'task' ? '/api/tasks/refresh 自动分析数据库并生成任务' : '非任务报表' },
    ...metricDefinitions.map(([name, desc]) => ({ item: `口径：${name}`, value: desc })),
  ]
  return sheet('报表说明', [['item', '项目'], ['value', '说明']], rows)
}

function sheet(name, columns, rows) {
  return {
    name,
    columns: columns.map(([key, label]) => ({ key, label })),
    rows,
  }
}

function formatSeconds(value) {
  const secondsValue = Number(value) || 0
  const minutes = Math.floor(secondsValue / 60)
  const seconds = String(secondsValue % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
}
</script>
