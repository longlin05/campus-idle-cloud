<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { productApi, adminCategoryApi } from '@/api'
import type { Category } from '@/api'
import { useToast } from '@/composables/useToast'
import { formatTime } from '@/utils/format'

const toast = useToast()
const list = ref<Category[]>([])
const loading = ref(false)
const showDialog = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ name: '', description: '', sort: 0 })

onMounted(() => loadList())

async function loadList() {
  loading.value = true
  try {
    const res = await productApi.getCategories()
    if (res.code === 200) list.value = res.data || []
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  form.name = ''; form.description = ''; form.sort = 0
  showDialog.value = true
}

function openEdit(c: Category) {
  editingId.value = (c.categoryId || c.id)!
  form.name = c.categoryName || c.name
  form.description = c.categoryDesc || c.description || ''
  form.sort = c.sortOrder || c.sort || 0
  showDialog.value = true
}

async function handleSubmit(e: Event) {
  e.preventDefault()
  if (!form.name.trim()) { toast.warning('请输入分类名称'); return }
  try {
    const body = { name: form.name, description: form.description, sort: form.sort }
    let res
    if (editingId.value) {
      res = await adminCategoryApi.update(editingId.value, body)
    } else {
      res = await adminCategoryApi.create(body)
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

async function handleDelete(c: Category) {
  if (!confirm(`确定删除分类「${c.categoryName || c.name}」？`)) return
  try {
    const res = await adminCategoryApi.remove((c.categoryId || c.id)!)
    if (res.code === 200) { loadList() }
    else toast.error(res.message || '操作失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}
</script>

<template>
  <div class="admin-list">
    <div class="filter-card">
      <div class="filters" style="justify-content:space-between">
        <span style="font-size:14px;color:#666">共 {{ list.length }} 个分类</span>
        <button class="btn-primary" @click="openCreate">➕ 新增分类</button>
      </div>
    </div>
    <div class="table-card">
      <div v-if="loading" class="loading">加载中...</div>
      <table v-else class="admin-table">
        <thead><tr><th style="width:80px">ID</th><th>名称</th><th>描述</th><th>排序</th><th>创建时间</th><th style="width:180px">操作</th></tr></thead>
        <tbody>
          <tr v-if="list.length === 0"><td colspan="6" class="empty">暂无分类，点击右上角新增</td></tr>
          <tr v-for="c in list" :key="c.categoryId || c.id">
            <td>{{ c.categoryId || c.id }}</td>
            <td style="font-weight:600">{{ c.categoryName || c.name }}</td>
            <td>{{ c.categoryDesc || c.description || '-' }}</td>
            <td>{{ c.sortOrder || c.sort || 0 }}</td>
            <td>{{ formatTime((c as any).createTime) }}</td>
            <td class="actions">
              <button class="btn-link" @click="openEdit(c)">编辑</button>
              <button class="btn-danger" @click="handleDelete(c)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showDialog" class="dialog-mask" @click.self="showDialog = false">
      <div class="dialog">
        <h3>{{ editingId ? '编辑分类' : '新增分类' }}</h3>
        <form @submit="handleSubmit">
          <div class="form-group">
            <label>分类名称 <span style="color:red">*</span></label>
            <input v-model="form.name" type="text" maxlength="20" placeholder="请输入分类名称">
          </div>
          <div class="form-group">
            <label>描述</label>
            <textarea v-model="form.description" rows="2" placeholder="描述（可选）"></textarea>
          </div>
          <div class="form-group">
            <label>排序（数字越小越靠前）</label>
            <input v-model.number="form.sort" type="number">
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
.btn-primary, .btn-danger, .btn-link, .btn-cancel, .btn-submit {
  height: 36px; padding: 0 14px; border-radius: 6px; font-size: 13px;
}
.btn-primary { background: #3498db; color: #fff; }
.btn-danger { background: #e74c3c; color: #fff; }
.btn-link { background: transparent; color: #3498db; }
.btn-cancel { background: #f0f0f0; color: #666; }
.btn-submit { background: #3498db; color: #fff; }

.table-card { background: #fff; border-radius: 10px; overflow: hidden; }
.loading, .empty { padding: 40px; text-align: center; color: #999; }
.admin-table { width: 100%; border-collapse: collapse; }
.admin-table th, .admin-table td { padding: 14px 16px; text-align: left; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
.admin-table thead { background: #f7f9fc; }
.admin-table th { color: #555; font-weight: 600; }
.actions { display: flex; gap: 8px; }

.dialog-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.dialog {
  width: 420px; background: #fff; border-radius: 12px; padding: 24px;
}
.dialog h3 { font-size: 18px; margin-bottom: 18px; }
.form-group { margin-bottom: 14px; }
.form-group label { display: block; font-size: 13px; color: #555; margin-bottom: 6px; }
.form-group input, .form-group textarea {
  width: 100%; padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px;
}
.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; }
</style>
