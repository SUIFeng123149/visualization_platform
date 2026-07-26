<template>
  <section class="panel content-panel" v-loading="loadingAccounts">
    <div class="panel-header">
      <div>
        <div class="panel-title">发布账号表现</div>
        <div class="panel-desc">统一比较创作者、频道和发行方；平台缺失的播放或点赞指标显示为不适用。</div>
      </div>
      <el-button :icon="Refresh" :loading="loadingAccounts" @click="load">刷新</el-button>
    </div>
    <el-table :data="accounts" empty-text="暂无账号表现数据" style="width: 100%">
      <el-table-column prop="displayName" label="账号" min-width="180" />
      <el-table-column label="平台" width="120"><template #default="{ row }">{{ platformName(row.platformCode) }}</template></el-table-column>
      <el-table-column label="账号类型" width="120"><template #default="{ row }">{{ accountTypeName(row.accountType) }}</template></el-table-column>
      <el-table-column prop="contentCount" label="内容数" width="100" sortable />
      <el-table-column label="累计播放" width="140" sortable><template #default="{ row }">{{ count(row.totalViewCount) }}</template></el-table-column>
      <el-table-column label="累计点赞" width="140" sortable><template #default="{ row }">{{ count(row.totalLikeCount) }}</template></el-table-column>
      <el-table-column label="平均归一化热度" width="170" sortable><template #default="{ row }">{{ percentile(row.averageNormalizedHeat) }}</template></el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { usePlatformContext } from '@/composables/usePlatformContext'
import { useUnifiedAnalytics } from '@/composables/useUnifiedAnalytics'

const { platforms, selectedPlatform, loadPlatforms } = usePlatformContext()
const { accounts, loadingAccounts, loadAccountPerformance } = useUnifiedAnalytics()

watch(selectedPlatform, load, { immediate: true })

async function load() {
  try {
    await loadPlatforms()
    await loadAccountPerformance()
  } catch (error) {
    ElMessage.error(error.message || '账号表现加载失败')
  }
}

function platformName(code) { return platforms.value.find((item) => item.platformCode === code)?.displayName ?? code }
function accountTypeName(type) { return { creator: '创作者', publisher: '发行方', channel: '频道' }[type] ?? type }
function count(value) { return value === null || value === undefined ? '不适用' : Number(value).toLocaleString('zh-CN') }
function percentile(value) { return value === null || value === undefined ? '--' : `P${Math.round(Number(value) * 100)}` }
</script>
