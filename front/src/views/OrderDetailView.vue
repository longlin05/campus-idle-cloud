<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ordersApi } from '@/api'
import type { OrderVO, OrderItemVO } from '@/api'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const toast = useToast()

const order = ref<OrderVO | null>(null)
const loading = ref(true)
const loadError = ref('')
// 异步下单轮询状态
const polling = ref(false)
const pollFailReason = ref('')
let pollTimer: ReturnType<typeof setTimeout> | null = null

const POLL_INTERVAL = 1000
const POLL_MAX_TIMES = 30
const DETAIL_RETRY_DELAYS = [500, 1200, 2500]
const DETAIL_MAX_RETRIES = 3

const placeholderImage = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIgdmlld0JveD0iMCAwIDYwIDYwIj48cmVjdCBmaWxsPSIjZjBmMGYwIiB3aWR0aD0iNjAiIGhlaWdodD0iNjAiLz48dGV4dCB4PSIzMCIgeT0iMzUiIGZpbGw9IiM5OTkiIGZvbnQtc2l6ZT0iMTQiIHRleHQtYW5jaG9yPSJtaWRkbGUiPuWtmOi/hyk8dGV4dD48L3N2Zz4='

function handleImageError(e: Event) {
  const img = e.target as HTMLImageElement
  if (img && img.src !== placeholderImage) {
    img.src = placeholderImage
  }
}

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

const routeParam = String(route.params.id)
const isOrderNo = routeParam.startsWith('ORD')
const orderId = computed(() => (isOrderNo ? 0 : Number(routeParam)))
const orderNo = computed(() => (isOrderNo ? routeParam : ''))
const isBuyer = computed(() => {
  const buyerId = String((order.value as any)?.buyerId ?? (order.value as any)?.buyer?.id ?? '')
  const currentUserId = String(userStore.userInfo?.id ?? '')
  return buyerId !== '' && buyerId === currentUserId
})

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login' })
    return
  }
  if (isOrderNo) {
    startPolling()
  } else {
    loadDetailWithRetry(orderId.value)
  }
})

onUnmounted(() => {
  stopPolling()
})

/** 启动订单状态轮询 */
function startPolling() {
  polling.value = true
  loading.value = true
  loadError.value = ''
  let times = 0
  const poll = async () => {
    times++
    try {
      const res = await ordersApi.getOrderStatus(orderNo.value)
      if (res.code === 200 && res.data) {
        const status = res.data.status
        if (status === 'success' && res.data.orderId) {
          stopPolling()
          // 替换路由，便于后续刷新直接加载详情
          router.replace({ name: 'OrderDetail', params: { id: res.data.orderId } })
          await loadDetailWithRetry(res.data.orderId)
          return
        }
        if (status === 'failed') {
          stopPolling()
          pollFailReason.value = res.data.reason || '订单创建失败'
          loading.value = false
          return
        }
      }
    } catch (e) {
      console.error('[订单轮询] 查询失败', e)
    }
    if (times >= POLL_MAX_TIMES) {
      stopPolling()
      pollFailReason.value = '订单创建超时，请稍后在订单列表查看'
      loading.value = false
      return
    }
    pollTimer = setTimeout(poll, POLL_INTERVAL)
  }
  poll()
}

