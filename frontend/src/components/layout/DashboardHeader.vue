<template>
  <section class="topbar">
    <div class="title">
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
    </div>

    <div class="filters">
      <el-select
        v-if="showChannelFilter"
        :model-value="filters.channel"
        aria-label="内容类型筛选"
        @update:model-value="updateFilter('channel', $event)"
      >
        <el-option label="全部类型" value="all" />
        <el-option label="知识" value="知识" />
        <el-option label="科技" value="科技" />
        <el-option label="游戏" value="游戏" />
        <el-option label="动画" value="动画" />
        <el-option label="音乐" value="音乐" />
        <el-option label="生活" value="生活" />
        <el-option label="娱乐" value="娱乐" />
        <el-option label="综合" value="综合" />
      </el-select>
      <el-select
        v-if="showPeriodFilter"
        :model-value="filters.period"
        aria-label="趋势周期筛选"
        @update:model-value="updateFilter('period', $event)"
      >
        <el-option label="趋势近 7 天" value="7d" />
        <el-option label="趋势近 30 天" value="30d" />
        <el-option label="趋势近 90 天" value="90d" />
      </el-select>
      <el-button type="primary" :icon="Refresh" @click="$emit('refresh')">刷新数据</el-button>
    </div>
  </section>
</template>

<script setup>
import { Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  filters: {
    type: Object,
    required: true,
  },
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    required: true,
  },
  showChannelFilter: {
    type: Boolean,
    default: true,
  },
  showPeriodFilter: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:filters', 'refresh'])

function updateFilter(key, value) {
  emit('update:filters', {
    ...props.filters,
    [key]: value,
  })
}
</script>
