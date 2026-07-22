<template>
  <section v-loading="loadingUnified" class="unified-overview">
    <el-alert v-if="unifiedError" :title="unifiedError" type="error" show-icon :closable="false" />

    <MetricGrid :metrics="overviewMetrics" />

    <section class="panel content-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">内容热度概览</div>
          <div class="panel-desc">跨平台按归一化热度比较，原始播放量仅作为平台内规模参考。</div>
        </div>
        <el-button
          type="primary"
          plain
          @click="router.push({ name: 'contents', query: selectedPlatform === 'all' ? undefined : { platform: selectedPlatform } })"
        >查看全部内容</el-button>
      </div>
      <el-table :data="contents.slice(0, 10)" empty-text="暂无统一内容数据" style="width: 100%" @row-click="openContent">
        <el-table-column prop="title" label="内容" min-width="280" />
        <el-table-column label="平台" width="110"><template #default="{ row }">{{ platformName(row.platformCode) }}</template></el-table-column>
        <el-table-column label="类型" width="100"><template #default="{ row }">{{ contentTypeName(row.contentType) }}</template></el-table-column>
        <el-table-column label="播放量" width="130"><template #default="{ row }">{{ count(row.viewCount) }}</template></el-table-column>
        <el-table-column label="归一化热度" width="140"><template #default="{ row }">{{ percentile(row.normalizedHeatScore) }}</template></el-table-column>
      </el-table>
    </section>

    <section class="overview-columns">
      <section class="panel content-panel">
        <div class="panel-header">
          <div>
            <div class="panel-title">互动趋势</div>
            <div class="panel-desc">评论、弹幕、评分和回复使用统一互动类型。</div>
          </div>
        </div>
        <UnifiedInteractionTrendChart :data="trends" />
      </section>

      <section class="panel content-panel">
        <div class="panel-header">
          <div>
            <div class="panel-title">讨论关键词</div>
            <div class="panel-desc">来自统一文本分析占位表，后续可直接替换为真实模型结果。</div>
          </div>
        </div>
        <div v-if="keywords.length" class="overview-word-cloud" role="list" aria-label="讨论关键词词云">
          <span
            v-for="item in wordCloudItems"
            :key="item.word"
            class="overview-word"
            :class="`word-tone-${item.tone}`"
            :style="{ '--word-size': `${item.size}px`, '--word-rotate': `${item.rotate}deg` }"
            :title="`${item.word}：${item.wordCount} 次`"
            role="listitem"
          >{{ item.word }}</span>
        </div>
        <div v-else class="empty-state small">暂无关键词数据</div>
      </section>
    </section>

    <section class="panel content-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">发布账号表现</div>
          <div class="panel-desc">统一覆盖创作者、频道和发行方，不再限定为 UP 主。</div>
        </div>
      </div>
      <el-table :data="accounts.slice(0, 10)" empty-text="暂无账号数据" style="width: 100%">
        <el-table-column prop="displayName" label="账号" min-width="180" />
        <el-table-column label="平台" width="110"><template #default="{ row }">{{ platformName(row.platformCode) }}</template></el-table-column>
        <el-table-column prop="accountType" label="类型" width="110" />
        <el-table-column prop="contentCount" label="内容数" width="100" />
        <el-table-column label="累计播放" width="140"><template #default="{ row }">{{ count(row.totalViewCount) }}</template></el-table-column>
        <el-table-column label="平均归一化热度" width="160"><template #default="{ row }">{{ percentile(row.averageNormalizedHeat) }}</template></el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MetricGrid from '@/components/metrics/MetricGrid.vue'
import UnifiedInteractionTrendChart from '@/components/charts/UnifiedInteractionTrendChart.vue'
import { usePlatformContext } from '@/composables/usePlatformContext'
import { useUnifiedAnalytics } from '@/composables/useUnifiedAnalytics'

const router = useRouter()
const { platforms, selectedPlatform, loadPlatforms } = usePlatformContext()
const { contents, trends, keywords, accounts, loadingUnified, unifiedError, summary, loadUnifiedAnalytics } = useUnifiedAnalytics()

const wordCloudItems = computed(() => {
  const max = Math.max(1, ...keywords.value.map((item) => Number(item.wordCount) || 0))
  return keywords.value.slice(0, 24).map((item, index) => ({
    ...item,
    size: Math.round(15 + ((Number(item.wordCount) || 0) / max) * 20),
    rotate: index % 5 === 0 ? -2 : index % 4 === 0 ? 2 : 0,
    tone: index % 5,
  }))
})

const overviewMetrics = computed(() => [
  metric('内容数量', summary.value.contentCount, `${summary.value.platformCount} 个平台`, '当前筛选'),
  metric('累计播放', count(summary.value.totalViews), '仅汇总可用值', '原始指标'),
  metric('互动样本', count(summary.value.interactionCount), '评论/弹幕/评分', '统一互动'),
  metric('平均归一化热度', percentile(summary.value.averageNormalizedHeat), '跨平台参考', '同类百分位'),
])

watch(selectedPlatform, load, { immediate: true })

async function load() {
  try {
    await loadPlatforms()
    await loadUnifiedAnalytics()
  } catch (error) {
    ElMessage.error(error.message || '统一总览加载失败')
  }
}

function metric(label, value, delta, note) { return { label, value, delta, note, status: 'up', description: `${label}来自 v2 统一分析接口。` } }
function openContent(row) {
  if (!row?.contentId) return
  router.push({
    name: 'contentDetail',
    params: { contentId: row.contentId },
    query: selectedPlatform.value === 'all' ? undefined : { platform: selectedPlatform.value },
  })
}
function platformName(code) { return platforms.value.find((item) => item.platformCode === code)?.displayName ?? code }
function count(value) { return value === null || value === undefined ? '不适用' : Number(value).toLocaleString('zh-CN') }
function percentile(value) { return value === null || value === undefined ? '--' : `P${Math.round(Number(value) * 100)}` }
function decimal(value) { return value === null || value === undefined ? '--' : Number(value).toFixed(3) }
function contentTypeName(type) { return { short_video: '短视频', video: '视频', series: '剧集', episode: '单集', movie: '电影' }[type] ?? type }
function interactionTypeName(type) { return { comment: '评论', danmaku: '弹幕', review: '评分/剧评', reply: '回复' }[type] ?? type }
</script>
