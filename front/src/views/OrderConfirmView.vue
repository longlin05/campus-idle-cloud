<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ordersApi, userApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const toast = useToast()

interface Address {
  id: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault?: number
}
interface Item { productId: number; title: string; image?: string; price: number; quantity: number }

const addresses = ref<Address[]>([])
const selectedAddrId = ref<number | null>(null)
const remark = ref('')
const items = ref<Item[]>([])
const submitting = ref(false)

function fullAddress(a: Address) {
  return `${a.province || ''}${a.city || ''}${a.district || ''}${a.detail || ''}`
}

const selectedAddr = computed<Address | undefined>(() =>
  addresses.value.find(a => a.id === selectedAddrId.value)
)
const totalQty = computed(() => items.value.reduce((s, i) => s + i.quantity, 0))
const totalAmount = computed(() =>
  items.value.reduce((s, i) => s + (i.price || 0) * (i.quantity || 1), 0)
)

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  await loadAddresses()
  buildItemsFromQuery()
})

async function loadAddresses() {
  try {
    const res = await userApi.getAddresses()
    if (res.code === 200) {
      addresses.value = res.data || []
      const def = addresses.value.find(a => a.isDefault === 1) || addresses.value[0]
      if (def) selectedAddrId.value = def.id
    }
  } catch (e) { console.error(e) }
}

function buildItemsFromQuery() {
  try {
    const source = route.query.source as string
    // 来源1：购物车（序列化的 items 数组）
    if (source === 'cart' && route.query.items) {
      const parsed = JSON.parse(decodeURIComponent(route.query.items as string))
      if (Array.isArray(parsed) && parsed.length > 0) {
        items.value = parsed.map((it: any) => ({
          productId: Number(it.productId),
          title: it.title || '商品',
          image: it.image || undefined,
          price: Number(it.price) || 0,
          quantity: Number(it.quantity) || 1,
        }))
        return
      }
    }
    // 来源2：直接购买（单个 productId + quantity）
    if (route.query.productId) {
      items.value = [{
        productId: Number(route.query.productId),
        title: (route.query.title as string) || '商品',
        image: (route.query.image as string) || undefined,
        price: Number(route.query.price) || 0,
        quantity: Number(route.query.quantity) || 1,
      }]
      return
    }
    // 兜底：空列表提示
    items.value = []
  } catch (e) { console.error('解析结算商品失败', e); items.value = [] }
}

