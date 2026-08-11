<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { adminSystemImageApi } from '@/api'
import type { SystemImageVO, PageResult } from '@/api'
import { useToast } from '@/composables/useToast'
import { formatTime } from '@/utils/format'

const toast = useToast()
const list = ref<SystemImageVO[]>([])
const page = reactive({ current: 1, size: 20, total: 0 })
const loading = ref(false)
const showDialog = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  file: null as File | null,
  imageName: '',
  sortOrder: 0,
})

onMounted(() => loadList())

async function loadList(reset = false) {
  if (reset) page.current = 1
  loading.value = true
  try {
    const res = await adminSystemImageApi.getList(0, page.current, page.size)
    if (res.code === 200) {
      const data = res.data as PageResult<SystemImageVO>
      list.value = data.records || []
      page.total = data.total || 0
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  form.file = null
  form.imageName = ''
  form.sortOrder = 0
  showDialog.value = true
}

function openEdit(img: SystemImageVO) {
  editingId.value = img.imageId
  form.file = null
  form.imageName = img.imageName || ''
  form.sortOrder = img.sortOrder || 0
  showDialog.value = true
}

function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  form.file = input.files?.[0] || null
}

async function handleSubmit(e: Event) {
  e.preventDefault()
  try {
    if (editingId.value) {
      // 编辑：只更新名称和排序（不换图）
      const res = await adminSystemImageApi.update(editingId.value, {
        imageName: form.imageName,
        sortOrder: form.sortOrder,
      })
      if (res.code === 200) {
        toast.success('更新成功')
        showDialog.value = false
        loadList()
      } else {
        toast.error(res.message || '更新失败')
      }
    } else {
      // 新增：必须选择图片
      if (!form.file) { toast.warning('请选择图片'); return }
      const res = await adminSystemImageApi.upload(form.file, {
        imageName: form.imageName,
        type: 0, // 轮播图
        sortOrder: form.sortOrder,
      })
      if (res.code === 200) {
        toast.success('上传成功')
        showDialog.value = false
        loadList(true)
      } else {
        toast.error(res.message || '上传失败')
      }
    }
  } catch (err: any) {
    toast.error(err.message || '操作失败')
  }
}

async function toggleStatus(img: SystemImageVO) {
  const next = img.status === 1 ? 0 : 1
  try {
    const res = await adminSystemImageApi.toggleStatus(img.imageId, next)
    if (res.code === 200) {
      toast.success(next === 1 ? '已启用' : '已禁用')
      loadList()
    } else {
      toast.error(res.message || '操作失败')
    }
  } catch (err: any) {
    toast.error(err.message || '操作失败')
  }
}

async function handleDelete(img: SystemImageVO) {
  if (!confirm(`确定删除该轮播图「${img.imageName || img.imageUrl}」？`)) return
  try {
    const res = await adminSystemImageApi.remove(img.imageId)
    if (res.code === 200) {
      toast.success('删除成功')
      loadList()
    } else {
      toast.error(res.message || '操作失败')
    }
  } catch (err: any) {
    toast.error(err.message || '操作失败')
  }
}
</script>

<template>
  <div class="admin-list">
    <div class="filter-card">
      <div class="filters" style="justify-content:space-between">
        <span style="font-size:14px;color:#666">首页轮播图（共 {{ page.total }} 张）</span>
        <button class="btn-primary" @click="openCreate">➕ 新增轮播图</button>
      </div>
    </div>
    <div class="table-card">
      <div v-if="loading" class="loading">加载中...</div>
      <table v-else class="admin-table">
        <thead><tr>
          <th style="width:110px">预览</th><th>名称</th><th>排序</th><th>状态</th><th>上传时间</th><th style="width:200px">操作</th>
        </tr></thead>
        <tbody>
          <tr v-if="list.length === 0"><td colspan="6" class="empty">暂无轮播图，点击右上角新增</td></tr>
          <tr v-for="img in list" :key="img.imageId">
            <td>
              <img :src="img.imageUrl" class="thumb" alt="">
            </td>
            <td style="font-weight:600;max-width:220px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ img.imageName || '-' }}</td>
            <td>{{ img.sortOrder }}</td>
            <td>
              <span class="tag" :class="img.status === 1 ? 'ok' : 'bad'">{{ img.status === 1 ? '启用' : '禁用' }}</span>
            </td>
            <td>{{ formatTime(img.createTime) }}</td>
            <td class="actions">
              <button class="btn-link" @click="openEdit(img)">编辑</button>
              <button class="btn-link" @click="toggleStatus(img)">{{ img.status === 1 ? '禁用' : '启用' }}</button>
              <button class="btn-danger" @click="handleDelete(img)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="page.total > page.size" class="pager">共 {{ page.total }} 张，第 {{ page.current }} 页</div>
    </div>

    <div v-if="showDialog" class="dialog-mask" @click.self="showDialog = false">
      <div class="dialog">
        <h3>{{ editingId ? '编辑轮播图' : '新增轮播图' }}</h3>
        <form @submit="handleSubmit">
          <div v-if="!editingId" class="form-group">
            <label>选择图片 <span style="color:red">*</span>（jpg/jpeg/png/gif/webp）</label>
            <input type="file" accept="image/*" @change="onFileChange">
          </div>
          <div class="form-group">
            <label>名称</label>
            <input v-model="form.imageName" type="text" maxlength="50" placeholder="可选，默认取文件名">
          </div>
          <div class="form-group">
            <label>排序（数字越小越靠前）</label>
            <input v-model.number="form.sortOrder" type="number">
          </div>
          <div class="dialog-footer">
            <button type="button" class="btn-cancel" @click="showDialog = false">取消</button>
            <button type="submit" class="btn-submit">{{ editingId ? '保存' : '上传' }}</button>
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
.admin-table th, .admin-table td { padding: 12px 14px; text-align: left; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
.admin-table thead { background: #f7f9fc; }
.admin-table th { color: #555; font-weight: 600; }
.thumb { width: 90px; height: 45px; object-fit: cover; border-radius: 6px; background: #f0f0f0; }
.tag { padding: 3px 10px; border-radius: 4px; font-size: 12px; }
.tag.ok { background: #eafaf0; color: #27ae60; }
.tag.bad { background: #fdecea; color: #e74c3c; }
.actions { display: flex; gap: 8px; }
.pager { padding: 14px 20px; text-align: right; font-size: 13px; color: #999; border-top: 1px solid #f0f0f0; }

.dialog-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.dialog { width: 420px; background: #fff; border-radius: 12px; padding: 24px; }
.dialog h3 { font-size: 18px; margin-bottom: 18px; }
.form-group { margin-bottom: 14px; }
.form-group label { display: block; font-size: 13px; color: #555; margin-bottom: 6px; }
.form-group input { width: 100%; padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; }
</style>
