<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import ProductCard from '@/components/ProductCard.vue'
import type { ProductVO } from '@/api'
import { favoriteApi, productApi } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const list = ref<ProductVO[]>([])
const loading = ref(true)

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  try {
    // 1. 获取收藏列表（仅含 productId）
    const favRes = await favoriteApi.getList()
    const favorites = favRes?.data || []
    if (favorites.length === 0) {
      list.value = []
      return
    }
    // 2. 批量查询商品详情
    const productIds = favorites.map(f => f.productId)
    const batchRes = await productApi.getBatch(productIds)
    list.value = batchRes?.data || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="list-page">
    <h1 class="page-title">❤️ 我的收藏</h1>
    <div v-if="loading" class="empty">加载中...</div>
    <div v-else-if="list.length === 0" class="empty">
      还没有收藏任何商品，<router-link to="/products" style="color:#ff6b35">去发现喜欢的商品吧</router-link>
    </div>
    <div v-else class="product-grid">
      <ProductCard v-for="p in list" :key="p.id" :product="p" />
    </div>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 20px; }
.empty {
  background: #fff; border-radius: 10px; padding: 80px 20px;
  text-align: center; color: #999;
}
.product-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
}
@media (max-width:1100px) { .product-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width:800px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
