<template>
  <div v-if="hasData" ref="chartEl" class="detail-timeline-chart" role="img" aria-label="互动数量与平均情感时间轴图"></div>
  <div v-else class="empty-state small">暂无时间轴数据</div>
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
const hasData = computed(() => props.data.length > 0)

useEChart(chartEl, () => ({
  color: ['#2563EB', '#059669'],
  tooltip: {
    trigger: 'axis',
    formatter(params) {
      const count = params.find((item) => item.seriesName === '互动数量')?.value ?? 0
      const sentiment = params.find((item) => item.seriesName === '平均情感')?.value
      return `${params[0]?.axisValueLabel || '--'}<br/>互动数量：${Number(count).toLocaleString('zh-CN')}<br/>平均情感：${sentiment == null ? '--' : Number(sentiment).toFixed(1) + '%'}`
    },
  },
  legend: { top: 8, right: 18, data: ['互动数量', '平均情感'] },
  grid: { top: 54, right: 58, bottom: 50, left: 56, containLabel: true },
  xAxis: {
    type: 'category',
    data: props.data.map((item) => formatVideoTime(item.timeBucket)),
    axisTick: { alignWithLabel: true },
    axisLabel: { hideOverlap: true },
  },
  yAxis: [
    { type: 'value', name: '互动数', minInterval: 1, splitLine: { lineStyle: { color: '#E2E8F0' } } },
    { type: 'value', name: '情感 %', min: 0, max: 100, splitLine: { show: false } },
  ],
  series: [
    {
      name: '互动数量',
      type: 'bar',
      barMaxWidth: 28,
      data: props.data.map((item) => item.interactionCount),
    },
    {
      name: '平均情感',
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      symbolSize: 7,
      data: props.data.map((item) => item.averageSentiment == null ? null : Number((item.averageSentiment * 100).toFixed(1))),
    },
  ],
}), [dataRef])
</script>
