<template>
  <section class="platform-grid">
    <article v-for="item in summaryCards" :key="item.label" class="platform-stat">
      <strong>{{ item.value }}</strong>
      <span>{{ item.label }}</span>
    </article>
  </section>

  <section class="panel video-panel">
    <div class="panel-header">
      <div>
        <div class="panel-title">数据源与同步监控</div>
        <div class="panel-desc">检查 v2 目录、事实、配置、运营和平台入库汇总是否可访问、是否有数据及最近更新时间是否正常。</div>
      </div>
      <el-button type="primary" @click="loadStatuses">刷新状态</el-button>
    </div>

    <el-table v-loading="loading" :data="statuses" empty-text="暂无数据源状态" style="width: 100%">
      <el-table-column prop="displayName" label="数据表" min-width="160">
        <template #default="{ row }">
          <strong>{{ row.displayName }}</strong>
          <div class="table-sub">{{ row.tableName }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="layer" label="层级" width="90" />
      <el-table-column prop="rowCount" label="行数" width="130">
        <template #default="{ row }">{{ formatNumber(row.rowCount) }}</template>
      </el-table-column>
      <el-table-column prop="latestAt" label="最近时间" width="190">
        <template #default="{ row }">{{ formatDateTime(row.latestAt) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="说明" min-width="220" />
    </el-table>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchDataSourceStatuses } from '@/api/platform'

const loading = ref(false)
const statuses = ref([])

const summaryCards = computed(() => {
  const healthy = statuses.value.filter((item) => item.status === 'healthy').length
  const empty = statuses.value.filter((item) => item.status === 'empty').length
  const stale = statuses.value.filter((item) => item.status === 'stale').length
  const missing = statuses.value.filter((item) => item.status === 'missing').length
  const totalRows = statuses.value.reduce((sum, item) => sum + item.rowCount, 0)
  return [
    { label: '健康数据表', value: healthy },
    { label: '空表', value: empty },
    { label: '已过期', value: stale },
    { label: '不可访问', value: missing },
    { label: '总行数', value: formatNumber(totalRows) },
  ]
})

onMounted(loadStatuses)

async function loadStatuses() {
  loading.value = true
  try {
    statuses.value = await fetchDataSourceStatuses()
  } catch (error) {
    ElMessage.error(error.message || '数据源状态加载失败')
  } finally {
    loading.value = false
  }
}

function statusType(status) {
  if (status === 'healthy') return 'success'
  if (status === 'empty' || status === 'stale') return 'warning'
  return 'danger'
}

function statusLabel(status) {
  return { healthy: '正常', empty: '空表', stale: '已过期', missing: '不可访问' }[status] ?? status
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '--'
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}
</script>
