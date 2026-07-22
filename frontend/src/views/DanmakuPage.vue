<template>
  <section class="analysis-grid">
    <ChartPanel title="弹幕高峰与情感走势" description="沿视频时间对照弹幕数量与平均情感，快速定位高能片段、争议点和情绪转折。">
      <template #actions>
        <el-select v-model="selectedBvid" class="module-select" placeholder="选择视频">
          <el-option v-for="video in heatRank" :key="video.bvid" :label="video.title" :value="video.bvid" />
        </el-select>
      </template>
      <DanmakuTimelineChart :data="danmakuTimeline" />
    </ChartPanel>

    <ChartPanel title="弹幕高峰列表" description="把图表中的高峰点转成可检查的时间节点，点击关联视频可进入复盘。">
      <el-table :data="danmakuHotspots" empty-text="暂无弹幕高峰数据" style="width: 100%" @row-click="openHotspot">
        <el-table-column prop="timeText" label="时间点" width="100" />
        <el-table-column prop="danmakuCount" label="弹幕数" width="100" />
        <el-table-column prop="avgSentimentText" label="情感" width="100" />
        <el-table-column prop="topWords" label="关键词" min-width="160" />
      </el-table>
    </ChartPanel>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router'
import ChartPanel from '@/components/charts/ChartPanel.vue'
import DanmakuTimelineChart from '@/components/charts/DanmakuTimelineChart.vue'
import { useDashboardData } from '@/composables/useDashboardData'

const router = useRouter()
const { selectedBvid, heatRank, danmakuTimeline, danmakuHotspots } = useDashboardData()

function openHotspot(row) {
  if (!row?.bvid) return
  router.push({ name: 'videoDetail', params: { bvid: row.bvid }, query: { from: 'danmaku' } })
}
</script>
