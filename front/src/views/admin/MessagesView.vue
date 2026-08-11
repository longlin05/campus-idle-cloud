<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { adminMessageApi } from '@/api'
import type { PageResult } from '@/api'
import { useToast } from '@/composables/useToast'
import { formatTime } from '@/utils/format'

interface MsgItem { id: number; title: string; content: string; type: number; batchNo?: string; batchSize?: number; createTime?: string }

const toast = useToast()
const keyword = ref('')
const list = ref<MsgItem[]>([])
const page = reactive({ current: 1, size: 10, total: 0 })
const loading = ref(false)
const showDialog = ref(false)
const form = reactive({
  title: '', content: '', targetType: 0, userId: '' as string | number,
})

const typeMap: Record<number, string> = { 2: '系统通知' }

onMounted(() => loadList())

async function loadList(reset = false) {
  if (reset) page.current = 1
  loading.value = true
  try {
    const res = await adminMessageApi.getList(keyword.value, page.current, page.size)
    if (res.code === 200) {
      const data = res.data as PageResult<MsgItem>
      list.value = data.records || []
      page.total = data.total || 0
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function openCreate() {
  form.title = ''; form.content = ''; form.targetType = 0; form.userId = ''
  showDialog.value = true
}

async function handleSubmit(e: Event) {
  e.preventDefault()
  if (!form.title.trim() || !form.content.trim()) { toast.warning('请填写完整标题和内容'); return }
  if (form.targetType === 1 && !form.userId) { toast.warning('请指定用户ID'); return }
  try {
    const res = await adminMessageApi.send({
      title: form.title,
      content: form.content,
      type: 2, // 系统通知
      targetType: form.targetType,
      userId: form.targetType === 1 ? Number(form.userId) : undefined,
    })
    if (res.code === 200) {
      toast.success('发送成功')
      showDialog.value = false
      loadList()
    } else {
      toast.error(res.message || '发送失败')
    }
  } catch (e: any) { toast.error(e.message || '发送失败') }
}

async function handleDelete(m: MsgItem) {
  if (!confirm('确定删除该消息？将同时删除该批次所有用户的接收记录')) return
  try {
    const res = await adminMessageApi.remove(m.id)
    if (res.code === 200) loadList()
    else toast.error(res.message || '操作失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}
</script>

<template>
  <div class="admin-list">
    <div class="filter-card">
      <div class="filters" style="justify-content:space-between">
        <input v-model="keyword" placeholder="搜索标题/内容" class="input" style="min-width:240px" @keyup.enter="loadList(true)">
        <div style="display:flex;gap:10px">
          <button class="btn-primary" @click="loadList(true)">🔍 搜索</button>
          <button class="btn-success" @click="openCreate">📢 发送消息</button>
        </div>
      </div>
    </div>
    <div class="table-card">
      <div v-if="loading" class="loading">加载中...</div>
      <table v-else class="admin-table">
        <thead><tr><th>ID</th><th>标题</th><th>类型</th><th>接收人数</th><th>内容摘要</th><th>发送时间</th><th style="width:100px">操作</th></tr></thead>
        <tbody>
          <tr v-if="list.length === 0"><td colspan="7" class="empty">暂无消息</td></tr>
          <tr v-for="m in list" :key="m.id">
            <td>{{ m.id }}</td>
            <td style="font-weight:500">{{ m.title }}</td>
            <td><span class="tag info">{{ typeMap[m.type] || '消息' }}</span></td>
            <td>{{ m.batchSize != null ? `${m.batchSize} 人` : '-' }}</td>
            <td style="max-width:260px;color:#666;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ m.content }}</td>
            <td>{{ formatTime(m.createTime) }}</td>
            <td><button class="btn-danger" @click="handleDelete(m)">删除</button></td>
          </tr>
        </tbody>
      </table>
      <div v-if="page.total > page.size" class="pager">共 {{ page.total }} 条</div>
    </div>

    <div v-if="showDialog" class="dialog-mask" @click.self="showDialog = false">
      <div class="dialog" style="width:480px">
        <h3>📢 发送系统消息</h3>
        <form @submit="handleSubmit">
          <div class="form-group">
            <label>接收对象</label>
            <select v-model="form.targetType">
              <option :value="0">全体用户</option>
              <option :value="1">指定用户</option>
            </select>
          </div>
          <div v-if="form.targetType === 1" class="form-group">
            <label>用户ID</label>
            <input v-model="form.userId" type="number" placeholder="请输入用户ID">
          </div>
          <div class="form-group">
            <label>消息标题 <span style="color:red">*</span></label>
            <input v-model="form.title" type="text" maxlength="50" placeholder="请输入标题">
          </div>
          <div class="form-group">
            <label>消息内容 <span style="color:red">*</span></label>
            <textarea v-model="form.content" rows="4" maxlength="1000" placeholder="请输入内容"></textarea>
          </div>
          <div class="dialog-footer">
            <button type="button" class="btn-cancel" @click="showDialog = false">取消</button>
            <button type="submit" class="btn-submit">发送</button>
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
.input { height: 36px; padding: 0 14px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; min-width: 180px; }
select { height: 36px; padding: 0 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; }
.btn-primary, .btn-success, .btn-danger, .btn-cancel, .btn-submit {
  height: 36px; padding: 0 14px; border-radius: 6px; font-size: 13px;
}
.btn-primary { background: #3498db; color: #fff; }
.btn-success { background: #27ae60; color: #fff; }
.btn-danger { background: #e74c3c; color: #fff; }
.btn-cancel { background: #f0f0f0; color: #666; }
.btn-submit { background: #27ae60; color: #fff; }
.table-card { background: #fff; border-radius: 10px; overflow: hidden; }
.loading, .empty { padding: 40px; text-align: center; color: #999; }
.admin-table { width: 100%; border-collapse: collapse; }
.admin-table th, .admin-table td { padding: 12px 14px; text-align: left; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
.admin-table thead { background: #f7f9fc; }
.admin-table th { color: #555; font-weight: 600; }
.tag { padding: 3px 10px; border-radius: 4px; font-size: 12px; }
.tag.info { background: #eaf5ff; color: #3498db; }
.pager { padding: 14px 20px; text-align: right; font-size: 13px; color: #999; border-top: 1px solid #f0f0f0; }
.dialog-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.dialog { background: #fff; border-radius: 12px; padding: 24px; }
.dialog h3 { font-size: 18px; margin-bottom: 16px; }
.form-group { margin-bottom: 12px; }
.form-group label { display: block; font-size: 13px; color: #555; margin-bottom: 6px; }
.form-group input, .form-group select, .form-group textarea {
  width: 100%; padding: 8px 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px;
}
.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
</style>
