<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { adminUserApi } from '@/api'
import type { UserInfo, PageResult } from '@/api'
import { useToast } from '@/composables/useToast'

const toast = useToast()
const keyword = ref('')
const status = ref<number | null>(null)
const users = ref<UserInfo[]>([])
const page = reactive({ current: 1, size: 10, total: 0 })
const loading = ref(false)

onMounted(() => loadList())

async function loadList(reset = false) {
  if (reset) page.current = 1
  loading.value = true
  try {
    const res = await adminUserApi.getList(keyword.value, page.current, page.size, status.value ?? undefined)
    if (res.code === 200) {
      const data = res.data as PageResult<UserInfo>
      users.value = data.records || []
      page.total = data.total || 0
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function handleStatus(u: UserInfo, newStatus: number) {
  const text = newStatus === 1 ? '启用' : newStatus === 0 ? '禁用' : '删除'
  if (!confirm(`确定要${text}该用户吗？`)) return
  try {
    const fn = newStatus === 1 ? adminUserApi.enable : newStatus === 0 ? adminUserApi.disable : adminUserApi.remove
    const res = await fn(u.id!)
    if (res.code === 200) { toast.success('操作成功'); loadList() }
    else toast.error(res.message || '操作失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}

function onSearch() { loadList(true) }
</script>

<template>
  <div class="admin-list">
    <div class="filter-card">
      <div class="filters">
        <input v-model="keyword" placeholder="搜索用户名/手机号" class="input" @keyup.enter="onSearch">
        <select v-model="status" class="input">
          <option :value="null">全部状态</option>
          <option :value="1">正常</option>
          <option :value="0">已禁用</option>
        </select>
        <button class="btn-primary" @click="onSearch">🔍 搜索</button>
      </div>
    </div>

    <div class="table-card">
      <div v-if="loading" class="loading">加载中...</div>
      <table v-else class="admin-table">
        <thead>
          <tr>
            <th>ID</th><th>头像</th><th>用户名</th><th>手机号</th>
            <th>状态</th><th>注册时间</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="users.length === 0">
            <td colspan="7" class="empty">暂无数据</td>
          </tr>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td><img :src="u.avatar || 'https://via.placeholder.com/40'" class="mini-avatar"></td>
            <td>{{ u.nickname || u.username }}</td>
            <td>{{ u.phone }}</td>
            <td>
              <span class="tag" :class="u.status === 1 ? 'ok' : 'bad'">
                {{ u.status === 1 ? '正常' : '禁用' }}
              </span>
            </td>
            <td>{{ u.createTime || '-' }}</td>
            <td class="actions">
              <button v-if="u.status === 1" class="btn-warning" @click="handleStatus(u, 0)">禁用</button>
              <button v-else class="btn-success" @click="handleStatus(u, 1)">启用</button>
              <button class="btn-danger" @click="handleStatus(u, -1)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="page.total > page.size" class="pager">
        共 {{ page.total }} 条，第 {{ page.current }} 页
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-list { display: flex; flex-direction: column; gap: 16px; }
.filter-card { background: #fff; border-radius: 10px; padding: 16px 20px; }
.filters { display: flex; gap: 10px; flex-wrap: wrap; }
.input {
  height: 36px; padding: 0 14px; border: 1px solid #ddd; border-radius: 6px;
  font-size: 13px; min-width: 200px;
}
.btn-primary, .btn-success, .btn-warning, .btn-danger {
  height: 36px; padding: 0 14px; border-radius: 6px; font-size: 13px;
}
.btn-primary { background: #3498db; color: #fff; }
.btn-success { background: #27ae60; color: #fff; }
.btn-warning { background: #f39c12; color: #fff; }
.btn-danger { background: #e74c3c; color: #fff; }

.table-card { background: #fff; border-radius: 10px; overflow: hidden; }
.loading, .empty { padding: 40px; text-align: center; color: #999; }
.admin-table { width: 100%; border-collapse: collapse; }
.admin-table th, .admin-table td {
  padding: 14px 16px; text-align: left; border-bottom: 1px solid #f0f0f0; font-size: 13px;
}
.admin-table thead { background: #f7f9fc; }
.admin-table th { color: #555; font-weight: 600; }
.mini-avatar { width: 36px; height: 36px; border-radius: 50%; }
.tag { padding: 3px 10px; border-radius: 4px; font-size: 12px; }
.tag.ok { background: #eafaf0; color: #27ae60; }
.tag.bad { background: #fdecea; color: #e74c3c; }
.actions { display: flex; gap: 6px; }
.pager { padding: 14px 20px; text-align: right; font-size: 13px; color: #999; border-top: 1px solid #f0f0f0; }
</style>
