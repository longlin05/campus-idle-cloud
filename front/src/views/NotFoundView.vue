<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'
const router = useRouter()
const route = useRoute()

const isAdmin = computed(() => route.path.startsWith('/admin'))
</script>

<template>
  <div class="not-found">
    <div class="code">404</div>
    <div class="desc">页面走丢了~</div>
    <div class="tip">您访问的页面不存在或已被移除</div>
    <div class="btns">
      <router-link :to="isAdmin ? '/admin/dashboard' : '/'" class="btn-primary">
        {{ isAdmin ? '返回管理首页' : '返回首页' }}
      </router-link>
      <button class="btn-ghost" @click="router.back()">返回上一页</button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 70vh;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 40px 20px;
}
.code {
  font-size: 120px; font-weight: 800;
  background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text; line-height: 1;
  margin-bottom: 10px;
}
.desc { font-size: 24px; color: #333; font-weight: 600; margin-bottom: 8px; }
.tip { font-size: 14px; color: #999; margin-bottom: 32px; }
.btns { display: flex; gap: 14px; }
.btn-primary {
  height: 42px; padding: 0 28px;
  background: linear-gradient(90deg, #ff6b35 0%, #f7931e 100%);
  color: #fff; border-radius: 8px;
  display: inline-flex; align-items: center; font-weight: 600;
}
.btn-ghost {
  height: 42px; padding: 0 28px;
  border: 1px solid #ddd; border-radius: 8px;
  background: #fff; color: #666;
}
</style>
