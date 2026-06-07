<template>
  <article class="panel">
    <div class="panel-header">
      <div>
        <div class="panel-title">运营动作建议</div>
        <div class="panel-desc">把图表结论转成可执行动作；带关联视频的建议可点击进入复盘。</div>
      </div>
    </div>
    <div class="recommend-list">
      <div
        v-for="item in items"
        :key="item.title"
        class="recommend-item"
        :class="{ clickable: item.bvid }"
        @click="openInsight(item)"
      >
        <div class="recommend-head">
          <strong>{{ item.title }}</strong>
          <el-tag :type="item.type" effect="light">{{ item.level }}</el-tag>
        </div>
        <p>{{ item.text }}</p>
        <small v-if="item.bvid">点击查看视频复盘</small>
      </div>
    </div>
  </article>
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
