<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi, uploadApi } from '@/api'
import type { Category, ProductVO } from '@/api'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const toast = useToast()

const productId = computed(() => Number(route.params.id) || 0)
const isEdit = computed(() => productId.value > 0)

const categories = ref<Category[]>([])
const form = reactive({
  title: '',
  description: '',
  categoryId: null as number | null,
  price: '' as string | number,
  originalPrice: '' as string | number | undefined,
  quantity: 1 as number,
  images: [] as string[],
})

// 上传状态管理
const uploadingCount = ref(0)
const fileInputRef = ref<HTMLInputElement | null>(null)
const loading = ref(false)
const submitting = ref(false)

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  await loadCategories()
  if (isEdit.value) await loadDetail()
})

async function loadCategories() {
  try {
    const res = await productApi.getCategories()
    if (res.code === 200) categories.value = res.data || []
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await productApi.getDetail(productId.value)
    if (res.code === 200 && res.data) {
      const d = res.data as ProductVO
      form.title = d.title
      form.description = d.description || ''
      form.categoryId = d.categoryId || null
      form.price = d.price
      form.originalPrice = d.originalPrice
      form.quantity = d.quantity || 1
      form.images = d.images || []
    }
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
}

/**
 * 点击上传按钮触发文件选择
 */
function triggerFileSelect() {
  if (form.images.length >= 9) {
    toast.warning('最多只能上传9张图片')
    return
  }
  fileInputRef.value?.click()
}

/**
 * 处理文件选择并上传
 */
async function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return

  // 计算还能上传多少张
  const remaining = 9 - form.images.length
  const filesToUpload = Array.from(files).slice(0, remaining)

  if (files.length > remaining) {
    toast.warning(`还能上传 ${remaining} 张图片，已自动截取`)
  }

  // 逐个上传文件
  for (const file of filesToUpload) {
    // 校验文件类型
    if (!file.type.startsWith('image/')) {
      toast.warning(`文件 ${file.name} 不是图片，已跳过`)
      continue
    }
    // 校验文件大小 10MB
    if (file.size > 10 * 1024 * 1024) {
      toast.warning(`图片 ${file.name} 超过10MB，已跳过`)
      continue
    }

    uploadingCount.value++
    try {
      const res = await uploadApi.uploadImage(file, 'products')
      if (res.code === 200 && res.data) {
        // 只保存服务端返回的URL，禁止使用blob
        form.images.push(res.data)
      } else {
        toast.error(`上传失败：${res.message || '未知错误'}`)
      }
    } catch (err: any) {
      toast.error(`上传 ${file.name} 失败：${err.message || '网络错误'}`)
    } finally {
      uploadingCount.value--
    }
  }

  // 清空input以允许再次选择相同文件
  input.value = ''
}

function removeImage(idx: number) {
  form.images.splice(idx, 1)
}

