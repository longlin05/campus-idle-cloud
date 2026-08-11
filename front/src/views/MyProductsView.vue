<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { productApi } from '@/api'
import ProductCard from '@/components/ProductCard.vue'
import type { ProductVO } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const list = ref<ProductVO[]>([])
const loading = ref(false)

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: '/my-products' } })
    return
  }
  loading.value = true
  try {
    const res = await productApi.getMyProducts()
    if (res.code === 200) list.value = (res.data?.records || res.data?.list || []) as ProductVO[]
  } catch (e) { console.error(e) }
  finally { loading.value = false }
})
</script>

<template>
  <div class="page">
    <div class="head">
      <h1 class="page-title">📦 我的发布</h1>
      <router-link to="/publish" class="btn-primary">➕ 发布新商品</router-link>
    </div>
    <div v-if="loading" class="card empty">加载中...</div>
    <div v-else-if="list.length === 0" class="card empty">
      还没有发布过商品，<router-link to="/publish" style="color:#ff6b35">去发布第一个商品吧</router-link>
    </div>
    <div v-else class="product-grid">
      <ProductCard v-for="p in list" :key="p.id" :product="p" />
    </div>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 22px; }
.btn-primary {
  height: 38px; padding: 0 18px; background: #ff6b35; color: #fff;
  border-radius: 8px; font-size: 14px; display: inline-flex; align-items: center;
  font-weight: 600;
}
.card { background: #fff; border-radius: 10px; padding: 50px; }
.empty { text-align: center; color: #999; }
.product-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
}
@media (max-width:1100px) { .product-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width:800px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
