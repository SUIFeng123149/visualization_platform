<template>
  <section class="dashboard-grid">
    <ChartPanel title="UP主能力画像" description="对比创作者的播放、热度、情感和互动能力，辅助判断合作与复盘优先级。">
      <UpRadarChart :data="upPerformance" />
    </ChartPanel>

    <ChartPanel title="UP主表现排行" description="按创作者维度汇总视频表现和互动数据；样本数不足时需谨慎解读。">
      <el-table :data="upPerformance" empty-text="暂无UP主表现数据" style="width: 100%">
        <el-table-column prop="upName" label="UP主" min-width="160" />
        <el-table-column prop="videoCount" label="视频数" width="90" />
        <el-table-column prop="avgViewCount" label="平均播放" width="120" :formatter="numberColumn" />
        <el-table-column prop="avgHeatScore" label="平均热度" width="120" :formatter="numberColumn" />
        <el-table-column prop="avgSentiment" label="情感" width="90" />
        <el-table-column prop="totalLikeCount" label="总点赞" width="120" :formatter="numberColumn" />
      </el-table>
    </ChartPanel>
  </section>

  <section class="analysis-list">
    <article v-for="item in creatorInsights" :key="item.title" class="analysis-card">
      <div class="analysis-card-head">
        <strong>{{ item.title }}</strong>
        <el-tag :type="item.type">{{ item.level }}</el-tag>
      </div>
      <p>{{ item.text }}</p>
    </article>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import ChartPanel from '@/components/charts/ChartPanel.vue'
import UpRadarChart from '@/components/charts/UpRadarChart.vue'
import { useDashboardData, formatCompact } from '@/composables/useDashboardData'

const { upPerformance } = useDashboardData()

const creatorInsights = computed(() => {
  const topHeat = upPerformance.value[0]
  const topLike = [...upPerformance.value].sort((a, b) => b.totalLikeCount - a.totalLikeCount)[0]
  const stable = [...upPerformance.value].filter((item) => item.videoCount >= 2).sort((a, b) => b.avgSentiment - a.avgSentiment)[0]

  return [
    {
      title: '热度样本',
      level: '增长',
      type: 'success',
      text: topHeat ? `${topHeat.upName} 平均热度最高，适合拆解选题、标题和发布时间策略。` : '暂无UP主样本。',
    },
    {
      title: '互动样本',
      level: '复盘',
      type: 'primary',
      text: topLike ? `${topLike.upName} 累计点赞最高，适合拆解观众参与机制。` : '暂无点赞样本。',
    },
    {
      title: '口碑样本',
      level: '观察',
      type: 'warning',
      text: stable ? `${stable.upName} 在多视频样本中情感表现较好，可作为稳定口碑参考。` : '多视频样本不足，暂不判断稳定性。',
    },
  ]
})

function numberColumn(_row, _column, value) {
  return formatCompact(value)
}
</script>
