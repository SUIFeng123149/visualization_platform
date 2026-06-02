<template>
  <div ref="chartEl" class="chart" role="img" aria-label="评论、弹幕和情感趋势图"></div>
</template>

<script setup>
import { computed, ref, toRef } from 'vue'
import { baseGrid } from '@/utils/chartOptions'
import { useEChart } from '@/composables/useEChart'

const props = defineProps({
  mode: {
    type: String,
    default: '日',
  },
  data: {
    type: Array,
    default: () => [],
  },
})

const chartEl = ref(null)
const modeRef = toRef(props, 'mode')
const dataRef = toRef(props, 'data')

const groupedData = computed(() => {
  const buckets = new Map()

  props.data.forEach((item) => {
    const key = getBucketKey(item.statDate, props.mode)
    const current = buckets.get(key) ?? {
      statDate: key,
      commentCount: 0,
      danmakuCount: 0,
      sentimentTotal: 0,
      negativeTotal: 0,
      count: 0,
    }

    current.commentCount += item.commentCount
    current.danmakuCount += item.danmakuCount
    current.sentimentTotal += item.avgSentiment
    current.negativeTotal += item.negativeRatio
    current.count += 1
    buckets.set(key, current)
  })

  return [...buckets.values()].map((item) => ({
    statDate: item.statDate,
    commentCount: item.commentCount,
    danmakuCount: item.danmakuCount,
    avgSentiment: item.count > 0 ? item.sentimentTotal / item.count : 0,
    negativeRatio: item.count > 0 ? item.negativeTotal / item.count : 0,
  }))
})

const chartData = computed(() => ({
  dates: groupedData.value.map((item) => item.statDate),
  comments: groupedData.value.map((item) => item.commentCount),
  danmaku: groupedData.value.map((item) => item.danmakuCount),
  sentiment: groupedData.value.map((item) => Number((item.avgSentiment * 100).toFixed(1))),
}))

useEChart(
  chartEl,
  () => ({
    color: ['#0f7cff', '#13a46f', '#e36b2c'],
    tooltip: { trigger: 'axis' },
    legend: { top: 8, right: 18, data: ['评论数', '弹幕数', '平均情感'] },
    grid: baseGrid(),
    xAxis: { type: 'category', boundaryGap: false, data: chartData.value.dates },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: '{value}' },
      splitLine: { lineStyle: { color: '#e7edf5' } },
    },
    series: [
      {
        name: '评论数',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        data: chartData.value.comments,
      },
      {
        name: '弹幕数',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.12 },
        data: chartData.value.danmaku,
      },
      {
        name: '平均情感',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.1 },
        data: chartData.value.sentiment,
      },
    ],
  }),
  [modeRef, dataRef],
)

function getBucketKey(dateText, mode) {
  const date = new Date(`${dateText}T00:00:00`)
  if (Number.isNaN(date.getTime()) || mode === '日') return dateText

  if (mode === '月') {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
  }

  const firstDay = new Date(date)
  const weekday = firstDay.getDay() || 7
  firstDay.setDate(firstDay.getDate() - weekday + 1)
  return `${firstDay.getFullYear()}-W${String(getWeekNumber(firstDay)).padStart(2, '0')}`
}

function getWeekNumber(date) {
  const firstDay = new Date(date.getFullYear(), 0, 1)
  const pastDays = Math.floor((date - firstDay) / 86400000)
  return Math.ceil((pastDays + firstDay.getDay() + 1) / 7)
}
</script>
