<template>
  <section class="panel video-panel">
    <div class="panel-header">
      <div>
        <div class="panel-title">{{ title }}</div>
        <div class="panel-desc">{{ description }}</div>
      </div>
      <el-input
        :model-value="keyword"
        placeholder="搜索视频或 UP 主"
        class="video-search"
        clearable
        @update:model-value="$emit('update:keyword', $event)"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <el-table :data="pagedVideos" empty-text="暂无视频数据" style="width: 100%">
      <el-table-column label="视频" min-width="300">
        <template #default="{ row, $index }">
          <div class="video-cell">
            <div class="rank">{{ pageStart + $index + 1 }}</div>
            <div>
              <div class="video-title">{{ row.title }}</div>
              <div class="video-meta">{{ row.up }} · {{ row.category || '未分类' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="views" label="播放量" sortable width="130" />
      <el-table-column prop="interaction" label="互动率" sortable width="120" />
      <el-table-column prop="sentiment" label="正向占比" sortable width="120" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.statusType">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleOpenReview(row)">查看复盘</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="videos.length > 0" class="video-table-footer">
      <span class="video-table-count">共 {{ videos.length }} 条结果，当前 {{ pageStart + 1 }}-{{ pageEnd }}</span>
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="pageSizes"
        :total="videos.length"
        :pager-count="5"
        layout="sizes, prev, pager, next, jumper"
        background
      />
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const props = defineProps({
  videos: {
    type: Array,
    required: true,
  },
  keyword: {
    type: String,
    default: '',
  },
  title: {
    type: String,
    default: '高潜视频榜单',
  },
  description: {
    type: String,
    default: '用于承接图表下钻，补齐后台管理的检索、排序和操作能力。',
  },
  defaultPageSize: {
    type: Number,
    default: 10,
  },
})

const emit = defineEmits(['update:keyword', 'view-detail'])
const router = useRouter()
const pageSizes = [10, 20, 50]
const currentPage = ref(1)
const pageSize = ref(props.defaultPageSize)

const pageStart = computed(() => (currentPage.value - 1) * pageSize.value)
const pageEnd = computed(() => Math.min(pageStart.value + pageSize.value, props.videos.length))
const pagedVideos = computed(() => props.videos.slice(pageStart.value, pageEnd.value))

watch(
  () => [props.videos.length, props.keyword],
  () => {
    currentPage.value = 1
  },
)

watch([pageSize, () => props.videos.length], () => {
  const maxPage = Math.max(1, Math.ceil(props.videos.length / pageSize.value))
  if (currentPage.value > maxPage) currentPage.value = maxPage
})

function handleOpenReview(row) {
  emit('view-detail', row)
  if (row.bvid) {
    router.push({ name: 'videoDetail', params: { bvid: row.bvid } })
    return
  }
  ElMessage.warning('该视频缺少 BVID，暂不能查看复盘')
}
</script>
