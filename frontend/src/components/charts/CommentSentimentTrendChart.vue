<template>
  <div
    v-if="hasData"
    ref="chartEl"
    class="chart"
    role="img"
    :aria-label="chartSummary"
  ></div>
  <div v-else class="empty-state">
    当前筛选周期暂无评论趋势数据
  </div>
</template>

<script setup>
import { computed, ref, toRef } from 'vue'
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
const hasData = computed(() => props.data.length > 0)

const groupedData = computed(() => {
  const buckets = new Map()

  props.data.forEach((item) => {
    const key = getBucketKey(item.statDate, props.mode)
    const interactionCount = Number(item.interactionCount ?? item.commentCount) || 0
    const weight = Math.max(1, Number(item.analyzedCount) || interactionCount)
    const current = buckets.get(key) ?? {
      statDate: key,
      commentCount: 0,
      sentimentWeighted: 0,
      negativeWeighted: 0,
      weight: 0,
    }

    current.commentCount += interactionCount
    current.sentimentWeighted += (Number(item.averageSentiment ?? item.avgSentiment) || 0) * weight
    current.negativeWeighted += (Number(item.negativeRatio) || 0) * weight
    current.weight += weight
    buckets.set(key, current)
  })

  return [...buckets.values()].map((item) => ({
    statDate: item.statDate,
    commentCount: item.commentCount,
    avgSentiment: item.weight > 0 ? item.sentimentWeighted / item.weight : null,
    negativeRatio: item.weight > 0 ? item.negativeWeighted / item.weight : null,
  }))
})

const chartSummary = computed(() => {
  if (!groupedData.value.length) return '评论量与情感风险趋势图。'
  const riskPeak = groupedData.value.reduce((highest, item) =>
    !highest || item.negativeRatio > highest.negativeRatio ? item : highest, null)
  return `评论量与情感风险趋势图。负向占比最高点为 ${riskPeak.statDate}，占比 ${(riskPeak.negativeRatio * 100).toFixed(1)}%。`
})

useEChart(chartEl, () => {
  const showZoom = groupedData.value.length > 14
  const dates = groupedData.value.map((item) => item.statDate)

  return {
    animationDuration: 300,
    color: ['#2563EB', '#0F766E', '#D97706'],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        const item = groupedData.value[params[0]?.dataIndex]
        if (!item) return ''
        return [
          `<strong>${item.statDate}</strong>`,
          `评论数量：${item.commentCount.toLocaleString('zh-CN')}`,
          `平均情感：${formatPercent(item.avgSentiment)}`,
          `负向占比：${formatPercent(item.negativeRatio)}`,
        ].join('<br/>')
      },
    },
    legend: {
      top: 8,
      right: 18,
      data: ['评论数量', '平均情感', '负向占比'],
    },
    grid: {
      top: 58,
      right: 64,
      bottom: showZoom ? 80 : 48,
      left: 58,
      containLabel: true,
    },
    dataZoom: showZoom
      ? [
          { type: 'inside', start: 0, end: 45 },
          { type: 'slider', height: 18, bottom: 10, start: 0, end: 45 },
        ]
      : [],
    xAxis: {
      type: 'category',
      data: dates,
      axisTick: { alignWithLabel: true },
      axisLabel: { hideOverlap: true },
    },
    yAxis: [
      {
        type: 'value',
        name: '评论数',
        minInterval: 1,
        splitLine: { lineStyle: { color: '#E2E8F0' } },
      },
      {
        type: 'value',
        name: '占比 %',
        min: 0,
        max: 100,
        axisLabel: { formatter: '{value}%' },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '评论数量',
        type: 'bar',
        barMaxWidth: 28,
        itemStyle: { borderRadius: [4, 4, 0, 0] },
        data: groupedData.value.map((item) => item.commentCount),
      },
      {
        name: '平均情感',
        type: 'line',
        yAxisIndex: 1,
        smooth: 0.25,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 3 },
        data: groupedData.value.map((item) => toPercent(item.avgSentiment)),
      },
      {
        name: '负向占比',
        type: 'line',
        yAxisIndex: 1,
        smooth: 0.25,
        symbol: 'diamond',
        symbolSize: 8,
        lineStyle: { width: 2, type: 'dashed' },
        data: groupedData.value.map((item) => toPercent(item.negativeRatio)),
      },
    ],
  }
}, [modeRef, dataRef])

function toPercent(value) {
  return value === null || value === undefined ? null : Number((Number(value) * 100).toFixed(1))
}

function formatPercent(value) {
  const percent = toPercent(value)
  return percent === null ? '--' : `${percent.toFixed(1)}%`
}

function getBucketKey(dateText, mode) {
  const date = new Date(`${dateText}T00:00:00`)
  if (Number.isNaN(date.getTime()) || mode === '日') return dateText

  if (mode === '月') {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
  }

  const firstDay = new Date(date)
  const weekday = firstDay.getDay() || 7
  firstDay.setDate(firstDay.getDate() - weekday + 1)
  return `${firstDay.getFullYear()}年第${getWeekNumber(firstDay)}周`
}

function getWeekNumber(date) {
  const firstDay = new Date(date.getFullYear(), 0, 1)
  const pastDays = Math.floor((date - firstDay) / 86400000)
  return Math.ceil((pastDays + firstDay.getDay() + 1) / 7)
}
</script>
