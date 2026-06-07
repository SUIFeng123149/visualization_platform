<template>
  <div class="insights">
    <button
      v-for="item in items"
      :key="item.title"
      class="insight"
      :class="{ clickable: item.bvid }"
      type="button"
      @click="openInsight(item)"
    >
      <strong>{{ item.title }}</strong>
      <span>{{ item.text }}</span>
      <small v-if="item.bvid">查看复盘</small>
    </button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

defineProps({
  items: {
    type: Array,
    required: true,
  },
})

const router = useRouter()

function openInsight(item) {
  if (!item.bvid) return
  router.push({ name: 'videoDetail', params: { bvid: item.bvid } })
}
</script>
