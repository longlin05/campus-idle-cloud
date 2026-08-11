<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi, favoriteApi, ordersApi, userApi, shoppingCartApi } from '@/api'
import type { ProductVO } from '@/api'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const toast = useToast()

const product = ref<ProductVO | null>(null)
const loading = ref(true)
const isFavorite = ref(false)
const showBuyDialog = ref(false)
const quantity = ref(1)
const submitting = ref(false)
const activeImageIndex = ref(0)

/* =================== 地址选择相关 =================== */
interface AddressVO {
  id: number
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  province?: string
  city?: string
  district?: string
  detailAddress?: string
  isDefault: number
}

const addresses = ref<AddressVO[]>([])
const selectedAddressId = ref<number | null>(null)
const loadingAddresses = ref(false)
const showAddressDialog = ref(false)
const editingAddressId = ref<number | null>(null)
const addressForm = ref({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  isDefault: 0 as 0 | 1,
})
const savingAddress = ref(false)

const productId = computed(() => Number(route.params.id))
const isLoggedIn = computed(() => userStore.isLoggedIn)
const isOwner = computed(() => product.value?.sellerId === userStore.userInfo?.id)

const mainImage = computed(() => product.value?.images?.[activeImageIndex.value] || product.value?.images?.[0] || 'https://via.placeholder.com/600x600?text=No+Image')

const selectedAddress = computed<AddressVO | null>(() => {
  if (!selectedAddressId.value) return null
  return addresses.value.find(a => a.id === selectedAddressId.value) || null
})

onMounted(async () => {
  try {
    await Promise.all([loadDetail(), checkFavorite()])
    if (product.value) {
      productApi.incrementViewCount(productId.value).catch(() => {})
    }
  } finally {
    loading.value = false
  }
})

async function loadDetail() {
  try {
    const res = await productApi.getDetail(productId.value)
    if (res.code === 200) {
      product.value = res.data
    }
  } catch (e) {
    console.error('加载商品详情失败', e)
  }
}

async function checkFavorite() {
  if (!isLoggedIn.value) return
  try {
    const res = await favoriteApi.check(productId.value)
    if (res.code === 200) {
      isFavorite.value = !!res.data
    }
  } catch (e) {
    console.error('检查收藏失败', e)
  }
}

async function toggleFavorite() {
  if (!isLoggedIn.value) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  const wasFavorite = isFavorite.value
  try {
    if (wasFavorite) {
      await favoriteApi.remove(productId.value)
      isFavorite.value = false
      toast.success('已取消收藏')
    } else {
      await favoriteApi.add(productId.value)
      isFavorite.value = true
      toast.success('已添加到我的收藏')
    }
  } catch (e: any) {
    isFavorite.value = wasFavorite
    console.error('收藏操作失败', e)
    toast.error(e?.message || (wasFavorite ? '取消收藏失败' : '添加收藏失败'))
  }
}

async function openBuyDialog() {
  if (!isLoggedIn.value) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  if (isOwner.value) {
    toast.warning('不能购买自己发布的商品')
    return
  }
  if (product.value && product.value.status !== 1) {
    toast.warning('该商品不可购买')
    return
  }
  // 跳转到统一的订单确认页，避免两处维护地址选择
  router.push({
    name: 'OrderConfirm',
    query: {
      source: 'direct',
      productId: String(productId.value),
      title: product.value?.title || '',
      image: (product.value?.images && product.value.images[0]) || product.value?.imageUrl || '',
      price: String(product.value?.price || 0),
      quantity: String(quantity.value),
    },
  })
}

async function handleAddToCart() {
  if (!isLoggedIn.value) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  if (isOwner.value) {
    toast.warning('不能添加自己发布的商品到购物车')
    return
  }
  if (product.value && product.value.status !== 1) {
    toast.warning('该商品不可购买')
    return
  }
  try {
    const res = await shoppingCartApi.add(productId.value, 1)
    if (res.code === 200) {
      toast.success('已加入购物车')
    } else {
      toast.error(res.message || '加入购物车失败')
    }
  } catch (e: any) {
    toast.error(e.message || '加入购物车失败')
  }
}

