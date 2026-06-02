<template>
  <div ref="chartEl" class="chart small" role="img" aria-label="评论情感占比环图"></div>
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
  color: ['#13a46f', '#0f7cff', '#e36b2c'],
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
