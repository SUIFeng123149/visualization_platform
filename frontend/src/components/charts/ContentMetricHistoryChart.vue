<template><div ref="chartRef" class="content-metric-history-chart" role="img" aria-label="内容指标生命周期趋势图" /></template>

<script setup>
import { ref } from 'vue'
import { useEChart } from '@/composables/useEChart'

const props = defineProps({ data: { type: Array, default: () => [] } })
const chartRef = ref(null)

useEChart(chartRef, () => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['播放量', '点赞量', '评论量'] },
  grid: { left: 58, right: 24, top: 42, bottom: 58 },
  xAxis: { type: 'category', boundaryGap: false, data: props.data.map((item) => String(item.capturedAt || '').replace('T', ' ').slice(0, 16)) },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#edf1f5' } } },
  series: [
    { name: '播放量', type: 'line', smooth: true, showSymbol: false, data: props.data.map((item) => item.viewCount) },
    { name: '点赞量', type: 'line', smooth: true, showSymbol: false, data: props.data.map((item) => item.likeCount) },
    { name: '评论量', type: 'line', smooth: true, showSymbol: false, data: props.data.map((item) => item.commentCount) },
  ],
}), [() => props.data])
</script>

<style scoped>
.content-metric-history-chart { width: 100%; height: 340px; }
</style>
