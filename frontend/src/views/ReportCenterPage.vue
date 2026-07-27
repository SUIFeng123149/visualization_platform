<template>
  <section class="platform-grid">
    <article v-for="item in summaryCards" :key="item.label" class="platform-stat">
      <strong>{{ item.value }}</strong>
      <span>{{ item.label }}</span>
    </article>
  </section>

  <AiInsightPanel
    title="AI生成报表摘要"
    description="读取报表历史和生成情况，自动整理日报/周报摘要、数据覆盖范围和风险提示。"
    mode="report-summary"
    :context="reportAiContext"
    :prompts="reportPrompts"
    placeholder="例如：帮我生成一段今日报表摘要"
  />

  <section class="panel video-panel">
    <div class="panel-header">
      <div>
        <div class="panel-title">报表中心</div>
        <div class="panel-desc">沉淀报表生成历史，后续可扩展为异步生成、下载管理和审批流。</div>
      </div>
      <div class="panel-actions">
        <el-button type="primary" :loading="exporting" @click="exportOperationsSnapshot">导出运营快照</el-button>
        <el-button @click="loadReports">刷新历史</el-button>
        <el-button type="primary" @click="recordDemoReport">记录完整报表</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="reports" empty-text="暂无报表历史；如果未创建 ops_report_history 表，记录不会持久化。" style="width: 100%">
      <el-table-column prop="reportName" label="报表名称" min-width="180" />
      <el-table-column prop="reportType" label="类型" width="120" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'success' ? 'success' : 'warning'">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="rowCount" label="数据行数" width="120">
        <template #default="{ row }">{{ Number(row.rowCount || 0).toLocaleString('zh-CN') }}</template>
      </el-table-column>
      <el-table-column prop="fileName" label="文件名" min-width="220" />
      <el-table-column prop="createdAt" label="生成时间" width="190">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="220" />
    </el-table>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AiInsightPanel from '@/components/ai/AiInsightPanel.vue'
import { createReportHistory, fetchDataSourceStatuses, fetchReportHistory } from '@/api/platform'
import { fetchDashboardSummary } from '@/api/content'
import { fetchTasks } from '@/api/tasks'
import { exportExcelWorkbook } from '@/utils/exportExcel'

const loading = ref(false)
const reports = ref([])
const exporting = ref(false)

const summaryCards = computed(() => {
  const total = reports.value.length
  const success = reports.value.filter((item) => item.status === 'success').length
  const rows = reports.value.reduce((sum, item) => sum + Number(item.rowCount || 0), 0)
  const latest = reports.value[0]?.createdAt ? formatDateTime(reports.value[0].createdAt) : '--'
  return [
    { label: '历史报表', value: total },
    { label: '成功生成', value: success },
    { label: '累计数据行', value: rows.toLocaleString('zh-CN') },
    { label: '最近生成', value: latest },
  ]
})

const reportAiContext = computed(() => ({
  page: '报表中心',
  summary: summaryCards.value,
  reports: reports.value.slice(0, 20),
  statusSummary: reports.value.reduce((result, item) => {
    const status = item.status || 'unknown'
    result[status] = (result[status] || 0) + 1
    return result
  }, {}),
}))

const reportPrompts = [
  '生成当前报表中心摘要',
  '哪些报表记录需要关注',
  '把报表历史整理成日报口径',
  '给出报表中心下一步建设建议',
]

onMounted(loadReports)

async function loadReports() {
  loading.value = true
  try {
    reports.value = await fetchReportHistory()
  } catch (error) {
    ElMessage.error(error.message || '报表历史加载失败')
  } finally {
    loading.value = false
  }
}

async function recordDemoReport() {
  try {
    const today = new Date().toISOString().slice(0, 10)
    const created = await createReportHistory({
      reportName: '完整数据报表',
      reportType: 'overview',
      fileName: `视频数据平台_完整数据报表_${today}.xlsx`,
      rowCount: 0,
      remark: '从报表中心手动记录，可作为报表生成链路验证。',
    })
    reports.value = [created, ...reports.value]
    ElMessage.success(created.status === 'memory' ? '已记录，但未持久化：请创建 ops_report_history 表' : '报表历史已记录')
  } catch (error) {
    ElMessage.error(error.message || '报表历史记录失败')
  }
}

async function exportOperationsSnapshot() {
  exporting.value = true
  try {
    const [summary, tasks, sources] = await Promise.all([fetchDashboardSummary(), fetchTasks(), fetchDataSourceStatuses()])
    const today = new Date().toISOString().slice(0, 10)
    const fileName = `video-analytics-operations-${today}.xlsx`
    exportExcelWorkbook(fileName, [
      { name: '运营概览', columns: Object.keys(summary).map((key) => ({ key, label: key })), rows: [summary] },
      { name: '待处理事项', columns: ['taskId', 'title', 'level', 'status', 'platformCode', 'text'].map((key) => ({ key, label: key })), rows: tasks },
      { name: '数据健康', columns: ['displayName', 'layer', 'rowCount', 'latestAt', 'status', 'message'].map((key) => ({ key, label: key })), rows: sources },
    ])
    const created = await createReportHistory({ reportName: '运营快照报告', reportType: 'operations-snapshot', fileName, rowCount: 1 + tasks.length + sources.length, remark: '仪表盘全量汇总、待处理事项与数据健康状态。' })
    reports.value = [created, ...reports.value]
    ElMessage.success('运营快照报告已导出')
  } catch (error) {
    ElMessage.error(error.message || '运营快照报告导出失败')
  } finally {
    exporting.value = false
  }
}

function statusLabel(status) {
  return { success: '成功', failed: '失败', memory: '未持久化' }[status] ?? status
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '--'
}
</script>