/* =================== 地址列表加载 =================== */
async function loadAddresses() {
  loadingAddresses.value = true
  try {
    const res = await userApi.getAddresses()
    if (res.code === 200) {
      addresses.value = (res.data as AddressVO[]) || []
      // 默认选中默认地址或第一个
      if (!selectedAddressId.value || !addresses.value.find(a => a.id === selectedAddressId.value)) {
        const def = addresses.value.find(a => a.isDefault === 1) || addresses.value[0]
        selectedAddressId.value = def ? def.id : null
      }
    }
  } catch (e: any) {
    console.error('[地址列表] 加载失败:', e)
    addresses.value = []
    selectedAddressId.value = null
    toast.warning(e?.message || '加载地址列表失败')
  } finally {
    loadingAddresses.value = false
  }
}

function selectAddress(id: number) {
  selectedAddressId.value = id
}

/* =================== 新增/编辑地址弹窗 =================== */
function openAddressDialog(addressId?: number) {
  editingAddressId.value = addressId ? Number(addressId) : null
  if (addressId) {
    const addr = addresses.value.find(a => a.id === addressId)
    if (addr) {
      addressForm.value = {
        receiverName: addr.receiverName || '',
        receiverPhone: addr.receiverPhone || '',
        receiverAddress: addr.receiverAddress || addr.detailAddress || '',
        isDefault: addr.isDefault === 1 ? 1 : 0,
      }
    }
  } else {
    addressForm.value = { receiverName: '', receiverPhone: '', receiverAddress: '', isDefault: 0 }
  }
  showAddressDialog.value = true
}

function closeAddressDialog() {
  showAddressDialog.value = false
  editingAddressId.value = null
}

async function saveAddress() {
  if (!addressForm.value.receiverName.trim()) {
    toast.warning('请输入收货人姓名')
    return
  }
  if (!addressForm.value.receiverPhone.trim()) {
    toast.warning('请输入联系电话')
    return
  }
  if (!addressForm.value.receiverAddress.trim()) {
    toast.warning('请输入收货地址')
    return
  }

  savingAddress.value = true
  try {
    const payload = {
      receiverName: addressForm.value.receiverName.trim(),
      receiverPhone: addressForm.value.receiverPhone.trim(),
      receiverAddress: addressForm.value.receiverAddress.trim(),
      isDefault: addressForm.value.isDefault,
    }
    const res = editingAddressId.value
      ? await userApi.updateAddress(editingAddressId.value, payload)
      : await userApi.addAddress(payload)
    if (res.code === 200) {
      toast.success('保存成功')
      closeAddressDialog()
      await loadAddresses()
      // 新增或设置默认时自动选中
      if (!selectedAddressId.value) {
        const newList = addresses.value
        if (newList.length > 0) {
          const def = newList.find(a => a.isDefault === 1) || newList[0]!
          selectedAddressId.value = def.id
        }
      }
    } else {
      toast.error(res.message || '保存失败')
    }
  } catch (e: any) {
    console.error('[保存地址] 失败:', e)
    toast.error(e?.message || '保存失败')
  } finally {
    savingAddress.value = false
  }
}

