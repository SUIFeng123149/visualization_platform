import * as echarts from 'echarts'
import { nextTick, onBeforeUnmount, onMounted, shallowRef, watch } from 'vue'

export function useEChart(targetRef, optionFactory, sources = []) {
  const chart = shallowRef(null)
  let resizeObserver

  function render() {
    if (!targetRef.value) return

    if (!chart.value) {
      chart.value = echarts.init(targetRef.value)
    }

    chart.value.setOption(optionFactory(), true)
    resize()
  }

  function resize() {
    chart.value?.resize()
  }

  onMounted(async () => {
    await nextTick()
    render()
    window.addEventListener('resize', resize)
    if ('ResizeObserver' in window && targetRef.value) {
      resizeObserver = new ResizeObserver(resize)
      resizeObserver.observe(targetRef.value)
    }
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', resize)
    resizeObserver?.disconnect()
    chart.value?.dispose()
  })

  if (sources.length > 0) {
    watch(sources, async () => {
      await nextTick()
      render()
    })
  }

  return {
    chart,
    render,
    resize,
  }
}
