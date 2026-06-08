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

    <div v-if="answer || loading" class="ai-answer">
      <div class="ai-answer-head">
        <div class="ai-answer-title">{{ loading ? 'AI正在生成...' : 'AI分析结果' }}</div>
        <el-button
          v-if="answer"
          class="ai-expand-button"
          size="small"
          link
          type="primary"
          @click="expanded = true"
        >
          放大查看
        </el-button>
      </div>
      <div ref="answerScrollRef" class="ai-answer-scroll">
        <div v-if="answer" class="markdown-body" v-html="answerHtml"></div>
        <p v-else class="ai-streaming-placeholder">正在等待 Dify 返回内容...</p>
      </div>
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

    <el-dialog
      v-model="expanded"
      :title="title"
      width="min(860px, 86vw)"
      class="ai-expanded-dialog"
      append-to-body
      destroy-on-close
    >
      <div class="ai-expanded-body markdown-body" v-html="answerHtml"></div>
    </el-dialog>
  </article>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import { consumeAssistantStream } from '@/api/ai'

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true,
})

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
const answerScrollRef = ref(null)
const expanded = ref(false)

const quickPrompts = computed(() => props.prompts.filter(Boolean).slice(0, 4))
const answerHtml = computed(() => md.render(answer.value))

async function ask(prompt) {
  const content = String(prompt || '').trim()
  if (!content || loading.value) return

  answer.value = ''
  query.value = ''
  expanded.value = false
  loading.value = true
  lastConfigured.value = true

  try {
    await consumeAssistantStream(
      {
        query: content,
        mode: props.mode,
        context: props.context,
        conversationId: conversationId.value,
        user: 'bililens-dashboard',
      },
      {
        onMessage(chunk) {
          answer.value += chunk
          scrollAnswerToBottom()
        },
        onDone(payload) {
          conversationId.value = payload?.conversation_id || conversationId.value
        },
        onError(message) {
          throw new Error(message || 'AI分析请求失败')
        },
      },
    )
  } catch (error) {
    ElMessage.error(error.message || 'AI分析请求失败')
    if (!answer.value) {
      answer.value = `AI分析请求失败：${error.message || '请检查后端 Dify 配置或网络连接。'}`
    }
  } finally {
    loading.value = false
    scrollAnswerToBottom()
  }
}

async function scrollAnswerToBottom() {
  await nextTick()
  if (answerScrollRef.value) {
    answerScrollRef.value.scrollTop = answerScrollRef.value.scrollHeight
  }
}
</script>
