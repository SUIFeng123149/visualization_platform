<template>
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

<script setup>
import { computed } from 'vue'
import ChartPanel from '@/components/charts/ChartPanel.vue'
import TrendChart from '@/components/charts/TrendChart.vue'
import SentimentPieChart from '@/components/charts/SentimentPieChart.vue'
import UpRadarChart from '@/components/charts/UpRadarChart.vue'
import DanmakuScatterChart from '@/components/charts/DanmakuScatterChart.vue'
import InsightCards from '@/components/insights/InsightCards.vue'
import RecommendationPanel from '@/components/insights/RecommendationPanel.vue'
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
} = useDashboardData()

const dynamicInsightCards = computed(() => [
  {
    title: '热度榜首',
    text: visibleHeatRank.value[0]
      ? `${visibleHeatRank.value[0].title} 当前热度最高，分数 ${Math.round(visibleHeatRank.value[0].heatScore).toLocaleString('zh-CN')}。`
      : '暂无热度排行数据。',
  },
  {
    title: '高频关键词',
    text: kw.value.length > 0 ? kw.value.slice(0, 4).map((item) => item.word).join('、') : '暂无关键词数据。',
  },
  {
    title: '弹幕高能段',
    text:
      danmakuHotspots.value.length > 0
        ? `峰值出现在 ${danmakuHotspots.value[0].timeText} 附近，可用于切片复盘。`
        : '暂无弹幕时间轴数据。',
  },
])
</script>
