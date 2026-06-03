<template>
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

<script setup>
import ChartPanel from '@/components/charts/ChartPanel.vue'
import DanmakuScatterChart from '@/components/charts/DanmakuScatterChart.vue'
import { useDashboardData } from '@/composables/useDashboardData'

const { selectedBvid, heatRank, danmakuTimeline, danmakuHotspots } = useDashboardData()
</script>
