<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ordersApi } from '@/api'
import type { OrderVO } from '@/api'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const toast = useToast()

const tabs = [
  { key: 'all', label: '全部订单' },
  { key: '0', label: '待付款' },
  { key: '1', label: '待发货' },
  { key: '2', label: '待收货' },
  { key: '3', label: '已完成' },
  { key: '-1', label: '已取消' },
]

const activeTab = ref<string>('all')
const orders = ref<OrderVO[]>([])
const loading = ref(false)

const statusMap: Record<string, { label: string; color: string }> = {
  '-1': { label: '已取消', color: '#999' },
  '0': { label: '待付款', color: '#ff6b35' },
  '1': { label: '待发货', color: '#f39c12' },
  '2': { label: '待收货', color: '#3498db' },
  '3': { label: '已完成', color: '#27ae60' },
}

const filteredOrders = computed(() => {
  if (activeTab.value === 'all') return orders.value
  return orders.value.filter(o => String(o.status) === activeTab.value)
})

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: '/orders' } })
    return
  }
  const tab = route.query.status as string
  if (tab) activeTab.value = tab
  loadOrders()
})

async function loadOrders() {
  loading.value = true
  try {
    const res = await ordersApi.getList()
    if (res.code === 200) orders.value = res.data || []
  } catch (e) {
    console.error('加载订单失败', e)
  } finally {
    loading.value = false
  }
}

async function handlePay(orderId: number) {
  if (!confirm('确认支付该订单？')) return
  try {
    const res = await ordersApi.pay(orderId)
    if (res.code === 200) {
      toast.success('支付成功！')
      loadOrders()
    } else {
      toast.error(res.message || '支付失败')
    }
  } catch (e: any) {
    toast.error(e.message || '支付失败')
  }
}

async function handleCancel(orderId: number) {
  if (!confirm('确认取消该订单？')) return
  try {
    const res = await ordersApi.cancel(orderId)
    if (res.code === 200) {
      toast.success('取消成功')
      loadOrders()
    } else {
      toast.error(res.message || '取消失败')
    }
  } catch (e: any) {
    toast.error(e.message || '取消失败')
  }
}

async function handleReceive(orderId: number) {
  if (!confirm('确认收货？')) return
  try {
    const res = await ordersApi.receive(orderId)
    if (res.code === 200) {
      toast.success('收货成功')
      loadOrders()
    } else {
      toast.error(res.message || '操作失败')
    }
  } catch (e: any) {
    toast.error(e.message || '操作失败')
  }
}
</script>

<template>
  <div class="orders-page">
    <h1 class="page-title">📦 我的订单</h1>

    <div class="tabs">
      <span
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >{{ tab.label }}</span>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="filteredOrders.length === 0" class="empty">
      暂无相关订单
    </div>
    <div v-else class="order-list">
      <div
        v-for="order in filteredOrders"
        :key="order.id"
        class="order-card"
      >
        <div class="order-header">
          <span class="order-id">订单号：{{ order.orderNo || order.id }}</span>
          <span class="order-status" :style="{ color: statusMap[String(order.status)]?.color }">
            {{ statusMap[String(order.status)]?.label || '未知' }}
          </span>
        </div>

        <div class="order-items">
          <router-link
            v-for="item in order.items"
            :key="item.id"
            :to="`/product/${item.productId}`"
            class="order-item"
          >
            <img :src="item.productImage || 'https://via.placeholder.com/80'" :alt="item.productTitle">
            <div class="info">
              <div class="title">{{ item.productTitle }}</div>
              <div class="qty">× {{ item.quantity }}</div>
            </div>
            <div class="price">¥{{ item.price?.toFixed(2) }}</div>
          </router-link>
        </div>

        <div class="order-footer">
          <div class="address">
            <span>📬 {{ order.name }} | {{ order.phone }}</span>
            <span style="color:#999;font-size:12px">{{ order.address }}</span>
          </div>
          <div class="total">
            <span style="color:#999;font-size:13px">共{{ order.totalQuantity }}件商品 实付：</span>
            <span class="amount">¥{{ order.totalAmount?.toFixed(2) }}</span>
          </div>
          <div class="actions">
            <template v-if="String(order.status) === '0'">
              <button class="btn-cancel" @click="handleCancel(order.id!)">取消订单</button>
              <button class="btn-primary" @click="handlePay(order.id!)">立即付款</button>
            </template>
            <template v-else-if="String(order.status) === '1'">
              <span style="color:#999;font-size:13px">等待卖家发货</span>
            </template>
            <template v-else-if="String(order.status) === '2'">
              <button class="btn-primary" @click="handleReceive(order.id!)">确认收货</button>
            </template>
            <router-link :to="`/order/${order.id}`" class="btn-detail">订单详情</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 22px;
  color: #333;
  margin-bottom: 20px;
}

.tabs {
  display: flex;
  background: #fff;
  border-radius: 10px;
  padding: 0 10px;
  margin-bottom: 20px;
  overflow-x: auto;
  white-space: nowrap;
}

.tab-item {
  display: inline-block;
  padding: 14px 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  margin-right: 10px;
}

.tab-item.active {
  color: #ff6b35;
  border-bottom-color: #ff6b35;
  font-weight: 600;
}

.loading, .empty {
  background: #fff;
  border-radius: 10px;
  padding: 60px 20px;
  text-align: center;
  color: #999;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: space-between;
  padding: 14px 20px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
  color: #666;
}

.order-status {
  font-weight: 600;
}

.order-items {
  padding: 10px 20px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 0;
  border-bottom: 1px dashed #f0f0f0;
}

.order-item:last-child {
  border-bottom: none;
}

.order-item img {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}

.order-item .info {
  flex: 1;
  min-width: 0;
}

.order-item .title {
  font-size: 14px;
  color: #333;
  margin-bottom: 6px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.order-item .qty {
  font-size: 12px;
  color: #999;
}

.order-item .price {
  color: #333;
  font-weight: 600;
}

.order-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  border-top: 1px solid #f0f0f0;
}

.address {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #555;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.total {
  display: flex;
  align-items: baseline;
}

.amount {
  font-size: 20px;
  color: #ff6b35;
  font-weight: 700;
  margin-left: 4px;
}

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.btn-primary {
  height: 34px;
  padding: 0 16px;
  background: #ff6b35;
  color: #fff;
  border-radius: 6px;
  font-size: 13px;
}

.btn-cancel {
  height: 34px;
  padding: 0 16px;
  border: 1px solid #ddd;
  background: #fff;
  color: #666;
  border-radius: 6px;
  font-size: 13px;
}

.btn-detail {
  height: 34px;
  padding: 0 16px;
  border: 1px solid #ff6b35;
  background: #fff;
  color: #ff6b35;
  border-radius: 6px;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
}
</style>
