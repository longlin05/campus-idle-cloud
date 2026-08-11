<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ordersApi } from '@/api'
import type { OrderVO, OrderItemVO } from '@/api'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const userStore = useUserStore()
const toast = useToast()

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

const soldOrders = computed(() => orders.value)

async function handleShip(o: OrderVO) {
  const t = prompt('请输入快递单号（可选）')
  if (t === null) return
  const oid = o.orderId || o.id
  if (!oid) return
  try {
    const res = await ordersApi.ship(oid, { trackingNo: t || undefined })
    if (res.code === 200) { toast.success('发货成功'); load() }
    else toast.error(res.message || '操作失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}

async function load() {
  loading.value = true
  try {
    const res = await ordersApi.getSellList()
    if (res.code === 200) {
      const list = res.data || []
      orders.value = list.map(normalizeOrder)
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: '/orders-sell' } })
    return
  }
  load()
})
</script>

<template>
  <div class="page">
    <h1 class="page-title">💼 我卖的</h1>
    <div v-if="loading" class="card empty">加载中...</div>
    <div v-else-if="soldOrders.length === 0" class="card empty">
      还没有卖出记录
    </div>
    <div v-else class="order-list">
      <div v-for="o in soldOrders" :key="o.id" class="order-card">
        <div class="order-head">
          <span>订单号：{{ o.orderNo || o.id }}</span>
          <div>
            <span :style="{ color: statusMap[String(o.status)]?.color, fontWeight: 600, marginRight: '10px' }">
              {{ statusMap[String(o.status)]?.label }}
            </span>
            <button v-if="String(o.status) === '1'" class="btn-ship" @click.stop="handleShip(o)">立即发货</button>
            <button class="btn-detail" @click="router.push(`/order/${o.id}`)">详情</button>
          </div>
        </div>
        <div class="buyer-info">
          👤 买家：用户{{ o.buyerId }}
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
.btn-ship, .btn-detail {
  height: 30px; padding: 0 12px; border-radius: 4px; font-size: 12px;
  margin-left: 6px;
}
.btn-ship { background: #27ae60; color: #fff; }
.btn-detail { background: transparent; border: 1px solid #ddd; color: #666; }
.order-list { display: flex; flex-direction: column; gap: 14px; }
.order-card {
  background: #fff; border-radius: 10px; overflow: hidden;
  cursor: pointer;
}
.order-head {
  display: flex; justify-content: space-between;
  padding: 12px 20px; background: #fafafa;
  font-size: 13px; color: #666;
  border-bottom: 1px solid #f0f0f0;
  align-items: center;
}
.buyer-info {
  padding: 10px 20px; font-size: 13px; color: #555;
  background: #fffbf7;
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
