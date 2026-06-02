<template>
  <div ref="chartEl" class="chart small" role="img" aria-label="UP主能力雷达图"></div>
</template>

<script setup>
import { computed, ref, toRef } from 'vue'
import { useEChart } from '@/composables/useEChart'

const props = defineProps({
  data: {
    type: Array,
    default: () => [],
  },
})

const chartEl = ref(null)
const dataRef = toRef(props, 'data')
const indicators = [
  { name: '视频数', max: 10 },
  { name: '平均播放', max: 2000000 },
  { name: '平均热度', max: 700000 },
  { name: '平均情感', max: 1 },
  { name: '点赞总量', max: 50000 },
]
const radarSeries = computed(() =>
  props.data.slice(0, 3).map((item) => ({
    name: item.upName,
    value: [
      item.videoCount,
      item.avgViewCount,
      item.avgHeatScore,
      item.avgSentiment,
      item.totalLikeCount,
    ],
  })),
)

useEChart(chartEl, () => ({
  color: ['#0f7cff', '#e36b2c', '#13a46f'],
  tooltip: {},
  legend: { bottom: 8, data: radarSeries.value.map((item) => item.name) },
  radar: {
    radius: '62%',
    indicator: indicators,
    splitLine: { lineStyle: { color: '#dbe4ef' } },
    splitArea: { areaStyle: { color: ['#ffffff', '#f8fafc'] } },
  },
  series: [
    {
      type: 'radar',
      data: radarSeries.value,
    },
  ],
}), [dataRef])
</script>