async function handleSubmit(e: Event) {
  e.preventDefault()
  if (!form.title.trim()) { toast.warning('请输入商品标题'); return }
  if (!form.categoryId) { toast.warning('请选择商品分类'); return }
  if (!form.price || Number(form.price) <= 0) { toast.warning('请输入正确的价格'); return }
  if (!form.quantity || Number(form.quantity) < 1) { toast.warning('商品数量至少为 1'); return }
  if (form.images.length === 0) { toast.warning('请至少上传一张商品图片'); return }
  if (uploadingCount.value > 0) { toast.warning('还有图片正在上传，请稍候'); return }

  submitting.value = true
  try {
    const body = {
      title: form.title,
      description: form.description,
      categoryId: form.categoryId!,
      price: Number(form.price),
      originalPrice: form.originalPrice ? Number(form.originalPrice) : undefined,
      quantity: Number(form.quantity),
      images: form.images,
    }
    let res
    if (isEdit.value) {
      res = await productApi.update(productId.value, body)
    } else {
      res = await productApi.create(body)
    }
    if (res.code === 200) {
      toast.success(isEdit.value ? '更新成功！' : '发布成功！')
      router.push('/user-center')
    } else {
      toast.error(res.message || '操作失败')
    }
  } catch (e: any) {
    toast.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="publish-page">
    <div class="page-head">
      <a @click="router.back()" class="back">← 返回</a>
      <h1>{{ isEdit ? '✏️ 编辑商品' : '➕ 发布商品' }}</h1>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <form v-else class="form" @submit="handleSubmit">
      <div class="form-group">
        <label>商品标题 <span class="required">*</span></label>
        <input
          v-model="form.title"
          type="text"
          maxlength="80"
          placeholder="请输入商品标题（建议20字以内，吸引人）"
        >
      </div>

      <div class="form-group">
        <label>商品分类 <span class="required">*</span></label>
        <select v-model="form.categoryId">
          <option :value="null" disabled>请选择分类</option>
          <option v-for="c in categories" :key="c.categoryId || c.id" :value="c.categoryId || c.id">{{ c.categoryName || c.name }}</option>
        </select>
      </div>

      <div class="form-row">
        <div class="form-group half">
          <label>售价 (元) <span class="required">*</span></label>
          <input
            v-model="form.price"
            type="number"
            step="0.01"
            min="0"
            placeholder="请输入售价"
          >
        </div>
        <div class="form-group half">
          <label>原价 (元，可选)</label>
          <input
            v-model="form.originalPrice"
            type="number"
            step="0.01"
            min="0"
            placeholder="购买时的原价"
          >
        </div>
        <div class="form-group half">
          <label>数量 <span class="required">*</span></label>
          <input
            v-model="form.quantity"
            type="number"
            min="1"
            step="1"
            placeholder="请输入商品数量"
          >
        </div>
      </div>

      <div class="form-group">
        <label>商品描述</label>
        <textarea
          v-model="form.description"
          rows="5"
          maxlength="2000"
          placeholder="详细描述你的商品，如新旧程度、购买日期、使用情况等"
        ></textarea>
      </div>

      <div class="form-group">
        <label>商品图片 <span class="required">*</span> (最多9张)</label>
        <div class="uploader">
          <div v-for="(img, idx) in form.images" :key="idx" class="image-item">
            <img :src="img" alt="">
            <button type="button" class="remove" @click="removeImage(idx)">×</button>
          </div>
          <div v-if="form.images.length < 9" class="add-image-btn" @click="triggerFileSelect">
            <div class="plus-icon">+</div>
            <div class="add-text">
              {{ uploadingCount > 0 ? `上传中(${uploadingCount})...` : '点击上传图片' }}
            </div>
          </div>
          <!-- 隐藏的文件选择框，支持多选 -->
          <input
            ref="fileInputRef"
            type="file"
            accept="image/*"
            multiple
            style="display: none"
            @change="handleFileChange"
          >
        </div>
        <p class="tip">提示：点击上方按钮从本地选择图片，支持多选。第一张图将作为商品封面。单张图片不超过10MB。</p>
      </div>

      <div class="form-actions">
        <button type="button" class="btn-cancel" @click="router.back()">取消</button>
        <button type="submit" class="btn-submit" :disabled="submitting">
          {{ submitting ? '提交中...' : (isEdit ? '保存修改' : '立即发布') }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.page-head h1 {
  font-size: 22px;
  color: #333;
}

.back {
  color: #ff6b35;
  font-size: 14px;
  cursor: pointer;
}

.loading {
  background: #fff;
  border-radius: 10px;
  padding: 60px 20px;
  text-align: center;
  color: #999;
}

.form {
  background: #fff;
  border-radius: 10px;
  padding: 24px 28px;
  max-width: 800px;
  margin: 0 auto;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  font-weight: 600;
}

.required {
  color: #e74c3c;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  border-color: #ff6b35;
  background: #fffaf7;
}

.form-row {
  display: flex;
  gap: 20px;
}

.form-group.half {
  flex: 1;
}

.uploader {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 6px;
}

.image-item {
  position: relative;
  width: 110px;
  height: 110px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #eee;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-item .remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(0,0,0,0.6);
  color: #fff;
  font-size: 16px;
  line-height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-image-btn {
  width: 110px;
  height: 110px;
  border: 2px dashed #ddd;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  transition: all 0.2s;
  color: #999;
  background: #fafafa;
}

.add-image-btn:hover {
  border-color: #ff6b35;
  color: #ff6b35;
  background: #fffaf7;
}

.plus-icon {
  font-size: 32px;
  line-height: 1;
  font-weight: 300;
}

.add-text {
  font-size: 12px;
}

.tip {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.btn-cancel {
  height: 42px;
  padding: 0 24px;
  border-radius: 8px;
  border: 1px solid #ddd;
  background: #fff;
  color: #666;
  font-size: 14px;
}

.btn-submit {
  height: 42px;
  padding: 0 32px;
  background: linear-gradient(90deg, #ff6b35 0%, #f7931e 100%);
  color: #fff;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
}

.btn-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
