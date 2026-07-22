<template>
  <section class="stack">
    <AiInsightPanel
      title="AI异常诊断助手"
      description="结合当前自动生成任务，解释异常来源、优先级和后续处理动作。"
      mode="anomaly-diagnosis"
      :context="taskAiContext"
      :prompts="taskPrompts"
      placeholder="例如：这些任务应该先处理哪一个？"
    />

    <section class="task-board">
      <article
        v-for="item in taskCards"
        :key="item.taskId"
        class="analysis-card task-card"
        :class="`task-card-${item.status}`"
      >
        <div class="analysis-card-head">
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.text }}</p>
          </div>
          <el-tag :type="item.type">{{ item.level }}</el-tag>
        </div>

        <div class="task-meta">
          <span>来源：{{ sourceLabel(item.source) }}</span>
          <span v-if="item.contentId">内容：{{ item.platformCode || '--' }} · {{ item.externalContentId || item.contentId }}</span>
          <span v-if="item.updatedAt">任务更新：{{ formatDateTime(item.updatedAt) }}</span>
          <span v-if="item.statusUpdatedAt">状态更新：{{ formatDateTime(item.statusUpdatedAt) }}</span>
        </div>

        <div class="task-card-footer">
          <el-segmented
            :model-value="item.status"
            :options="statusOptions"
            size="small"
            @update:model-value="updateStatus(item.taskId, $event)"
          />
          <el-button v-if="item.contentId" link type="primary" @click="openContent(item.contentId)">查看复盘</el-button>
          <el-button v-else-if="item.bvid" link type="primary" @click="openLegacyVideo(item.bvid)">查看旧版复盘</el-button>
        </div>
      </article>

      <section v-if="!loading && taskCards.length === 0" class="empty-state">
        暂无运营任务。点击右上角“刷新数据”后，系统会基于内容热度、情感和互动高峰自动生成任务。
      </section>
    </section>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AiInsightPanel from '@/components/ai/AiInsightPanel.vue'
import { fetchTasks, refreshTasks, updateTaskStatus } from '@/api/tasks'

const router = useRouter()
const loading = ref(false)
const taskCards = ref([])
const statusOptions = [
  { label: '待处理', value: 'todo' },
  { label: '处理中', value: 'doing' },
  { label: '已完成', value: 'done' },
  { label: '已忽略', value: 'ignored' },
]

const taskAiContext = computed(() => ({
  page: '任务中心',
  totalTasks: taskCards.value.length,
  statusSummary: countBy(taskCards.value, 'status'),
  levelSummary: countBy(taskCards.value, 'level'),
  typeSummary: countBy(taskCards.value, 'type'),
  tasks: taskCards.value.slice(0, 12),
}))

const taskPrompts = [
  '诊断当前任务中心的主要异常',
  '给这些任务排一个处理优先级',
  '哪些任务需要进入内容复盘',
  '把任务整理成今日运营动作清单',
]

onMounted(() => {
  loadTasks()
  window.addEventListener('bililens:refresh-tasks', loadTasks)
})

onBeforeUnmount(() => {
  window.removeEventListener('bililens:refresh-tasks', loadTasks)
})

async function loadTasks() {
  loading.value = true
  try {
    taskCards.value = await refreshTasks()
  } catch (error) {
    ElMessage.warning(error.message || '任务自动生成失败，正在读取已有任务')
    try {
      taskCards.value = await fetchTasks()
    } catch (fallbackError) {
      ElMessage.error(fallbackError.message || '任务列表加载失败')
    }
  } finally {
    loading.value = false
  }
}

async function updateStatus(taskId, status) {
  const task = taskCards.value.find((item) => item.taskId === taskId)
  if (!task) return

  const previousStatus = task.status
  task.status = status
  try {
    const saved = await updateTaskStatus(taskId, status)
    task.status = saved.status
    task.statusUpdatedAt = saved.updatedAt
    ElMessage.success('任务状态已保存')
  } catch (error) {
    task.status = previousStatus
    ElMessage.error(error.message || '任务状态保存失败')
  }
}

function openContent(contentId) {
  router.push({ name: 'contentDetail', params: { contentId }, query: { from: 'task' } })
}

function openLegacyVideo(bvid) {
  router.push({ name: 'contents', query: { keyword: bvid, legacy: 'bvid', from: 'task' } })
}

function sourceLabel(source) {
  return source === 'auto' ? '系统自动生成' : '人工录入'
}

function formatDateTime(value) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 19)
}

function countBy(rows, key) {
  return rows.reduce((result, row) => {
    const value = row[key] || 'unknown'
    result[value] = (result[value] || 0) + 1
    return result
  }, {})
}
</script>
