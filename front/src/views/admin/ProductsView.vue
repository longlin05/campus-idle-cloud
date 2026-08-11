<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { adminProductApi } from '@/api'
import type { ProductVO, PageResult } from '@/api'
import { useToast } from '@/composables/useToast'
import { formatTime } from '@/utils/format'

const toast = useToast()
const keyword = ref('')
const status = ref<number | null>(null)
const list = ref<ProductVO[]>([])
const page = reactive({ current: 1, size: 10, total: 0 })
const loading = ref(false)

const statusMap: Record<number, { label: string; cls: string }> = {
  1: { label: '在架', cls: 'ok' },
  0: { label: '已售', cls: 'warn' },
  2: { label: '已下架', cls: 'bad' },
}

onMounted(() => loadList())

async function loadList(reset = false) {
  if (reset) page.current = 1
  loading.value = true
  try {
    const res = await adminProductApi.getList(keyword.value, page.current, page.size, status.value ?? undefined)
    if (res.code === 200) {
      const data = res.data as PageResult<ProductVO>
      list.value = data.records || []
      page.total = data.total || 0
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function handleStatus(p: ProductVO, op: string) {
  try {
    let res, msg = ''
    if (op === 'off') {
      if (!confirm('确定下架？')) return
      res = await adminProductApi.offline(p.id!)
      msg = '下架'
    } else if (op === 'on') {
      res = await adminProductApi.online(p.id!)
      msg = '上架'
    } else if (op === 'del') {
      if (!confirm('确定删除？')) return
      res = await adminProductApi.remove(p.id!)
      msg = '删除'
    } else if (op === 'view') {
      window.open(`/product/${p.id}`, '_blank')
      return
    }
    if (res!.code === 200) { toast.success(msg + '成功'); loadList() }
    else toast.error(res!.message || '操作失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}
</script>

<template>
  <div class="admin-list">
    <div class="filter-card">
      <div class="filters">
        <input v-model="keyword" placeholder="搜索商品标题/描述" class="input" style="min-width:240px" @keyup.enter="loadList(true)">
        <select v-model="status" class="input">
          <option :value="null">全部状态</option>
          <option :value="1">在架</option>
          <option :value="0">已售</option>
          <option :value="2">已下架</option>
        </select>
        <button class="btn-primary" @click="loadList(true)">🔍 搜索</button>
      </div>
    </div>
    <div class="table-card">
      <div v-if="loading" class="loading">加载中...</div>
      <table v-else class="admin-table">
        <thead><tr>
          <th>ID</th><th>图片</th><th>标题</th><th>价格</th>
          <th>卖家</th><th>浏览</th><th>状态</th><th>发布时间</th><th>操作</th>
        </tr></thead>
        <tbody>
          <tr v-if="list.length === 0"><td colspan="9" class="empty">暂无数据</td></tr>
          <tr v-for="p in list" :key="p.id">
            <td>{{ p.id }}</td>
            <td><img :src="p.images?.[0] || 'https://via.placeholder.com/48'" class="mini-img"></td>
            <td style="max-width:220px">{{ p.title }}</td>
            <td class="price">¥{{ p.price?.toFixed(2) }}</td>
            <td>{{ p.sellerName || '-' }}</td>
            <td>{{ p.viewCount || 0 }}</td>
            <td><span class="tag" :class="statusMap[p.status ?? 0]?.cls">
              {{ statusMap[p.status ?? 0]?.label || '未知' }}
            </span></td>
            <td>{{ formatTime(p.createTime) }}</td>
            <td class="actions">
              <button class="btn-link" @click="handleStatus(p, 'view')">查看</button>
              <button v-if="p.status === 1" class="btn-warning" @click="handleStatus(p, 'off')">下架</button>
              <button v-else-if="p.status === 2" class="btn-success" @click="handleStatus(p, 'on')">上架</button>
              <button class="btn-danger" @click="handleStatus(p, 'del')">删除</button>
            </td>
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
.btn-primary,.btn-success,.btn-warning,.btn-danger,.btn-link { height: 36px; padding: 0 12px; border-radius: 6px; font-size: 13px; }
.btn-primary { background: #3498db; color: #fff; }
.btn-success { background: #27ae60; color: #fff; }
.btn-warning { background: #f39c12; color: #fff; }
.btn-danger { background: #e74c3c; color: #fff; }
.btn-link { background: transparent; color: #3498db; }
.table-card { background: #fff; border-radius: 10px; overflow: hidden; }
.loading, .empty { padding: 40px; text-align: center; color: #999; }
.admin-table { width: 100%; border-collapse: collapse; }
.admin-table th, .admin-table td { padding: 12px 14px; text-align: left; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
.admin-table thead { background: #f7f9fc; }
.admin-table th { color: #555; font-weight: 600; }
.mini-img { width: 48px; height: 48px; border-radius: 6px; object-fit: cover; }
.price { color: #ff6b35; font-weight: 600; }
.tag { padding: 3px 10px; border-radius: 4px; font-size: 12px; }
.tag.ok { background: #eafaf0; color: #27ae60; }
.tag.warn { background: #fff2e5; color: #f39c12; }
.tag.bad { background: #fdecea; color: #e74c3c; }
.actions { display: flex; gap: 6px; flex-wrap: wrap; }
.pager { padding: 14px 20px; text-align: right; font-size: 13px; color: #999; border-top: 1px solid #f0f0f0; }
</style>
