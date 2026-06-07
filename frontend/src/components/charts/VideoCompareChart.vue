<template>
  <div v-if="hasData" ref="chartEl" class="chart" role="img" aria-label="视频对比柱状图"></div>
  <div v-else class="empty-state">请选择至少 1 个视频进行对比</div>
</template>

<script setup>
import { computed, ref, toRef } from 'vue'
import { baseGrid } from '@/utils/chartOptions'
import { useEChart } from '@/composables/useEChart'
import { getInteractions } from '@/composables/useDashboardData'

const props = defineProps({
  videos: {
    type: Array,
    default: () => [],
  },
  sentiments: {
    type: Array,
    default: () => [],
  },
})

const chartEl = ref(null)
const videosRef = toRef(props, 'videos')
const sentimentsRef = toRef(props, 'sentiments')
const hasData = computed(() => props.videos.length > 0)
const sentimentMap = computed(() => new Map(props.sentiments.map((item) => [item.bvid, item])))
const chartData = computed(() => ({
  names: props.videos.map((item) => trimTitle(item.title)),
  heat: props.videos.map((item) => Math.round(item.heatScore || 0)),
  interaction: props.videos.map((item) => {
    const rate = item.viewCount > 0 ? getInteractions(item) / item.viewCount : 0
    return Number((rate * 100).toFixed(2))
  }),
  positive: props.videos.map((item) => {
    const sentiment = sentimentMap.value.get(item.bvid)
    return sentiment ? Number((sentiment.positiveRatio * 100).toFixed(1)) : 0
  }),
}))

useEChart(chartEl, () => ({
  color: ['#1E40AF', '#059669', '#D97706'],
  tooltip: { trigger: 'axis' },
  legend: { top: 8, right: 16, data: ['热度分', '互动率(%)', '正向占比(%)'] },
  grid: baseGrid(),
  xAxis: {
    type: 'category',
    data: chartData.value.names,
    axisLabel: { interval: 0, rotate: 18 },
  },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#E2E8F0' } } },
  series: [
    { name: '热度分', type: 'bar', barMaxWidth: 28, data: chartData.value.heat },
    { name: '互动率(%)', type: 'bar', barMaxWidth: 28, data: chartData.value.interaction },
    { name: '正向占比(%)', type: 'bar', barMaxWidth: 28, data: chartData.value.positive },
  ],
}), [videosRef, sentimentsRef])

function trimTitle(title) {
  if (!title) return '--'
  return title.length > 12 ? `${title.slice(0, 12)}...` : title
}
</script>
