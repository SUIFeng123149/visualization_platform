<template>
  <section class="overview-columns">
    <div class="stack">
      <ChartPanel title="播放量 / 互动量动态趋势" description="用于观察评论、弹幕和情感随时间变化的节奏，周期由右上角日期筛选控制。">
        <template #actions>
          <el-segmented v-model="trendMode" :options="['日', '周', '月']" />
        </template>
        <TrendChart :mode="trendMode" :data="sentimentTrend" />
      </ChartPanel>

      <ChartPanel title="弹幕时间轴高峰点" description="散点大小代表同一时间段弹幕密度，用来定位视频高潮点。点击下方高能卡片可进入视频复盘。">
        <DanmakuScatterChart :data="danmakuTimeline" />
        <InsightCards :items="dynamicInsightCards" />
      </ChartPanel>

      <VideoTable v-model:keyword="keyword" :videos="filteredVideos" />
    </div>

    <div class="stack">
      <DataQualityPanel :quality="dataQuality" />

      <ChartPanel title="评论情感占比" description="快速判断当前评论区口碑结构；没有评论情感样本的视频不会计算正向占比。">
        <SentimentPieChart :data="videoSentiments" />
      </ChartPanel>

      <ChartPanel title="UP主能力雷达" description="对比创作者的播放、热度、口碑和互动能力，避免只按播放量判断。">
        <UpRadarChart :data="upPerformance" />
      </ChartPanel>

      <RecommendationPanel :items="globalInsights.length ? globalInsights : recommendations" />
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import ChartPanel from '@/components/charts/ChartPanel.vue'
import TrendChart from '@/components/charts/TrendChart.vue'
import SentimentPieChart from '@/components/charts/SentimentPieChart.vue'
import UpRadarChart from '@/components/charts/UpRadarChart.vue'
import DanmakuScatterChart from '@/components/charts/DanmakuScatterChart.vue'
import InsightCards from '@/components/insights/InsightCards.vue'
import RecommendationPanel from '@/components/insights/RecommendationPanel.vue'
import DataQualityPanel from '@/components/quality/DataQualityPanel.vue'
import VideoTable from '@/components/video/VideoTable.vue'
import { recommendations } from '@/data/dashboard'
import { useDashboardData } from '@/composables/useDashboardData'

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
</script>
