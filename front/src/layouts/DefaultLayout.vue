<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import logo from '@/assets/web-image.png'

const userStore = useUserStore()
const messageStore = useMessageStore()
const router = useRouter()
const route = useRoute()
const searchKeyword = ref('')

const userMenuOpen = ref(false)

const userInfo = computed(() => userStore.userInfo)
const isLoggedIn = computed(() => userStore.isLoggedIn)

let pollTimer: number | null = null

function startPolling() {
  stopPolling()
  messageStore.fetchAll()
  pollTimer = window.setInterval(() => messageStore.fetchAll(), 30000)
}

function stopPolling() {
  if (pollTimer !== null) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

watch(isLoggedIn, (val) => {
  if (val) {
    startPolling()
  } else {
    stopPolling()
    messageStore.clearAll()
  }
})

watch(() => route.path, () => {
  if (isLoggedIn.value) {
    messageStore.fetchAll()
  }
})

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ name: 'Products', query: { keyword: searchKeyword.value } })
  } else {
    router.push({ name: 'Products' })
  }
}

function handleLogout() {
  userStore.logout()
  userMenuOpen.value = false
  router.push({ name: 'Home' })
}

onMounted(() => {
  if (isLoggedIn.value) {
    startPolling()
  }
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <div class="default-layout">
    <header class="header">
      <div class="header-top">
        <div class="container">
          <div class="header-top-left">
            <RouterLink to="/" class="logo">
              <img :src="logo" alt="大学二手交易平台">
              <span>大学二手交易平台</span>
            </RouterLink>
          </div>
          <div class="header-top-center">
            <div class="search-box">
              <input v-model="searchKeyword" type="text" placeholder="搜索二手商品" @keyup.enter="handleSearch" />
              <button @click="handleSearch">搜索</button>
            </div>
          </div>
          <div class="header-top-right">
            <RouterLink v-if="!isLoggedIn" to="/admin/login" class="admin-login-btn">管理员登录</RouterLink>
            <div v-if="!isLoggedIn" class="user-menu">
              <RouterLink to="/login" class="login-btn">登录</RouterLink>
              <RouterLink to="/register" class="register-btn">注册</RouterLink>
            </div>
            <div v-else class="user-info-wrapper" @mouseenter="userMenuOpen = true" @mouseleave="userMenuOpen = false">
              <div class="user-info">
                <img :src="userInfo?.avatar || 'https://via.placeholder.com/32'" alt="avatar" class="avatar">
                <span>{{ userInfo?.nickname || userInfo?.username }}</span>
                <i class="arrow">▼</i>
              </div>
              <div v-if="userMenuOpen" class="dropdown">
                <RouterLink to="/user-center">个人中心</RouterLink>
                <RouterLink to="/my-products">我的发布</RouterLink>
                <RouterLink to="/user-center?tab=orders-bought">我买的</RouterLink>
                <RouterLink to="/user-center?tab=orders-sold">我卖的</RouterLink>
                <RouterLink to="/favorites">我的收藏</RouterLink>
                <RouterLink to="/account-settings">账号设置</RouterLink>
                <a class="logout" @click="handleLogout">退出登录</a>
              </div>
            </div>
          </div>
        </div>
      </div>
      <nav class="header-nav">
        <div class="container">
          <ul class="nav-list">
            <li><RouterLink to="/" exact-active-class="active">首页</RouterLink></li>
            <li><RouterLink to="/products" active-class="active">全部商品</RouterLink></li>
            <li><RouterLink to="/cart" active-class="active">🛒 购物车</RouterLink></li>
            <li><RouterLink to="/favorites" active-class="active">❤️ 我的收藏</RouterLink></li>
            <li><RouterLink to="/publish" active-class="active">📤 发布商品</RouterLink></li>
            <li>
              <RouterLink to="/message-center" active-class="active" class="nav-message">
                <span>💬 消息中心</span>
                <span v-if="messageStore.totalUnread > 0" class="msg-badge">{{ messageStore.totalUnread > 99 ? '99+' : messageStore.totalUnread }}</span>
              </RouterLink>
            </li>
          </ul>
        </div>
      </nav>
    </header>

    <main class="main">
      <div class="container">
        <RouterView />
      </div>
    </main>

    <footer class="footer">
      <div class="container">
        <div class="footer-content">
          <div class="footer-section">
            <h3>关于我们</h3>
            <ul>
              <li><a href="#">平台介绍</a></li>
              <li><a href="#">用户协议</a></li>
              <li><a href="#">隐私政策</a></li>
            </ul>
          </div>
          <div class="footer-section">
            <h3>帮助中心</h3>
            <ul>
              <li><a href="#">常见问题</a></li>
              <li><a href="#">交易流程</a></li>
              <li><a href="#">联系客服</a></li>
            </ul>
          </div>
          <div class="footer-section">
            <h3>友情链接</h3>
            <ul>
              <li><a href="#">校园官网</a></li>
              <li><a href="#">教务处</a></li>
              <li><a href="#">图书馆</a></li>
            </ul>
          </div>
          <div class="footer-section">
            <h3>联系我们</h3>
            <p>邮箱: 3190005411@qq.com</p>
            <p>电话: 15079141949</p>
          </div>
        </div>
        <div class="footer-bottom">
          <p>&copy; 2026 大学二手交易平台 版权所有 Lin</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.container {
  width: 1200px;
  margin: 0 auto;
}

.header {
  background: #fff;
  border-bottom: 2px solid #ff6b35;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-top {
  border-bottom: 1px solid #eee;
}

.header-top .container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 80px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: #ff6b35;
}

.logo img {
  width: 40px;
  height: 40px;
  border-radius: 8px;
}

.search-box {
  display: flex;
  width: 500px;
  height: 40px;
  border: 2px solid #ff6b35;
  border-radius: 20px;
  overflow: hidden;
}

.search-box input {
  flex: 1;
  border: none;
  padding: 0 16px;
  font-size: 14px;
}

.search-box button {
  width: 80px;
  background: #ff6b35;
  color: #fff;
  font-size: 14px;
}

.header-top-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.admin-login-btn {
  font-size: 13px;
  color: #999;
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.admin-login-btn:hover {
  color: #ff6b35;
  border-color: #ff6b35;
}

.user-menu a,
.login-btn,
.register-btn {
  margin-left: 12px;
  font-size: 14px;
  color: #666;
}

.user-menu a:hover,
.login-btn:hover,
.register-btn:hover {
  color: #ff6b35;
}

.user-info-wrapper {
  position: relative;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 6px;
}

.user-info:hover {
  background: #f7f7f7;
}

.user-info .avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.user-info .arrow {
  font-size: 10px;
  color: #999;
}

.dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  width: 160px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  z-index: 200;
  padding: 6px 0;
}

.dropdown a {
  display: block;
  padding: 10px 16px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
}

.dropdown a:hover {
  background: #f7f7f7;
  color: #ff6b35;
}

.dropdown .logout {
  color: #e74c3c;
  border-top: 1px solid #f0f0f0;
  margin-top: 4px;
}

.header-nav .container {
  display: flex;
}

.nav-list {
  display: flex;
  gap: 28px;
  align-items: center;
  height: 50px;
  overflow: visible;
}

.nav-list li {
  overflow: visible;
}

.nav-list a {
  font-size: 15px;
  color: #333;
  font-weight: 500;
  padding: 4px 0;
  border-bottom: 2px solid transparent;
}

.nav-list a.active {
  color: #ff6b35;
  border-bottom-color: #ff6b35;
}

.nav-list a:hover {
  color: #ff6b35;
}

.nav-message {
  position: relative;
  display: inline-flex;
  align-items: center;
  overflow: visible;
}

.msg-badge {
  position: absolute;
  top: -10px;
  right: -22px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: #e74c3c;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 20px;
  text-align: center;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(231, 76, 60, 0.5);
  animation: msg-badge-pulse 1.6s ease-in-out infinite;
  border: 2px solid #fff;
  white-space: nowrap;
}

@keyframes msg-badge-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.12); }
}

.main {
  min-height: calc(100vh - 80px - 50px - 260px);
  padding: 24px 0;
}

.footer {
  background: #2d2d2d;
  color: #aaa;
  margin-top: 40px;
}

.footer-content {
  display: flex;
  justify-content: space-between;
  padding: 40px 0 24px;
}

.footer-section h3 {
  color: #fff;
  font-size: 15px;
  margin-bottom: 16px;
}

.footer-section ul li {
  margin-bottom: 10px;
}

.footer-section a {
  color: #aaa;
  font-size: 13px;
}

.footer-section a:hover {
  color: #ff6b35;
}

.footer-section p {
  font-size: 13px;
  margin-bottom: 8px;
}

.footer-bottom {
  text-align: center;
  padding: 20px 0;
  border-top: 1px solid #444;
  font-size: 13px;
}
</style>
