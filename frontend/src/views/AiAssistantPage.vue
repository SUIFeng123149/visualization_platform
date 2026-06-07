<template>
  <section class="ai-layout">
    <article class="panel ai-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">Dify AI 数据助手</div>
          <div class="panel-desc">面向整个平台的问数入口，可解释指标、生成复盘、诊断异常和整理报表摘要。API Key 由后端代理保存，不暴露给浏览器。</div>
        </div>
        <el-tag v-if="lastConfigured === false" type="warning">Dify未配置</el-tag>
      </div>

      <div class="quick-prompts">
        <button v-for="prompt in quickPrompts" :key="prompt" type="button" :disabled="loading" @click="sendPrompt(prompt)">
          {{ prompt }}
        </button>
      </div>

      <div class="chat-window">
        <div v-for="message in messages" :key="message.id" class="chat-message" :class="`chat-${message.role}`">
          <strong>{{ message.role === 'user' ? '我' : 'AI助手' }}</strong>
          <p>{{ message.content }}</p>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="query"
          type="textarea"
          :rows="3"
          placeholder="例如：6月2日为什么只剩一个视频？哪个视频最需要优先复盘？"
          @keydown.ctrl.enter.prevent="sendMessage"
        />
        <el-button type="primary" :loading="loading" @click="sendMessage">发送</el-button>
      </div>
    </article>

    <article class="panel ai-side">
      <div class="panel-header">
        <div>
          <div class="panel-title">当前可问的数据</div>
          <div class="panel-desc">AI助手会携带当前平台数据快照，回答时优先基于这些上下文。</div>
        </div>
      </div>
      <div class="config-list">
        <code>视频样本</code>
        <span>{{ platformContext.videoCount }} 个，当前筛选 {{ platformContext.visibleVideoCount }} 个。</span>
        <code>情感样本</code>
        <span>{{ platformContext.sentimentCount }} 个视频有情感统计，缺失 {{ platformContext.missingSentimentCount }} 个。</span>
        <code>关键词</code>
        <span>{{ platformContext.keywords.length ? platformContext.keywords.map((item) => item.word).join('、') : '暂无关键词' }}</span>
        <code>Dify配置</code>
        <span>后端读取 <strong>DIFY_BASE_URL</strong> 和 <strong>DIFY_API_KEY</strong>，未配置时返回本地上下文摘要。</span>
      </div>
      <el-alert
        title="建议把 Dify 应用提示词设置为“数据分析助手”，并要求它只基于传入上下文分析，不要编造数据库中不存在的数据。"
        type="info"
        :closable="false"
        show-icon
      />
    </article>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { chatWithAssistant } from '@/api/ai'
import { getInteractions, useDashboardData } from '@/composables/useDashboardData'

const {
  filters,
  heatRank,
  visibleHeatRank,
  videoSentiments,
  keywords,
  danmakuHotspots,
  dataQuality,
  globalInsights,
} = useDashboardData()

const loading = ref(false)
const query = ref('')
const conversationId = ref('')
const lastConfigured = ref(null)
const messages = ref([
  {
    id: 1,
    role: 'assistant',
    content: '我是 BiliLens 的 AI 数据助手。你可以问我数据口径、异常原因、视频复盘建议、报表摘要或平台建设建议。',
  },
])

const platformContext = computed(() => ({
  page: 'AI助手',
  filters: { ...filters },
  videoCount: heatRank.value.length,
  visibleVideoCount: visibleHeatRank.value.length,
  sentimentCount: videoSentiments.value.length,
  missingSentimentCount: dataQuality.value.missingSentimentCount,
  dataQuality: dataQuality.value,
  topVideos: visibleHeatRank.value.slice(0, 10).map((item) => ({
    bvid: item.bvid,
    title: item.title,
    upName: item.upName,
    category: item.category,
    viewCount: item.viewCount,
    heatScore: Math.round(item.heatScore || 0),
    interactionCount: getInteractions(item),
  })),
  sentiments: videoSentiments.value.slice(0, 10),
  keywords: keywords.value.slice(0, 12),
  danmakuHotspots: danmakuHotspots.value.slice(0, 8),
  insights: globalInsights.value,
}))

const quickPrompts = [
  '解读当前平台的整体数据表现',
  '为什么有些视频没有正向占比数据？',
  '帮我生成一份视频复盘报告大纲',
  '重复导入数据应该怎么治理？',
  '当前最值得优先处理的异常是什么？',
]

async function sendPrompt(prompt) {
  query.value = prompt
  await sendMessage()
}

async function sendMessage() {
  const content = query.value.trim()
  if (!content || loading.value) return

  messages.value.push({ id: Date.now(), role: 'user', content })
  query.value = ''
  loading.value = true
  try {
    const response = await chatWithAssistant({
      query: content,
      mode: 'platform-qa',
      context: platformContext.value,
      conversationId: conversationId.value,
      user: 'bililens-dashboard',
    })
    conversationId.value = response.conversationId || conversationId.value
    lastConfigured.value = response.configured
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: response.answer,
    })
  } catch (error) {
    ElMessage.error(error.message || 'AI助手请求失败')
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: 'AI助手请求失败，请检查后端 Dify 配置或网络连接。',
    })
  } finally {
    loading.value = false
  }
}
</script>
