<template>
  <div
    v-if="hasData"
    ref="chartEl"
    class="chart"
    role="img"
    :aria-label="chartSummary"
  ></div>
  <div v-else class="empty-state">暂无弹幕时间轴数据</div>
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
const timeline = computed(() => [...props.data].sort((a, b) => a.timeBucket - b.timeBucket))
const peak = computed(() => timeline.value.reduce(
  (highest, item) => !highest || item.danmakuCount > highest.danmakuCount ? item : highest,
  null,
))
const chartSummary = computed(() => peak.value
  ? `弹幕数量与平均情感时间轴。最高峰位于 ${formatVideoTime(peak.value.timeBucket)}，共 ${Number(peak.value.danmakuCount).toLocaleString('zh-CN')} 条弹幕。`
  : '弹幕数量与平均情感时间轴。')

function sentimentPercent(value) {
  if (value === null || value === undefined || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? Number((number * 100).toFixed(1)) : null
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (character) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  })[character])
}

useEChart(chartEl, () => {
  const showZoom = timeline.value.length > 12
  const maxCount = peak.value?.danmakuCount
  const countData = timeline.value.map((item) => {
    const isPeak = item.danmakuCount === maxCount
    return {
      value: item.danmakuCount,
      itemStyle: {
        color: isPeak ? '#D97706' : '#2563EB',
        borderRadius: [4, 4, 0, 0],
      },
      label: isPeak
        ? { show: true, position: 'top', formatter: '峰值 {c}', color: '#92400E', fontWeight: 600 }
        : { show: false },
    }
  })

  return {
    animationDuration: 300,
    color: ['#2563EB', '#0F766E'],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        const item = timeline.value[params[0]?.dataIndex]
        if (!item) return ''
        const sentiment = sentimentPercent(item.avgSentiment)
        return [
          `<strong>${formatVideoTime(item.timeBucket)}</strong>`,
          `弹幕数量：${Number(item.danmakuCount).toLocaleString('zh-CN')}`,
          `平均情感：${sentiment === null ? '--' : `${sentiment.toFixed(1)}%`}`,
          `关键词：${escapeHtml(item.topWords || '--')}`,
        ].join('<br/>')
      },
    },
    legend: {
      top: 8,
      right: 18,
      data: ['弹幕数量', '平均情感'],
    },
    grid: {
      top: 58,
      right: 64,
      bottom: showZoom ? 88 : 52,
      left: 58,
      containLabel: true,
    },
    dataZoom: showZoom
      ? [
          { type: 'inside', start: 0, end: 55 },
          { type: 'slider', height: 18, bottom: 12, start: 0, end: 55 },
        ]
      : [],
    xAxis: {
      type: 'category',
      name: '视频时间',
      nameLocation: 'middle',
      nameGap: 32,
      data: timeline.value.map((item) => formatVideoTime(item.timeBucket)),
      axisTick: { alignWithLabel: true },
      axisLabel: { hideOverlap: true },
    },
    yAxis: [
      {
        type: 'value',
        name: '弹幕数',
        minInterval: 1,
        splitLine: { lineStyle: { color: '#E2E8F0' } },
      },
      {
        type: 'value',
        name: '情感 %',
        min: 0,
        max: 100,
        splitLine: { show: false },
        axisLabel: { formatter: '{value}%' },
      },
    ],
    series: [
      {
        name: '弹幕数量',
        type: 'bar',
        barMaxWidth: 30,
        data: countData,
      },
      {
        name: '平均情感',
        type: 'line',
        yAxisIndex: 1,
        smooth: 0.25,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 3 },
        data: timeline.value.map((item) => sentimentPercent(item.avgSentiment)),
      },
    ],
  }
}, [dataRef])
</script>
