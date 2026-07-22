<template>
  <el-aside width="236px" class="side">
    <div class="brand">
      <div class="brand-mark">BL</div>
      <div>
        <div class="brand-name">视频数据平台</div>
        <div class="brand-sub">内容增长分析台</div>
      </div>
    </div>

    <nav class="nav" aria-label="主导航">
      <button
        v-for="item in items"
        :key="item.key"
        class="nav-item"
        :class="{ active: item.key === activeKey }"
        type="button"
        @click="$emit('change', item.key)"
      >
        <el-icon>
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.label }}</span>
      </button>
    </nav>
  </el-aside>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    required: true,
  },
  activeKey: {
    type: String,
    required: true,
  },
})

defineEmits(['change'])
</script>