/* =================== 提交订单 =================== */
async function handleBuy() {
  if (!selectedAddress.value) {
    toast.warning('请选择收货地址')
    return
  }
  const addr = selectedAddress.value
  const receiverInfo = {
    name: addr.receiverName,
    phone: addr.receiverPhone,
    address: addr.receiverAddress || addr.detailAddress || (addr.province || '') + (addr.city || '') + (addr.district || '') + (addr.detailAddress || ''),
  }
  submitting.value = true
  try {
    const res = await ordersApi.createDirect(productId.value, quantity.value, receiverInfo)
    if (res.code === 200) {
      showBuyDialog.value = false
      const data = res.data as any
      const orderId = data?.id || data?.orderId || data?.orderNo
      // 下单成功：如果是异步创建，有 orderNo 就用轮询；否则直接跳详情
      if (data?.orderNo && !data?.id) {
        toast.success('订单创建中，即将跳转...')
        // 简化：直接按 orderNo 跳详情页（路由兼容）
        router.push({ name: 'OrderDetail', params: { id: data.orderNo } })
      } else if (orderId) {
        toast.success('订单提交成功')
        router.push({ name: 'OrderDetail', params: { id: orderId } })
      } else {
        toast.success('订单提交成功')
        router.push({ name: 'OrderList' })
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
  <div class="product-detail">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="!product" class="empty">商品不存在或已下架</div>
    <div v-else class="detail-container">
      <div class="product-main">
        <div class="images-section">
          <div class="main-image">
            <img :src="mainImage" :alt="product.title">
          </div>
          <div v-if="product.images && product.images.length > 1" class="thumbs">
            <div
              v-for="(img, i) in product.images"
              :key="i"
              class="thumb"
              :class="{ active: i === activeImageIndex }"
              @click="activeImageIndex = i"
            >
              <img :src="img" alt="">
            </div>
          </div>
        </div>

        <div class="info-section">
          <h1 class="title">{{ product.title }}</h1>
          <div class="meta">
            <span class="view-count">👁 浏览 {{ product.viewCount || 0 }}</span>
            <span class="publish-time">发布于 {{ product.createTime }}</span>
          </div>

          <div class="price-box">
            <span class="label">价格</span>
            <span class="currency">¥</span>
            <span class="current">{{ (product.price ?? 0).toFixed(2) }}</span>
            <span v-if="product.originalPrice != null && product.originalPrice > product.price" class="original">
              原价 ¥{{ product.originalPrice.toFixed(2) }}
            </span>
          </div>

          <div v-if="product.categoryName" class="info-row">
            <span class="label">分类</span>
            <span class="value">{{ product.categoryName }}</span>
          </div>

          <div v-if="product.quantity != null && product.quantity > 0" class="info-row">
            <span class="label">库存</span>
            <span class="value">{{ product.quantity }} 件</span>
          </div>

          <div class="info-row seller">
            <span class="label">卖家</span>
            <router-link :to="`/user-home/${product.sellerId}`" class="seller-info">
              <img
                :src="product.sellerAvatar || 'https://api.dicebear.com/7.x/initials/svg?seed=' + (product.sellerName || 'user')"
                alt=""
                class="avatar"
                @error="($event.target as HTMLImageElement).src = 'https://api.dicebear.com/7.x/initials/svg?seed=' + (product.sellerName || 'user')"
              >
              <span>{{ product.sellerName || '未知用户' }}</span>
            </router-link>
          </div>

          <div class="status-box" :class="'status-' + product.status">
            <span v-if="product.status === 1" class="tag available">在架可售</span>
            <span v-else-if="product.status === 0" class="tag sold">已售罄</span>
            <span v-else class="tag off">已下架</span>
          </div>

          <div class="actions">
            <button
              v-if="!isOwner && product.status === 1"
              class="btn-cart"
              @click="handleAddToCart"
            >🛒 加入购物车</button>
            <button
              v-if="!isOwner && product.status === 1"
              class="btn-buy"
              @click="openBuyDialog"
            >⚡ 立即购买</button>
            <button
              class="btn-favorite"
              :class="{ active: isFavorite }"
              @click="toggleFavorite"
            >
              {{ isFavorite ? '❤️ 已收藏' : '🤍 收藏' }}
            </button>
            <router-link
              v-if="product.status === 1 && !isOwner"
              :to="`/chat/${product.sellerId}`"
              class="btn-chat"
            >💬 联系卖家</router-link>
          </div>
        </div>
      </div>

      <div class="description-section">
        <h3>商品描述</h3>
        <div class="desc-content">{{ product.description || '卖家很懒，没有留下描述~' }}</div>
      </div>
    </div>

    <!-- ========== 确认下单对话框（地址选择版） ========== -->
    <div v-if="showBuyDialog" class="dialog-mask" @click.self="showBuyDialog = false">
      <div class="dialog">
        <h3>确认下单</h3>
        <div class="dialog-body">
          <div class="order-product">
            <img :src="mainImage" alt="">
            <div class="info">
              <div class="title">{{ product?.title }}</div>
              <div class="price">¥{{ product?.price?.toFixed(2) }} × {{ quantity }}</div>
            </div>
          </div>

          <div class="form-group">
            <label>数量</label>
            <input v-model.number="quantity" type="number" min="1" max="99">
          </div>

          <!-- 地址选择区 -->
          <div class="address-section">
            <div class="section-header">
              <span class="section-title">收货地址</span>
              <button class="btn-add-addr" type="button" @click="openAddressDialog()">
                + 添加地址
              </button>
            </div>

            <div v-if="loadingAddresses" class="address-loading">加载中...</div>
            <div v-else-if="addresses.length === 0" class="address-empty">
              <p>暂无收货地址</p>
              <button class="btn-add-addr-primary" type="button" @click="openAddressDialog()">
                + 新增收货地址
              </button>
            </div>
            <div v-else class="address-list">
              <div
                v-for="addr in addresses"
                :key="addr.id"
                class="address-item"
                :class="{ selected: selectedAddressId === addr.id }"
                @click="selectAddress(addr.id)"
              >
                <input
                  type="radio"
                  :value="addr.id"
                  v-model="selectedAddressId"
                  class="addr-radio"
                >
                <div class="addr-content">
                  <div class="addr-name">
                    {{ addr.receiverName }}
                    <span class="addr-phone">{{ addr.receiverPhone }}</span>
                    <span v-if="addr.isDefault === 1" class="default-tag">默认</span>
                  </div>
                  <div class="addr-detail">
                    {{ addr.receiverAddress || (addr.province || '') + (addr.city || '') + (addr.district || '') + (addr.detailAddress || '') }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="total">
            合计：<strong>¥{{ ((product?.price ?? 0) * quantity).toFixed(2) }}</strong>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-cancel" @click="showBuyDialog = false">取消</button>
          <button class="btn-submit" :disabled="submitting" @click="handleBuy">
            {{ submitting ? '提交中...' : '提交订单' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ========== 新增/编辑地址对话框 ========== -->
    <div v-if="showAddressDialog" class="dialog-mask" @click.self="closeAddressDialog">
      <div class="dialog small">
        <h3>{{ editingAddressId ? '编辑地址' : '新增地址' }}</h3>
        <div class="dialog-body">
          <div class="form-group">
            <label>收货人</label>
            <input v-model="addressForm.receiverName" type="text" placeholder="请输入收货人姓名">
          </div>
          <div class="form-group">
            <label>联系电话</label>
            <input v-model="addressForm.receiverPhone" type="tel" placeholder="请输入手机号">
          </div>
          <div class="form-group">
            <label>详细地址</label>
            <textarea v-model="addressForm.receiverAddress" rows="3" placeholder="请输入详细收货地址"></textarea>
          </div>
          <label class="default-check">
            <input v-model="addressForm.isDefault" :true-value="1" :false-value="0" type="checkbox">
            设为默认地址
          </label>
        </div>
        <div class="dialog-footer">
          <button class="btn-cancel" @click="closeAddressDialog">取消</button>
          <button class="btn-submit" :disabled="savingAddress" @click="saveAddress">
            {{ savingAddress ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.loading, .empty {
  text-align: center;
  padding: 80px 20px;
  background: #fff;
  border-radius: 10px;
  color: #999;
}

.detail-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.product-main {
  display: flex;
  gap: 28px;
  background: #fff;
  border-radius: 10px;
  padding: 24px;
}

.images-section {
  width: 480px;
  flex-shrink: 0;
}

.main-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f5f5f5;
  border-radius: 10px;
  overflow: hidden;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbs {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.thumb {
  width: 76px;
  height: 76px;
  border: 2px solid #eee;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
}

.thumb.active {
  border-color: #ff6b35;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-section {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.title {
  font-size: 22px;
  font-weight: 600;
  color: #222;
  line-height: 1.4;
  margin-bottom: 12px;
}

.meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #999;
  margin-bottom: 20px;
}

.price-box {
  background: #fff6f2;
  padding: 18px 20px;
  border-radius: 8px;
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 20px;
}

.price-box .label {
  font-size: 13px;
  color: #666;
}

.price-box .currency {
  font-size: 16px;
  color: #ff6b35;
  font-weight: 600;
}

.price-box .current {
  font-size: 30px;
  color: #ff6b35;
  font-weight: 700;
}

.price-box .original {
  font-size: 13px;
  color: #aaa;
  text-decoration: line-through;
}

.info-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed #f0f0f0;
}

.info-row .label {
  width: 60px;
  font-size: 13px;
  color: #999;
}

.info-row .value {
  color: #333;
  font-size: 14px;
}

.seller-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #333;
}

.seller-info:hover {
  color: #ff6b35;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
}

.status-box {
  padding: 16px 0;
}

.tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
}

.tag.available {
  background: #e8f7ea;
  color: #27ae60;
}

.tag.sold, .tag.off {
  background: #f0f0f0;
  color: #999;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: auto;
  padding-top: 20px;
}

.btn-buy, .btn-cart, .btn-favorite, .btn-chat {
  height: 46px;
  padding: 0 28px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-cart {
  background: #fff;
  border: 1px solid #ff6b35;
  color: #ff6b35;
}

.btn-cart:hover {
  background: #fff6f2;
}

.btn-buy {
  background: linear-gradient(90deg, #ff6b35 0%, #f7931e 100%);
  color: #fff;
}

.btn-favorite {
  background: #fff;
  border: 1px solid #ddd;
  color: #555;
}

.btn-favorite.active {
  border-color: #ff6b35;
  color: #ff6b35;
  background: #fff6f2;
}

.btn-chat {
  background: #fff;
  border: 1px solid #ff6b35;
  color: #ff6b35;
}

.description-section {
  background: #fff;
  border-radius: 10px;
  padding: 24px;
}

.description-section h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.desc-content {
  color: #555;
  line-height: 1.8;
  font-size: 14px;
  white-space: pre-wrap;
}

/* ========== 通用对话框 ========== */
.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  width: 460px;
  max-width: 92vw;
  max-height: 90vh;
  overflow-y: auto;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.dialog.small {
  width: 420px;
}

.dialog h3 {
  font-size: 18px;
  margin-bottom: 16px;
}

.order-product {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 16px;
}

.order-product img {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  object-fit: cover;
}

.order-product .title {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.order-product .price {
  font-size: 13px;
  color: #ff6b35;
  font-weight: 600;
}

.form-group {
  margin-bottom: 14px;
}

.form-group label {
  display: block;
  font-size: 13px;
  color: #555;
  margin-bottom: 6px;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
  font-family: inherit;
}

/* ========== 地址选择区 ========== */
.address-section {
  margin: 16px 0 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.btn-add-addr {
  background: none;
  border: none;
  color: #ff6b35;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.btn-add-addr:hover { background: #fff3ee; }

.btn-add-addr-primary {
  display: block;
  margin: 12px auto 0;
  height: 40px;
  padding: 0 20px;
  border-radius: 6px;
  background: #ff6b35;
  color: #fff;
  font-weight: 500;
  font-size: 14px;
  border: none;
  cursor: pointer;
}

.address-loading,
.address-empty {
  text-align: center;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
  color: #999;
  font-size: 14px;
}
.address-empty p { margin: 0; }

.address-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 240px;
  overflow-y: auto;
}

.address-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  border: 2px solid #eaeaea;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.address-item:hover { border-color: #ffd2be; }
.address-item.selected {
  border-color: #ff6b35;
  background: #fff6f2;
}

.addr-radio {
  margin-top: 4px;
  accent-color: #ff6b35;
}

.addr-content { flex: 1; min-width: 0; }

.addr-name {
  font-size: 14px;
  font-weight: 600;
  color: #222;
  margin-bottom: 4px;
}

.addr-phone {
  margin-left: 8px;
  font-weight: 400;
  color: #666;
  font-size: 13px;
}

.default-tag {
  display: inline-block;
  margin-left: 8px;
  padding: 1px 6px;
  font-size: 11px;
  font-weight: 500;
  color: #fff;
  background: #ff6b35;
  border-radius: 4px;
}

.addr-detail {
  font-size: 13px;
  color: #888;
  line-height: 1.5;
}

/* 默认地址 checkbox */
.default-check {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #555;
  cursor: pointer;
}
.default-check input { accent-color: #ff6b35; }

.total {
  text-align: right;
  font-size: 14px;
  color: #666;
  margin: 16px 0;
}

.total strong {
  font-size: 20px;
  color: #ff6b35;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-cancel {
  height: 40px;
  padding: 0 20px;
  border-radius: 6px;
  background: #f5f5f5;
  color: #666;
  border: none;
  font-size: 14px;
  cursor: pointer;
}

.btn-submit {
  height: 40px;
  padding: 0 24px;
  border-radius: 6px;
  background: #ff6b35;
  color: #fff;
  font-weight: 600;
  border: none;
  font-size: 14px;
  cursor: pointer;
}

.btn-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
