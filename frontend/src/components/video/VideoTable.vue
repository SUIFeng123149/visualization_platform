<template>
  <section class="panel video-panel">
    <div class="panel-header">
      <div>
        <div class="panel-title">{{ title }}</div>
        <div class="panel-desc">{{ description }}</div>
      </div>
      <el-input
        :model-value="keyword"
        placeholder="搜索视频或UP主"
        class="video-search"
        clearable
        @update:model-value="$emit('update:keyword', $event)"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <el-table
      :data="videos"
      style="width: 100%"
      :header-cell-style="{ background: '#f8fafc', color: '#475569' }"
    >
      <el-table-column label="视频" min-width="300">
        <template #default="{ row, $index }">
          <div class="video-cell">
            <div class="rank">{{ $index + 1 }}</div>
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
      <el-table-column label="操作" width="150">
        <template #default>
          <el-button link type="primary">查看详情</el-button>
          <el-button link>复盘</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'

defineProps({
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
})

defineEmits(['update:keyword'])
</script>
