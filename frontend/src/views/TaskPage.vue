<template>
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

      <div class="task-card-footer">
        <el-segmented
          :model-value="item.status"
          :options="statusOptions"
          size="small"
          @update:model-value="updateStatus(item.taskId, $event)"
        />
        <el-button v-if="item.bvid" link type="primary" @click="openVideo(item.bvid)">查看复盘</el-button>
      </div>
    </article>

    <section v-if="!loading && taskCards.length === 0" class="empty-state">
      暂无运营任务，请先向 ops_task 表写入任务。
    </section>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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

function openVideo(bvid) {
  router.push({ name: 'videoDetail', params: { bvid } })
}
</script>
