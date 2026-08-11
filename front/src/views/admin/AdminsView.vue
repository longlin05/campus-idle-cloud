<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { adminSystemApi } from '@/api'
import type { PageResult } from '@/api'
import { useToast } from '@/composables/useToast'

interface AdminUser { id: number; username: string; nickname: string; avatar?: string; role: string; phone?: string; email?: string; status: number; createTime?: string }

const toast = useToast()
const keyword = ref('')
const list = ref<AdminUser[]>([])
const page = reactive({ current: 1, size: 10, total: 0 })
const loading = ref(false)
const showDialog = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  username: '', nickname: '', password: '', phone: '', email: '', role: 'admin', status: 1,
})

onMounted(() => loadList())

async function loadList(reset = false) {
  if (reset) page.current = 1
  loading.value = true
  try {
    const res = await adminSystemApi.getAdmins(keyword.value, page.current, page.size)
    if (res.code === 200) {
      const data = res.data as PageResult<AdminUser>
      list.value = data.records || []
      page.total = data.total || 0
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  form.username = ''; form.nickname = ''; form.password = ''
  form.phone = ''; form.email = ''; form.role = 'admin'; form.status = 1
  showDialog.value = true
}

function openEdit(a: AdminUser) {
  editingId.value = a.id
  form.username = a.username; form.nickname = a.nickname; form.password = ''
  form.phone = a.phone || ''; form.email = a.email || ''
  form.role = a.role || 'admin'; form.status = a.status
  showDialog.value = true
}

async function handleSubmit(e: Event) {
  e.preventDefault()
  if (!form.username.trim()) { toast.warning('请输入账号'); return }
  if (!editingId.value && !form.password) { toast.warning('请输入初始密码'); return }
  try {
    const body: any = {
      username: form.username, nickname: form.nickname,
      phone: form.phone, email: form.email, role: form.role, status: form.status,
    }
    if (form.password) body.password = form.password
    let res
    if (editingId.value) {
      res = await adminSystemApi.updateAdmin(editingId.value, body)
    } else {
      res = await adminSystemApi.createAdmin(body)
    }
    if (res.code === 200) {
      toast.success('操作成功')
      showDialog.value = false
      loadList()
    } else {
      toast.error(res.message || '操作失败')
    }
  } catch (e: any) { toast.error(e.message || '操作失败') }
}

async function toggleStatus(a: AdminUser) {
  const next = a.status === 1 ? 0 : 1
  if (!confirm(`确定要${next === 1 ? '启用' : '禁用'}该管理员？`)) return
  try {
    const fn = next === 1 ? adminSystemApi.enableAdmin : adminSystemApi.disableAdmin
    const res = await fn(a.id)
    if (res.code === 200) loadList()
    else toast.error(res.message || '操作失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}

async function handleDelete(a: AdminUser) {
  if (!confirm(`确定删除管理员「${a.nickname || a.username}」？此操作不可恢复。`)) return
  try {
    const res = await adminSystemApi.removeAdmin(a.id)
    if (res.code === 200) loadList()
    else toast.error(res.message || '操作失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}
</script>

<template>
  <div class="admin-list">
    <div class="filter-card">
      <div class="filters" style="justify-content:space-between">
        <input v-model="keyword" placeholder="搜索账号/昵称" class="input" style="min-width:240px" @keyup.enter="loadList(true)">
        <div style="display:flex;gap:10px">
          <button class="btn-primary" @click="loadList(true)">🔍 搜索</button>
          <button class="btn-success" @click="openCreate">➕ 新增管理员</button>
        </div>
      </div>
    </div>
    <div class="table-card">
      <div v-if="loading" class="loading">加载中...</div>
      <table v-else class="admin-table">
        <thead><tr>
          <th>ID</th><th>账号</th><th>昵称</th><th>角色</th>
          <th>手机号</th><th>邮箱</th><th>状态</th><th>创建时间</th><th style="width:200px">操作</th>
        </tr></thead>
        <tbody>
          <tr v-if="list.length === 0"><td colspan="9" class="empty">暂无数据</td></tr>
          <tr v-for="a in list" :key="a.id">
            <td>{{ a.id }}</td>
            <td style="font-weight:600">{{ a.username }}</td>
            <td>{{ a.nickname }}</td>
            <td><span class="tag">{{ a.role }}</span></td>
            <td>{{ a.phone || '-' }}</td>
            <td style="font-size:12px;color:#666">{{ a.email || '-' }}</td>
            <td><span class="tag" :class="a.status === 1 ? 'ok' : 'bad'">
              {{ a.status === 1 ? '启用' : '禁用' }}
            </span></td>
            <td>{{ a.createTime || '-' }}</td>
            <td class="actions">
              <button class="btn-link" @click="openEdit(a)">编辑</button>
              <button :class="a.status === 1 ? 'btn-warning' : 'btn-success'" @click="toggleStatus(a)">
                {{ a.status === 1 ? '禁用' : '启用' }}
              </button>
              <button class="btn-danger" @click="handleDelete(a)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="page.total > page.size" class="pager">共 {{ page.total }} 条</div>
    </div>

    <div v-if="showDialog" class="dialog-mask" @click.self="showDialog = false">
      <div class="dialog" style="width:440px">
        <h3>{{ editingId ? '编辑管理员' : '新增管理员' }}</h3>
        <form @submit="handleSubmit">
          <div class="form-group">
            <label>登录账号 <span style="color:red">*</span></label>
            <input v-model="form.username" type="text" :disabled="!!editingId" placeholder="登录账号，唯一">
          </div>
          <div class="form-group">
            <label>昵称 <span style="color:red">*</span></label>
            <input v-model="form.nickname" type="text" placeholder="显示名称">
          </div>
          <div class="form-group">
            <label>{{ editingId ? '新密码（留空不修改）' : '初始密码 *' }}</label>
            <input v-model="form.password" type="password" placeholder="至少6位">
          </div>
          <div class="form-row">
            <div class="form-group half">
              <label>手机号</label>
              <input v-model="form.phone" type="text" placeholder="可选">
            </div>
            <div class="form-group half">
              <label>邮箱</label>
              <input v-model="form.email" type="text" placeholder="可选">
            </div>
          </div>
          <div class="form-row">
            <div class="form-group half">
              <label>角色</label>
              <select v-model="form.role">
                <option value="super_admin">超级管理员</option>
                <option value="admin">普通管理员</option>
                <option value="operator">运营人员</option>
              </select>
            </div>
            <div class="form-group half">
              <label>状态</label>
              <select v-model="form.status">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </div>
          </div>
          <div class="dialog-footer">
            <button type="button" class="btn-cancel" @click="showDialog = false">取消</button>
            <button type="submit" class="btn-submit">确定</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-list { display: flex; flex-direction: column; gap: 16px; }
.filter-card { background: #fff; border-radius: 10px; padding: 16px 20px; }
.filters { display: flex; gap: 10px; }
.input { height: 36px; padding: 0 14px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; min-width: 160px; }
select { height: 36px; padding: 0 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; background:#fff }
.btn-primary, .btn-success, .btn-warning, .btn-danger, .btn-link, .btn-cancel, .btn-submit {
  height: 36px; padding: 0 14px; border-radius: 6px; font-size: 13px;
}
.btn-primary { background: #3498db; color: #fff; }
.btn-success { background: #27ae60; color: #fff; }
.btn-warning { background: #f39c12; color: #fff; }
.btn-danger { background: #e74c3c; color: #fff; }
.btn-link { background: transparent; color: #3498db; }
.btn-cancel { background: #f0f0f0; color: #666; }
.btn-submit { background: #3498db; color: #fff; }
.table-card { background: #fff; border-radius: 10px; overflow: hidden; }
.loading, .empty { padding: 40px; text-align: center; color: #999; }
.admin-table { width: 100%; border-collapse: collapse; }
.admin-table th, .admin-table td { padding: 12px 14px; text-align: left; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
.admin-table thead { background: #f7f9fc; }
.admin-table th { color: #555; font-weight: 600; }
.tag { padding: 3px 10px; border-radius: 4px; font-size: 12px; background: #f0f0f0; color: #666; }
.tag.ok { background: #eafaf0; color: #27ae60; }
.tag.bad { background: #fdecea; color: #e74c3c; }
.actions { display: flex; gap: 6px; flex-wrap: wrap; }
.pager { padding: 14px 20px; text-align: right; font-size: 13px; color: #999; border-top: 1px solid #f0f0f0; }
.form-row { display: flex; gap: 12px; }
.form-group.half { flex: 1; }
.dialog-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.dialog { background: #fff; border-radius: 12px; padding: 24px; }
.dialog h3 { font-size: 18px; margin-bottom: 16px; }
.form-group { margin-bottom: 12px; }
.form-group label { display: block; font-size: 13px; color: #555; margin-bottom: 6px; }
.form-group input, .form-group select {
  width: 100%; padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px;
}
.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
</style>
