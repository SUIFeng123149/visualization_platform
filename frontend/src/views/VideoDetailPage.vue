<template>
  <section v-loading="loading" class="detail-page">
    <div class="detail-hero">
      <div>
        <el-button link type="primary" @click="router.back()">返回</el-button>
        <h2>{{ detail?.video?.title || '视频复盘' }}</h2>
        <p>{{ detail?.video?.bvid }} · {{ detail?.video?.upName || '--' }} · {{ detail?.video?.category || '未分类' }}</p>
      </div>
      <el-tag v-if="detail?.video" size="large" type="success">Rank {{ detail.video.rankNo }}</el-tag>
    </div>

    <MetricGrid v-if="detail" :metrics="detailMetrics" />

    <section v-if="detail" class="review-score-card">
      <div class="score-summary">
        <span>复盘总分</span>
        <strong>{{ reviewScore.total }}</strong>
        <small>{{ reviewScore.verdict }}</small>
      </div>
      <div class="score-breakdown">
        <div v-for="item in reviewScore.items" :key="item.label" class="score-row">
          <div class="score-row-head">
            <span>{{ item.label }}</span>
            <strong>{{ item.score }}</strong>
          </div>
          <el-progress :percentage="item.score" :show-text="false" :stroke-width="8" />
          <p>{{ item.reason }}</p>
        </div>
      </div>
    </section>

    <section v-if="detail" class="detail-grid">
      <ChartPanel title="视频情感结构" description="判断该视频是否在高热度的同时具备稳定口碑。">
        <SentimentPieChart :data="detail.sentiment ? [detail.sentiment] : []" />
      </ChartPanel>

      <ChartPanel title="弹幕高能切片" description="自动定位观众反应最集中的片段，辅助制作二创切片。">
        <DanmakuScatterChart :data="detail.danmakuTimeline" />
      </ChartPanel>
    </section>

    <section v-if="detail" class="detail-grid">
      <ChartPanel title="异常与运营建议" description="基于热度、互动、情感、弹幕和评论的规则化诊断。">
        <div class="insight-list">
          <article v-for="item in detail.insights" :key="item.title" class="detail-insight">
            <div class="recommend-head">
              <strong>{{ item.title }}</strong>
              <el-tag :type="item.type">{{ item.level }}</el-tag>
            </div>
            <p>{{ item.reason }}</p>
            <span>{{ item.action }}</span>
          </article>
        </div>
      </ChartPanel>

      <ChartPanel title="主题关键词" description="用于判断观众关注点、疑问点或争议点。">
        <div v-if="detail.keywords.length" class="keyword-board">
          <div class="keyword-hero">
            <span class="keyword-hero-label">主讨论点</span>
            <strong>#{{ keywordStats.primary.word }}</strong>
            <small>出现 {{ keywordStats.primary.wordCount }} 次，占关键词权重 {{ keywordStats.primaryShare }}%</small>
          </div>

          <div class="keyword-bars">
            <div v-for="item in keywordStats.top" :key="item.word" class="keyword-row">
              <div class="keyword-row-head">
                <span>#{{ item.word }}</span>
                <strong>{{ item.wordCount }}</strong>
              </div>
              <div class="keyword-track">
                <i :style="{ width: `${item.percent}%` }"></i>
              </div>
            </div>
          </div>

          <div class="keyword-cloud">
            <span
              v-for="item in keywordStats.rest"
              :key="item.word"
              class="keyword-chip"
              :style="{ '--chip-scale': item.scale }"
            >
              #{{ item.word }}
            </span>
          </div>
        </div>
        <div v-else class="empty-state small">暂无关键词数据</div>
      </ChartPanel>
    </section>

    <section v-if="detail" class="panel video-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">相似视频对比</div>
          <div class="panel-desc">按分区、UP主、热度接近度和口碑样本匹配，用于判断当前视频强弱项。</div>
        </div>
      </div>
      <el-table :data="similarVideos" empty-text="暂无可对比视频" style="width: 100%">
        <el-table-column prop="title" label="视频" min-width="280" />
        <el-table-column prop="upName" label="UP主" width="140" />
        <el-table-column prop="category" label="类型" width="100" />
        <el-table-column label="播放量" width="120">
          <template #default="{ row }">{{ formatCompact(row.viewCount) }}</template>
        </el-table-column>
        <el-table-column label="互动率" width="110">
          <template #default="{ row }">{{ (row.interactionRate * 100).toFixed(1) }}%</template>
        </el-table-column>
        <el-table-column label="正向占比" width="110">
          <template #default="{ row }">{{ row.positiveRatioText }}</template>
        </el-table-column>
        <el-table-column label="对比结论" min-width="180">
          <template #default="{ row }">
            <el-tag :type="row.compareType">{{ row.compareText }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="detail" class="panel video-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">负面评论样本</div>
          <div class="panel-desc">优先处理高赞负面评论，降低舆情扩散风险。</div>
        </div>
      </div>
      <el-table :data="detail.negativeComments" empty-text="暂无负面评论" style="width: 100%">
        <el-table-column prop="userName" label="用户" width="150" />
        <el-table-column prop="cleanContent" label="评论内容" min-width="300" />
        <el-table-column prop="likeCount" label="点赞" width="100" sortable />
        <el-table-column prop="sentimentScore" label="情感分" width="110" />
        <el-table-column prop="crawledAt" label="采集时间" width="190" />
      </el-table>
    </section>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ChartPanel from '@/components/charts/ChartPanel.vue'
import DanmakuScatterChart from '@/components/charts/DanmakuScatterChart.vue'
import SentimentPieChart from '@/components/charts/SentimentPieChart.vue'
import MetricGrid from '@/components/metrics/MetricGrid.vue'
import { fetchVideoDetail } from '@/api/analysis'
import { formatCompact, getInteractions, getVideoCategory, useDashboardData } from '@/composables/useDashboardData'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)
const { heatRank, videoSentiments } = useDashboardData()

