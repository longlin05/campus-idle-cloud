<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { productApi, userApi, followApi } from '@/api'
import type { ProductVO } from '@/api'
import ProductCard from '@/components/ProductCard.vue'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const userStore = useUserStore()
const toast = useToast()
const userId = computed(() => Number(route.params.userId))

const userInfo = ref<any>(null)
const products = ref<ProductVO[]>([])
const loading = ref(true)
const isFollowed = ref(false)
const totalProducts = ref(0)

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'onsale', label: '在售' },
  { key: 'sold', label: '已售' },
]
const activeTab = ref('all')

const filteredProducts = computed(() => {
  if (activeTab.value === 'all') return products.value
  // status 1 = 在架可售 / 0 = 已下架（已售/下架）
  if (activeTab.value === 'onsale') return products.value.filter(p => (p.status ?? 1) === 1)
  if (activeTab.value === 'sold') return products.value.filter(p => (p.status ?? 1) !== 1)
  return products.value
})

// 首次挂载
onMounted(load)

// 监听路由参数变化（从 /user-home/1 点到 /user-home/2）
watch(() => route.params.userId, async (newVal, oldVal) => {
  if (newVal !== oldVal) await load()
})

async function load() {
  loading.value = true
  try {
    const [userRes, prodRes] = await Promise.all([
      userApi.getUserHome(userId.value),
      productApi.getBySeller(userId.value, 1, 50),
    ])
    userInfo.value = userRes.data || {}
    isFollowed.value = userInfo.value?.followedByMe ?? false

    // 后端返回的是 PageResult 结构 { records: [...], total, ... }
    if (prodRes.data && Array.isArray((prodRes.data as any).records)) {
      products.value = (prodRes.data as any).records || []
      totalProducts.value = (prodRes.data as any).total ?? products.value.length
    } else if (Array.isArray(prodRes.data)) {
      // 兼容旧接口
      products.value = prodRes.data as any
      totalProducts.value = products.value.length
    } else {
      products.value = []
      totalProducts.value = 0
    }
  } catch (e) {
    console.error('[UserHome] 加载失败:', e)
    products.value = []
    totalProducts.value = 0
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  if (!userStore.isLoggedIn) {
    toast.warning('请先登录后再关注')
    return
  }
  try {
    if (isFollowed.value) {
      await followApi.unfollow(userId.value)
    } else {
      await followApi.follow(userId.value)
    }
    isFollowed.value = !isFollowed.value
  } catch (e: any) {
    toast.error((e && e.message) || '操作失败')
  }
}

const statsOnSaleCount = computed(() => products.value.filter(p => (p.status ?? 1) === 1).length)
const statsSoldCount = computed(() => products.value.filter(p => (p.status ?? 1) !== 1).length)
</script>

<template>
  <div class="page">
    <div v-if="loading" class="card empty">加载中...</div>
    <template v-else>
      <!-- 用户资料卡 -->
      <div class="profile-card">
        <div class="avatar-wrap">
          <img
            :src="userInfo?.avatar || 'https://api.dicebear.com/7.x/initials/svg?seed=' + (userInfo?.nickname || userId)"
            class="big-avatar"
            @error="($event.target as HTMLImageElement).src = 'https://api.dicebear.com/7.x/initials/svg?seed=' + (userInfo?.nickname || userId)"
          >
        </div>
        <div class="info">
          <h2 class="name">{{ userInfo?.nickname || userInfo?.sellerName || ('用户' + userId) }}</h2>
          <div class="stats">
            <span>📦 在售：{{ statsOnSaleCount }}</span>
            <span>🏷️ 已售：{{ statsSoldCount }}</span>
            <span>👥 粉丝：{{ userInfo?.fansCount || 0 }}</span>
            <span>❤️ 关注：{{ userInfo?.followCount || 0 }}</span>
          </div>
          <div class="meta-row" v-if="userInfo?.phone">
            📱 {{ userInfo.phone }}
          </div>
          <div class="meta-row" v-if="userInfo?.email">
            📧 {{ userInfo.email }}
          </div>
          <div class="meta-row" v-if="userInfo?.createTime">
            📅 注册于 {{ String(userInfo.createTime).slice(0, 10) }}
          </div>
        </div>
        <div class="actions">
          <button
            v-if="!userStore.isLoggedIn || (userStore.userInfo?.id !== userId)"
            class="btn-follow"
            :class="{ active: isFollowed }"
            @click="toggleFollow"
          >
            {{ isFollowed ? '已关注' : '+ 关注' }}
          </button>
          <router-link
            v-if="userStore.isLoggedIn && userStore.userInfo?.id !== userId"
            :to="`/chat/${userId}`"
            class="btn-chat"
          >💬 私信 TA</router-link>
          <div v-if="userStore.isLoggedIn && userStore.userInfo?.id === userId" class="self-tip">
            👀 这是你的主页
          </div>
        </div>
      </div>

      <!-- 商品列表 -->
      <div class="card">
        <div class="tabs">
          <span
            v-for="t in tabs"
            :key="t.key"
            class="tab"
            :class="{ active: activeTab === t.key }"
            @click="activeTab = t.key"
          >
            {{ t.label }}
            <small class="count">
              ({{
                t.key === 'all' ? totalProducts :
                (t.key === 'onsale' ? statsOnSaleCount : statsSoldCount)
              }})
            </small>
          </span>
        </div>
        <div v-if="filteredProducts.length === 0" class="empty">
          🚧 该用户暂无{{ tabs.find(t => t.key === activeTab)?.label }}商品
        </div>
        <div v-else class="product-grid">
          <ProductCard v-for="p in filteredProducts" :key="p.id" :product="p" />
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 20px 24px 60px;
}
.card {
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.empty {
  padding: 60px 20px;
  text-align: center;
  color: #999;
}

/* ====== 用户资料卡 ====== */
.profile-card {
  background: #fff;
  border-radius: 10px;
  padding: 30px 28px;
  display: flex;
  align-items: center;
  gap: 28px;
  margin-bottom: 18px;
  background-image: linear-gradient(135deg, #fff6f2 0%, #ffe8d6 100%);
  box-shadow: 0 4px 14px rgba(255, 107, 53, 0.08);
}
.avatar-wrap {
  flex-shrink: 0;
}
.big-avatar {
  width: 108px;
  height: 108px;
  border-radius: 50%;
  border: 5px solid #fff;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  object-fit: cover;
  background: #fff;
}
.info {
  flex: 1;
  min-width: 0;
}
.name {
  font-size: 24px;
  color: #222;
  font-weight: 700;
  margin: 0 0 14px;
}
.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  font-size: 14px;
  color: #555;
  margin-bottom: 10px;
}
.meta-row {
  font-size: 13px;
  color: #777;
  line-height: 1.9;
}
.actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.btn-follow,
.btn-chat {
  height: 40px;
  padding: 0 26px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
  white-space: nowrap;
}
.btn-follow {
  background: #ff6b35;
  color: #fff;
  border: none;
}
.btn-follow:hover { background: #e85b25; }
.btn-follow.active {
  background: #f0f0f0;
  color: #666;
}
.btn-chat {
  background: #fff;
  color: #ff6b35;
  border: 1px solid #ff6b35;
}
.btn-chat:hover { background: #fff6f2; }
.self-tip {
  font-size: 13px;
  color: #ff6b35;
  background: rgba(255, 107, 53, 0.1);
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
  white-space: nowrap;
}

/* ====== 商品 Tab 切换 ====== */
.tabs {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
  padding: 0 10px;
}
.tab {
  padding: 14px 22px;
  font-size: 15px;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: all 0.2s;
}
.tab:hover { color: #ff6b35; }
.tab.active {
  color: #ff6b35;
  font-weight: 600;
  border-bottom-color: #ff6b35;
}
.tab .count {
  color: #bbb;
  font-weight: normal;
  margin-left: 4px;
}

/* ====== 商品网格 ====== */
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
@media (max-width: 1100px) {
  .product-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 800px) {
  .product-grid { grid-template-columns: repeat(2, 1fr); }
  .profile-card {
    flex-direction: column;
    text-align: center;
  }
  .stats { justify-content: center; }
  .actions { align-items: stretch; }
}
</style>
