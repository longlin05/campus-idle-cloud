<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { productApi, favoriteApi, ordersApi } from '@/api'
import type { ProductVO, OrderVO } from '@/api'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const toast = useToast()

const menuItems = [
  { key: 'publish', label: '发布商品', icon: '➕' },
  { key: 'products', label: '我的商品', icon: '📦' },
  { key: 'favorites', label: '我的收藏', icon: '❤️' },
  { key: 'orders-bought', label: '我买的', icon: '🛒' },
  { key: 'orders-sold', label: '我卖的', icon: '💼' },
  { key: 'addresses', label: '收货地址', icon: '📮' },
  { key: 'chat', label: '私信', icon: '💬' },
  { key: 'profile', label: '个人资料', icon: '👤' },
]

const activeMenu = ref('products')
const products = ref<ProductVO[]>([])
const favorites = ref<ProductVO[]>([])
const boughtOrders = ref<OrderVO[]>([])
const soldOrders = ref<OrderVO[]>([])
const loading = ref(false)

const boughtStatus = ref<number | null>(null)
const soldStatus = ref<number | null>(null)

const profile = computed(() => userStore.userInfo)

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login' })
    return
  }
  const tab = route.query.tab as string
  if (tab && ['orders-bought', 'orders-sold', 'products', 'favorites'].includes(tab)) {
    handleMenu(tab)
  } else {
    handleMenu('products')
  }
})

watch(() => route.query.tab, (newTab) => {
  if (newTab && ['orders-bought', 'orders-sold', 'products', 'favorites'].includes(newTab as string)) {
    handleMenu(newTab as string)
  }
})

function handleMenu(key: string) {
  activeMenu.value = key
  if (key === 'publish') {
    router.push('/publish')
    return
  }
  if (key === 'favorites') {
    loadFavorites()
    return
  }
  if (key === 'orders-bought') {
    loadBought()
    return
  }
  if (key === 'orders-sold') {
    loadSold()
    return
  }
  if (key === 'products') {
    loadMyProducts()
    return
  }
  if (key === 'addresses') {
    router.push('/addresses')
    return
  }
  if (key === 'chat') {
    router.push('/chat')
    return
  }
}

async function loadMyProducts() {
  loading.value = true
  try {
    const res = await productApi.getMyProducts()
    if (res.code === 200) products.value = (res.data?.records || res.data?.list || []) as ProductVO[]
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
}

async function loadFavorites() {
  loading.value = true
  try {
    const favRes = await favoriteApi.getList()
    const favList = favRes?.data || []
    if (favList.length === 0) {
      favorites.value = []
      return
    }
    const productIds = favList.map(f => f.productId)
    const batchRes = await productApi.getBatch(productIds)
    favorites.value = batchRes?.data || []
  } catch (e) {
    console.error('加载收藏失败', e)
    favorites.value = []
  } finally {
    loading.value = false
  }
}

async function loadBought() {
  loading.value = true
  try {
    const res = await ordersApi.getBuyList(boughtStatus.value, 1, 50)
    console.log('[UserCenter] loadBought response:', res)
    if (res.code === 200) {
      const data: any = res.data
      if (Array.isArray(data)) {
        boughtOrders.value = data
      } else if (data && Array.isArray(data.records)) {
        boughtOrders.value = data.records
      } else {
        boughtOrders.value = []
      }
      console.log('[UserCenter] boughtOrders:', boughtOrders.value)
    }
  } catch (e) {
    console.error('加载买入订单失败', e)
    boughtOrders.value = []
  } finally {
    loading.value = false
  }
}

async function loadSold() {
  loading.value = true
  try {
    const res = await ordersApi.getSellList(soldStatus.value, 1, 50)
    if (res.code === 200) {
      const data: any = res.data
      if (Array.isArray(data)) {
        soldOrders.value = data
      } else if (data && Array.isArray(data.records)) {
        soldOrders.value = data.records
      } else {
        soldOrders.value = []
      }
    }
  } catch (e) {
    console.error('加载卖出订单失败', e)
    soldOrders.value = []
  } finally {
    loading.value = false
  }
}

function filterBought(status: number | null) {
  boughtStatus.value = status
  loadBought()
}

function filterSold(status: number | null) {
  soldStatus.value = status
  loadSold()
}

function getStatusText(status: number): string {
  const m: Record<number, string> = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '退款中', 6: '已退款' }
  return m[status] || '未知'
}

function getStatusClass(status: number): string {
  const m: Record<number, string> = { 0: 'st-pending', 1: 'st-paid', 2: 'st-shipped', 3: 'st-done', 4: 'st-cancel', 5: 'st-refund', 6: 'st-refunded' }
  return m[status] || ''
}

