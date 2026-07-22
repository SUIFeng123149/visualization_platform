<template>
  <section class="collector-layout">
    <section class="panel collector-form-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">通用采集任务</div>
          <div class="panel-desc">任务使用统一目标和 options 契约；真实连接器接入后无需修改页面或后端任务格式。</div>
        </div>
      </div>

      <el-form label-position="top" class="collector-form">
        <el-form-item label="任务名称"><el-input v-model="form.taskName" maxlength="120" clearable /></el-form-item>
        <el-form-item label="自然语言需求">
          <el-input v-model="form.naturalLanguage" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="描述时间范围、内容主题和需要采集的互动类型。" />
        </el-form-item>

        <div class="collector-inline-grid">
          <el-form-item label="平台">
            <el-select v-model="form.platformCode" :loading="loadingPlatforms" @change="handlePlatformChange">
              <el-option v-for="item in platformOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="连接器">
            <el-input v-model="form.connectorName" readonly aria-label="平台注册连接器" />
          </el-form-item>
          <el-form-item label="目标类型">
            <el-select v-model="form.targetType">
              <el-option v-for="item in targetTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="采集目标">
          <el-input v-model="targetText" type="textarea" :rows="3" placeholder="每行一个关键词、账号 ID、内容 ID 或剧集 ID" />
        </el-form-item>

        <el-form-item label="请求能力">
          <el-checkbox-group v-model="form.requestedCapabilities" class="collector-source-grid">
            <el-checkbox-button v-for="item in capabilityOptions" :key="item.value" :label="item.value" :disabled="!item.available">
              {{ item.label }}
            </el-checkbox-button>
          </el-checkbox-group>
        </el-form-item>

        <div class="collector-option-grid">
          <el-form-item label="最大内容数"><el-input-number v-model="form.maxContents" :min="1" :max="5000" controls-position="right" /></el-form-item>
          <el-form-item label="每内容最大互动数"><el-input-number v-model="form.maxInteractions" :min="0" :max="5000" controls-position="right" /></el-form-item>
          <el-form-item label="并发 worker"><el-input-number v-model="form.workers" :min="1" :max="8" controls-position="right" /></el-form-item>
          <el-form-item label="请求间隔秒"><el-input-number v-model="form.requestDelaySeconds" :min="0" :max="180" :step="0.5" controls-position="right" /></el-form-item>
        </div>

        <el-form-item label="扩展 options JSON">
          <el-input v-model="optionsText" type="textarea" :rows="4" placeholder='例如 {"maxEpisodes":20,"includeReviews":true}' />
        </el-form-item>

        <div class="collector-actions">
          <el-button :icon="Refresh" @click="loadTasks">刷新任务</el-button>
          <el-button type="primary" :icon="Upload" :loading="submitting" @click="submitTask">下发任务</el-button>
        </div>
      </el-form>
    </section>

    <section class="panel collector-help-panel">
      <div class="panel-header"><div><div class="panel-title">连接器状态</div><div class="panel-desc">当前先验证任务逻辑，真实平台访问后续替换。</div></div></div>
      <div class="collector-help">
        <div><strong>任务协议</strong><span>schemaVersion 2.0</span></div>
        <div><strong>拉取任务</strong><span>POST /api/collector/tasks/pull</span></div>
        <div><strong>回传状态</strong><span>PUT /api/collector/tasks/{taskId}/status</span></div>
        <div><strong>当前实现</strong><span>连接器和能力由平台注册表统一约束，worker 按任务合同执行。</span></div>
      </div>
    </section>
  </section>

  <section class="panel video-panel">
    <div class="panel-header">
      <div><div class="panel-title">采集任务流转</div><div class="panel-desc">展示最近任务及模拟或真实 worker 回传状态。</div></div>
      <el-tag :type="activeTaskCount ? 'warning' : 'success'">{{ activeTaskCount ? `${activeTaskCount} 个进行中` : '暂无运行任务' }}</el-tag>
    </div>
    <el-table v-loading="loading" :data="tasks" empty-text="暂无采集任务" style="width: 100%" @row-click="selectTask">
      <el-table-column prop="taskName" label="任务" min-width="220"><template #default="{ row }"><strong>{{ row.taskName }}</strong><div class="table-sub">{{ row.taskId }}</div></template></el-table-column>
      <el-table-column prop="sourceType" label="平台" width="110" />
      <el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag :type="taskStatusType(row.status)">{{ taskStatusLabel(row.status) }}</el-tag></template></el-table-column>
      <el-table-column prop="progress" label="进度" width="150"><template #default="{ row }"><el-progress :percentage="row.progress ?? 0" :stroke-width="8" /></template></el-table-column>
      <el-table-column prop="rowCount" label="入库行数" width="120"><template #default="{ row }">{{ formatNumber(row.rowCount) }}</template></el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180"><template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template></el-table-column>
      <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="danger" :disabled="isFinished(row.status)" @click.stop="cancelTask(row)">取消</el-button></template></el-table-column>
    </el-table>
  </section>

  <section v-if="selectedTask" class="panel video-panel">
    <div class="panel-header">
      <div><div class="panel-title">任务详情</div><div class="panel-desc">{{ selectedTask.message || '等待 worker 回传状态' }}</div></div>
      <el-button :icon="Refresh" @click="refreshSelectedTask">刷新详情</el-button>
    </div>
    <div class="collector-result-grid">
      <div><span>原始数据</span><strong>{{ selectedTask.rawHdfsPath || '--' }}</strong></div>
      <div><span>标准化数据</span><strong>{{ selectedTask.cleanHdfsPath || '--' }}</strong></div>
      <div><span>批次号</span><strong>{{ selectedTask.batchId || '--' }}</strong></div>
      <div><span>结果表</span><strong>{{ selectedTask.resultTables?.length ? selectedTask.resultTables.join(', ') : '--' }}</strong></div>
    </div>
    <div class="collector-detail-body">
      <div><div class="collector-detail-title">自然语言需求</div><p>{{ selectedTask.naturalLanguage || '未填写' }}</p></div>
      <div><div class="collector-detail-title">统一结构化参数</div><pre>{{ prettyParams(selectedTask.paramsJson) }}</pre></div>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Upload } from '@element-plus/icons-vue'
