import { BarChart, LineChart, PieChart, RadarChart, ScatterChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  RadarComponent,
  TitleComponent,
  TooltipComponent,
} from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { nextTick, onBeforeUnmount, onMounted, shallowRef, watch } from 'vue'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  RadarChart,
  ScatterChart,
  GridComponent,
  LegendComponent,
  RadarComponent,
  TitleComponent,
  TooltipComponent,
  CanvasRenderer,
])

export function useEChart(targetRef, optionFactory, sources = []) {
  const chart = shallowRef(null)
  let resizeObserver

  function scheduleResize() {
    requestAnimationFrame(() => {
      resize()
      requestAnimationFrame(resize)
    })
  }

  function render() {
    if (!targetRef.value) {
      dispose()
      return
    }

    if (!chart.value) {
      chart.value = echarts.init(targetRef.value)
    }

    chart.value.setOption(optionFactory(), true)
    scheduleResize()
  }

  function resize() {
    chart.value?.resize()
  }

  function dispose() {
    chart.value?.dispose()
    chart.value = null
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
    dispose()
  })

  if (sources.length > 0) {
    watch(sources, async () => {
      await nextTick()
      render()
    }, { flush: 'post' })
  }

  return {
    chart,
    render,
    resize,
  }
}
