<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { userApi } from '@/api'
import { useToast } from '@/composables/useToast'

interface Address {
  id: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault?: number
}

const list = ref<Address[]>([])
const loading = ref(false)
const showDialog = ref(false)
const editing = ref<Address | null>(null)
const toast = useToast()
const form = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false,
})

onMounted(loadList)

function fullAddress(a: Address) {
  return `${a.province || ''}${a.city || ''}${a.district || ''}${a.detail || ''}`
}

async function loadList() {
  loading.value = true
  try {
    const res = await userApi.getAddresses()
    if (res.code === 200) list.value = res.data || []
  } catch (e) { list.value = [] }
  finally { loading.value = false }
}

function openCreate() {
  editing.value = null
  form.receiverName = ''
  form.receiverPhone = ''
  form.province = ''
  form.city = ''
  form.district = ''
  form.detail = ''
  form.isDefault = false
  showDialog.value = true
}

function openEdit(a: Address) {
  editing.value = a
  form.receiverName = a.receiverName
  form.receiverPhone = a.receiverPhone
  form.province = a.province || ''
  form.city = a.city || ''
  form.district = a.district || ''
  form.detail = a.detail || ''
  form.isDefault = a.isDefault === 1
  showDialog.value = true
}

async function handleSubmit(e: Event) {
  e.preventDefault()
  if (!form.receiverName || !form.receiverPhone) {
    toast.warning('请填写收货人和手机号'); return
  }
  if (!form.province || !form.city || !form.district) {
    toast.warning('请选择省市区'); return
  }
  if (!form.detail) {
    toast.warning('请填写详细地址'); return
  }
  try {
    const body = {
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      province: form.province,
      city: form.city,
      district: form.district,
      detail: form.detail,
      isDefault: form.isDefault ? 1 : 0,
    }
    let res
    if (editing.value) {
      res = await userApi.updateAddress(editing.value.id, body)
    } else {
      res = await userApi.addAddress(body)
    }
    if (res.code === 200) {
      showDialog.value = false
      loadList()
      toast.success(editing.value ? '地址已更新' : '地址已添加')
    } else toast.error(res.message || '操作失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}

async function handleDelete(a: Address) {
  if (!confirm('确定删除该地址？')) return
  try {
    await userApi.removeAddress(a.id)
    loadList()
    toast.success('删除成功')
  } catch (e: any) { toast.error(e.message || '删除失败') }
}

async function setDefault(a: Address) {
  try {
    await userApi.setDefaultAddress(a.id)
    loadList()
    toast.success('已设为默认')
  } catch (e: any) { toast.error(e.message || '操作失败') }
}

const sortedList = computed(() => {
  return [...list.value].sort((a, b) => (b.isDefault || 0) - (a.isDefault || 0))
})
</script>

<template>
  <div class="page">
    <div class="head">
      <h1 class="page-title">📮 收货地址</h1>
      <button class="btn-primary" @click="openCreate">➕ 新增地址</button>
    </div>

    <div class="card">
      <div v-if="loading" class="empty">加载中...</div>
      <div v-else-if="list.length === 0" class="empty">
        还没有收货地址，<span class="link" @click="openCreate">去添加一个吧</span>
      </div>
      <div v-else class="addr-list">
        <div
          v-for="a in sortedList"
          :key="a.id"
          class="addr-item"
          :class="{ default: a.isDefault === 1 }"
        >
          <div class="info">
            <div class="top">
              <span class="name">{{ a.receiverName }}</span>
              <span class="phone">{{ a.receiverPhone }}</span>
              <span v-if="a.isDefault === 1" class="tag">默认</span>
            </div>
            <div class="address">{{ fullAddress(a) }}</div>
          </div>
          <div class="actions">
            <button v-if="a.isDefault !== 1" class="btn-link" @click="setDefault(a)">设为默认</button>
            <button class="btn-link" @click="openEdit(a)">编辑</button>
            <button class="btn-danger" @click="handleDelete(a)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showDialog" class="dialog-mask" @click.self="showDialog = false">
      <div class="dialog">
        <h3>{{ editing ? '编辑地址' : '新增地址' }}</h3>
        <form @submit="handleSubmit">
          <div class="form-group">
            <label>收货人 <span style="color:red">*</span></label>
            <input v-model="form.receiverName" type="text" placeholder="请输入收货人姓名">
          </div>
          <div class="form-group">
            <label>手机号 <span style="color:red">*</span></label>
            <input v-model="form.receiverPhone" type="tel" maxlength="11" placeholder="请输入手机号">
          </div>
          <div class="form-row">
            <div class="form-group col-4">
              <label>省份 <span style="color:red">*</span></label>
              <input v-model="form.province" type="text" placeholder="省/直辖市">
            </div>
            <div class="form-group col-4">
              <label>城市 <span style="color:red">*</span></label>
              <input v-model="form.city" type="text" placeholder="市">
            </div>
            <div class="form-group col-4">
              <label>区/县 <span style="color:red">*</span></label>
              <input v-model="form.district" type="text" placeholder="区/县">
            </div>
          </div>
          <div class="form-group">
            <label>详细地址 <span style="color:red">*</span></label>
            <textarea v-model="form.detail" rows="2" placeholder="详细街道门牌号"></textarea>
          </div>
          <label class="default-row">
            <input v-model="form.isDefault" type="checkbox">
            <span>设为默认地址</span>
          </label>
          <div class="dialog-footer">
            <button type="button" class="btn-cancel" @click="showDialog = false">取消</button>
            <button type="submit" class="btn-primary">保存</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 22px; }
.btn-primary,.btn-cancel,.btn-danger,.btn-link {
  height: 36px; padding: 0 16px; border-radius: 6px; font-size: 13px;
}
.btn-primary { background: #ff6b35; color: #fff; border: none; cursor: pointer; }
.btn-primary:hover { background: #e55a2b; }
.btn-cancel { background: #f0f0f0; color: #666; border: none; cursor: pointer; }
.btn-danger { background: transparent; color: #e74c3c; border: none; cursor: pointer; }
.btn-link { background: transparent; color: #3498db; border: none; cursor: pointer; }
.card { background: #fff; border-radius: 10px; padding: 20px; }
.empty { padding: 50px; text-align: center; color: #999; }
.link { color: #ff6b35; cursor: pointer; }
.addr-list { display: flex; flex-direction: column; gap: 12px; }
.addr-item {
  padding: 16px; border: 1px solid #eee; border-radius: 8px;
  display: flex; justify-content: space-between; align-items: center; gap: 16px;
  transition: border-color .2s;
}
.addr-item:hover { border-color: #ffd4b5; }
.addr-item.default {
  border-color: #ff6b35;
  background: #fffaf7;
}
.info { flex: 1; }
.top { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.name { font-weight: 600; color: #333; }
.phone { color: #666; }
.tag {
  padding: 1px 8px; border-radius: 10px; font-size: 11px;
  background: #ff6b35; color: #fff;
}
.address { font-size: 13px; color: #666; }
.actions { display: flex; gap: 6px; flex-shrink: 0; }

.dialog-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.dialog {
  width: 480px; background: #fff; border-radius: 12px; padding: 24px;
}
.dialog h3 { font-size: 18px; margin-bottom: 18px; color: #333; }
.form-group { margin-bottom: 14px; }
.form-group label { display: block; font-size: 13px; color: #555; margin-bottom: 6px; }
.form-group input, .form-group textarea {
  width: 100%; padding: 9px 12px;
  border: 1px solid #ddd; border-radius: 6px; font-size: 13px;
  box-sizing: border-box;
}
.form-group input:focus, .form-group textarea:focus {
  outline: none; border-color: #ff6b35;
}
.form-row {
  display: flex; gap: 10px; margin-bottom: 14px;
}
.form-row .form-group { margin-bottom: 0; }
.col-4 { flex: 1; }
.default-row {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 13px; color: #666; margin-bottom: 14px;
}
.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; }
</style>