import { cancelCollectorTask, createCollectorTask, fetchCollectorTask, fetchCollectorTasks } from '@/api/collector'
import { usePlatformContext } from '@/composables/usePlatformContext'

const allTargetTypeOptions = [
  { label: '关键词', value: 'keyword' }, { label: '发布账号', value: 'account' },
  { label: '指定内容', value: 'content' }, { label: '剧集', value: 'series' },
  { label: '热门内容', value: 'popular' },
]
const capabilityLabels = { content: '内容', metrics: '指标', comments: '评论', danmaku: '弹幕', reviews: '评分/剧评', series: '剧集', completion_rate: '完播率', creator_metrics: '账号指标', official_heat: '官方热度' }

const targetText = ref('')
const optionsText = ref('{}')
const submitting = ref(false)
const loading = ref(false)
const tasks = ref([])
const selectedTask = ref(null)
let timer = null
const { platforms, loadingPlatforms, loadPlatforms } = usePlatformContext()
const form = reactive({
  taskName: '通用内容数据采集', naturalLanguage: '', platformCode: '',
  connectorName: '', targetType: 'keyword',
  requestedCapabilities: [], maxContents: 100,
  maxInteractions: 50, workers: 2, requestDelaySeconds: 1, priority: 'normal',
})
const activeTaskCount = computed(() => tasks.value.filter((task) => !isFinished(task.status)).length)
const platformOptions = computed(() => platforms.value.map((platform) => ({
  label: platform.displayName,
  value: platform.platformCode,
  connector: platform.connectorName,
  capabilities: platform.capabilities ?? {},
})))
const targetTypeOptions = computed(() => {
  const platform = platformOptions.value.find((item) => item.value === form.platformCode)
  return allTargetTypeOptions.filter((item) => item.value !== 'series' || platform?.capabilities?.series)
})
const capabilityOptions = computed(() => {
  const platform = platformOptions.value.find((item) => item.value === form.platformCode)
  const available = new Set(['content', 'metrics'])
  Object.entries(platform?.capabilities ?? {}).forEach(([key, enabled]) => {
    if (enabled && key in capabilityLabels) available.add(key)
  })
  return Object.entries(capabilityLabels).map(([value, label]) => ({ value, label, available: available.has(value) }))
})

