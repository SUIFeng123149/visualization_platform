<template>
  <el-container class="app-shell" v-loading="loading">
    <AppSidebar :items="navItems" :active-key="activeModule" @change="activeModule = $event" />

    <el-main class="main">
      <DashboardHeader
        :filters="filters"
        :title="moduleMeta.title"
        :description="moduleMeta.description"
        @update:filters="handleFiltersChange"
        @refresh="handleRefresh"
      />

      <el-alert
        v-if="loadError"
        class="load-error"
        type="error"
        :title="loadError"
        show-icon
        :closable="false"
      >
        <template #default>
          <el-button link type="primary" @click="handleRefresh">重新加载</el-button>
        </template>
      </el-alert>

      <div class="export-toolbar">
        <div>
          <strong>{{ exportMeta.title }}</strong>
          <span>{{ exportMeta.description }}</span>
        </div>
        <el-button class="export-button" type="success" :icon="Download" @click="handleExportReport">
          {{ exportMeta.buttonText }}
        </el-button>
      </div>

      <MetricGrid :metrics="moduleMetrics" />

      <template v-if="activeModule === 'overview'">
        <section class="overview-columns">
          <div class="stack">
            <ChartPanel title="播放量 / 互动量动态趋势" description="用于观察评论、弹幕和情感随时间的变化节奏。">
              <template #actions>
                <el-segmented v-model="trendMode" :options="['日', '周', '月']" />
              </template>
              <TrendChart :mode="trendMode" :data="sentimentTrend" />
            </ChartPanel>

            <ChartPanel title="弹幕时间轴高峰点" description="散点大小代表同一时间段弹幕密度，用来定位视频高潮点。">
              <DanmakuScatterChart :data="danmakuTimeline" />
              <InsightCards :items="dynamicInsightCards" />
            </ChartPanel>

            <VideoTable v-model:keyword="keyword" :videos="filteredVideos" />
          </div>

          <div class="stack">
            <ChartPanel title="评论情感占比" description="快速判断当前评论区口碑结构。">
              <SentimentPieChart :data="videoSentiments" />
            </ChartPanel>

            <ChartPanel title="UP主能力雷达" description="对比创作者的播放、热度、口碑和互动能力。">
              <UpRadarChart :data="upPerformance" />
            </ChartPanel>

            <RecommendationPanel :items="recommendations" />
          </div>
        </section>
      </template>

      <template v-else-if="activeModule === 'video'">
        <section class="video-summary-grid">
          <ChartPanel title="视频情感结构" description="用于判断高播放视频是否同时具备好口碑。">
            <SentimentPieChart :data="videoSentiments" />
          </ChartPanel>
          <section class="analysis-list compact">
            <article v-for="item in videoCards.slice(0, 4)" :key="item.bvid" class="analysis-card">
              <div class="analysis-card-head">
                <strong>{{ item.title }}</strong>
                <el-tag :type="item.rankNo <= 3 ? 'success' : 'info'">Rank {{ item.rankNo }}</el-tag>
              </div>
              <div class="analysis-card-grid">
                <span>播放量：{{ formatCompact(item.viewCount) }}</span>
                <span>互动量：{{ formatCompact(item.interactions) }}</span>
                <span>热度：{{ Math.round(item.heatScore).toLocaleString('zh-CN') }}</span>
                <span>UP主：{{ item.upName || '--' }}</span>
              </div>
            </article>
          </section>
        </section>
        <VideoTable
          v-model:keyword="keyword"
          :videos="filteredVideos"
          title="视频表现分析"
          description="按热度、互动率和情感表现筛选可复盘视频。"
        />
      </template>

      <template v-else-if="activeModule === 'danmaku'">
        <section class="analysis-grid">
          <ChartPanel title="弹幕时间轴散点图" description="定位弹幕最密集的片段，辅助找名场面、争议点和剪辑切点。">
            <template #actions>
              <el-select v-model="selectedBvid" class="module-select" placeholder="选择视频">
                <el-option v-for="video in heatRank" :key="video.bvid" :label="video.title" :value="video.bvid" />
              </el-select>
            </template>
            <DanmakuScatterChart :data="danmakuTimeline" />
          </ChartPanel>
          <ChartPanel title="弹幕高峰列表" description="把图表中的高峰点转成可检查的时间节点。">
            <el-table :data="danmakuHotspots" style="width: 100%">
              <el-table-column prop="timeText" label="时间点" width="100" />
              <el-table-column prop="danmakuCount" label="弹幕数" width="100" />
              <el-table-column prop="avgSentimentText" label="情感" width="100" />
              <el-table-column prop="topWords" label="关键词" min-width="160" />
            </el-table>
          </ChartPanel>
        </section>
      </template>

      <template v-else-if="activeModule === 'comment'">
        <section class="analysis-grid">
          <ChartPanel title="评论情感占比" description="观察正向、中性、负向评论的整体结构。">
            <SentimentPieChart :data="videoSentiments" />
          </ChartPanel>
          <ChartPanel title="情感趋势" description="识别负向情绪是否在某一日期集中上升。">
            <TrendChart :mode="trendMode" :data="sentimentTrend" />
          </ChartPanel>
        </section>
        <section class="panel video-panel">
          <div class="panel-header">
            <div>
              <div class="panel-title">负面评论样本</div>
              <div class="panel-desc">用于舆情排查、运营回复和内容解释补充。</div>
            </div>
          </div>
          <el-table :data="negativeComments" style="width: 100%">
            <el-table-column prop="userName" label="用户" width="150" />
            <el-table-column prop="cleanContent" label="评论内容" min-width="300" />
            <el-table-column prop="likeCount" label="点赞" width="100" sortable />
            <el-table-column prop="sentimentScore" label="情感分" width="110" />
            <el-table-column prop="crawledAt" label="采集时间" width="190" />
          </el-table>
        </section>
      </template>

      <template v-else-if="activeModule === 'creator'">
        <section class="dashboard-grid">
          <ChartPanel title="UP主能力雷达图" description="比较创作者在产量、播放、热度、口碑和点赞上的综合能力。">
            <UpRadarChart :data="upPerformance" />
          </ChartPanel>
          <ChartPanel title="UP主表现排行" description="按平均热度排序，找到值得重点复盘的账号。">
            <el-table :data="upPerformance" style="width: 100%">
              <el-table-column prop="upName" label="UP主" min-width="160" />
              <el-table-column prop="videoCount" label="视频数" width="90" />
              <el-table-column prop="avgViewCount" label="平均播放" width="120" :formatter="numberColumn" />
              <el-table-column prop="avgHeatScore" label="平均热度" width="120" :formatter="numberColumn" />
              <el-table-column prop="avgSentiment" label="情感" width="90" />
            </el-table>
          </ChartPanel>
        </section>
      </template>

      <template v-else>
        <section class="analysis-list">
          <article v-for="item in taskCards" :key="item.title" class="analysis-card">
            <div class="analysis-card-head">
              <strong>{{ item.title }}</strong>
              <el-tag :type="item.type">{{ item.level }}</el-tag>
            </div>
            <p>{{ item.text }}</p>
          </article>
        </section>
      </template>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import DashboardHeader from '@/components/layout/DashboardHeader.vue'
