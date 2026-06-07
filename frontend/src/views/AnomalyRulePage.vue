<template>
  <section class="panel video-panel">
    <div class="panel-header">
      <div>
        <div class="panel-title">异常规则管理</div>
        <div class="panel-desc">管理热度、负向情绪、互动效率、弹幕峰值等规则阈值；规则会用于后续异常检测中心扩展。</div>
      </div>
      <el-button type="primary" @click="loadRules">刷新规则</el-button>
    </div>

    <el-table v-loading="loading" :data="rules" empty-text="暂无异常规则" style="width: 100%">
      <el-table-column prop="name" label="规则" min-width="160">
        <template #default="{ row }">
          <strong>{{ row.name }}</strong>
          <div class="table-sub">{{ row.description }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="metric" label="指标" width="140" />
      <el-table-column prop="operator" label="条件" width="90" />
      <el-table-column label="阈值" width="150">
        <template #default="{ row }">
          <el-input-number v-model="row.threshold" :min="0" :step="thresholdStep(row.metric)" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="级别" width="130">
        <template #default="{ row }">
          <el-select v-model="row.level" size="small">
            <el-option label="风险" value="danger" />
            <el-option label="预警" value="warning" />
            <el-option label="增长" value="success" />
            <el-option label="提示" value="primary" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="90">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" />
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
          <el-button link type="primary" @click="saveRule(row)">保存</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchAnomalyRules, updateAnomalyRule } from '@/api/platform'

const loading = ref(false)
const rules = ref([])

onMounted(loadRules)

async function loadRules() {
  loading.value = true
  try {
    rules.value = await fetchAnomalyRules()
  } catch (error) {
    ElMessage.error(error.message || '异常规则加载失败')
  } finally {
    loading.value = false
  }
}

async function saveRule(rule) {
  try {
    const saved = await updateAnomalyRule({
      ruleKey: rule.ruleKey,
      name: rule.name,
      metric: rule.metric,
      operator: rule.operator,
      threshold: Number(rule.threshold),
      level: rule.level,
      enabled: rule.enabled,
      description: rule.description,
    })
    Object.assign(rule, saved)
    ElMessage.success(saved.description?.includes('未持久化') ? '规则已应用但未持久化：请创建 ops_anomaly_rule 表' : '规则已保存')
  } catch (error) {
    ElMessage.error(error.message || '规则保存失败')
  }
}

function thresholdStep(metric) {
  return metric?.includes('Ratio') || metric === 'interactionRate' ? 0.01 : 100
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '--'
}
</script>
