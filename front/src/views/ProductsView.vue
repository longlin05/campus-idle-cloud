<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import ProductCard from '@/components/ProductCard.vue'
import { productApi } from '@/api'
import type { ProductVO, Category, PageResult } from '@/api'

const route = useRoute()

const categories = ref<Category[]>([])
const products = ref<ProductVO[]>([])
const page = reactive({
  current: 1,
  size: 20,
  total: 0,
})
const keyword = ref('')
const selectedCategory = ref<number | null>(null)
const loading = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(page.total / page.size)))

onMounted(async () => {
  const [catRes] = await Promise.all([productApi.getCategories()])
  if (catRes.code === 200) categories.value = catRes.data || []

  const qk = route.query.keyword as string
  const qc = route.query.categoryId as string
  if (qk) keyword.value = qk
  if (qc) selectedCategory.value = Number(qc)

  await loadProducts()
})

async function loadProducts(reset = false) {
  if (reset) page.current = 1
  loading.value = true
  try {
    const res = await productApi.search(
      keyword.value,
      page.current,
      page.size,
      selectedCategory.value ?? undefined
    )
    if (res.code === 200) {
      const data = (res.data as PageResult<ProductVO>)
      products.value = data.records || []
      page.total = data.total || 0
    }
  } catch (e) {
    console.error('加载商品失败', e)
  } finally {
    loading.value = false
  }
}

function selectCategory(id: number | null) {
  selectedCategory.value = id
  loadProducts(true)
}

function handleSearch() {
  loadProducts(true)
}

function changePage(p: number) {
  page.current = p
  loadProducts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<template>
  <div class="products-page">
    <div class="filter-bar">
      <div class="filters">
        <div class="filter-row">
          <span class="filter-label">分类：</span>
          <div class="filter-options">
            <span
              class="option"
              :class="{ active: selectedCategory === null }"
              @click="selectCategory(null)"
            >全部</span>
            <span
              v-for="c in categories"
              :key="c.categoryId || c.id"
              class="option"
              :class="{ active: selectedCategory === (c.categoryId || c.id) }"
              @click="selectCategory(c.categoryId || c.id)"
            >{{ c.categoryName || c.name }}</span>
          </div>
        </div>
        <div class="filter-row search-row">
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索商品名称或描述"
            class="search-input"
            @keyup.enter="handleSearch"
          >
          <button class="search-btn" @click="handleSearch">搜索</button>
        </div>
      </div>
      <div class="result-info">
        共 <b>{{ page.total }}</b> 件商品
      </div>
    </div>

    <div class="products-area">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="products.length === 0" class="empty">
        🛒 暂无符合条件的商品，换个关键词试试吧~
      </div>
      <template v-else>
        <div class="product-grid">
          <ProductCard v-for="p in products" :key="p.id" :product="p" />
        </div>
        <div v-if="page.total > page.size" class="pagination">
          <button :disabled="page.current <= 1" @click="changePage(page.current - 1)">上一页</button>
          <span v-for="n in Math.min(5, totalPages)" :key="n"
            :class="{ active: n === page.current }"
            @click="changePage(n)"
          >{{ n }}</span>
          <span v-if="totalPages > 5" class="ellipsis">...</span>
          <span v-if="totalPages > 5" :class="{ active: totalPages === page.current }" @click="changePage(totalPages)">
            {{ totalPages }}
          </span>
          <button :disabled="page.current >= totalPages" @click="changePage(page.current + 1)">下一页</button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.filter-bar {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.filters {
  flex: 1;
}

.filter-row {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-row.search-row {
  margin-bottom: 0;
}

.filter-label {
  font-size: 14px;
  color: #666;
  font-weight: 600;
  width: 50px;
  flex-shrink: 0;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option {
  padding: 5px 14px;
  border-radius: 16px;
  background: #f5f5f5;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.option:hover {
  background: #fff2eb;
  color: #ff6b35;
}

.option.active {
  background: #ff6b35;
  color: #fff;
}

.search-input {
  flex: 1;
  max-width: 320px;
  height: 36px;
  padding: 0 14px;
  border: 1px solid #ddd;
  border-radius: 18px;
  font-size: 13px;
}

.search-input:focus {
  border-color: #ff6b35;
}

.search-btn {
  height: 36px;
  padding: 0 20px;
  background: #ff6b35;
  color: #fff;
  border-radius: 18px;
  font-size: 13px;
}

.result-info {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  padding-top: 4px;
}

.result-info b {
  color: #ff6b35;
  font-size: 16px;
  margin: 0 2px;
}

.products-area {
  background: transparent;
}

.loading {
  text-align: center;
  padding: 60px 0;
  color: #999;
  background: #fff;
  border-radius: 10px;
}

.empty {
  text-align: center;
  padding: 80px 20px;
  color: #999;
  background: #fff;
  border-radius: 10px;
  font-size: 15px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 20px 0;
  background: #fff;
  border-radius: 10px;
}

.pagination button,
.pagination span {
  min-width: 36px;
  height: 36px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 6px;
  font-size: 13px;
  color: #555;
  cursor: pointer;
}

.pagination button:hover:not(:disabled),
.pagination span:hover {
  color: #ff6b35;
  border-color: #ff6b35;
}

.pagination span.active {
  background: #ff6b35;
  color: #fff;
  border-color: #ff6b35;
  cursor: default;
}

.pagination button:disabled {
  color: #ccc;
  cursor: not-allowed;
}

.pagination .ellipsis {
  border: none;
  cursor: default;
  background: transparent;
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
  .filter-bar {
    flex-direction: column;
  }
}
</style>