onMounted(async () => {
  try {
    await loadPlatforms()
    if (platformOptions.value.length) handlePlatformChange(platformOptions.value[0].value)
  } catch (error) {
    ElMessage.error(error.message || '平台注册数据加载失败')
  }
  loadTasks()
  timer = window.setInterval(loadTasks, 5000)
})
onBeforeUnmount(() => { if (timer) window.clearInterval(timer) })

function handlePlatformChange(platformCode) {
  form.platformCode = platformCode
  const platform = platformOptions.value.find((item) => item.value === platformCode)
  form.connectorName = platform?.connector || `${platformCode}-default`
  const available = new Set(capabilityOptions.value.filter((item) => item.available).map((item) => item.value))
  form.requestedCapabilities = ['content', 'metrics'].filter((item) => available.has(item))
  if (!available.has('series') && form.targetType === 'series') form.targetType = 'content'
}

async function submitTask() {
  if (!form.platformCode) return ElMessage.warning('请先选择平台')
  const targets = targetText.value.split(/[\n,，]+/).map((item) => item.trim()).filter(Boolean)
  if (!targets.length && !form.naturalLanguage.trim()) return ElMessage.warning('请填写采集目标或自然语言需求')
  let options
  try { options = optionsText.value.trim() ? JSON.parse(optionsText.value) : {} } catch { return ElMessage.error('扩展 options 必须是合法 JSON') }
  submitting.value = true
  try {
    const task = await createCollectorTask({
      taskName: form.taskName, naturalLanguage: form.naturalLanguage, platformCode: form.platformCode,
      sourceType: form.platformCode, connectorName: form.connectorName, targetType: form.targetType,
      targets, requestedCapabilities: form.requestedCapabilities, priority: form.priority,
      maxVideos: form.maxContents, maxCommentsPerVideo: form.maxInteractions, workers: form.workers,
      commentDelay: form.requestDelaySeconds, options: { ...options, maxContents: form.maxContents, maxInteractionsPerContent: form.maxInteractions, workers: form.workers, requestDelaySeconds: form.requestDelaySeconds },
    })
    selectedTask.value = task
    ElMessage.success('采集任务已下发')
    await loadTasks()
  } catch (error) { ElMessage.error(error.message || '采集任务下发失败') } finally { submitting.value = false }
}

async function loadTasks() { loading.value = true; try { tasks.value = await fetchCollectorTasks(); if (selectedTask.value) selectedTask.value = tasks.value.find((item) => item.taskId === selectedTask.value.taskId) ?? selectedTask.value } catch (error) { ElMessage.error(error.message || '任务加载失败') } finally { loading.value = false } }
async function refreshSelectedTask() { if (selectedTask.value) selectedTask.value = await fetchCollectorTask(selectedTask.value.taskId) }
function selectTask(task) { selectedTask.value = task }
async function cancelTask(task) { try { await ElMessageBox.confirm(`确认取消任务「${task.taskName}」？`, '取消任务', { type: 'warning' }); await cancelCollectorTask(task.taskId); await loadTasks() } catch (error) { if (error !== 'cancel') ElMessage.error(error.message || '取消失败') } }
function taskStatusType(status) { return { pending: 'info', dispatched: 'primary', running: 'warning', uploaded: 'warning', analyzing: 'warning', importing: 'warning', success: 'success', failed: 'danger', cancelled: 'info' }[status] ?? 'info' }
function taskStatusLabel(status) { return { pending: '待领取', dispatched: '已下发', running: '采集中', uploaded: '已上传', analyzing: '分析中', importing: '入库中', success: '完成', failed: '失败', cancelled: '已取消' }[status] ?? status }
function isFinished(status) { return ['success', 'failed', 'cancelled'].includes(status) }
function formatDateTime(value) { return value ? String(value).replace('T', ' ').slice(0, 19) : '--' }
function formatNumber(value) { return value == null ? '--' : Number(value).toLocaleString('zh-CN') }
function prettyParams(value) { try { return JSON.stringify(JSON.parse(value || '{}'), null, 2) } catch { return value || '{}' } }
</script>