async function submitOrder() {
  if (!selectedAddr.value) { toast.warning('请选择收货地址'); return }
  if (items.value.length === 0) { toast.warning('没有结算商品'); return }
  submitting.value = true
  try {
    const receiverInfo = {
      name: selectedAddr.value!.receiverName,
      phone: selectedAddr.value!.receiverPhone,
      address: fullAddress(selectedAddr.value!),
    }
    let res
    // 单商品走 createDirect，多商品走 createFromCart
    if (items.value.length === 1) {
      const it = items.value[0]!
      res = await ordersApi.createDirect(it.productId, it.quantity, receiverInfo)
    } else {
      res = await ordersApi.createFromCart({
        productIds: items.value.map(i => i.productId),
        quantities: items.value.map(i => i.quantity),
        remark: remark.value,
        ...receiverInfo,
      })
    }
    if (res.code === 200) {
      const data = (res.data as any)
      const orderId = data?.id || data?.orderId
      const orderNo = data?.orderNo
      const orderIds = data?.orderIds
      // 优先使用 id/orderId（同步下单已直接落库），没有 id 时才用 orderNo 轮询
      if (orderId) {
        toast.success('下单成功')
        router.replace({ name: 'OrderDetail', params: { id: orderId } })
      } else if (orderNo) {
        toast.success('订单创建中，即将跳转...')
        router.replace({ name: 'OrderDetail', params: { id: orderNo } })
      } else if (orderIds && orderIds.length > 0) {
        // 多订单：跳转第一个订单详情
        toast.success(`下单成功，共 ${orderIds.length} 个订单`)
        router.replace({ name: 'OrderDetail', params: { id: orderIds[0] } })
      } else {
        toast.success('下单成功')
        router.replace('/user-center')
      }
    } else {
      toast.error(res.message || '下单失败')
    }
  } catch (e: any) {
    toast.error(e.message || '下单失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <h1 class="page-title">🛒 确认订单</h1>

    <div class="card">
      <h3>📮 收货信息</h3>
      <div v-if="addresses.length === 0" class="tip-warn">
        还没有收货地址，<router-link to="/addresses" style="color:#ff6b35">去添加</router-link>
      </div>
      <div v-else class="addr-list">
        <label
          v-for="a in addresses"
          :key="a.id"
          class="addr-item"
          :class="{ active: a.id === selectedAddrId }"
        >
          <input
            type="radio"
            class="addr-radio"
            :value="a.id"
            v-model="selectedAddrId"
          >
          <div class="info">
            <div class="top">
              <span class="name">{{ a.receiverName }}</span>
              <span class="phone">{{ a.receiverPhone }}</span>
              <span v-if="a.isDefault === 1" class="tag">默认</span>
            </div>
            <div class="address">{{ fullAddress(a) }}</div>
          </div>
        </label>
      </div>
      <router-link to="/addresses" class="manage-addr">管理地址 ›</router-link>
    </div>

    <div class="card">
      <h3>🛍️ 商品清单</h3>
      <div class="items-list">
        <div v-if="items.length === 0" class="empty-tip">没有要结算的商品</div>
        <div v-for="item in items" :key="item.productId" class="i-row">
          <img :src="item.image || 'https://via.placeholder.com/72'" alt="">
          <div class="info">{{ item.title }}</div>
          <div class="price">¥{{ item.price?.toFixed(2) }}</div>
          <div class="qty">×{{ item.quantity }}</div>
          <div class="sub">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
        </div>
      </div>
    </div>

    <div class="card">
      <h3>📝 订单备注</h3>
      <textarea
        v-model="remark"
        rows="2"
        placeholder="有什么想对卖家说的？（选填）"
        class="remark"
        maxlength="200"
      ></textarea>
    </div>

    <div class="summary-card">
      <div class="row"><span>商品数量</span><span>{{ totalQty }} 件</span></div>
      <div class="row total">
        <span>应付金额</span>
        <span class="price">¥{{ totalAmount.toFixed(2) }}</span>
      </div>
      <button class="btn-submit" :disabled="submitting || !selectedAddr" @click="submitOrder">
        {{ submitting ? '提交中...' : '提交订单' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.page { padding-bottom: 40px; }
.page-title { font-size: 22px; margin-bottom: 20px; }
.card {
  background: #fff; border-radius: 10px; padding: 20px 24px;
  margin-bottom: 16px;
}
.card h3 {
  font-size: 16px; color: #333; margin-bottom: 16px;
  padding-bottom: 10px; border-bottom: 1px solid #f0f0f0;
}
.tip-warn {
  padding: 12px; background: #fff6e5;
  border-radius: 6px; color: #c47e00; font-size: 13px;
}
.addr-list { display: flex; flex-direction: column; gap: 10px; }
.addr-item {
  padding: 14px; border: 2px solid #eee; border-radius: 8px;
  display: flex; gap: 12px; cursor: pointer;
  transition: border-color .15s, background .15s;
  align-items: flex-start;
}
.addr-item.active {
  border-color: #ff6b35; background: #fffaf7;
}
.addr-radio {
  width: 18px; height: 18px;
  margin-top: 4px;
  accent-color: #ff6b35;
  cursor: pointer;
  flex-shrink: 0;
}
.addr-item .info { flex: 1; }
.addr-item .top { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.addr-item .name { font-weight: 600; color: #333; }
.addr-item .phone { color: #666; }
.addr-item .tag {
  font-size: 11px; padding: 1px 6px;
  background: #ff6b35; color: #fff; border-radius: 3px;
}
.addr-item .address { font-size: 13px; color: #666; }
.manage-addr {
  display: inline-block; margin-top: 12px;
  font-size: 13px; color: #ff6b35;
}
.items-list { display: flex; flex-direction: column; }
.i-row {
  display: grid;
  grid-template-columns: 72px 1fr 100px 60px 100px;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 14px;
}
.i-row:last-child { border-bottom: none; }
.i-row img {
  width: 72px; height: 72px; border-radius: 6px; object-fit: cover;
}
.i-row .price { color: #666; }
.i-row .qty { color: #999; text-align: center; }
.i-row .sub { color: #ff6b35; font-weight: 600; text-align: right; }
.empty-tip { padding: 30px; text-align: center; color: #999; }
.remark {
  width: 100%; padding: 10px 14px;
  border: 1px solid #ddd; border-radius: 6px; font-size: 13px;
  resize: none;
}
.summary-card {
  background: #fff; border-radius: 10px; padding: 20px 24px;
}
.summary-card .row {
  display: flex; justify-content: flex-end; align-items: baseline;
  padding: 6px 0; font-size: 14px; color: #666;
}
.summary-card .row span:last-child {
  width: 160px; text-align: right;
}
.summary-card .row.total {
  padding-top: 14px; border-top: 1px solid #f0f0f0;
  margin-top: 8px;
}
.summary-card .row.total .price {
  font-size: 22px; color: #ff6b35; font-weight: 700;
}
.btn-submit {
  width: 100%; height: 46px; margin-top: 18px;
  background: linear-gradient(90deg, #ff6b35 0%, #f7931e 100%);
  color: #fff; border-radius: 8px; font-size: 16px; font-weight: 600;
}
.btn-submit:disabled { opacity: .7; cursor: not-allowed; }
</style>
