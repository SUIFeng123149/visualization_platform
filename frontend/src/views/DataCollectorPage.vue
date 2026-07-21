<template>
  <section class="collector-layout">
    <section class="panel collector-form-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">采集任务下发</div>
          <div class="panel-desc">把自然语言需求和结构化采集参数保存为平台任务，等待 Python 采集端拉取执行。</div>
        </div>
      </div>

      <el-form label-position="top" class="collector-form">
        <el-form-item label="任务名称">
          <el-input v-model="form.taskName" maxlength="120" placeholder="例如：B站 AI 工具内容采集" clearable />
        </el-form-item>

        <el-form-item label="自然语言采集需求">
          <el-input
            v-model="form.naturalLanguage"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
            placeholder="例如：采集近 7 天 B 站关于 AI 工具的视频、评论和弹幕，重点关注播放量高且负面评论集中的内容。"
          />
        </el-form-item>

        <div class="collector-inline-grid">
          <el-form-item label="数据源">
            <el-select v-model="form.sourceType">
              <el-option label="Bilibili" value="bilibili" />
              <el-option label="Web 页面" value="web" />
              <el-option label="通用任务" value="general" />
            </el-select>
          </el-form-item>
          <el-form-item label="采集模式">
            <el-select v-model="form.collectMode">
              <el-option label="爬虫 + OCR 混合" value="crawler_ocr_hybrid" />
              <el-option label="传统爬虫" value="crawler" />
              <el-option label="全屏 OCR" value="screen_ocr" />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级">
            <el-select v-model="form.priority">
              <el-option label="普通" value="normal" />
              <el-option label="高" value="high" />
              <el-option label="紧急" value="urgent" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="采集来源">
          <el-checkbox-group v-model="sourceTypes" class="collector-source-grid">
            <el-checkbox-button label="popular">热门页</el-checkbox-button>
            <el-checkbox-button label="homepage">首页推荐</el-checkbox-button>
            <el-checkbox-button label="mid">UP 主投稿</el-checkbox-button>
            <el-checkbox-button label="keyword">关键词搜索</el-checkbox-button>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item v-if="sourceTypes.includes('mid')" label="UP 主 MID">
          <el-input v-model="midText" placeholder="多个 MID 用英文逗号分隔" clearable />
        </el-form-item>

        <el-form-item v-if="sourceTypes.includes('keyword')" label="关键词">
          <el-input v-model="keywordText" placeholder="多个关键词用英文逗号分隔，例如 数据分析,AI" clearable />
        </el-form-item>

        <div class="collector-option-grid">
          <el-form-item label="每来源视频数">
            <el-input-number v-model="form.maxVideos" :min="1" :max="5000" controls-position="right" />
          </el-form-item>
          <el-form-item label="每视频评论数">
            <el-input-number v-model="form.maxCommentsPerVideo" :min="0" :max="1000" controls-position="right" />
          </el-form-item>
          <el-form-item label="并发 worker">
            <el-input-number v-model="form.workers" :min="1" :max="8" controls-position="right" />
          </el-form-item>
          <el-form-item label="评论间隔秒">
            <el-input-number v-model="form.commentDelay" :min="0" :max="120" :step="0.5" controls-position="right" />
          </el-form-item>
          <el-form-item label="弹幕间隔秒">
            <el-input-number v-model="form.danmakuDelay" :min="0" :max="180" :step="0.5" controls-position="right" />
          </el-form-item>
          <el-form-item label="OCR 置信度">
            <el-input-number v-model="form.minOcrConfidence" :min="0" :max="1" :step="0.05" controls-position="right" />
          </el-form-item>
        </div>

        <div class="collector-toggle-row">
          <el-checkbox v-model="form.ocrEnabled">启用 OCR 补充识别</el-checkbox>
          <el-checkbox v-model="form.screenCaptureEnabled">允许全屏截图识别</el-checkbox>
        </div>

        <div class="collector-actions">
          <el-button :icon="Refresh" @click="loadTasks">刷新任务</el-button>
          <el-button type="primary" :icon="Upload" :loading="submitting" @click="submitTask">下发任务</el-button>
        </div>
      </el-form>
    </section>

    <section class="panel collector-help-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">采集端对接接口</div>
          <div class="panel-desc">Python 端只需要拉取任务并回传状态，不直接依赖可视化页面。</div>
        </div>
      </div>
      <div class="collector-help">
        <div>
          <strong>拉取任务</strong>
          <span>POST /api/collector/tasks/pull</span>
        </div>
        <div>
          <strong>回传状态</strong>
          <span>PUT /api/collector/tasks/{taskId}/status</span>
        </div>
        <div>
          <strong>产物登记</strong>
          <span>回传 HDFS 路径、SQL 表名、批次号和入库行数。</span>
        </div>
      </div>
    </section>
  </section>

  <section class="panel video-panel">
    <div class="panel-header">
      <div>
        <div class="panel-title">采集任务流转</div>
        <div class="panel-desc">展示平台侧最近 50 个采集任务，采集端回传后这里会更新 HDFS 和 SQL 产物信息。</div>
      </div>
      <el-tag :type="activeTaskCount ? 'warning' : 'success'">{{ activeTaskCount ? `${activeTaskCount} 个进行中` : '暂无运行任务' }}</el-tag>
    </div>

    <el-table v-loading="loading" :data="tasks" empty-text="暂无采集任务" style="width: 100%" @row-click="selectTask">
      <el-table-column prop="taskName" label="任务" min-width="220">
        <template #default="{ row }">
          <strong>{{ row.taskName }}</strong>
          <div class="table-sub">{{ row.taskId }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="taskStatusType(row.status)">{{ taskStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="progress" label="进度" width="150">
        <template #default="{ row }">
          <el-progress :percentage="row.progress ?? 0" :stroke-width="8" />
        </template>
      </el-table-column>
      <el-table-column prop="collectMode" label="模式" width="150">
        <template #default="{ row }">{{ collectModeLabel(row.collectMode) }}</template>
      </el-table-column>
      <el-table-column prop="rowCount" label="入库行数" width="120">
        <template #default="{ row }">{{ formatNumber(row.rowCount) }}</template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="danger"
            :disabled="isFinished(row.status)"
            @click.stop="cancelTask(row)"
          >
            取消
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>

  <section v-if="selectedTask" class="panel video-panel">
    <div class="panel-header">
      <div>
        <div class="panel-title">任务详情与数据产物</div>
        <div class="panel-desc">{{ selectedTask.message || '等待采集端回传状态' }}</div>
      </div>
      <el-button :icon="Refresh" @click="refreshSelectedTask">刷新详情</el-button>
    </div>

    <div class="collector-result-grid">
      <div>
        <span>原始 HDFS</span>
        <strong>{{ selectedTask.rawHdfsPath || '--' }}</strong>
      </div>
      <div>
        <span>清洗 HDFS</span>
        <strong>{{ selectedTask.cleanHdfsPath || '--' }}</strong>
      </div>
      <div>
        <span>批次号</span>
        <strong>{{ selectedTask.batchId || '--' }}</strong>
      </div>
      <div>
        <span>SQL 表</span>
        <strong>{{ selectedTask.resultTables?.length ? selectedTask.resultTables.join(', ') : '--' }}</strong>
      </div>
    </div>

    <div class="collector-detail-body">
      <div>
        <div class="collector-detail-title">自然语言需求</div>
        <p>{{ selectedTask.naturalLanguage || '未填写，自采集参数生成任务。' }}</p>
      </div>
      <div>
        <div class="collector-detail-title">结构化参数</div>
        <pre>{{ selectedTask.paramsJson || '{}' }}</pre>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Upload } from '@element-plus/icons-vue'
import {
  cancelCollectorTask,
  createCollectorTask,
  fetchCollectorTask,
  fetchCollectorTasks,
} from '@/api/collector'

const sourceTypes = ref(['keyword'])
const midText = ref('')
const keywordText = ref('AI工具,数据分析')
const submitting = ref(false)
const loading = ref(false)
const tasks = ref([])
const selectedTask = ref(null)
let timer = null

const form = reactive({
  taskName: 'B站内容数据采集',
  naturalLanguage: '',
  sourceType: 'bilibili',
  collectMode: 'crawler_ocr_hybrid',
  priority: 'normal',
  maxVideos: 100,
  maxCommentsPerVideo: 50,
  workers: 2,
  commentDelay: 1,
  danmakuDelay: 6,
  ocrEnabled: true,
  screenCaptureEnabled: true,
  ocrLanguage: 'ch',
  minOcrConfidence: 0.7,
})

const activeTaskCount = computed(() => tasks.value.filter((task) => !isFinished(task.status)).length)

onMounted(() => {
  loadTasks()
  timer = window.setInterval(loadTasks, 5000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})

async function submitTask() {
  const payload = buildPayload()
  if (!payload) return

  submitting.value = true
  try {
    const task = await createCollectorTask(payload)
    ElMessage.success('采集任务已下发')
    selectedTask.value = task
    await loadTasks()
  } catch (error) {
    ElMessage.error(error.message || '采集任务下发失败')
  } finally {
    submitting.value = false
  }
}

async function loadTasks() {
  loading.value = true
  try {
    tasks.value = await fetchCollectorTasks()
    if (selectedTask.value) {
      const latest = tasks.value.find((task) => task.taskId === selectedTask.value.taskId)
      if (latest) selectedTask.value = latest
    }
  } catch (error) {
    ElMessage.error(error.message || '采集任务加载失败')
  } finally {
    loading.value = false
  }
}

async function refreshSelectedTask() {
  if (!selectedTask.value) return
  try {
    selectedTask.value = await fetchCollectorTask(selectedTask.value.taskId)
  } catch (error) {
    ElMessage.error(error.message || '详情刷新失败')
  }
}

function selectTask(task) {
  selectedTask.value = task
}

async function cancelTask(task) {
  try {
    await ElMessageBox.confirm(`确认取消任务「${task.taskName}」？`, '取消采集任务', {
      type: 'warning',
      confirmButtonText: '取消任务',
      cancelButtonText: '返回',
    })
    selectedTask.value = await cancelCollectorTask(task.taskId)
    await loadTasks()
    ElMessage.success('任务已取消')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '任务取消失败')
    }
  }
}

function buildPayload() {
  const mids = midText.value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .map(Number)
    .filter((item) => Number.isFinite(item))
  const keywords = keywordText.value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)

  if (!form.naturalLanguage.trim() && sourceTypes.value.length === 0) {
    ElMessage.warning('请填写自然语言需求或至少选择一个采集来源')
    return null
  }
  if (sourceTypes.value.includes('mid') && mids.length === 0) {
    ElMessage.warning('请输入有效的 UP 主 MID')
    return null
  }
  if (sourceTypes.value.includes('keyword') && keywords.length === 0) {
    ElMessage.warning('请输入关键词')
    return null
  }

  return {
    ...form,
    keywords: sourceTypes.value.includes('keyword') ? keywords : [],
    mids: sourceTypes.value.includes('mid') ? mids : [],
    collectPopular: sourceTypes.value.includes('popular'),
    collectHomepage: sourceTypes.value.includes('homepage'),
  }
}

function taskStatusType(status) {
  return {
    pending: 'info',
    dispatched: 'primary',
    running: 'warning',
    uploaded: 'warning',
    analyzing: 'warning',
    importing: 'warning',
    success: 'success',
    failed: 'danger',
    cancelled: 'info',
  }[status] ?? 'info'
}

function taskStatusLabel(status) {
  return {
    pending: '待领取',
    dispatched: '已下发',
    running: '采集中',
    uploaded: '已入 HDFS',
    analyzing: '分析中',
    importing: '入库中',
    success: '完成',
    failed: '失败',
    cancelled: '已取消',
  }[status] ?? status
}

function collectModeLabel(value) {
  return {
    crawler_ocr_hybrid: '爬虫 + OCR',
    crawler: '传统爬虫',
    screen_ocr: '全屏 OCR',
  }[value] ?? value
}

function isFinished(status) {
  return ['success', 'failed', 'cancelled'].includes(status)
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '--'
}

function formatNumber(value) {
  return value == null ? '--' : Number(value).toLocaleString('zh-CN')
}
</script>
