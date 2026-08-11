<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { adminOrderApi } from '@/api'
import type { OrderVO, PageResult } from '@/api'
import { formatTime } from '@/utils/format'

const keyword = ref('')
const status = ref<number | null>(null)
const list = ref<OrderVO[]>([])
const page = reactive({ current: 1, size: 10, total: 0 })
const loading = ref(false)

const statusMap: Record<string, { label: string; cls: string }> = {
  '-1': { label: '已取消', cls: 'bad' },
  '0': { label: '待付款', cls: 'warn' },
  '1': { label: '待发货', cls: 'warn' },
  '2': { label: '待收货', cls: 'warn' },
  '3': { label: '已完成', cls: 'ok' },
}

onMounted(() => loadList())

async function loadList(reset = false) {
  if (reset) page.current = 1
  loading.value = true
  try {
    const res = await adminOrderApi.getList(keyword.value, page.current, page.size, status.value ?? undefined)
    if (res.code === 200) {
      const data = res.data as PageResult<OrderVO>
      list.value = data.records || []
      page.total = data.total || 0
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function goDetail(id?: number) {
  if (!id) return
  window.open(`/order/${id}`, '_blank')
}
</script>

<template>
  <div class="admin-list">
    <div class="filter-card">
      <div class="filters">
        <input v-model="keyword" placeholder="搜索订单号/商品名" class="input" style="min-width:240px" @keyup.enter="loadList(true)">
        <select v-model="status" class="input">
          <option :value="null">全部状态</option>
          <option :value="0">待付款</option>
          <option :value="1">待发货</option>
          <option :value="2">待收货</option>
          <option :value="3">已完成</option>
          <option :value="-1">已取消</option>
        </select>
        <button class="btn-primary" @click="loadList(true)">🔍 搜索</button>
      </div>
    </div>
    <div class="table-card">
      <div v-if="loading" class="loading">加载中...</div>
      <table v-else class="admin-table">
        <thead><tr>
          <th>订单号</th><th>商品</th><th>数量</th><th>金额</th>
          <th>状态</th><th>下单时间</th><th>操作</th>
        </tr></thead>
        <tbody>
          <tr v-if="list.length === 0"><td colspan="7" class="empty">暂无订单</td></tr>
          <tr v-for="o in list" :key="o.orderId || o.id">
            <td style="font-family:monospace">{{ o.orderNo || o.orderId || o.id }}</td>
            <td>
              <div class="prod-cell">
                <img v-if="o.productImage" :src="o.productImage" class="prod-thumb">
                <span>{{ o.productName || '-' }}</span>
              </div>
            </td>
            <td>{{ o.quantity ?? (o.totalQuantity ?? '-') }}</td>
            <td class="price">¥{{ (o.orderAmount ?? o.totalAmount ?? 0).toFixed(2) }}</td>
            <td>
              <span class="tag" :class="statusMap[String(o.status)]?.cls">
                {{ o.statusName || statusMap[String(o.status)]?.label || '未知' }}
              </span>
            </td>
            <td>{{ formatTime(o.createTime) }}</td>
            <td><button class="btn-link" @click="goDetail(o.orderId || o.id)">详情</button></td>
          </tr>
        </tbody>
      </table>
      <div v-if="page.total > page.size" class="pager">共 {{ page.total }} 条，第 {{ page.current }} 页</div>
    </div>
  </div>
</template>

<style scoped>
.admin-list { display: flex; flex-direction: column; gap: 16px; }
.filter-card { background: #fff; border-radius: 10px; padding: 16px 20px; }
.filters { display: flex; gap: 10px; flex-wrap: wrap; }
.input { height: 36px; padding: 0 14px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; min-width: 160px; }
.btn-primary, .btn-link { height: 36px; padding: 0 14px; border-radius: 6px; font-size: 13px; }
.btn-primary { background: #3498db; color: #fff; }
.btn-link { background: transparent; color: #3498db; }
.table-card { background: #fff; border-radius: 10px; overflow: hidden; }
.loading, .empty { padding: 40px; text-align: center; color: #999; }
.admin-table { width: 100%; border-collapse: collapse; }
.admin-table th, .admin-table td { padding: 12px 14px; text-align: left; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
.admin-table thead { background: #f7f9fc; }
.admin-table th { color: #555; font-weight: 600; }
.price { color: #ff6b35; font-weight: 600; }
.prod-cell { display: flex; align-items: center; gap: 8px; }
.prod-thumb { width: 40px; height: 40px; border-radius: 6px; object-fit: cover; flex-shrink: 0; }
.tag { padding: 3px 10px; border-radius: 4px; font-size: 12px; }
.tag.ok { background: #eafaf0; color: #27ae60; }
.tag.warn { background: #fff2e5; color: #f39c12; }
.tag.bad { background: #fdecea; color: #e74c3c; }
.pager { padding: 14px 20px; text-align: right; font-size: 13px; color: #999; border-top: 1px solid #f0f0f0; }
</style>