import MetricGrid from '@/components/metrics/MetricGrid.vue'
import ChartPanel from '@/components/charts/ChartPanel.vue'
import TrendChart from '@/components/charts/TrendChart.vue'
import SentimentPieChart from '@/components/charts/SentimentPieChart.vue'
import UpRadarChart from '@/components/charts/UpRadarChart.vue'
import DanmakuScatterChart from '@/components/charts/DanmakuScatterChart.vue'
import InsightCards from '@/components/insights/InsightCards.vue'
import RecommendationPanel from '@/components/insights/RecommendationPanel.vue'
import VideoTable from '@/components/video/VideoTable.vue'
import { navItems, recommendations } from '@/data/dashboard'
import { exportExcelWorkbook } from '@/utils/exportExcel'
import {
  fetchDanmakuTimeline,
  fetchHeatRank,
  fetchKeywords,
  fetchNegativeComments,
  fetchSentimentTrend,
  fetchUpPerformance,
  fetchVideoSentiment,
} from '@/api/analysis'
import { formatVideoTime } from '@/utils/chartOptions'

const activeModule = ref('overview')
const trendMode = ref('日')
const keyword = ref('')
const loading = ref(false)
const loadError = ref('')
const selectedBvid = ref('')
const heatRank = ref([])
const videoSentiments = ref([])
const sentimentTrend = ref([])
const danmakuTimeline = ref([])
const keywords = ref([])
const upPerformance = ref([])
const negativeComments = ref([])
const filters = reactive({ channel: 'all', period: '30d' })

