<template>
  <section class="video-summary-grid">
    <ChartPanel title="视频情感结构" description="用于判断高热度视频是否同时具备好口碑。">
      <SentimentPieChart :data="videoSentiments" />
    </ChartPanel>
    <section class="analysis-list compact">
      <article v-for="item in allVideoCards.slice(0, 4)" :key="item.bvid" class="analysis-card">
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
    :videos="allFilteredVideos"
    title="视频表现分析"
    description="按热度、互动率和情感表现筛选可复盘视频；顶部全局筛选不会影响本页样本池。"
  />
</template>

<script setup>
import ChartPanel from '@/components/charts/ChartPanel.vue'
import SentimentPieChart from '@/components/charts/SentimentPieChart.vue'
import VideoTable from '@/components/video/VideoTable.vue'
import { useDashboardData, formatCompact } from '@/composables/useDashboardData'

const { keyword, videoSentiments, allVideoCards, allFilteredVideos } = useDashboardData()
</script>
