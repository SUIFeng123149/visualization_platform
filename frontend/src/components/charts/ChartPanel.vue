<template>
  <article class="panel chart-panel">
    <div class="panel-header">
      <div>
        <div class="panel-title">{{ title }}</div>
        <div class="panel-desc">{{ description }}</div>
      </div>
      <slot name="actions" />
    </div>
    <slot />
  </article>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    required: true,
  },
})
</script>