function stopPolling() {
  polling.value = false
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

async function loadDetailWithRetry(id: number, attempt = 0) {
  loading.value = true
  loadError.value = ''
  try {
    const res = await ordersApi.getDetail(id)
    if (res.code === 200 && res.data) {
      // 后端返回 OrderDetailVO（扁平结构），需要转换为前端 OrderVO
      order.value = normalizeOrder(res.data as any)
      loading.value = false
      return
    }
    // 后端返回错误
    loadError.value = res.message || '订单不存在'
    loading.value = false
  } catch (e: any) {
    const msg = e?.message || ''
    // 网络/超时错误：可重试
    const retryable = msg.includes('超时') || msg.includes('网络') || msg.includes('500') || attempt < DETAIL_MAX_RETRIES
    if (retryable && attempt < DETAIL_MAX_RETRIES) {
      const delay = DETAIL_RETRY_DELAYS[attempt] ?? 2500
      console.warn(`[订单详情] 第${attempt + 1}次失败，${delay}ms 后重试`, e)
      await new Promise(r => setTimeout(r, delay))
      return loadDetailWithRetry(id, attempt + 1)
    }
    loadError.value = msg || '加载订单失败'
    loading.value = false
  }
}

/** 将后端 OrderDetailVO 转换为前端模板需要的 OrderVO 结构 */
function normalizeOrder(src: any): OrderVO {
  const product = src.product || {}
  const productImage = product.imageUrl || (Array.isArray(product.images) && product.images.length > 0 ? product.images[0] : '') || ''
  const productTitle = product.title || product.name || src.productName || '商品'

  const item: OrderItemVO = {
    id: src.orderId,
    productId: product.id ?? src.productId,
    productTitle,
    productImage,
    price: Number(product.price ?? src.price ?? 0),
    quantity: Number(src.quantity ?? 1),
  }

  const orderAmount = Number(src.orderAmount ?? 0)
  const shippingFee = Number(src.shippingFee ?? 0)

  return {
    id: src.orderId,
    orderId: src.orderId,
    orderNo: src.orderNo,
    status: Number(src.status ?? 0),
    statusText: src.statusName,
    buyerId: src.buyer?.id ?? src.buyerId ?? 0,
    buyerName: src.buyer?.nickname ?? '',
    sellerId: src.seller?.id ?? src.sellerId,
    sellerName: src.seller?.nickname ?? '',
    items: [item],
    totalQuantity: Number(src.quantity ?? 1),
    totalAmount: orderAmount,
    orderAmount,
    shippingFee,
    name: src.receiverName,
    phone: src.receiverPhone,
    address: src.receiverAddress,
    receiverName: src.receiverName,
    receiverPhone: src.receiverPhone,
    receiverAddress: src.receiverAddress,
    payTime: formatDate(src.payTime),
    shipTime: formatDate(src.shipTime),
    receiveTime: formatDate(src.confirmTime),
    createTime: formatDate(src.createTime),
    trackingNo: src.trackingNo || '',
    productId: product.id ?? src.productId,
    productName: productTitle,
    productImage,
    quantity: Number(src.quantity ?? 1),
  }
}

function formatDate(d: any): string {
  if (!d) return ''
  let date: Date
  if (d instanceof Date) {
    date = d
  } else if (typeof d === 'string') {
    date = new Date(d)
    if (isNaN(date.getTime())) return d
  } else if (typeof d === 'number') {
    date = new Date(d)
  } else {
    return String(d)
  }
  try {
    if (isNaN(date.getTime())) return String(d)
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  } catch {
    return String(d)
  }
}

async function handlePay() {
  const oid = order.value?.orderId || orderId.value
  if (!oid) return
  if (!confirm('确认支付该订单？')) return
  try {
    const res = await ordersApi.pay(oid)
    if (res.code === 200) { toast.success('支付成功！'); await loadDetailWithRetry(oid) }
    else toast.error(res.message || '支付失败')
  } catch (e: any) { toast.error(e.message || '支付失败') }
}

async function handleCancel() {
  const oid = order.value?.orderId || orderId.value
  if (!oid) return
  if (!confirm('确认取消该订单？')) return
  try {
    const res = await ordersApi.cancel(oid)
    if (res.code === 200) { toast.success('取消成功'); await loadDetailWithRetry(oid) }
    else toast.error(res.message || '取消失败')
  } catch (e: any) { toast.error(e.message || '取消失败') }
}

async function handleShip() {
  if (!order.value) return
  const oid = order.value.orderId
  if (!oid) return
  const tracking = prompt('请输入快递单号（可留空）：', '')
  if (tracking === null) return
  try {
    const res = await ordersApi.ship(oid, { trackingNo: tracking || undefined })
    if (res.code === 200) { toast.success('发货成功'); await loadDetailWithRetry(oid) }
    else toast.error(res.message || '发货失败')
  } catch (e: any) { toast.error(e.message || '发货失败') }
}

async function handleReceive() {
  const oid = order.value?.orderId || orderId.value
  if (!oid) return
  if (!confirm('确认收货？')) return
  try {
    const res = await ordersApi.receive(oid)
    if (res.code === 200) { toast.success('收货成功'); await loadDetailWithRetry(oid) }
    else toast.error(res.message || '操作失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}

function handleRetryLoad() {
  const oid = order.value?.orderId || orderId.value
  if (oid) loadDetailWithRetry(oid)
}
</script>

<template>
  <div class="order-detail">
    <div class="back-bar">
      <a @click="router.push('/user-center')">← 返回个人中心</a>
    </div>

    <!-- 异步下单：处理中 -->
    <div v-if="polling" class="loading">
      <div class="polling-icon">⏳</div>
      <div class="polling-text">订单创建中，请稍候...</div>
      <div class="polling-sub">订单号：{{ orderNo }}</div>
    </div>

    <!-- 异步下单：失败/超时 -->
    <div v-else-if="pollFailReason" class="empty">
      <div class="fail-icon">⚠️</div>
      <div class="fail-text">{{ pollFailReason }}</div>
      <button class="btn-primary" style="margin-top: 16px" @click="router.push('/user-center')">返回个人中心</button>
    </div>

    <!-- 加载中 -->
    <div v-else-if="loading" class="loading">
      <div class="polling-icon">⏳</div>
      <div class="polling-text">订单加载中...</div>
    </div>

    <!-- 加载失败 -->
    <div v-else-if="loadError" class="empty">
      <div class="fail-icon">⚠️</div>
      <div class="fail-text">{{ loadError }}</div>
      <button class="btn-primary" style="margin-top: 16px" @click="handleRetryLoad">重新加载</button>
      <button class="btn-cancel" style="margin-top: 12px" @click="router.push('/user-center')">返回个人中心</button>
    </div>

    <!-- 无订单 -->
    <div v-else-if="!order" class="empty">订单不存在</div>

    <!-- 订单详情 -->
    <div v-else class="detail">
      <div class="status-bar">
        <div class="status" :style="{ color: statusMap[String(order.status)]?.color }">
          <span class="label">订单状态：</span>
          <span class="text">{{ statusMap[String(order.status)]?.label || order.statusText || '未知' }}</span>
        </div>
        <div class="actions">
          <template v-if="isBuyer">
            <button v-if="String(order.status) === '0'" class="btn-cancel" @click="handleCancel">取消订单</button>
            <button v-if="String(order.status) === '0'" class="btn-primary" @click="handlePay">立即付款</button>
            <button v-if="String(order.status) === '2'" class="btn-primary" @click="handleReceive">确认收货</button>
          </template>
          <template v-else>
            <button v-if="String(order.status) === '1'" class="btn-primary" @click="handleShip">立即发货</button>
          </template>
        </div>
      </div>

      <div class="section info-box">
        <h3>收货信息</h3>
        <div class="info-row"><span class="label">收货人</span><span>{{ order.name || order.receiverName }}</span></div>
        <div class="info-row"><span class="label">联系电话</span><span>{{ order.phone || order.receiverPhone }}</span></div>
        <div class="info-row"><span class="label">收货地址</span><span>{{ order.address || order.receiverAddress }}</span></div>
        <div class="info-row"><span class="label">下单时间</span><span>{{ order.createTime }}</span></div>
        <div class="info-row" v-if="order.payTime"><span class="label">付款时间</span><span>{{ order.payTime }}</span></div>
        <div class="info-row" v-if="order.shipTime"><span class="label">发货时间</span><span>{{ order.shipTime }}</span></div>
        <div class="info-row" v-if="order.trackingNo"><span class="label">快递单号</span><span>{{ order.trackingNo }}</span></div>
      </div>

      <div class="section items-box">
        <h3>商品信息</h3>
        <table class="items-table">
          <thead>
            <tr>
              <th>商品</th>
              <th class="price-col">单价</th>
              <th class="qty-col">数量</th>
              <th class="subtotal-col">小计</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in (order.items || [])" :key="item.id">
              <td>
                <router-link :to="`/product/${item.productId}`" class="product-link">
                  <img
                    :src="item.productImage || placeholderImage"
                    alt=""
                    @error="handleImageError"
                  >
                  <span>{{ item.productTitle }}</span>
                </router-link>
              </td>
              <td class="price-col">¥{{ (item.price ?? 0).toFixed(2) }}</td>
              <td class="qty-col">× {{ item.quantity }}</td>
              <td class="subtotal-col">¥{{ ((item.price ?? 0) * (item.quantity || 1)).toFixed(2) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="section order-summary">
        <div class="row"><span>商品金额</span><span>¥{{ (order.totalAmount ?? 0).toFixed(2) }}</span></div>
        <div class="row"><span>运费</span><span>¥{{ (order.shippingFee ?? 0).toFixed(2) }}</span></div>
        <div class="row total-row"><span>实付金额</span><span class="total-amount">¥{{ (order.totalAmount ?? 0).toFixed(2) }}</span></div>
        <div class="order-no">
          <span>订单编号：{{ order.orderNo || order.id }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.back-bar {
  margin-bottom: 16px;
}
.back-bar a {
  color: #ff6b35;
  font-size: 14px;
  cursor: pointer;
}
.loading, .empty {
  background: #fff;
  border-radius: 10px;
  padding: 60px 20px;
  text-align: center;
  color: #999;
}
.polling-icon {
  font-size: 48px;
  margin-bottom: 16px;
  animation: pulse 1.2s ease-in-out infinite;
}
.polling-text {
  font-size: 18px;
  color: #333;
  font-weight: 600;
  margin-bottom: 8px;
}
.polling-sub {
  font-size: 13px;
  color: #999;
}
.fail-icon {
  font-size: 48px;
  margin-bottom: 16px;
}
.fail-text {
  font-size: 16px;
  color: #e74c3c;
  margin-bottom: 8px;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.1); }
}
.status-bar {
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.status {
  font-size: 16px;
}
.status .text {
  font-size: 18px;
  font-weight: 700;
}
.actions {
  display: flex;
  gap: 10px;
}
.btn-primary {
  height: 38px;
  padding: 0 20px;
  background: #ff6b35;
  color: #fff;
  border-radius: 6px;
  font-size: 14px;
}
.btn-cancel {
  height: 38px;
  padding: 0 20px;
  border: 1px solid #ddd;
  background: #fff;
  color: #666;
  border-radius: 6px;
  font-size: 14px;
}
.section {
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 16px;
}
.section h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}
.info-box .info-row {
  display: flex;
  padding: 8px 0;
  font-size: 14px;
}
.info-box .label {
  width: 100px;
  color: #999;
  flex-shrink: 0;
}
.items-table {
  width: 100%;
  border-collapse: collapse;
}
.items-table th, .items-table td {
  padding: 14px;
  text-align: left;
  font-size: 14px;
}
.items-table thead {
  background: #fafafa;
}
.items-table th {
  color: #666;
  font-weight: 600;
  border-bottom: 1px solid #f0f0f0;
}
.price-col, .qty-col, .subtotal-col {
  width: 100px;
}
.subtotal-col {
  color: #ff6b35;
  font-weight: 600;
}
.product-link {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #333;
}
.product-link:hover {
  color: #ff6b35;
}
.product-link img {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  object-fit: cover;
}
.order-summary .row {
  display: flex;
  justify-content: flex-end;
  padding: 6px 0;
  font-size: 14px;
  color: #666;
}
.order-summary .row span:last-child {
  width: 120px;
  text-align: right;
}
.order-summary .total-row {
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  margin-top: 8px;
}
.total-amount {
  font-size: 22px;
  color: #ff6b35;
  font-weight: 700;
}
.order-no {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
  color: #999;
  font-size: 13px;
  text-align: right;
}
</style>
