<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ProductCard from '@/components/ProductCard.vue'
import { productApi, adminSystemImageApi } from '@/api'
import type { ProductVO, Category } from '@/api'

const router = useRouter()
const categories = ref<Category[]>([])
const hotProducts = ref<ProductVO[]>([])
const newProducts = ref<ProductVO[]>([])
const banners = ref<any[]>([])
const currentBanner = ref(0)

const carouselTimer = ref<number | null>(null)
const catLoading = ref(true)
const catError = ref('')

onMounted(async () => {
  loadData()
  startCarousel()
})

async function loadCategories() {
  catLoading.value = true
  catError.value = ''
  try {
    const catRes = await productApi.getCategories()
    if (catRes.code === 200) {
      categories.value = catRes.data || []
    } else {
      catError.value = catRes.message || '加载失败'
    }
  } catch (e) {
    console.error('加载分类失败', e)
    catError.value = '后端服务未启动'
  } finally {
    catLoading.value = false
  }
}

async function loadData() {
  loadCategories()
  loadBanners()
  try {
    const [hotRes, newRes] = await Promise.all([
      productApi.getHot(),
      productApi.getList(1, 8),
    ])
    if (hotRes.code === 200) hotProducts.value = (hotRes.data || []).slice(0, 8)
    if (newRes.code === 200) newProducts.value = newRes.data?.records || []
  } catch (e) {
    console.error('加载首页数据失败', e)
  }
}

async function loadBanners() {
  try {
    const res = await adminSystemImageApi.getBanners()
    if (res.code === 200) {
      banners.value = (res.data || []).map((b: any) => ({ url: b.imageUrl, title: b.imageName || 'banner' }))
    }
  } catch (e) {
    console.error('加载轮播图失败', e)
  }
}

function startCarousel() {
  carouselTimer.value = window.setInterval(() => {
    currentBanner.value = (currentBanner.value + 1) % (banners.value.length || 1)
  }, 4000)
}

function goCategory(categoryId: number) {
  router.push({ name: 'Products', query: { categoryId } })
}
</script>

<template>
  <div class="home">
    <div class="main-content">
      <aside class="sidebar">
        <div class="category-title">商品分类</div>
        <ul class="category-list">
          <li v-if="catLoading" class="cat-tip">加载中...</li>
          <li v-else-if="catError" class="cat-tip cat-error">
            <span>{{ catError }}</span>
            <button class="retry-btn" @click="loadCategories">重试</button>
          </li>
          <li v-else-if="categories.length === 0" class="cat-tip">暂无分类</li>
          <template v-else>
            <li
              v-for="c in categories"
              :key="c.categoryId || c.id"
              @click="goCategory(c.categoryId || c.id)"
            >
              <span class="cat-name">{{ c.categoryName || c.name }}</span>
              <span class="cat-arrow">›</span>
            </li>
          </template>
        </ul>
      </aside>

      <div class="content-area">
        <div v-if="banners.length > 0" class="carousel">
          <div class="carousel-inner">
            <div
              v-for="(banner, i) in banners"
              :key="i"
              class="carousel-item"
              :class="{ active: i === currentBanner }"
            >
              <img :src="banner.url" :alt="banner.title">
            </div>
          </div>
          <div v-if="banners.length > 1" class="carousel-indicators">
            <span
              v-for="(_, i) in banners"
              :key="i"
              :class="{ active: i === currentBanner }"
              @click="currentBanner = i"
            ></span>
          </div>
        </div>
        <div v-else class="carousel placeholder">
          <div class="ph-text">🎓 校园闲置 · 让闲置物品流转起来 🎓</div>
        </div>

        <section class="section">
          <div class="section-title">
            <h2>🔥 热门推荐</h2>
            <a @click="router.push({ name: 'Products' })" class="more">查看更多 ›</a>
          </div>
          <div v-if="hotProducts.length > 0" class="product-grid">
            <ProductCard v-for="p in hotProducts" :key="p.id" :product="p" />
          </div>
          <div v-else class="empty">暂无热门商品</div>
        </section>

        <section class="section">
          <div class="section-title">
            <h2>✨ 最新发布</h2>
            <a @click="router.push({ name: 'Products' })" class="more">查看更多 ›</a>
          </div>
          <div v-if="newProducts.length > 0" class="product-grid">
            <ProductCard v-for="p in newProducts" :key="p.id" :product="p" />
          </div>
          <div v-else class="empty">暂无最新商品</div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.main-content {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.sidebar {
  width: 200px;
  background: #fff;
  border-radius: 10px;
  padding: 12px 0;
  flex-shrink: 0;
}

.category-title {
  font-size: 15px;
  font-weight: 700;
  color: #333;
  padding: 8px 20px 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 6px;
}

.category-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  cursor: pointer;
  font-size: 14px;
  color: #444;
  transition: background 0.15s;
}

.category-list li:hover {
  background: #fff6f2;
  color: #ff6b35;
}

.cat-tip {
  justify-content: center !important;
  padding: 16px 20px !important;
  color: #999 !important;
  font-size: 13px !important;
  cursor: default !important;
  flex-direction: column;
  gap: 8px;
}

.cat-tip:hover {
  background: transparent !important;
  color: #999 !important;
}

.cat-error {
  color: #e74c3c !important;
}

.retry-btn {
  height: 26px;
  padding: 0 12px;
  background: #ff6b35;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.retry-btn:hover {
  background: #e85a2c;
}

.cat-arrow {
  color: #ccc;
  font-size: 16px;
}

.content-area {
  flex: 1;
  min-width: 0;
}

.carousel {
  height: 320px;
  background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%);
  border-radius: 10px;
  overflow: hidden;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.carousel.placeholder {
  background: linear-gradient(135deg, #ff6b35 0%, #ffb366 100%);
}

.ph-text {
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  text-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

.carousel-inner {
  width: 100%;
  height: 100%;
  position: relative;
}

.carousel-item {
  position: absolute;
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: opacity 0.6s;
}

.carousel-item.active {
  opacity: 1;
}

.carousel-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carousel-indicators {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
}

.carousel-indicators span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255,255,255,0.5);
  cursor: pointer;
}

.carousel-indicators span.active {
  background: #fff;
  width: 24px;
  border-radius: 5px;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title h2 {
  font-size: 18px;
  color: #333;
}

.more {
  font-size: 13px;
  color: #999;
  cursor: pointer;
}

.more:hover {
  color: #ff6b35;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.empty {
  padding: 60px 20px;
  text-align: center;
  color: #999;
  background: #fff;
  border-radius: 10px;
}

@media (max-width: 1100px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 800px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .sidebar {
    display: none;
  }
}
</style>
