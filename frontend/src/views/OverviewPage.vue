<template>
  <ChartPanel class="overview-trend-panel" title="播放量 / 互动量动态趋势" description="用于观察评论、弹幕和情感随时间变化的节奏，周期由右上角日期筛选控制。">
    <template #actions>
      <el-segmented v-model="trendMode" :options="['日', '周', '月']" />
    </template>
    <TrendChart :mode="trendMode" :data="sentimentTrend" />
  </ChartPanel>

  <section class="overview-chart-grid">
    <ChartPanel title="弹幕时间轴高峰点" description="散点大小代表同一时间段弹幕密度，用来定位视频高潮点。点击下方高能卡片可进入视频复盘。">
      <DanmakuScatterChart :data="danmakuTimeline" />
      <InsightCards :items="dynamicInsightCards" />
    </ChartPanel>

    <ChartPanel title="评论情感占比" description="快速判断当前评论区口碑结构；没有评论情感样本的视频不会计算正向占比。">
      <SentimentPieChart :data="videoSentiments" />
    </ChartPanel>

    <ChartPanel title="UP主能力雷达" description="对比创作者的播放、热度、口碑和互动能力，避免只按播放量判断。">
      <UpRadarChart :data="upPerformance" />
    </ChartPanel>
  </section>

  <section class="overview-ops-grid">
    <DataQualityPanel :quality="dataQuality" />

    <AiInsightPanel
      title="AI解读当前总览"
      description="读取当前筛选后的视频、情感、关键词、弹幕高峰和数据质量，生成页面级分析结论。"
      mode="overview-insight"
      :context="overviewAiContext"
      :prompts="overviewPrompts"
    />

    <article class="panel anomaly-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">异常检测中心</div>
          <div class="panel-desc">自动识别热度、互动、情感和弹幕中的优先关注对象。</div>
        </div>
      </div>
      <div class="anomaly-list">
        <article
          v-for="item in anomalyInsights"
          :key="item.title"
          class="anomaly-item"
          :class="`anomaly-${item.type}`"
          @click="openAnomaly(item)"
        >
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.text }}</p>
          </div>
          <el-tag :type="item.type">{{ item.level }}</el-tag>
        </article>
      </div>
    </article>
  </section>

  <VideoTable v-model:keyword="keyword" :videos="filteredVideos" />

  <RecommendationPanel
    class="overview-actions-panel"
    :items="globalInsights.length ? globalInsights : recommendations"
  />
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import ChartPanel from '@/components/charts/ChartPanel.vue'
import TrendChart from '@/components/charts/TrendChart.vue'
import SentimentPieChart from '@/components/charts/SentimentPieChart.vue'
import UpRadarChart from '@/components/charts/UpRadarChart.vue'
import DanmakuScatterChart from '@/components/charts/DanmakuScatterChart.vue'
import InsightCards from '@/components/insights/InsightCards.vue'
import RecommendationPanel from '@/components/insights/RecommendationPanel.vue'
import DataQualityPanel from '@/components/quality/DataQualityPanel.vue'
import VideoTable from '@/components/video/VideoTable.vue'
import AiInsightPanel from '@/components/ai/AiInsightPanel.vue'
import { recommendations } from '@/data/dashboard'
import { formatCompact, getInteractions, useDashboardData } from '@/composables/useDashboardData'

const router = useRouter()

const {
  trendMode,
  keyword,
  sentimentTrend,
  danmakuTimeline,
  videoSentiments,
  upPerformance,
  filteredVideos,
  visibleHeatRank,
  keywords: kw,
  danmakuHotspots,
  dataQuality,
  globalInsights,
} = useDashboardData()

