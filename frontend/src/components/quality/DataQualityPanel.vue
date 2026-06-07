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

    <div class="quality-stats">
      <div>
        <b>{{ quality.visibleVideoCount }}</b>
        <span>当前命中视频</span>
      </div>
      <div>
        <b>{{ quality.fallbackCategoryCount }}</b>
        <span>规则推断分区</span>
      </div>
      <div>
        <b>{{ quality.missingSentimentCount }}</b>
        <span>缺少情感样本</span>
      </div>
      <div>
        <b>{{ quality.commentSampleCount }}</b>
        <span>评论样本数</span>
      </div>
    </div>

    <ul v-if="quality.warnings.length" class="quality-warnings">
      <li v-for="warning in quality.warnings" :key="warning">{{ warning }}</li>
    </ul>
    <p v-else class="quality-ok">当前筛选范围内数据链路完整，图表可按现有口径解读。</p>

    <small v-if="quality.lastRefreshedAt">最近刷新：{{ quality.lastRefreshedAt }}</small>
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