const detailMetrics = computed(() => {
  if (!detail.value?.video) return []
  const video = detail.value.video
  const sentiment = detail.value.sentiment
  const interactionRate = video.viewCount > 0 ? getInteractions(video) / video.viewCount : 0
  const hotspot = [...detail.value.danmakuTimeline].sort((a, b) => b.danmakuCount - a.danmakuCount)[0]

  return [
    metric('播放量', formatCompact(video.viewCount), `热度 ${Math.round(video.heatScore).toLocaleString('zh-CN')}`, '当前视频', 'up', '播放量来自热度排行表，热度分由后端综合指标计算。'),
    metric('互动率', `${(interactionRate * 100).toFixed(1)}%`, formatCompact(getInteractions(video)), '总互动', 'up', '互动率 = 点赞/投币/收藏/评论/弹幕合计 ÷ 播放量。'),
    metric('正向占比', sentiment ? `${(sentiment.positiveRatio * 100).toFixed(1)}%` : '--', sentiment ? `${sentiment.totalCount} 条样本` : '无样本', '评论情感', 'up', '正向占比来自评论情感样本；无样本时不计算。'),
    metric('弹幕峰值', hotspot ? formatCompact(hotspot.danmakuCount) : '--', hotspot ? formatVideoTime(hotspot.timeBucket) : '无峰值', '高能片段', 'up', '弹幕峰值用于定位观众集中反应片段。'),
  ]
})