function formatTime(t?: string): string {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function handlePay(orderId: number) {
  try {
    const res = await ordersApi.pay(orderId)
    if (res.code === 200) { toast.success('支付成功'); loadBought() }
    else toast.error(res.message || '支付失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}

async function handleCancelOrder(orderId: number) {
  if (!confirm('确定要取消该订单吗？')) return
  try {
    const res = await ordersApi.cancel(orderId)
    if (res.code === 200) { toast.success('订单已取消'); loadBought() }
    else toast.error(res.message || '取消失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}

async function handleConfirm(orderId: number) {
  if (!confirm('确定已收到商品吗？')) return
  try {
    const res = await ordersApi.confirm(orderId)
    if (res.code === 200) { toast.success('已确认收货'); loadBought() }
    else toast.error(res.message || '确认失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}

async function handleShip(orderId: number) {
  if (!confirm('确定已发货？')) return
  try {
    const res = await ordersApi.ship(orderId)
    if (res.code === 200) { toast.success('发货成功'); loadSold() }
    else toast.error(res.message || '发货失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}

async function handleOff(id: number) {
  if (!confirm('确定要下架该商品？')) return
  try {
    const res = await productApi.offline(id)
    if (res.code === 200) { toast.success('下架成功'); loadMyProducts() }
    else toast.error(res.message || '操作失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}

async function handleOn(id: number) {
  try {
    const res = await productApi.online(id)
    if (res.code === 200) { toast.success('上架成功'); loadMyProducts() }
    else toast.error(res.message || '操作失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}

async function handleDelete(id: number) {
  if (!confirm('确定删除该商品？删除后不可恢复。')) return
  try {
    await productApi.delete(id)
    toast.success('删除成功')
    loadMyProducts()
  } catch (e: any) { toast.error(e.message || '删除失败') }
}

function handleLogout() {
  if (!confirm('确定要退出登录？')) return
  userStore.logout()
  router.replace('/')
}
</script>

<template>
  <div class="user-center">
    <aside class="sidebar">
      <div class="user-card">
        <img :src="profile?.avatar || 'https://via.placeholder.com/64'" alt="" class="avatar">
        <div class="info">
          <div class="name">{{ profile?.nickname || profile?.username || '用户' }}</div>
          <div class="phone">{{ profile?.phone }}</div>
        </div>
      </div>
      <ul class="menu">
        <li
          v-for="m in menuItems"
          :key="m.key"
          :class="{ active: activeMenu === m.key }"
          @click="handleMenu(m.key)"
        >
          <span class="icon">{{ m.icon }}</span>
          <span class="label">{{ m.label }}</span>
        </li>
        <li class="logout" @click="handleLogout">
          <span class="icon">🚪</span>
          <span class="label">退出登录</span>
        </li>
      </ul>
    </aside>

    <main class="content">
      <div v-if="activeMenu === 'products'" class="panel">
        <div class="panel-head">
          <h2>📦 我的商品</h2>
          <router-link to="/publish" class="btn-add">➕ 发布新商品</router-link>
        </div>
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="products.length === 0" class="empty">
          还没有发布过商品，<router-link to="/publish" style="color:#ff6b35">去发布一个吧</router-link>
        </div>
        <div v-else class="product-list">
          <div v-for="p in products" :key="p.id" class="product-row">
            <router-link :to="`/product/${p.id}`" class="product-link">
              <img :src="p.images?.[0] || 'https://via.placeholder.com/80'" alt="">
              <div class="info">
                <div class="title">{{ p.title }}</div>
                <div class="meta">
                  <span class="price">¥{{ p.price?.toFixed(2) }}</span>
                  <span class="status" :class="'s-' + p.status">
                    {{ p.status === 1 ? '在架' : p.status === 0 ? '已售' : '已下架' }}
                  </span>
                </div>
              </div>
            </router-link>
            <div class="row-actions">
              <button v-if="p.status === 1" class="btn-off" @click="handleOff(p.id)">下架</button>
              <button v-else-if="p.status === 2" class="btn-on" @click="handleOn(p.id)">上架</button>
              <router-link :to="`/publish/${p.id}`" class="btn-edit">编辑</router-link>
              <button class="btn-del" @click="handleDelete(p.id)">删除</button>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="activeMenu === 'favorites'" class="panel">
        <div class="panel-head"><h2>❤️ 我的收藏</h2></div>
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="favorites.length === 0" class="empty">
          还没有收藏任何商品，<router-link to="/products" style="color:#ff6b35">去发现喜欢的商品吧</router-link>
        </div>
        <div v-else class="fav-grid">
          <div v-for="p in favorites" :key="p.id" class="fav-card" @click="router.push(`/product/${p.id}`)">
            <img :src="p.imageUrl || p.images?.[0] || 'https://via.placeholder.com/200'" alt="">
            <div class="fav-info">
              <div class="fav-title">{{ p.title }}</div>
              <div class="fav-price">¥{{ p.price?.toFixed(2) }}</div>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="activeMenu === 'orders-bought'" class="panel">
        <div class="panel-head"><h2>🛒 我买的</h2></div>
        <div class="status-tabs">
          <button :class="{ active: boughtStatus === null }" @click="filterBought(null)">全部</button>
          <button :class="{ active: boughtStatus === 0 }" @click="filterBought(0)">待付款</button>
          <button :class="{ active: boughtStatus === 1 }" @click="filterBought(1)">待发货</button>
          <button :class="{ active: boughtStatus === 2 }" @click="filterBought(2)">待收货</button>
          <button :class="{ active: boughtStatus === 3 }" @click="filterBought(3)">已完成</button>
        </div>
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="!boughtOrders || boughtOrders.length === 0" class="empty">暂时还没有订单</div>
        <div v-else class="order-list">
          <div v-for="o in boughtOrders" :key="o.orderId" class="order-card">
            <div class="order-top">
              <span class="order-no">订单号：{{ o.orderNo || o.orderId }}</span>
              <span class="order-time">{{ formatTime(o.createTime) }}</span>
              <span class="order-status" :class="getStatusClass(o.status)">{{ o.statusName || getStatusText(o.status) }}</span>
            </div>
            <div class="order-body">
              <img v-if="o.productImage" :src="o.productImage" alt="">
              <div class="order-info">
                <div class="order-title">{{ o.productName }}</div>
                <div class="order-meta">数量：{{ o.quantity }}</div>
                <div class="order-amount">¥{{ o.orderAmount }}</div>
              </div>
              <div class="order-actions">
                <button v-if="o.status === 0" class="btn-act btn-outline" @click="handleCancelOrder(o.orderId!)">取消订单</button>
                <button v-if="o.status === 0" class="btn-act btn-primary" @click="handlePay(o.orderId!)">立即支付</button>
                <button v-if="o.status === 2" class="btn-act btn-primary" @click="handleConfirm(o.orderId!)">确认收货</button>
                <router-link :to="`/order/${o.orderId}`" class="btn-act btn-text">查看详情</router-link>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="activeMenu === 'orders-sold'" class="panel">
        <div class="panel-head"><h2>💼 我卖出的</h2></div>
        <div class="status-tabs">
          <button :class="{ active: soldStatus === null }" @click="filterSold(null)">全部</button>
          <button :class="{ active: soldStatus === 0 }" @click="filterSold(0)">待付款</button>
          <button :class="{ active: soldStatus === 1 }" @click="filterSold(1)">待发货</button>
          <button :class="{ active: soldStatus === 2 }" @click="filterSold(2)">待收货</button>
          <button :class="{ active: soldStatus === 3 }" @click="filterSold(3)">已完成</button>
        </div>
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="!soldOrders || soldOrders.length === 0" class="empty">暂时还没有订单</div>
        <div v-else class="order-list">
          <div v-for="o in soldOrders" :key="o.orderId" class="order-card">
            <div class="order-top">
              <span class="order-no">订单号：{{ o.orderNo || o.orderId }}</span>
              <span class="order-time">{{ formatTime(o.createTime) }}</span>
              <span class="order-status" :class="getStatusClass(o.status)">{{ o.statusName || getStatusText(o.status) }}</span>
            </div>
            <div class="order-body">
              <img v-if="o.productImage" :src="o.productImage" alt="">
              <div class="order-info">
                <div class="order-title">{{ o.productName }}</div>
                <div class="order-meta">数量：{{ o.quantity }}</div>
                <div class="order-amount">¥{{ o.orderAmount }}</div>
              </div>
              <div class="order-actions">
                <button v-if="o.status === 1" class="btn-act btn-primary" @click="handleShip(o.orderId!)">立即发货</button>
                <router-link :to="`/order/${o.orderId}`" class="btn-act btn-text">查看详情</router-link>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="activeMenu === 'profile'" class="panel">
        <div class="panel-head"><h2>👤 个人资料</h2></div>
        <div class="profile-box">
          <div class="row"><span class="label">头像</span><img class="big-avatar" :src="profile?.avatar || 'https://via.placeholder.com/80'" alt=""></div>
          <div class="row"><span class="label">昵称</span><span>{{ profile?.nickname || '—' }}</span></div>
          <div class="row"><span class="label">手机号</span><span>{{ profile?.phone }}</span></div>
          <div class="row"><span class="label">注册时间</span><span>{{ profile?.createTime || '—' }}</span></div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.user-center {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}

.user-card {
  padding: 20px 16px;
  display: flex;
  gap: 12px;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #fff6f2 0%, #ffe8d6 100%);
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.user-card .name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.user-card .phone {
  font-size: 12px;
  color: #999;
}

.menu {
  padding: 8px 0;
}

.menu li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  font-size: 14px;
  color: #555;
  cursor: pointer;
  transition: all 0.15s;
}

.menu li:hover {
  background: #fafafa;
  color: #ff6b35;
}

.menu li.active {
  background: #fff2eb;
  color: #ff6b35;
  font-weight: 600;
  border-left: 3px solid #ff6b35;
}

.menu li.logout {
  border-top: 1px solid #f0f0f0;
  margin-top: 8px;
  padding-top: 16px;
}

.menu .icon {
  font-size: 16px;
}

.content {
  flex: 1;
  min-width: 0;
}

.panel {
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-head h2 {
  font-size: 18px;
  color: #333;
}

.btn-add {
  height: 34px;
  padding: 0 16px;
  background: #ff6b35;
  color: #fff;
  border-radius: 6px;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
}

.loading, .empty {
  padding: 60px 20px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.empty {
  font-size: 16px;
  color: #bbb;
}

.empty::before {
  content: '📦';
  display: block;
  font-size: 48px;
  margin-bottom: 16px;
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 14px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  transition: border-color 0.2s;
}

.product-row:hover {
  border-color: #ffd4b5;
}

.product-link {
  display: flex;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.product-link img {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}

.product-link .info {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 4px 0;
}

.product-link .title {
  font-size: 15px;
  color: #333;
  font-weight: 500;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-link .meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-link .price {
  color: #ff6b35;
  font-size: 18px;
  font-weight: 700;
}

.status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.status.s-1 {
  background: #e8f7ea;
  color: #27ae60;
}

.status.s-0 {
  background: #fff2e5;
  color: #f39c12;
}

.status.s-2 {
  background: #f0f0f0;
  color: #999;
}

.row-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.row-actions button,
.row-actions .btn-edit {
  height: 32px;
  padding: 0 14px;
  border-radius: 4px;
  font-size: 13px;
}

.btn-on {
  background: #e8f7ea;
  color: #27ae60;
}

.btn-off {
  background: #fff6f2;
  color: #f39c12;
}

.btn-edit {
  border: 1px solid #ddd;
  color: #555;
  background: #fff;
  display: inline-flex;
  align-items: center;
}

.btn-del {
  background: #fdecea;
  color: #e74c3c;
}

.profile-box .row {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #333;
}

.profile-box .label {
  width: 100px;
  color: #999;
}

.big-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
}

.fav-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.fav-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.fav-card:hover {
  border-color: #ffd4b5;
  box-shadow: 0 2px 8px rgba(255, 107, 53, 0.1);
}

.fav-card img {
  width: 100%;
  height: 160px;
  object-fit: cover;
}

.fav-info {
  padding: 10px 12px;
}

.fav-title {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 6px;
}

.fav-price {
  color: #ff6b35;
  font-size: 18px;
  font-weight: 700;
}

/* ====== 订单列表样式 ====== */
.status-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.status-tabs button {
  padding: 6px 16px;
  border: none;
  border-radius: 16px;
  font-size: 13px;
  color: #666;
  background: #f5f5f5;
  cursor: pointer;
  transition: all 0.2s;
}

.status-tabs button:hover {
  color: #ff6b35;
}

.status-tabs button.active {
  background: #ff6b35;
  color: #fff;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 14px;
  transition: border-color 0.2s;
}

.order-card:hover {
  border-color: #ffd4b5;
}

.order-top {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f5f5f5;
  margin-bottom: 12px;
  font-size: 13px;
}

.order-no {
  color: #999;
}

.order-time {
  color: #bbb;
  font-size: 12px;
}

.order-status {
  margin-left: auto;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
}

.order-status.st-pending { background: #fff2e5; color: #f39c12; }
.order-status.st-paid { background: #e8f0fe; color: #4a90d9; }
.order-status.st-shipped { background: #e8f7ea; color: #27ae60; }
.order-status.st-done { background: #f0f0f0; color: #999; }
.order-status.st-cancel { background: #fdecea; color: #e74c3c; }
.order-status.st-refund { background: #fff3e0; color: #ff9800; }
.order-status.st-refunded { background: #f0f0f0; color: #999; }

.order-body {
  display: flex;
  gap: 14px;
  align-items: center;
}

.order-body img {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}

.order-info {
  flex: 1;
  min-width: 0;
}

.order-title {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.order-meta {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.order-amount {
  color: #ff6b35;
  font-size: 18px;
  font-weight: 700;
  margin-top: 6px;
}

.order-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.btn-act {
  height: 32px;
  padding: 0 14px;
  border-radius: 4px;
  font-size: 13px;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  text-decoration: none;
}

.btn-primary {
  background: #ff6b35;
  color: #fff;
}

.btn-outline {
  background: #fff;
  color: #666;
  border: 1px solid #ddd;
}

.btn-text {
  background: transparent;
  color: #ff6b35;
  padding: 0 8px;
}
</style>
