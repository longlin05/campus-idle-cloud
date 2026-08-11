<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const router = useRouter()
const route = useRoute()
const adminStore = useAdminStore()

const menuList = [
  { key: 'dashboard', label: '数据概览', icon: '📊', path: '/admin/dashboard' },
  { key: 'users', label: '用户管理', icon: '👥', path: '/admin/users' },
  { key: 'products', label: '商品管理', icon: '📦', path: '/admin/products' },
  { key: 'orders', label: '订单管理', icon: '🛒', path: '/admin/orders' },
  { key: 'categories', label: '分类管理', icon: '🏷️', path: '/admin/categories' },
  { key: 'system-images', label: '轮播图管理', icon: '🖼️', path: '/admin/system-images' },
  { key: 'messages', label: '系统消息', icon: '📢', path: '/admin/messages' },
  { key: 'admins', label: '管理员管理', icon: '🛡️', path: '/admin/admins' },
]

const currentTime = ref('')

onMounted(() => {
  updateTime()
  setInterval(updateTime, 1000)
})

function updateTime() {
  const now = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')
  currentTime.value = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

function handleLogout() {
  if (!confirm('确定退出登录？')) return
  adminStore.logout()
  router.replace('/admin/login')
}

function isActive(path: string) {
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="logo">
        <img src="@/assets/web-image.png" alt="">
        <div class="text">
          <h1>校园闲置后台</h1>
          <p>Admin Dashboard</p>
        </div>
      </div>
      <nav class="menu">
        <router-link
          v-for="m in menuList"
          :key="m.key"
          :to="m.path"
          class="menu-item"
          :class="{ active: isActive(m.path) }"
        >
          <span class="icon">{{ m.icon }}</span>
          <span class="label">{{ m.label }}</span>
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <div>v1.0.0</div>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="topbar-left">
          <h2 class="page-title">
            {{ menuList.find(m => route.path.startsWith(m.path))?.label || '管理后台' }}
          </h2>
        </div>
        <div class="topbar-right">
          <span class="clock">🕒 {{ currentTime }}</span>
          <div class="user-info">
            <img
              :src="adminStore.adminInfo?.avatar || 'https://via.placeholder.com/32'"
              alt=""
              class="avatar"
            >
            <span>{{ adminStore.adminInfo?.nickname || adminStore.adminInfo?.username || '管理员' }}</span>
          </div>
          <button class="btn-logout" @click="handleLogout">退出</button>
        </div>
      </header>

      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
  background: #f5f7fa;
}

.sidebar {
  width: 220px;
  background: linear-gradient(180deg, #1e3c72 0%, #2a5298 100%);
  display: flex;
  flex-direction: column;
  color: #fff;
  flex-shrink: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 18px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.logo img {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  box-shadow: 0 4px 10px rgba(0,0,0,0.2);
}

.logo h1 {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 2px;
}

.logo p {
  font-size: 10px;
  opacity: 0.6;
}

.menu {
  flex: 1;
  padding: 10px 0;
  overflow-y: auto;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  color: rgba(255,255,255,0.75);
  font-size: 14px;
  margin: 2px 10px;
  border-radius: 8px;
  transition: all 0.2s;
}

.menu-item:hover {
  background: rgba(255,255,255,0.08);
  color: #fff;
}

.menu-item.active {
  background: rgba(255,255,255,0.18);
  color: #fff;
  font-weight: 600;
  box-shadow: inset 3px 0 0 #fff;
}

.menu-item .icon {
  font-size: 16px;
}

.sidebar-footer {
  padding: 16px;
  font-size: 11px;
  opacity: 0.5;
  text-align: center;
  border-top: 1px solid rgba(255,255,255,0.08);
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  height: 60px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  z-index: 10;
}

.page-title {
  font-size: 17px;
  color: #333;
  font-weight: 600;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.clock {
  font-size: 13px;
  color: #666;
  font-family: 'Consolas', monospace;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #333;
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
}

.btn-logout {
  height: 30px;
  padding: 0 14px;
  border: 1px solid #e0e0e0;
  background: #fff;
  border-radius: 6px;
  color: #666;
  font-size: 13px;
}

.btn-logout:hover {
  border-color: #ff6b35;
  color: #ff6b35;
}

.content {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
}
</style>
