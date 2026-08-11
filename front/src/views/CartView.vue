<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { shoppingCartApi, productApi } from '@/api'
import type { CartItemVO } from '@/api'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const userStore = useUserStore()
const toast = useToast()

const items = ref<CartItemVO[]>([])
const checkedIds = ref<Set<number>>(new Set())
const loading = ref(false)
const allChecked = computed(() => items.value.length > 0 && checkedIds.value.size === items.value.length)
const checkedItems = computed(() => items.value.filter(it => checkedIds.value.has(it.productId)))
const totalAmount = computed(() =>
  checkedItems.value.reduce((sum, it) => sum + (it.price || 0) * (it.quantity || 1), 0)
)
const totalCount = computed(() =>
  checkedItems.value.reduce((sum, it) => sum + (it.quantity || 0), 0)
)

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: '/cart' } })
    return
  }
  loadList()
})

async function loadList() {
  loading.value = true
  try {
    const res = await shoppingCartApi.getList()
    if (res.code === 200) {
      const rawItems: any[] = res.data || []
      if (rawItems.length === 0) {
        items.value = []
        return
      }
      // 后端返回 itemId/productId/quantity/selected，需要批量查商品详情补充 title/price/images
      const productIds = rawItems.map(it => it.productId)
      const batchRes = await productApi.getBatch(productIds)
      const productMap = new Map<number, any>()
      if (batchRes?.data) {
        for (const p of batchRes.data) {
          productMap.set(p.id, p)
        }
      }
      items.value = rawItems.map((it: any) => {
        const product = productMap.get(it.productId)
        return {
          id: it.itemId || it.id,
          productId: it.productId,
          title: product?.title || '商品#' + it.productId,
          description: product?.description || '',
          image: product?.imageUrl || product?.images?.[0] || '',
          images: product?.images || [],
          price: product?.price || 0,
          quantity: it.quantity,
          selected: it.selected,
          categoryName: product?.categoryName || '',
        }
      })
    }
  } catch (e) {
    console.error('加载购物车失败', e)
  } finally {
    loading.value = false
  }
}

function toggleCheck(productId: number) {
  if (checkedIds.value.has(productId)) {
    checkedIds.value.delete(productId)
  } else {
    checkedIds.value.add(productId)
  }
  checkedIds.value = new Set(checkedIds.value)
}

function toggleAll() {
  if (allChecked.value) {
    checkedIds.value = new Set()
  } else {
    checkedIds.value = new Set(items.value.map(it => it.productId))
  }
}

async function changeQty(item: CartItemVO, delta: number) {
  const newQty = (item.quantity || 1) + delta
  if (newQty < 1) return
  try {
    await shoppingCartApi.update(item.productId, newQty)
    item.quantity = newQty
  } catch (e) {
    console.error('修改数量失败', e)
  }
}

async function removeItem(productId: number) {
  if (!confirm('确定要删除该商品吗？')) return
  try {
    await shoppingCartApi.remove(productId)
    checkedIds.value.delete(productId)
    items.value = items.value.filter(it => it.productId !== productId)
  } catch (e) {
    console.error('删除失败', e)
  }
}

function checkout() {
  if (checkedItems.value.length === 0) {
    toast.warning('请选择要购买的商品')
    return
  }
  // 把选中商品信息序列化后传给订单确认页，由用户选地址后再下单
  const payload = checkedItems.value.map(it => ({
    productId: it.productId,
    title: it.title,
    image: it.image,
    price: it.price,
    quantity: it.quantity,
  }))
  router.push({
    name: 'OrderConfirm',
    query: {
      source: 'cart',
      items: encodeURIComponent(JSON.stringify(payload)),
    },
  })
}
</script>

