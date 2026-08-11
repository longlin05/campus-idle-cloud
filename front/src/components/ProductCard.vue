<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import type { ProductVO } from '@/api'

const props = defineProps<{
  product: ProductVO
}>()

// 兼容后端 Product 实体主键 productId 与前端 ProductVO.id
const productId = computed(() => props.product.id || (props.product as any).productId)
const mainImage = computed(() => props.product.images?.[0] || (props.product as any).imageUrl || 'https://via.placeholder.com/300x300?text=No+Image')
const title = computed(() => props.product.title || (props.product as any).name || '未命名商品')
</script>

<template>
  <RouterLink :to="`/product/${productId}`" class="product-card">
    <div class="product-image">
      <img :src="mainImage" :alt="title" loading="lazy">
      <div v-if="product.status === 0" class="status-tag sold-out">已售罄</div>
    </div>
    <div class="product-info">
      <h3 class="product-title">{{ title }}</h3>
      <div class="product-desc">{{ product.description }}</div>
      <div class="product-bottom">
        <div class="price">
          <span class="symbol">¥</span>
          <span class="current">{{ product.price?.toFixed(2) }}</span>
          <span v-if="product.originalPrice && product.originalPrice > product.price" class="original">
            ¥{{ product.originalPrice?.toFixed(2) }}
          </span>
        </div>
        <div class="meta">
          <span>👁 {{ product.viewCount || 0 }}</span>
        </div>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.product-card {
  display: block;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}

.product-image {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f5f5f5;
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.product-card:hover .product-image img {
  transform: scale(1.05);
}

.status-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
}

.sold-out {
  background: #999;
}

.product-info {
  padding: 12px;
}

.product-title {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  line-height: 1.4;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 40px;
}

.product-desc {
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.price {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.price .symbol {
  font-size: 12px;
  color: #ff6b35;
  font-weight: 600;
}

.price .current {
  font-size: 18px;
  color: #ff6b35;
  font-weight: 700;
}

.price .original {
  font-size: 12px;
  color: #bbb;
  text-decoration: line-through;
  margin-left: 4px;
}

.meta {
  font-size: 12px;
  color: #999;
}
</style>