const moduleCopy = {
  overview: ['B站内容运营数据总览', '聚合视频热度、弹幕、评论情感和UP主表现，快速判断整体增长状态。'],
  video: ['视频分析', '从播放、互动、热度和口碑拆解单个视频表现，定位爆款和待优化内容。'],
  danmaku: ['弹幕分析', '用时间轴识别观众集中反应的片段，辅助剪辑、复盘和内容解释。'],
  comment: ['评论洞察', '分析评论情感结构和负面样本，服务舆情处理与用户反馈归因。'],
  creator: ['UP主画像', '横向比较创作者能力，识别高热度账号、口碑短板和合作优先级。'],
  task: ['任务中心', '把数据发现转成运营动作，沉淀可执行的分析工作流。'],
}

const exportCopy = {
  overview: ['完整数据报表', '导出热度、情感、弹幕、关键词、UP主和负面评论全部工作表。', '导出完整报表'],
  video: ['视频专项报表', '导出视频热度排行和视频情感统计，用于内容复盘。', '导出视频专项'],
  danmaku: ['弹幕专项报表', '导出当前视频的弹幕高峰时间点、弹幕数、情感和关键词。', '导出弹幕专项'],
  comment: ['评论专项报表', '导出评论情感趋势、情感占比和负面评论样本。', '导出评论专项'],
  creator: ['UP主专项报表', '导出UP主表现排行和能力评估数据。', '导出UP主专项'],
  task: ['运营任务报表', '导出基于数据洞察生成的运营动作建议。', '导出任务专项'],
}

const moduleMeta = computed(() => {
  const [title, description] = moduleCopy[activeModule.value] ?? moduleCopy.overview
  return { title, description }
})

const exportMeta = computed(() => {
  const [title, description, buttonText] = exportCopy[activeModule.value] ?? exportCopy.overview
  return { title, description, buttonText }
})

