<template>
  <div v-if="hasData" ref="chartEl" class="chart small" role="img" aria-label="UP主能力雷达图"></div>
  <div v-else class="empty-state small">暂无UP主表现数据</div>
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
const hasData = computed(() => props.data.length > 0)
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
  color: ['#3B82F6', '#D97706', '#059669'],
  tooltip: {},
  legend: { bottom: 8, data: radarSeries.value.map((item) => item.name) },
  radar: {
    radius: '62%',
    indicator: indicators,
    splitLine: { lineStyle: { color: '#E2E8F0' } },
    splitArea: { areaStyle: { color: ['#FFFFFF', '#F8FAFC'] } },
  },
  series: [
    {
      type: 'radar',
      data: radarSeries.value,
    },
  ],
}), [dataRef])
</script>
