<template>
  <article class="data-quality-panel">
    <div class="quality-head">
      <div>
        <strong>数据质量提示</strong>
        <span>解释筛选、分类和正向占比缺失的原因</span>
      </div>
      <el-tag :type="quality.warnings.length ? 'warning' : 'success'" effect="light">
        {{ quality.warnings.length ? '需关注' : '正常' }}
      </el-tag>
    </div>

    <div class="quality-primary">
      <span>当前命中视频</span>
      <strong>{{ quality.visibleVideoCount }}</strong>
      <small>共 {{ quality.videoCount }} 个热度样本</small>
    </div>

    <div class="quality-body">
      <div class="quality-stats">
        <div class="quality-stat-row">
          <span>规则推断分区</span>
          <b>{{ quality.fallbackCategoryCount }}</b>
        </div>
        <div class="quality-stat-row">
          <span>缺少情感样本</span>
          <b>{{ quality.missingSentimentCount }}</b>
        </div>
        <div class="quality-stat-row">
          <span>评论样本数</span>
          <b>{{ quality.commentSampleCount }}</b>
        </div>
      </div>

      <ul v-if="quality.warnings.length" class="quality-warnings">
        <li v-for="warning in quality.warnings" :key="warning">{{ warning }}</li>
      </ul>
      <p v-else class="quality-ok">当前筛选范围内数据链路完整，图表可按现有口径解读。</p>
    </div>

    <small v-if="quality.lastRefreshedAt" class="quality-refresh-time">最近刷新：{{ quality.lastRefreshedAt }}</small>
  </article>
</template>

<script setup>
defineProps({
  quality: {
    type: Object,
    required: true,
  },
})
</script>
