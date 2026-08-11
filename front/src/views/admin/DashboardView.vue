<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api'

const stats = ref({
  userCount: 0,
  productCount: 0,
  orderCount: 0,
  totalAmount: 0,
  todayUserCount: 0,
  todayOrderCount: 0,
  todayAmount: 0,
  onSaleCount: 0,
  categoryCount: 0,
})

const loading = ref(true)

const statCards = computed(() => [
  { label: '累计用户', value: stats.value.userCount, icon: '👥', color: '#3498db', bg: '#eaf5ff' },
  { label: '累计商品', value: stats.value.productCount, icon: '📦', color: '#27ae60', bg: '#eafaf0' },
  { label: '累计订单', value: stats.value.orderCount, icon: '🛒', color: '#ff6b35', bg: '#fff3ec' },
  { label: '累计交易额', value: '¥' + (stats.value.totalAmount || 0).toFixed(2), icon: '💰', color: '#f39c12', bg: '#fff7ea' },
  { label: '今日新增用户', value: stats.value.todayUserCount, icon: '🆕', color: '#9b59b6', bg: '#f7f0fb' },
  { label: '今日订单', value: stats.value.todayOrderCount, icon: '📋', color: '#16a085', bg: '#e7f7f3' },
  { label: '今日交易额', value: '¥' + (stats.value.todayAmount || 0).toFixed(2), icon: '💹', color: '#e67e22', bg: '#fef2e4' },
  { label: '在售商品', value: stats.value.onSaleCount, icon: '✅', color: '#2980b9', bg: '#e8f3fb' },
])

import { computed } from 'vue'

onMounted(async () => {
  try {
    const res = await adminApi.getStats()
    if (res.code === 200 && res.data) {
      Object.assign(stats.value, res.data)
    }
  } catch (e) {
    console.error('加载统计失败', e)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="dashboard">
    <div class="welcome-card">
      <div class="left">
        <h2>🎉 欢迎回来，管理员！</h2>
        <p>这里是校园闲置物品交易平台的数据概览</p>
      </div>
      <div class="right">
        <div class="stat-mini"><span>系统状态</span><b class="ok">● 运行正常</b></div>
      </div>
    </div>

    <div v-if="loading" class="loading">加载数据中...</div>
    <div v-else>
      <div class="stat-grid">
        <div
          v-for="(card, i) in statCards"
          :key="i"
          class="stat-card"
          :style="{ '--card-bg': card.bg, '--card-color': card.color }"
        >
          <div class="icon">{{ card.icon }}</div>
          <div class="content">
            <div class="label">{{ card.label }}</div>
            <div class="value">{{ card.value }}</div>
          </div>
        </div>
      </div>

      <div class="panel-row">
        <div class="panel">
          <h3>📈 平台运行情况</h3>
          <ul class="info-list">
            <li><span>今日新增用户</span><b>{{ stats.todayUserCount }}</b></li>
            <li><span>今日订单数</span><b>{{ stats.todayOrderCount }}</b></li>
            <li><span>今日交易额</span><b class="highlight">¥{{ stats.todayAmount?.toFixed(2) || '0.00' }}</b></li>
            <li><span>在售商品数</span><b>{{ stats.onSaleCount }}</b></li>
            <li><span>累计注册用户</span><b>{{ stats.userCount }}</b></li>
            <li><span>累计交易额</span><b class="highlight">¥{{ stats.totalAmount?.toFixed(2) || '0.00' }}</b></li>
          </ul>
        </div>
        <div class="panel">
          <h3>⚙️ 快捷操作</h3>
          <div class="quick-actions">
            <router-link to="/admin/users" class="action">
              <span class="icon">👥</span><span>用户管理</span>
            </router-link>
            <router-link to="/admin/products" class="action">
              <span class="icon">📦</span><span>商品审核</span>
            </router-link>
            <router-link to="/admin/orders" class="action">
              <span class="icon">🛒</span><span>订单管理</span>
            </router-link>
            <router-link to="/admin/messages" class="action">
              <span class="icon">📢</span><span>发送消息</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.welcome-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 24px 28px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-card h2 {
  font-size: 20px;
  margin-bottom: 6px;
}

.welcome-card p {
  font-size: 13px;
  opacity: 0.85;
}

.stat-mini {
  background: rgba(255,255,255,0.15);
  padding: 8px 14px;
  border-radius: 20px;
  font-size: 13px;
  backdrop-filter: blur(4px);
}

.stat-mini span {
  margin-right: 6px;
  opacity: 0.85;
}

.stat-mini .ok {
  color: #a6ffbc;
}

.loading {
  background: #fff;
  border-radius: 10px;
  padding: 60px 20px;
  text-align: center;
  color: #999;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.03);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0,0,0,0.08);
}

.stat-card .icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: var(--card-bg);
  color: var(--card-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-card .label {
  font-size: 13px;
  color: #999;
  margin-bottom: 6px;
}

.stat-card .value {
  font-size: 22px;
  font-weight: 700;
  color: #333;
}

.panel-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.panel {
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px;
}

.panel h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.info-list li {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed #f0f0f0;
  font-size: 14px;
  color: #555;
}

.info-list li:last-child {
  border-bottom: none;
}

.info-list b {
  color: #333;
  font-weight: 600;
}

.info-list b.highlight {
  color: #ff6b35;
  font-size: 16px;
}

.quick-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 18px 12px;
  background: #f9fafc;
  border-radius: 8px;
  transition: all 0.2s;
  color: #555;
}

.action:hover {
  background: #fff2eb;
  color: #ff6b35;
}

.action .icon {
  font-size: 24px;
}

@media (max-width: 1100px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .panel-row { grid-template-columns: 1fr; }
}
</style>