<template>
  <div class="cart-page">
    <h1 class="page-title">🛒 购物车</h1>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="items.length === 0" class="empty">
      <div class="icon">🛒</div>
      <p>购物车空空如也，快去挑点好物吧~</p>
      <router-link to="/products" class="btn-go">去逛逛</router-link>
    </div>
    <template v-else>
      <div class="cart-table-wrap">
        <table class="cart-table">
          <thead>
            <tr>
              <th class="check-col">
                <input type="checkbox" :checked="allChecked" @change="toggleAll">
              </th>
              <th class="product-col">商品信息</th>
              <th class="price-col">单价</th>
              <th class="qty-col">数量</th>
              <th class="subtotal-col">小计</th>
              <th class="action-col">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.productId">
              <td class="check-col">
                <input
                  type="checkbox"
                  :checked="checkedIds.has(item.productId)"
                  @change="toggleCheck(item.productId)"
                >
              </td>
              <td class="product-col">
                <router-link :to="`/product/${item.productId}`" class="product-link">
                  <img :src="item.image || 'https://via.placeholder.com/80'" :alt="item.title">
                  <div class="info">
                    <div class="title">{{ item.title }}</div>
                    <div class="desc">{{ item.categoryName }}</div>
                  </div>
                </router-link>
              </td>
              <td class="price-col">¥{{ item.price?.toFixed(2) }}</td>
              <td class="qty-col">
                <div class="qty-box">
                  <button @click="changeQty(item, -1)">-</button>
                  <input :value="item.quantity" readonly>
                  <button @click="changeQty(item, 1)">+</button>
                </div>
              </td>
              <td class="subtotal-col">¥{{ (item.price * (item.quantity || 1)).toFixed(2) }}</td>
              <td class="action-col">
                <button class="btn-remove" @click="removeItem(item.productId)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="cart-footer">
        <div class="footer-left">
          <label>
            <input type="checkbox" :checked="allChecked" @change="toggleAll">
            全选
          </label>
          <span class="info-text">已选 <b>{{ totalCount }}</b> 件商品</span>
        </div>
        <div class="footer-right">
          <span class="total-label">合计：</span>
          <span class="total-price">¥{{ totalAmount.toFixed(2) }}</span>
          <button class="btn-checkout" :disabled="checkedItems.length === 0" @click="checkout">
            去结算 ({{ totalCount }})
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 22px;
  color: #333;
  margin-bottom: 20px;
}

.loading, .empty {
  background: #fff;
  border-radius: 10px;
  padding: 80px 20px;
  text-align: center;
}

.empty .icon {
  font-size: 60px;
  opacity: 0.5;
  margin-bottom: 16px;
}

.empty p {
  color: #999;
  margin-bottom: 20px;
}

.btn-go {
  display: inline-block;
  padding: 10px 28px;
  background: #ff6b35;
  color: #fff;
  border-radius: 8px;
  font-weight: 600;
}

.cart-table-wrap {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}

.cart-table {
  width: 100%;
  border-collapse: collapse;
}

.cart-table th, .cart-table td {
  padding: 16px;
  text-align: left;
  font-size: 14px;
}

.cart-table thead {
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.cart-table th {
  color: #666;
  font-weight: 600;
}

.cart-table tbody tr {
  border-bottom: 1px solid #f5f5f5;
}

.check-col { width: 50px; text-align: center; }
.product-col { width: 45%; }
.price-col { width: 12%; color: #666; }
.qty-col { width: 15%; }
.subtotal-col { width: 12%; color: #ff6b35; font-weight: 600; }
.action-col { width: 10%; text-align: center; }

.product-link {
  display: flex;
  gap: 14px;
  align-items: center;
}

.product-link img {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}

.product-link .title {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-link .desc {
  font-size: 12px;
  color: #999;
}

.qty-box {
  display: inline-flex;
  align-items: center;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
}

.qty-box button {
  width: 30px;
  height: 30px;
  background: #f8f8f8;
  font-size: 16px;
  color: #666;
}

.qty-box input {
  width: 40px;
  height: 30px;
  text-align: center;
  border: none;
  font-size: 14px;
  border-left: 1px solid #eee;
  border-right: 1px solid #eee;
}

.btn-remove {
  color: #e74c3c;
  background: transparent;
  font-size: 13px;
}

.cart-footer {
  position: sticky;
  bottom: 0;
  background: #fff;
  border-top: 2px solid #f5f5f5;
  border-radius: 10px;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  box-shadow: 0 -4px 12px rgba(0,0,0,0.04);
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 14px;
  color: #555;
}

.footer-left input {
  margin-right: 6px;
}

.info-text b {
  color: #ff6b35;
  font-size: 16px;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.total-label {
  font-size: 14px;
  color: #666;
}

.total-price {
  font-size: 22px;
  color: #ff6b35;
  font-weight: 700;
}

.btn-checkout {
  height: 44px;
  padding: 0 32px;
  background: linear-gradient(90deg, #ff6b35 0%, #f7931e 100%);
  color: #fff;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
}

.btn-checkout:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