const reviewScore = computed(() => {
  if (!detail.value?.video) return { total: 0, verdict: '--', items: [] }
  const video = detail.value.video
  const sentiment = detail.value.sentiment
  const hotspot = [...detail.value.danmakuTimeline].sort((a, b) => b.danmakuCount - a.danmakuCount)[0]
  const interactionRate = video.viewCount > 0 ? getInteractions(video) / video.viewCount : 0
  const heatScore = scoreByRank(video.rankNo)
  const interactionScore = clamp(Math.round(interactionRate * 900), 20, 100)
  const sentimentScore = sentiment ? clamp(Math.round(sentiment.positiveRatio * 100), 10, 100) : 45
  const danmakuScore = hotspot ? clamp(Math.round((hotspot.danmakuCount / Math.max(1, video.danmakuCount)) * 260), 20, 100) : 35
  const riskScore = clamp(100 - Math.round((sentiment?.negativeRatio ?? 0) * 140) - detail.value.negativeComments.length * 8, 10, 100)
  const total = Math.round(heatScore * 0.25 + interactionScore * 0.25 + sentimentScore * 0.2 + danmakuScore * 0.15 + riskScore * 0.15)

  return {
    total,
    verdict: total >= 80 ? '可沉淀为爆款样本' : total >= 65 ? '具备复盘价值' : '需要定位短板',
    items: [
      { label: '热度表现', score: heatScore, reason: `当前热度排名 ${video.rankNo}，用于衡量传播规模。` },
      { label: '互动效率', score: interactionScore, reason: `互动率 ${(interactionRate * 100).toFixed(1)}%，反映观众参与强度。` },
      { label: '口碑质量', score: sentimentScore, reason: sentiment ? `正向占比 ${(sentiment.positiveRatio * 100).toFixed(1)}%。` : '缺少情感样本，暂按保守值计算。' },
      { label: '爆点集中度', score: danmakuScore, reason: hotspot ? `弹幕峰值出现在 ${formatVideoTime(hotspot.timeBucket)}。` : '暂无弹幕峰值样本。' },
      { label: '风险控制', score: riskScore, reason: `负面样本 ${detail.value.negativeComments.length} 条，风险越低得分越高。` },
    ],
  }
})

const similarVideos = computed(() => {
  if (!detail.value?.video) return []
  const current = detail.value.video
  const currentCategory = getVideoCategory(current)
  const currentInteractionRate = current.viewCount > 0 ? getInteractions(current) / current.viewCount : 0
  const sentimentMap = new Map(videoSentiments.value.map((item) => [item.bvid, item]))

  return heatRank.value
    .filter((item) => item.bvid !== current.bvid)
    .map((item) => {
      const sentiment = sentimentMap.get(item.bvid)
      const interactionRate = item.viewCount > 0 ? getInteractions(item) / item.viewCount : 0
      const sameCategory = getVideoCategory(item) === currentCategory ? 35 : 0
      const sameUp = item.upName && item.upName === current.upName ? 30 : 0
      const heatDistance = Math.abs(item.heatScore - current.heatScore) / Math.max(1, current.heatScore)
      const heatClose = Math.max(0, 25 - heatDistance * 25)
      const score = sameCategory + sameUp + heatClose
      const compareText = interactionRate >= currentInteractionRate ? '互动更强' : '当前更强'
      return {
        ...item,
        category: getVideoCategory(item),
        interactionRate,
        positiveRatioText: sentiment ? `${(sentiment.positiveRatio * 100).toFixed(1)}%` : '暂无样本',
        compareText,
        compareType: compareText === '互动更强' ? 'warning' : 'success',
        similarityScore: score,
      }
    })
    .sort((a, b) => b.similarityScore - a.similarityScore)
    .slice(0, 5)
})

const keywordStats = computed(() => {
  const keywords = detail.value?.keywords ?? []
  const maxCount = Math.max(1, ...keywords.map((item) => item.wordCount))
  const total = keywords.reduce((sum, item) => sum + item.wordCount, 0)
  const enriched = keywords.map((item) => ({
    ...item,
    percent: Math.max(8, Math.round((item.wordCount / maxCount) * 100)),
    scale: (0.92 + (item.wordCount / maxCount) * 0.28).toFixed(2),
  }))

  return {
    primary: enriched[0] ?? { word: '--', wordCount: 0 },
    primaryShare: total > 0 ? ((enriched[0]?.wordCount ?? 0) / total * 100).toFixed(1) : '0.0',
    top: enriched.slice(0, 5),
    rest: enriched.slice(5),
  }
})

watch(
  () => route.params.bvid,
  async (bvid) => {
    if (!bvid) return
    loading.value = true
    try {
      detail.value = await fetchVideoDetail(bvid)
    } catch (error) {
      ElMessage.error(error.message || '视频复盘加载失败')
      detail.value = null
    } finally {
      loading.value = false
    }
  },
  { immediate: true },
)

function metric(label, value, delta, note, status, description) {
  return { label, value, delta, note, status, description }
}

function scoreByRank(rankNo) {
  if (rankNo <= 3) return 100
  if (rankNo <= 10) return 86
  if (rankNo <= 30) return 72
  if (rankNo <= 60) return 58
  return 45
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value))
}

function formatVideoTime(value) {
  const minutes = Math.floor(value / 60)
  const seconds = String(value % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
}
</script>
