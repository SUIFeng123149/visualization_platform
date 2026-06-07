<template>
  <section class="video-summary-grid">
    <ChartPanel title="视频情感结构" description="判断高热视频是否同时具备稳定口碑；没有评论样本的视频不会计入情感占比。">
      <SentimentPieChart :data="videoSentiments" />
    </ChartPanel>

    <section class="analysis-list compact">
      <article
        v-for="item in allVideoCards.slice(0, 4)"
        :key="item.bvid"
        class="analysis-card"
        @click="openVideo(item.bvid)"
      >
        <div class="analysis-card-head">
          <strong>{{ item.title }}</strong>
          <el-tag :type="item.rankNo <= 3 ? 'success' : 'info'">Rank {{ item.rankNo }}</el-tag>
        </div>
        <div class="analysis-card-grid">
          <span>播放量：{{ formatCompact(item.viewCount) }}</span>
          <span>互动量：{{ formatCompact(item.interactions) }}</span>
          <span>热度：{{ Math.round(item.heatScore).toLocaleString('zh-CN') }}</span>
          <span>UP主：{{ item.upName || '--' }}</span>
        </div>
      </article>
    </section>
  </section>

  <section class="panel video-panel">
    <div class="panel-header">
      <div>
        <div class="panel-title">多视频对比分析</div>
        <div class="panel-desc">对比热度分、互动率和正向占比，快速识别“高热但低口碑”或“低热但高互动”的复盘对象。</div>
      </div>
      <el-select
        v-model="selectedCompareBvids"
        class="module-select compare-select"
        multiple
        collapse-tags
        collapse-tags-tooltip
        :multiple-limit="5"
        placeholder="选择对比视频"
      >
        <el-option v-for="video in heatRank" :key="video.bvid" :label="video.title" :value="video.bvid" />
      </el-select>
    </div>
    <VideoCompareChart :videos="compareVideos" :sentiments="videoSentiments" />
    <div class="compare-summary">
      <article v-for="item in compareInsights" :key="item.label">
        <strong>{{ item.label }}</strong>
        <span>{{ item.text }}</span>
      </article>
    </div>
  </section>

  <VideoTable
    v-model:keyword="keyword"
    :videos="allFilteredVideos"
    title="视频表现分析"
    description="按热度、互动率和情感表现筛选可复盘视频；本页不受总览分区筛选影响，保留完整样本池。"
  />
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import ChartPanel from '@/components/charts/ChartPanel.vue'
import SentimentPieChart from '@/components/charts/SentimentPieChart.vue'
import VideoCompareChart from '@/components/charts/VideoCompareChart.vue'
import VideoTable from '@/components/video/VideoTable.vue'
import { useDashboardData, formatCompact, getInteractions } from '@/composables/useDashboardData'

const router = useRouter()
const { keyword, heatRank, videoSentiments, allVideoCards, allFilteredVideos } = useDashboardData()
const selectedCompareBvids = ref([])

const compareVideos = computed(() => {
  const selected = new Set(selectedCompareBvids.value)
  return heatRank.value.filter((item) => selected.has(item.bvid))
})

const sentimentMap = computed(() => new Map(videoSentiments.value.map((item) => [item.bvid, item])))
const compareInsights = computed(() => {
  if (!compareVideos.value.length) {
    return [{ label: '对比提示', text: '请选择视频后查看差异结论。' }]
  }

  const highestHeat = maxBy(compareVideos.value, (item) => item.heatScore)
  const highestInteraction = maxBy(compareVideos.value, (item) => (
    item.viewCount > 0 ? getInteractions(item) / item.viewCount : 0
  ))
  const highestPositive = maxBy(compareVideos.value, (item) => sentimentMap.value.get(item.bvid)?.positiveRatio ?? -1)

  return [
    { label: '热度领先', text: highestHeat ? `${highestHeat.title} 的热度分最高，适合拆解传播来源。` : '--' },
    { label: '互动领先', text: highestInteraction ? `${highestInteraction.title} 的互动率最高，适合拆解观众参与机制。` : '--' },
    { label: '口碑领先', text: highestPositive ? `${highestPositive.title} 的正向占比最高，适合沉淀评论区认可点。` : '暂无情感样本。' },
  ]
})

watch(
  heatRank,
  (rows) => {
    if (selectedCompareBvids.value.length || !rows.length) return
    selectedCompareBvids.value = rows.slice(0, 4).map((item) => item.bvid)
  },
  { immediate: true },
)

function openVideo(bvid) {
  if (!bvid) return
  router.push({ name: 'videoDetail', params: { bvid } })
}

function maxBy(items, getter) {
  return items.reduce((best, item) => {
    if (!best) return item
    return getter(item) > getter(best) ? item : best
  }, null)
}
</script>
