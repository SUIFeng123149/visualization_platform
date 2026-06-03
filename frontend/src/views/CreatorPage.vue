<template>
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

<script setup>
import ChartPanel from '@/components/charts/ChartPanel.vue'
import UpRadarChart from '@/components/charts/UpRadarChart.vue'
import { useDashboardData, formatCompact } from '@/composables/useDashboardData'

const { upPerformance } = useDashboardData()

function numberColumn(_row, _column, value) {
  return formatCompact(value)
}
</script>
