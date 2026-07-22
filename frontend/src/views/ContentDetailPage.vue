<template>
  <section class="detail-page" v-loading="loading">
    <div class="detail-hero">
      <div class="detail-hero-main">
        <button class="detail-back-button" type="button" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回内容列表</span>
        </button>
        <h2>{{ content?.title || '内容详情' }}</h2>
        <p v-if="content">{{ platformName }} · {{ content.externalContentId }} · {{ content.accountName || '未知发布账号' }}</p>
      </div>
      <el-tag v-if="content" size="large">{{ contentTypeName(content.contentType) }}</el-tag>
    </div>

    <MetricGrid v-if="content" :metrics="metrics" />

    <section v-if="content" class="detail-grid">
      <section class="panel detail-data-card content-capabilities">
        <div class="panel-header">
          <div>
            <div class="panel-title">平台数据能力</div>
            <div class="panel-desc">不支持的能力不会以零值参与指标或排行。</div>
          </div>
        </div>
        <div class="capability-list">
          <div v-for="item in capabilityItems" :key="item.key" class="capability-item" :class="{ supported: item.supported }">
            <span class="capability-label">{{ item.label }}</span>
            <el-tag size="small" :type="item.supported ? 'success' : 'info'">{{ item.supported ? '支持' : '不适用' }}</el-tag>
          </div>
        </div>
      </section>

      <section class="panel detail-data-card sentiment-summary">
        <div class="panel-header">
          <div>
            <div class="panel-title">互动情感概览</div>
            <div class="panel-desc">基于当前内容已完成文本分析的互动样本。</div>
          </div>
        </div>
        <div v-if="sentiment?.totalCount" class="sentiment-bars">
          <div class="sentiment-row positive"><span>正向</span><el-progress :percentage="sentiment.positiveRatio * 100" status="success" /><strong>{{ percent(sentiment.positiveRatio) }}</strong></div>
          <div class="sentiment-row negative"><span>负向</span><el-progress :percentage="sentiment.negativeRatio * 100" status="exception" /><strong>{{ percent(sentiment.negativeRatio) }}</strong></div>
          <div class="sentiment-footnote">已分析互动 {{ count(sentiment.totalCount) }} 条</div>
        </div>
        <div v-else class="empty-state small">暂无已分析的互动文本</div>
      </section>
    </section>

    <section v-if="content?.contentType === 'series'" class="panel content-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">子内容与单集</div>
          <div class="panel-desc">父子层级来自统一内容模型，真实剧集表接入后无需修改页面逻辑。</div>
        </div>
      </div>
      <el-table :data="children" empty-text="暂无子内容" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="280" />
        <el-table-column label="类型" width="100"><template #default="{ row }">{{ contentTypeName(row.contentType) }}</template></el-table-column>
        <el-table-column label="播放量" width="130"><template #default="{ row }">{{ count(row.viewCount) }}</template></el-table-column>
        <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="primary" @click="openChild(row.contentId)">查看</el-button></template></el-table-column>
      </el-table>
    </section>

    <section v-if="supportsTimeline" class="panel content-panel detail-timeline-card">
      <div class="panel-header">
        <div>
          <div class="panel-title">{{ interactionTypeName(timelineType) }}时间轴</div>
          <div class="panel-desc">按 30 秒聚合具有视频内时间点的互动。</div>
        </div>
      </div>
      <div class="timeline-visual-grid">
        <InteractionTimelineChart :data="timeline" />
        <el-table class="timeline-table" :data="timeline" empty-text="暂无时间轴数据" height="340" style="width: 100%">
          <el-table-column label="时间点" width="100" align="center" header-align="center"><template #default="{ row }"><span class="timeline-time">{{ formatVideoTime(row.timeBucket) }}</span></template></el-table-column>
          <el-table-column prop="interactionCount" label="数量" width="90" align="right" header-align="right" />
          <el-table-column label="情感" min-width="90" align="right" header-align="right"><template #default="{ row }"><span class="timeline-score">{{ decimal(row.averageSentiment) }}</span></template></el-table-column>
        </el-table>
      </div>
    </section>

    <section class="panel content-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">互动明细</div>
          <div class="panel-desc">评论、弹幕和评分统一为互动类型，平台专属字段按需为空。</div>
        </div>
        <el-select v-model="interactionType" class="module-select" aria-label="互动类型筛选">
          <el-option label="全部互动" value="all" />
          <el-option v-for="item in availableInteractionTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
      <el-table :data="pagedInteractions" empty-text="暂无互动数据" style="width: 100%">
        <el-table-column label="类型" width="100"><template #default="{ row }">{{ interactionTypeName(row.interactionType) }}</template></el-table-column>
        <el-table-column prop="userName" label="用户" width="150" />
        <el-table-column prop="text" label="内容" min-width="300" />
        <el-table-column label="点赞" width="100"><template #default="{ row }">{{ count(row.likeCount) }}</template></el-table-column>
        <el-table-column label="情感" width="110"><template #default="{ row }">{{ sentimentLabel(row.sentimentLabel) }}</template></el-table-column>
        <el-table-column label="时间" width="180"><template #default="{ row }">{{ formatDateTime(row.occurredAt || row.capturedAt) }}</template></el-table-column>
      </el-table>
      <div class="interaction-pagination">
        <span>当前加载 {{ interactions.length }} 条互动</span>
        <el-pagination
          v-model:current-page="interactionPage"
          :page-size="interactionPageSize"
          :total="interactions.length"
          layout="prev, pager, next"
          background
          hide-on-single-page
        />
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MetricGrid from '@/components/metrics/MetricGrid.vue'
import InteractionTimelineChart from '@/components/charts/InteractionTimelineChart.vue'
import {
  fetchContent,
  fetchContentChildren,
  fetchContentInteractions,
  fetchContentSentiment,
  fetchContentTimeline,
} from '@/api/content'
import { usePlatformContext } from '@/composables/usePlatformContext'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const content = ref(null)
const sentiment = ref(null)
const interactions = ref([])
const timeline = ref([])
const children = ref([])
const interactionType = ref('all')
const interactionPage = ref(1)
const interactionPageSize = 10
const { platforms, loadPlatforms } = usePlatformContext()

