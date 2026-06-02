<template>
  <section class="metric-grid" aria-label="关键指标">
    <article v-for="metric in metrics" :key="metric.label" class="metric">
      <div class="metric-label">{{ metric.label }}</div>
      <div class="metric-value">{{ metric.value }}</div>
      <div class="metric-foot">
        <span class="delta" :class="{ warn: metric.status === 'warn' }">{{ metric.delta }}</span>
        <span>{{ metric.note }}</span>
      </div>
    </article>
  </section>
</template>

<script setup>
defineProps({
  metrics: {
    type: Array,
    required: true,
  },
})
</script>
