<template>
  <article class="panel">
    <div class="panel-header">
      <div>
        <div class="panel-title">运营动作建议</div>
        <div class="panel-desc">把图表结论转成可执行动作，适合后台日常复盘。</div>
      </div>
    </div>
    <div class="recommend-list">
      <div v-for="item in items" :key="item.title" class="recommend-item">
        <div class="recommend-head">
          <strong>{{ item.title }}</strong>
          <el-tag :type="item.type" effect="light">{{ item.level }}</el-tag>
        </div>
        <p>{{ item.text }}</p>
      </div>
    </div>
  </article>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    required: true,
  },
})
</script>