const platform = computed(() => platforms.value.find((item) => item.platformCode === content.value?.platformCode))
const platformName = computed(() => platform.value?.displayName ?? content.value?.platformCode ?? '--')
const capabilities = computed(() => platform.value?.capabilities ?? {})
const timelineType = computed(() => capabilities.value.danmaku ? 'danmaku' : capabilities.value.reviews ? 'review' : 'comment')
const supportsTimeline = computed(() => capabilities.value.danmaku === true)
const availableInteractionTypes = computed(() => [
  capabilities.value.comments && { label: '评论', value: 'comment' },
  capabilities.value.danmaku && { label: '弹幕', value: 'danmaku' },
  capabilities.value.reviews && { label: '评分/剧评', value: 'review' },
].filter(Boolean))
const capabilityItems = computed(() => {
  const labels = { comments: '评论', danmaku: '弹幕', reviews: '评分', series: '剧集层级', completion_rate: '完播率' }
  return Object.entries(labels).map(([key, label]) => ({ key, label, supported: Boolean(capabilities.value[key]) }))
})
const metrics = computed(() => content.value ? [
  metric('播放量', count(content.value.viewCount), '原始指标', platformName.value),
  metric('点赞量', count(content.value.likeCount), '原始指标', '互动'),
  metric('平台热度', score(content.value.platformHeatScore), '平台内比较', '不可跨平台直比'),
  metric('归一化热度', percentile(content.value.normalizedHeatScore), '跨平台参考', '同类百分位'),
] : [])
const pagedInteractions = computed(() => {
  const start = (interactionPage.value - 1) * interactionPageSize
  return interactions.value.slice(start, start + interactionPageSize)
})

watch(() => route.params.contentId, loadDetail, { immediate: true })
watch(interactionType, loadInteractions)

onMounted(loadPlatforms)

async function loadDetail(contentId) {
  if (!contentId) return
  loading.value = true
  try {
    await loadPlatforms()
    content.value = await fetchContent(contentId)
    const requests = [fetchContentSentiment(contentId), fetchContentInteractions(contentId, { limit: 100 })]
    if (content.value.contentType === 'series') requests.push(fetchContentChildren(contentId))
    const [sentimentData, interactionData, childData = []] = await Promise.all(requests)
    sentiment.value = sentimentData
    interactions.value = interactionData
    children.value = childData
    timeline.value = supportsTimeline.value ? await fetchContentTimeline(contentId, timelineType.value) : []
    interactionType.value = 'all'
  } catch (error) {
    ElMessage.error(error.message || '内容详情加载失败')
  } finally {
    loading.value = false
  }
}

async function loadInteractions() {
  if (!content.value) return
  try {
    interactions.value = await fetchContentInteractions(content.value.contentId, {
      type: interactionType.value === 'all' ? undefined : interactionType.value,
      limit: 100,
    })
    interactionPage.value = 1
  } catch (error) {
    ElMessage.error(error.message || '互动数据加载失败')
  }
}

function openChild(contentId) { router.push({ name: 'contentDetail', params: { contentId }, query: route.query }) }
function goBack() {
  if (route.query.platform || route.query.contentType) {
    router.push({ name: 'contents', query: route.query })
    return
  }
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push({ name: 'contents' })
}
function metric(label, value, delta, note) { return { label, value, delta, note, status: 'up', description: `${label}来自统一内容指标快照。` } }
function count(value) { return value === null || value === undefined ? '不适用' : Number(value).toLocaleString('zh-CN') }
function score(value) { return value === null || value === undefined ? '--' : Math.round(Number(value)).toLocaleString('zh-CN') }
function percentile(value) { return value === null || value === undefined ? '--' : `P${Math.round(Number(value) * 100)}` }
function decimal(value) { return value === null || value === undefined ? '--' : Number(value).toFixed(3) }
function percent(value) { return `${(Number(value || 0) * 100).toFixed(1)}%` }
function contentTypeName(type) { return { short_video: '短视频', video: '视频', series: '剧集', episode: '单集', movie: '电影' }[type] ?? type }
function interactionTypeName(type) { return { comment: '评论', danmaku: '弹幕', review: '评分/剧评', reply: '回复' }[type] ?? type }
function sentimentLabel(label) { return { positive: '正向', neutral: '中性', negative: '负向' }[label] ?? '未分析' }
function formatDateTime(value) { return value ? String(value).replace('T', ' ').slice(0, 19) : '--' }
function formatVideoTime(value) { const seconds = Number(value) || 0; return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}` }
</script>
