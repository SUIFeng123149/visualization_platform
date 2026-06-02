<template>
  <div class="insights">
    <div v-for="item in items" :key="item.title" class="insight">
      <strong>{{ item.title }}</strong>
      <span>{{ item.text }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    required: true,
  },
})
</script>
