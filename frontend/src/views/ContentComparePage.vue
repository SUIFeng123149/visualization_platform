<template>
  <section class="panel content-panel" v-loading="loading">
    <div class="panel-header"><div><div class="panel-title">内容横向对比</div><div class="panel-desc">比较所选内容的最新快照指标；归一化热度可用于跨平台参考。</div></div><el-button plain @click="backToContents">返回内容分析</el-button></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-table :data="rows" empty-text="请选择 2 至 5 条内容进行对比" style="width:100%">
      <el-table-column prop="title" label="内容" min-width="260" show-overflow-tooltip /><el-table-column prop="platformCode" label="平台" width="100" /><el-table-column prop="contentType" label="类型" width="100" />
      <el-table-column label="播放量" width="130" align="right"><template #default="{row}">{{ count(row.viewCount) }}</template></el-table-column><el-table-column label="点赞量" width="120" align="right"><template #default="{row}">{{ count(row.likeCount) }}</template></el-table-column><el-table-column label="评论量" width="120" align="right"><template #default="{row}">{{ count(row.commentCount) }}</template></el-table-column><el-table-column label="归一化热度" width="140" align="right"><template #default="{row}">{{ percentile(row.normalizedHeatScore) }}</template></el-table-column><el-table-column label="指标时间" width="175"><template #default="{row}">{{ dateTime(row.metricsCapturedAt) }}</template></el-table-column>
    </el-table>
  </section>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchContent } from '@/api/content'
const route = useRoute(); const router = useRouter(); const rows = ref([]); const loading = ref(false); const error = ref('')
onMounted(async () => { const ids = [...new Set(String(route.query.ids || '').split(',').filter((id) => /^\d+$/.test(id)))].slice(0, 5); if (ids.length < 2) return; loading.value = true; try { rows.value = await Promise.all(ids.map(fetchContent)) } catch (e) { error.value = e.message || '内容对比加载失败' } finally { loading.value = false } })
function backToContents() { const { ids, ...query } = route.query; router.push({ name: 'contents', query }) }
function count(value) { return value == null ? '--' : Number(value).toLocaleString('zh-CN') }
function percentile(value) { return value == null ? '--' : `P${Math.round(Number(value) * 100)}` }
function dateTime(value) { return value ? String(value).replace('T', ' ').slice(0, 19) : '--' }
</script>
