<script setup lang="ts">
import { ref } from 'vue'

const tabs = [
  { key: 'following', label: '我的关注' },
  { key: 'followers', label: '粉丝' },
]
const activeTab = ref('following')
</script>

<template>
  <div class="page">
    <h1 class="page-title">👥 关注与粉丝</h1>
    <div class="card">
      <div class="tabs">
        <span
          v-for="t in tabs"
          :key="t.key"
          class="tab"
          :class="{ active: activeTab === t.key }"
          @click="activeTab = t.key"
        >{{ t.label }}</span>
      </div>
      <div class="empty">
        <div class="ph">🤝</div>
        <p>暂无{{ tabs.find(t => t.key === activeTab)?.label }}数据</p>
        <p style="font-size:12px;color:#bbb;margin-top:6px">
          {{ activeTab === 'following' ? '关注其他用户后，在这里可以看到他们发布的动态' : '还没有粉丝哦，快去发布优质商品吸引关注吧~' }}
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 20px; }
.card { background: #fff; border-radius: 10px; overflow: hidden; }
.tabs { display: flex; border-bottom: 1px solid #f0f0f0; padding: 0 16px; }
.tab {
  padding: 14px 24px; font-size: 14px; color: #666; cursor: pointer;
  border-bottom: 2px solid transparent; margin-bottom: -1px;
}
.tab.active { color: #ff6b35; font-weight: 600; border-bottom-color: #ff6b35; }
.empty {
  padding: 70px 20px; text-align: center; color: #999;
}
.ph { font-size: 56px; margin-bottom: 14px; opacity: 0.5; }
</style>
