<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ordersApi } from '@/api'
import type { OrderVO, OrderItemVO } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const orders = ref<any[]>([])
const loading = ref(false)

const statusMap: Record<string, { label: string; color: string }> = {
  '-1': { label: '已取消', color: '#999' },
  '0': { label: '待付款', color: '#ff6b35' },
  '1': { label: '待发货', color: '#f39c12' },
  '2': { label: '待收货', color: '#3498db' },
  '3': { label: '已完成', color: '#27ae60' },
  '4': { label: '已取消', color: '#999' },
  '5': { label: '退款中', color: '#9b59b6' },
  '6': { label: '已退款', color: '#999' },
}

/** 将后端 OrderListItemVO 转换为前端模板需要的 OrderVO 结构 */
function normalizeOrder(src: any): OrderVO {
  const item: OrderItemVO = {
    id: src.orderId,
    productId: src.productId,
    productTitle: src.productName || '商品',
    productImage: src.productImage,
    price: Number(src.price ?? 0),
    quantity: Number(src.quantity ?? 1),
  }
  return {
    id: src.orderId,
    orderId: src.orderId,
    orderNo: src.orderNo,
    status: Number(src.status ?? 0),
    statusText: src.statusName,
    buyerId: src.buyerId,
    sellerId: src.sellerId,
    items: [item],
    totalQuantity: Number(src.quantity ?? 1),
    totalAmount: Number(src.orderAmount ?? 0),
    orderAmount: Number(src.orderAmount ?? 0),
    createTime: src.createTime,
  }
}

const boughtOrders = computed(() => orders.value)

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: '/orders-buy' } })
    return
  }
  loading.value = true
  try {
    const res = await ordersApi.getBuyList()
    if (res.code === 200) {
      const list = res.data || []
      orders.value = list.map(normalizeOrder)
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
})
</script>

<template>
  <div class="page">
    <h1 class="page-title">🛍️ 我买的</h1>
    <div v-if="loading" class="card empty">加载中...</div>
    <div v-else-if="boughtOrders.length === 0" class="card empty">
      还没有购买记录，<router-link to="/products" style="color:#ff6b35">去逛逛吧</router-link>
    </div>
    <div v-else class="order-list">
      <div v-for="o in boughtOrders" :key="o.id" class="order-card" @click="router.push(`/order/${o.id}`)">
        <div class="order-head">
          <span>订单号：{{ o.orderNo || o.id }}</span>
          <span :style="{ color: statusMap[String(o.status)]?.color, fontWeight: 600 }">
            {{ statusMap[String(o.status)]?.label }}
          </span>
        </div>
        <div class="order-items">
          <div v-for="item in o.items" :key="item.id" class="o-item">
            <img :src="item.productImage || 'https://via.placeholder.com/60'" alt="">
            <div class="info">
              <div class="title">{{ item.productTitle }}</div>
              <div class="qty">×{{ item.quantity }}</div>
            </div>
            <div class="p">¥{{ item.price?.toFixed(2) }}</div>
          </div>
        </div>
        <div class="order-foot">
          <span style="color:#999;font-size:13px">共{{ o.totalQuantity || (o.items?.length || 0) }}件</span>
          <span>实付：<strong style="color:#ff6b35;font-size:16px">¥{{ o.totalAmount?.toFixed(2) }}</strong></span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 20px; }
.card { background: #fff; border-radius: 10px; padding: 50px; }
.empty { text-align: center; color: #999; }
.order-list { display: flex; flex-direction: column; gap: 14px; }
.order-card {
  background: #fff; border-radius: 10px; overflow: hidden;
  cursor: pointer; transition: box-shadow .2s;
}
.order-card:hover { box-shadow: 0 4px 14px rgba(0,0,0,0.06); }
.order-head {
  display: flex; justify-content: space-between;
  padding: 12px 20px; background: #fafafa;
  font-size: 13px; color: #666;
  border-bottom: 1px solid #f0f0f0;
}
.order-items { padding: 10px 20px; }
.o-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 0; border-bottom: 1px dashed #f5f5f5;
}
.o-item:last-child { border-bottom: none; }
.o-item img {
  width: 60px; height: 60px; border-radius: 6px; object-fit: cover;
}
.o-item .info { flex: 1; }
.o-item .title {
  font-size: 14px; color: #333; margin-bottom: 4px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.o-item .qty { font-size: 12px; color: #999; }
.o-item .p { color: #333; }
.order-foot {
  display: flex; justify-content: flex-end; gap: 16px; align-items: baseline;
  padding: 12px 20px; border-top: 1px solid #f0f0f0;
}
</style>
