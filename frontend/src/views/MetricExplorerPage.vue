<template>
  <section class="panel metric-explorer" v-loading="loading">
    <div class="panel-header metric-explorer-header">
      <div>
        <div class="panel-title">统一指标对比</div>
        <div class="panel-desc">读取各内容的最新指标快照；仅标记为可比较的指标适合跨平台横向判断。</div>
      </div>
      <div class="metric-explorer-controls">
        <label class="metric-explorer-field">
          <span>指标</span>
          <el-select v-model="metricKey" aria-label="选择分析指标" :loading="definitionsLoading" @change="loadComparison">
            <el-option v-for="item in definitions" :key="item.metricKey" :label="item.displayName" :value="item.metricKey" />
          </el-select>
        </label>
        <label class="metric-explorer-field">
          <span>内容类型</span>
          <el-select v-model="contentType" aria-label="选择内容类型" @change="loadComparison">
            <el-option label="全部类型" value="all" />
            <el-option label="短视频" value="short_video" />
            <el-option label="视频" value="video" />
            <el-option label="剧集" value="series" />
            <el-option label="单集" value="episode" />
          </el-select>
        </label>
      </div>
    </div>

    <div v-if="selectedDefinition" class="metric-explorer-context">
      <el-tag :type="selectedDefinition.comparable ? 'success' : 'info'" effect="plain">
        {{ selectedDefinition.comparable ? '可跨平台比较' : '仅平台内参考' }}
      </el-tag>
      <span>{{ selectedDefinition.definition }}</span>
      <span>单位：{{ unitLabel(selectedDefinition.unit) }}</span>
    </div>

    <el-alert
      v-if="selectedDefinition"
      :title="qualitySummary"
      :type="qualityAlertType"
      :closable="false"
      show-icon
      class="metric-explorer-alert"
    />

    <el-alert
      v-if="selectedDefinition && !selectedDefinition.comparable"
      title="该指标保留平台原始语义，请在同一平台内比较，不用于跨平台排名。"
      type="warning"
      :closable="false"
      show-icon
      class="metric-explorer-alert"
    />

    <el-table :data="rows" empty-text="当前筛选条件下没有可用的指标快照" style="width: 100%">
      <el-table-column label="平台" min-width="160">
        <template #default="{ row }">{{ platformName(row.platformCode) }}</template>
      </el-table-column>
      <el-table-column label="内容数" width="130" align="right" header-align="right">
        <template #default="{ row }">{{ count(row.contentCount) }}</template>
      </el-table-column>
      <el-table-column label="有指标内容" width="150" align="right" header-align="right">
        <template #default="{ row }">{{ count(row.availableCount) }}</template>
      </el-table-column>
      <el-table-column label="覆盖率" width="130" align="right" header-align="right">
        <template #default="{ row }">{{ percent(row.availableCount, row.contentCount) }}</template>
      </el-table-column>
      <el-table-column label="数据质量" width="130">
        <template #default="{ row }">
          <el-tag :type="qualityType(row)" effect="plain">{{ qualityLabel(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近快照" min-width="180">
        <template #default="{ row }">{{ formatDateTime(row.latestCapturedAt) }}</template>
      </el-table-column>
      <el-table-column label="平均值" min-width="150" align="right" header-align="right">
        <template #default="{ row }">{{ metricValue(row.averageValue) }}</template>
      </el-table-column>
      <el-table-column label="最小值" min-width="150" align="right" header-align="right">
        <template #default="{ row }">{{ metricValue(row.minValue) }}</template>
      </el-table-column>
      <el-table-column label="最大值" min-width="150" align="right" header-align="right">
        <template #default="{ row }">{{ metricValue(row.maxValue) }}</template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchMetricComparison, fetchMetricDefinitions } from '@/api/content'
import { usePlatformContext } from '@/composables/usePlatformContext'

const loading = ref(false)
const definitionsLoading = ref(false)
const definitions = ref([])
const rows = ref([])
const metricKey = ref('')
const contentType = ref('all')
const { platforms, selectedPlatform, loadPlatforms } = usePlatformContext()

const selectedDefinition = computed(() => definitions.value.find((item) => item.metricKey === metricKey.value) ?? null)
const qualitySummary = computed(() => {
  if (!rows.value.length) return '当前筛选范围内没有可评估的数据质量样本。'
  const healthy = rows.value.filter((row) => qualityLevel(row) === 'healthy').length
  return `数据质量：${healthy}/${rows.value.length} 个平台的指标覆盖率不低于 80%，且最近快照未超过 7 天。`
})
const qualityAlertType = computed(() => rows.value.length && rows.value.every((row) => qualityLevel(row) === 'healthy') ? 'success' : 'warning')

onMounted(loadPage)
watch(selectedPlatform, loadComparison)

async function loadPage() {
  definitionsLoading.value = true
  try {
    await loadPlatforms()
    definitions.value = await fetchMetricDefinitions()
    metricKey.value = definitions.value.find((item) => item.comparable)?.metricKey ?? definitions.value[0]?.metricKey ?? ''
    await loadComparison()
  } catch (error) {
    ElMessage.error(error.message || '指标定义加载失败')
  } finally {
    definitionsLoading.value = false
  }
}

async function loadComparison() {
  if (!metricKey.value) return
  loading.value = true
  try {
    rows.value = await fetchMetricComparison({
      metricKey: metricKey.value,
      platform: selectedPlatform.value === 'all' ? undefined : selectedPlatform.value,
      contentType: contentType.value === 'all' ? undefined : contentType.value,
    })
  } catch (error) {
    rows.value = []
    ElMessage.error(error.message || '指标对比数据加载失败')
  } finally {
    loading.value = false
  }
}

function platformName(code) {
  return platforms.value.find((item) => item.platformCode === code)?.displayName ?? code
}
function unitLabel(unit) {
  return { count: '次数', ratio: '比例', percentile: '百分位', score: '分值' }[unit] ?? unit ?? '原始数值'
}
function count(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}
function percent(value, total) {
  return total ? `${((Number(value) / Number(total)) * 100).toFixed(1)}%` : '--'
}
function metricValue(value) {
  if (value === null || value === undefined) return '--'
  const unit = selectedDefinition.value?.unit
  if (unit === 'ratio') return `${(Number(value) * 100).toFixed(1)}%`
  if (unit === 'percentile') return `P${Math.round(Number(value) * 100)}`
  if (unit === 'count') return Math.round(Number(value)).toLocaleString('zh-CN')
  return Number(value).toFixed(3)
}
function qualityLevel(row) {
  if (!row.contentCount || !row.availableCount) return 'missing'
  if (Number(row.availableCount) / Number(row.contentCount) < 0.8) return 'incomplete'
  if (!row.latestCapturedAt || Date.now() - new Date(row.latestCapturedAt).getTime() > 7 * 24 * 60 * 60 * 1000) return 'stale'
  return 'healthy'
}
function qualityLabel(row) {
  return { healthy: '可用', incomplete: '覆盖不足', stale: '已过期', missing: '无数据' }[qualityLevel(row)]
}
function qualityType(row) {
  return { healthy: 'success', incomplete: 'warning', stale: 'warning', missing: 'danger' }[qualityLevel(row)]
}
function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '--'
}
</script>
