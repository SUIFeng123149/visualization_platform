<template>
  <div v-if="hasData" ref="chartEl" class="overview-trend-chart" role="img" aria-label="互动数量与平均情感趋势图"></div>
  <div v-else class="empty-state small">暂无互动趋势</div>
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
const typeNames = { comment: '评论', danmaku: '弹幕', review: '评分/剧评', reply: '回复' }
const typeColors = { comment: '#2563EB', danmaku: '#7C3AED', review: '#D97706', reply: '#059669' }

const chartData = computed(() => {
  const dates = [...new Set(props.data.map((item) => item.statDate))].sort()
  const types = [...new Set(props.data.map((item) => item.interactionType))]
  const lookup = new Map(props.data.map((item) => [`${item.statDate}:${item.interactionType}`, item]))
  return { dates, types, lookup }
})

useEChart(chartEl, () => ({
  color: chartData.value.types.map((type) => typeColors[type] || '#64748B'),
  tooltip: {
    trigger: 'axis',
    formatter(params) {
      const rows = params.map((item) => {
        const source = chartData.value.lookup.get(`${chartData.value.dates[item.dataIndex]}:${item.seriesName}`)
        return `${item.marker}${item.seriesName}：${Number(item.value || 0).toLocaleString('zh-CN')}${source?.averageSentiment == null ? '' : `（情感 ${(source.averageSentiment * 100).toFixed(1)}%）`}`
      })
      return `${params[0]?.axisValueLabel || '--'}<br/>${rows.join('<br/>')}`
    },
  },
  legend: { top: 8, right: 16, data: chartData.value.types.map((type) => typeNames[type] || type) },
  grid: { top: 54, right: 62, bottom: 44, left: 56, containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: chartData.value.dates, axisLabel: { hideOverlap: true } },
  yAxis: [
    { type: 'value', name: '互动数', minInterval: 1, splitLine: { lineStyle: { color: '#E2E8F0' } } },
    { type: 'value', name: '情感 %', min: 0, max: 100, splitLine: { show: false } },
  ],
  series: [
    ...chartData.value.types.map((type) => ({
      name: typeNames[type] || type,
      type: 'line',
      smooth: true,
      symbolSize: 7,
      data: chartData.value.dates.map((date) => chartData.value.lookup.get(`${date}:${type}`)?.interactionCount ?? 0),
    })),
    {
      name: '平均情感',
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      symbolSize: 6,
      lineStyle: { type: 'dashed', color: '#DC2626' },
      itemStyle: { color: '#DC2626' },
      data: chartData.value.dates.map((date) => {
        const rows = chartData.value.types.map((type) => chartData.value.lookup.get(`${date}:${type}`)?.averageSentiment).filter((value) => value != null)
        return rows.length ? Number((rows.reduce((sum, value) => sum + Number(value), 0) / rows.length * 100).toFixed(1)) : null
      }),
    },
  ],
}), [dataRef])
</script>