const anomalyInsights = computed(() => {
  const insights = []
  const topHeat = visibleHeatRank.value[0]
  const topInteraction = [...visibleHeatRank.value].sort((a, b) => interactionRate(b) - interactionRate(a))[0]
  const riskySentiment = [...videoSentiments.value].sort((a, b) => b.negativeRatio - a.negativeRatio)[0]
  const hotspot = danmakuHotspots.value[0]

  if (topHeat) {
    insights.push({
      title: '热度异常高',
      level: '复盘',
      type: 'warning',
      text: `${topHeat.title} 当前热度 ${Math.round(topHeat.heatScore).toLocaleString('zh-CN')}，建议拆解传播来源。`,
      bvid: topHeat.bvid,
    })
  }

  if (topInteraction) {
    insights.push({
      title: '互动效率突出',
      level: '增长',
      type: 'success',
      text: `${topInteraction.title} 互动率 ${(interactionRate(topInteraction) * 100).toFixed(1)}%，适合沉淀互动机制。`,
      bvid: topInteraction.bvid,
    })
  }

  if (riskySentiment && riskySentiment.negativeRatio >= 0.2) {
    insights.push({
      title: '负向情绪预警',
      level: '风险',
      type: 'danger',
      text: `${riskySentiment.title} 负向占比 ${(riskySentiment.negativeRatio * 100).toFixed(1)}%，建议优先查看评论样本。`,
      bvid: riskySentiment.bvid,
    })
  }

  if (hotspot) {
    insights.push({
      title: '弹幕峰值片段',
      level: '切片',
      type: 'primary',
      text: `${hotspot.timeText} 附近弹幕 ${formatCompact(hotspot.danmakuCount)} 条，可作为剪辑切点。`,
      bvid: hotspot.bvid,
    })
  }

  return insights.length ? insights : [{ title: '暂无明显异常', level: '正常', type: 'success', text: '当前样本未触发异常规则。' }]
})

const dynamicInsightCards = computed(() => [
  {
    title: '热度榜首',
    text: visibleHeatRank.value[0]
      ? `${visibleHeatRank.value[0].title} 当前热度最高，分数 ${Math.round(visibleHeatRank.value[0].heatScore).toLocaleString('zh-CN')}。`
      : '暂无热度排行数据。',
    bvid: visibleHeatRank.value[0]?.bvid,
  },
  {
    title: '高频关键词',
    text: kw.value.length > 0 ? kw.value.slice(0, 4).map((item) => item.word).join('、') : '暂无关键词数据。',
    bvid: visibleHeatRank.value[0]?.bvid,
  },
  {
    title: '弹幕高能段',
    text:
      danmakuHotspots.value.length > 0
        ? `峰值出现在 ${danmakuHotspots.value[0].timeText} 附近，可用于切片复盘。`
        : '暂无弹幕时间轴数据。',
    bvid: danmakuHotspots.value[0]?.bvid,
  },
])

const overviewAiContext = computed(() => ({
  page: '数据总览',
  filters: dataQuality.value.filters,
  dataQuality: dataQuality.value,
  topVideos: visibleHeatRank.value.slice(0, 8).map((item) => ({
    bvid: item.bvid,
    title: item.title,
    upName: item.upName,
    category: item.category,
    viewCount: item.viewCount,
    heatScore: Math.round(item.heatScore || 0),
    interactionCount: getInteractions(item),
  })),
  sentiments: videoSentiments.value.slice(0, 8).map((item) => ({
    bvid: item.bvid,
    title: item.title,
    positiveRatio: item.positiveRatio,
    negativeRatio: item.negativeRatio,
    totalCount: item.totalCount,
  })),
  keywords: kw.value.slice(0, 10),
  danmakuHotspots: danmakuHotspots.value.slice(0, 5),
  anomalyInsights: anomalyInsights.value,
}))

const overviewPrompts = [
  '解读当前总览页面的核心结论',
  '当前数据质量有什么风险',
  '哪些视频最值得优先复盘',
  '根据当前图表生成运营建议',
]

function interactionRate(item) {
  return item?.viewCount > 0 ? getInteractions(item) / item.viewCount : 0
}

function openAnomaly(item) {
  if (!item.bvid) return
  router.push({ name: 'videoDetail', params: { bvid: item.bvid } })
}
</script>
