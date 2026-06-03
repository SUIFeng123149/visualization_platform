<template>
  <div v-if="hasData" ref="chartEl" class="chart small" role="img" aria-label="评论情感占比环图"></div>
  <div v-else class="empty-state small">暂无情感数据</div>
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
const hasData = computed(() => props.data.some((item) => item.totalCount > 0))
const chartData = computed(() => {
  const positive = props.data.reduce((sum, item) => sum + item.positiveCount, 0)
  const neutral = props.data.reduce((sum, item) => sum + item.neutralCount, 0)
  const negative = props.data.reduce((sum, item) => sum + item.negativeCount, 0)

  return [
    { name: '正向', value: positive },
    { name: '中性', value: neutral },
    { name: '负向', value: negative },
  ]
})

useEChart(chartEl, () => ({
  color: ['#059669', '#3B82F6', '#D97706'],
  tooltip: { trigger: 'item' },
  legend: { bottom: 8 },
  series: [
    {
      name: '评论情感',
      type: 'pie',
      radius: ['46%', '70%'],
      center: ['50%', '46%'],
      label: { formatter: '{b}\n{d}%' },
      data: chartData.value,
    },
  ],
}), [dataRef])
</script>
