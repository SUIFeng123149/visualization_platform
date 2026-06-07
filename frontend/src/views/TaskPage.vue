<template>
  <section class="task-board">
    <article
      v-for="item in taskCards"
      :key="item.id"
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
          @update:model-value="updateTaskStatus(item.id, $event)"
        />
        <el-button v-if="item.bvid" link type="primary" @click="openVideo(item.bvid)">查看复盘</el-button>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { recommendations } from '@/data/dashboard'
import { useDashboardData } from '@/composables/useDashboardData'

const router = useRouter()
const { globalInsights } = useDashboardData()
const taskStatus = reactive({})
const statusOptions = [
  { label: '待处理', value: 'todo' },
  { label: '处理中', value: 'doing' },
  { label: '已完成', value: 'done' },
  { label: '已忽略', value: 'ignored' },
]

const taskCards = computed(() => {
  const source = globalInsights.value.length ? globalInsights.value : recommendations
  return source.map((item, index) => {
    const id = `${item.title}-${item.bvid ?? index}`
    return {
      ...item,
      id,
      status: taskStatus[id] ?? 'todo',
    }
  })
})

function updateTaskStatus(id, status) {
  taskStatus[id] = status
}

function openVideo(bvid) {
  router.push({ name: 'videoDetail', params: { bvid } })
}
</script>
