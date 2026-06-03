<template>
  <section class="analysis-list">
    <article v-for="item in taskCards" :key="item.title" class="analysis-card">
      <div class="analysis-card-head">
        <strong>{{ item.title }}</strong>
        <el-tag :type="item.type">{{ item.level }}</el-tag>
      </div>
      <p>{{ item.text }}</p>
    </article>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { recommendations } from '@/data/dashboard'

const taskCards = computed(() => recommendations)
</script>
