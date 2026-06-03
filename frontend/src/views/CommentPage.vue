<template>
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

<script setup>
import ChartPanel from '@/components/charts/ChartPanel.vue'
import TrendChart from '@/components/charts/TrendChart.vue'
import SentimentPieChart from '@/components/charts/SentimentPieChart.vue'
import { useDashboardData } from '@/composables/useDashboardData'

const { trendMode, videoSentiments, sentimentTrend, negativeComments } = useDashboardData()
</script>
