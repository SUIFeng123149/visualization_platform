<template>
  <article class="panel ai-insight-panel">
    <div class="panel-header">
      <div>
        <div class="panel-title">{{ title }}</div>
        <div class="panel-desc">{{ description }}</div>
      </div>
      <el-tag v-if="lastConfigured === false" type="warning">Dify未配置</el-tag>
    </div>

    <div v-if="quickPrompts.length" class="ai-prompt-grid">
      <button
        v-for="prompt in quickPrompts"
        :key="prompt"
        type="button"
        :disabled="loading"
        @click="ask(prompt)"
      >
        {{ prompt }}
      </button>
    </div>

    <div v-if="answer" class="ai-answer">
      <div class="ai-answer-title">AI分析结果</div>
      <p>{{ answer }}</p>
    </div>

    <div class="ai-inline-input">
      <el-input
        v-model="query"
        :placeholder="placeholder"
        clearable
        @keydown.enter.prevent="ask(query)"
      />
      <el-button type="primary" :loading="loading" @click="ask(query)">提问</el-button>
    </div>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { chatWithAssistant } from '@/api/ai'

const props = defineProps({
  title: {
    type: String,
    default: 'AI智能解读',
  },
  description: {
    type: String,
    default: '结合当前页面数据生成解释、风险判断和下一步建议。',
  },
  mode: {
    type: String,
    default: 'page-insight',
  },
  context: {
    type: Object,
    default: () => ({}),
  },
  prompts: {
    type: Array,
    default: () => [],
  },
  placeholder: {
    type: String,
    default: '输入你想追问的数据问题',
  },
})

const loading = ref(false)
const query = ref('')
const answer = ref('')
const conversationId = ref('')
const lastConfigured = ref(null)

const quickPrompts = computed(() => props.prompts.filter(Boolean).slice(0, 4))

async function ask(prompt) {
  const content = String(prompt || '').trim()
  if (!content || loading.value) return

  loading.value = true
  try {
    const response = await chatWithAssistant({
      query: content,
      mode: props.mode,
      context: props.context,
      conversationId: conversationId.value,
      user: 'bililens-dashboard',
    })
    answer.value = response.answer
    conversationId.value = response.conversationId || conversationId.value
    lastConfigured.value = response.configured
    query.value = ''
  } catch (error) {
    ElMessage.error(error.message || 'AI分析请求失败')
  } finally {
    loading.value = false
  }
}
</script>
