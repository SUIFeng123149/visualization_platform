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
        @update:filters="handleFiltersChange"
        @refresh="handleRefresh"
      />

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

      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import DashboardHeader from '@/components/layout/DashboardHeader.vue'
import MetricGrid from '@/components/metrics/MetricGrid.vue'
import { navItems, recommendations } from '@/data/dashboard'
import { exportExcelWorkbook } from '@/utils/exportExcel'
import {
  useDashboardData,
  formatCompact,
  getInteractions,
  average,
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
  handleRefresh,
  handleFiltersChange,
} = useDashboardData()

const router = useRouter()
const route = useRoute()

watch(
  () => route.name,
  (name) => {
    if (name && name !== activeModule.value) {
      activeModule.value = name
    }
  },
  { immediate: true },
)

function navigate(key) {
  activeModule.value = key
  router.push({ name: key })
}

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

function metric(label, value, delta, note, status) {
  return { label, value, delta, note, status }
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
</script>