const moduleMetrics = computed(() => {
  const totalViews = visibleHeatRank.value.reduce((sum, item) => sum + item.viewCount, 0)
  const totalInteraction = visibleHeatRank.value.reduce((sum, item) => sum + getInteractions(item), 0)
  const avgSentiment = average(videoSentiments.value.map((item) => item.avgSentiment))
  const highestHeat = visibleHeatRank.value[0]?.heatScore ?? 0

  if (activeModule.value === 'comment') {
    const negativeCount = videoSentiments.value.reduce((sum, item) => sum + item.negativeCount, 0)
    return [
      metric('负向评论', formatCompact(negativeCount), `${negativeComments.value.length} 条样本`, '待排查', 'warn'),
      metric('平均情感', avgSentiment.toFixed(3), avgSentiment >= 0.55 ? '偏正向' : '需关注', '按视频均值', avgSentiment >= 0.55 ? 'up' : 'warn'),
      metric('评论总量', formatCompact(videoSentiments.value.reduce((sum, item) => sum + item.totalCount, 0)), '情感样本', '来自分析表', 'up'),
      metric('关键词数', keywords.value.length, 'TopN', '可用于归因', 'up'),
    ]
  }

  if (activeModule.value === 'creator') {
    return [
      metric('UP主数量', upPerformance.value.length, '参与排行', '当前样本', 'up'),
      metric('最高平均热度', formatCompact(Math.max(0, ...upPerformance.value.map((item) => item.avgHeatScore))), 'Top 1', '按平均热度', 'up'),
      metric('总点赞量', formatCompact(upPerformance.value.reduce((sum, item) => sum + item.totalLikeCount, 0)), '累计', '账号互动', 'up'),
      metric('平均情感', avgSentiment.toFixed(3), '口碑参考', '跨视频均值', 'up'),
    ]
  }

  return [
    metric('总播放量', formatCompact(totalViews), `${visibleHeatRank.value.length} 个视频`, '来自热度排行', 'up'),
    metric('互动总量', formatCompact(totalInteraction), '点赞/投币/收藏', '评论弹幕合计', 'up'),
    metric('平均情感', avgSentiment.toFixed(3), avgSentiment >= 0.55 ? '偏正向' : '需关注', '按视频均值', avgSentiment >= 0.55 ? 'up' : 'warn'),
    metric('最高热度', Math.round(highestHeat).toLocaleString('zh-CN'), visibleHeatRank.value[0]?.bvid ?? '--', '当前榜首', 'up'),
  ]
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

const visibleHeatRank = computed(() => {
  if (filters.channel === 'all') return heatRank.value

  const matched = heatRank.value.filter((item) => item.category === filters.channel)
  return matched.length > 0 ? matched : heatRank.value
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

const dynamicInsightCards = computed(() => [
  {
    title: '热度榜首',
    text: visibleHeatRank.value[0]
      ? `${visibleHeatRank.value[0].title} 当前热度最高，分数 ${Math.round(visibleHeatRank.value[0].heatScore).toLocaleString('zh-CN')}。`
      : '暂无热度排行数据。',
  },
  {
    title: '高频关键词',
    text: keywords.value.length > 0 ? keywords.value.slice(0, 4).map((item) => item.word).join('、') : '暂无关键词数据。',
  },
  {
    title: '弹幕高能段',
    text:
      danmakuHotspots.value.length > 0
        ? `峰值出现在 ${danmakuHotspots.value[0].timeText} 附近，可用于切片复盘。`
        : '暂无弹幕时间轴数据。',
  },
])

const taskCards = computed(() => recommendations)

watch(selectedBvid, async (bvid) => {
  if (!bvid) return
  try {
    danmakuTimeline.value = await fetchDanmakuTimeline(bvid)
  } catch (error) {
    loadError.value = error.message || '弹幕数据加载失败'
    ElMessage.error(loadError.value)
  }
})

async function loadDashboardData() {
  loading.value = true
  loadError.value = ''
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
    loadError.value = error.message || '数据加载失败'
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

async function handleRefresh() {
  await loadDashboardData()
  if (!loadError.value) {
    ElMessage.success('数据已刷新')
  }
}

async function handleFiltersChange(nextFilters) {
  Object.assign(filters, nextFilters)
  await loadDashboardData()
}

function handleExportReport() {
  const sheets = buildReportSheets(activeModule.value)
  const totalRows = sheets.reduce((sum, sheet) => sum + sheet.rows.length, 0)
  if (totalRows === 0) {
    ElMessage.warning('暂无可导出的数据')
    return
  }
  exportExcelWorkbook(`BiliLens_${exportMeta.value.title}_${new Date().toISOString().slice(0, 10)}.xls`, sheets)
  ElMessage.success(`${exportMeta.value.title}已导出`)
}

function buildReportSheets(moduleKey) {
  const sheets = {
    videoRank: {
      name: '视频热度排行',
      columns: [
        ['rankNo', '排名'], ['bvid', 'BVID'], ['title', '标题'], ['upName', 'UP主'], ['category', '分区'],
        ['viewCount', '播放量'], ['likeCount', '点赞数'], ['coinCount', '投币数'], ['favoriteCount', '收藏数'],
        ['replyCount', '评论数'], ['danmakuCount', '弹幕数'], ['heatScore', '热度分'], ['interactionCount', '互动总量'],
      ].map(([key, label]) => ({ key, label })),
      rows: visibleHeatRank.value.map((item) => ({ ...item, interactionCount: getInteractions(item) })),
    },
    videoSentiment: {
      name: '视频情感统计',
      columns: [
        ['bvid', 'BVID'], ['title', '标题'], ['avgSentiment', '平均情感'], ['positiveCount', '正向数'],
        ['neutralCount', '中性数'], ['negativeCount', '负向数'], ['totalCount', '总数'],
        ['positiveRatio', '正向占比'], ['negativeRatio', '负向占比'],
      ].map(([key, label]) => ({ key, label })),
      rows: videoSentiments.value,
    },
    trend: {
      name: '情感趋势',
      columns: [
        ['statDate', '日期'], ['commentCount', '评论数'], ['danmakuCount', '弹幕数'],
        ['avgSentiment', '平均情感'], ['negativeRatio', '负向占比'],
      ].map(([key, label]) => ({ key, label })),
      rows: sentimentTrend.value,
    },
    danmaku: {
      name: '弹幕高峰',
      columns: [
        ['bvid', 'BVID'], ['timeText', '时间点'], ['timeBucket', '秒数'],
        ['danmakuCount', '弹幕数'], ['avgSentiment', '平均情感'], ['topWords', '关键词'],
      ].map(([key, label]) => ({ key, label })),
      rows: danmakuHotspots.value,
    },
    keywords: {
      name: '关键词TopN',
      columns: [['rankNo', '排名'], ['word', '关键词'], ['wordCount', '出现次数']].map(([key, label]) => ({ key, label })),
      rows: keywords.value,
    },
    creator: {
      name: 'UP主表现',
      columns: [
        ['upName', 'UP主'], ['videoCount', '视频数'], ['avgViewCount', '平均播放'],
        ['avgHeatScore', '平均热度'], ['avgSentiment', '平均情感'], ['totalLikeCount', '总点赞'],
      ].map(([key, label]) => ({ key, label })),
      rows: upPerformance.value,
    },
    negative: {
      name: '负面评论样本',
      columns: [
        ['bvid', 'BVID'], ['rpid', '评论ID'], ['userName', '用户'], ['cleanContent', '评论内容'],
        ['likeCount', '点赞数'], ['crawledAt', '采集时间'], ['sentimentScore', '情感分'],
      ].map(([key, label]) => ({ key, label })),
      rows: negativeComments.value,
    },
    task: {
      name: '运营动作建议',
      columns: [['title', '建议'], ['level', '优先级'], ['text', '说明']].map(([key, label]) => ({ key, label })),
      rows: recommendations,
    },
  }

  const map = {
    overview: [sheets.videoRank, sheets.videoSentiment, sheets.trend, sheets.danmaku, sheets.keywords, sheets.creator, sheets.negative],
    video: [sheets.videoRank, sheets.videoSentiment],
    danmaku: [sheets.danmaku],
    comment: [sheets.videoSentiment, sheets.trend, sheets.negative],
    creator: [sheets.creator],
    task: [sheets.task],
  }
  return map[moduleKey] ?? map.overview
}

function metric(label, value, delta, note, status) {
  return { label, value, delta, note, status }
}

function getInteractions(item) {
  return item.likeCount + item.coinCount + item.favoriteCount + item.replyCount + item.danmakuCount
}

function average(values) {
  return values.length === 0 ? 0 : values.reduce((sum, value) => sum + value, 0) / values.length
}

function getDateRange(period) {
  const daysMap = {
    '7d': 7,
    '30d': 30,
    '90d': 90,
  }
  const days = daysMap[period] ?? 30
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - days + 1)

  return {
    startDate: formatDate(start),
    endDate: formatDate(end),
  }
}

function formatDate(date) {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-')
}

function formatCompact(value) {
  const number = Number(value) || 0
  if (number >= 10000) return `${(number / 10000).toFixed(1)}万`
  return Math.round(number).toLocaleString('zh-CN')
}

function numberColumn(row, column, value) {
  return formatCompact(value)
}

onMounted(loadDashboardData)
</script>
