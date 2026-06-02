<template>
  <div ref="chartEl" class="chart" role="img" aria-label="弹幕时间轴散点图"></div>
</template>

<script setup>
import { computed, ref, toRef } from 'vue'
import { useEChart } from '@/composables/useEChart'
import { formatVideoTime } from '@/utils/chartOptions'

const props = defineProps({
  data: {
    type: Array,
    default: () => [],
  },
})

const chartEl = ref(null)
const dataRef = toRef(props, 'data')
const scatterData = computed(() =>
  props.data.map((item) => [
    item.timeBucket,
    item.danmakuCount,
    Number((item.avgSentiment * 100).toFixed(1)),
    item.topWords,
  ]),
)

useEChart(chartEl, () => ({
  color: ['#0f7cff'],
  tooltip: {
    formatter(params) {
      return `${formatVideoTime(params.value[0])}<br/>弹幕数：${params.value[1]}<br/>平均情感：${params.value[2]}<br/>关键词：${params.value[3] || '--'}`
    },
  },
  grid: {
    top: 42,
    right: 34,
    bottom: 54,
    left: 58,
    containLabel: true,
  },
  xAxis: {
    type: 'value',
    name: '视频时间',
    nameLocation: 'middle',
    nameGap: 34,
    axisLabel: { formatter: formatVideoTime },
    splitLine: { lineStyle: { color: '#e7edf5' } },
  },
  yAxis: {
    type: 'value',
    name: '弹幕密度',
    nameGap: 18,
    splitLine: { lineStyle: { color: '#e7edf5' } },
  },
  series: [
    {
      name: '弹幕时间轴',
      type: 'scatter',
      symbolSize: (value) => Math.max(10, Math.min(56, value[1] * 1.7)),
      data: scatterData.value,
    },
  ],
}), [dataRef])
</script>
