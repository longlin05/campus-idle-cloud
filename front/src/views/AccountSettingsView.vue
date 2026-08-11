<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { userApi, uploadApi } from '@/api'
import { useToast } from '@/composables/useToast'

const userStore = useUserStore()
const toast = useToast()
const saving = ref(false)
const avatarUploading = ref(false)
const avatarFileInput = ref<HTMLInputElement | null>(null)

/**
 * 表单字段严格对齐 sys_user 表：nickname / avatar / phone / email
 * phone 由后端返回，前端只读展示；avatar 通过 OSS 上传获得 URL
 */
const form = reactive({
  nickname: userStore.userInfo?.nickname || '',
  phone: userStore.userInfo?.phone || '',
  email: userStore.userInfo?.email || '',
  avatar: userStore.userInfo?.avatar || '',
})

const passwordForm = reactive({ oldPwd: '', newPwd: '', confirm: '' })
const pwdSaving = ref(false)

function triggerAvatarSelect() {
  avatarFileInput.value?.click()
}

async function handleAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return

  const file = files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    toast.warning('请选择图片文件')
    input.value = ''
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    toast.warning('头像图片不能超过5MB')
    input.value = ''
    return
  }

  avatarUploading.value = true
  try {
    const res = await uploadApi.uploadImage(file, 'avatars')
    if (res.code === 200 && res.data) {
      form.avatar = res.data
      // 头像上传到 OSS 成功后立即保存到服务器，同步 campus_auth 库，
      // 避免重新登录后仍显示旧头像
      const saveRes = await userApi.updateAvatar(res.data)
      if (saveRes.code === 200 && saveRes.data) {
        userStore.updateUserInfo({ avatar: res.data })
      } else {
        toast.error(`头像保存失败：${saveRes.message || '未知错误'}`)
      }
    } else {
      toast.error(`上传失败：${res.message || '未知错误'}`)
    }
  } catch (err: any) {
    toast.error(`头像上传失败：${err.message || '网络错误'}`)
  } finally {
    avatarUploading.value = false
    input.value = ''
  }
}

async function handleSave() {
  saving.value = true
  try {
    const res = await userApi.updateProfile(form)
    if (res.code === 200) {
      userStore.updateUserInfo(form)
      toast.success('保存成功')
    } else {
      toast.error(res.message || '保存失败')
    }
  } catch (e: any) {
    toast.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleChangePwd() {
  if (!passwordForm.oldPwd || !passwordForm.newPwd || !passwordForm.confirm) {
    toast.warning('请完整填写密码信息'); return
  }
  if (passwordForm.newPwd.length < 6) { toast.warning('新密码至少6位'); return }
  if (passwordForm.newPwd !== passwordForm.confirm) { toast.warning('两次输入的新密码不一致'); return }
  pwdSaving.value = true
  try {
    const res = await userApi.changePassword(passwordForm.oldPwd, passwordForm.newPwd)
    if (res.code === 200) {
      toast.success('密码修改成功')
      passwordForm.oldPwd = ''; passwordForm.newPwd = ''; passwordForm.confirm = ''
    } else toast.error(res.message || '操作失败')
  } catch (e: any) { toast.error(e.message || '操作失败') }
  finally { pwdSaving.value = false }
}
</script>

<template>
  <div class="page">
    <h1 class="page-title">⚙️ 账号设置</h1>
    <div class="two-col">
      <div class="card">
        <h3>基本信息</h3>
        <div class="form-group">
          <label>头像</label>
          <div class="avatar-row">
            <div class="avatar-uploader" @click="triggerAvatarSelect">
              <img :src="form.avatar || 'https://via.placeholder.com/64'" class="big-avatar">
              <div class="avatar-mask" v-if="!avatarUploading">
                <span>更换头像</span>
              </div>
              <div class="avatar-mask uploading" v-else>
                <span>上传中...</span>
              </div>
            </div>
            <div class="avatar-tip">
              <p>点击头像可更换图片</p>
              <p class="tip-small">支持 JPG/PNG 格式，大小不超过 5MB</p>
            </div>
            <input
              ref="avatarFileInput"
              type="file"
              accept="image/*"
              style="display: none"
              @change="handleAvatarChange"
            >
          </div>
        </div>
        <div class="form-group">
          <label>昵称</label>
          <input v-model="form.nickname" type="text" maxlength="20">
        </div>
        <div class="form-group">
          <label>手机号 (不可修改)</label>
          <input :value="form.phone" type="text" disabled>
        </div>
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="form.email" type="email">
        </div>
        <button class="btn-primary" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存修改' }}
        </button>
      </div>

      <div class="card">
        <h3>修改密码</h3>
        <div class="form-group">
          <label>原密码</label>
          <input v-model="passwordForm.oldPwd" type="password" placeholder="请输入原密码">
        </div>
        <div class="form-group">
          <label>新密码</label>
          <input v-model="passwordForm.newPwd" type="password" placeholder="至少6位">
        </div>
        <div class="form-group">
          <label>确认新密码</label>
          <input v-model="passwordForm.confirm" type="password" placeholder="再次输入新密码">
        </div>
        <button class="btn-primary" :disabled="pwdSaving" @click="handleChangePwd">
          {{ pwdSaving ? '提交中...' : '修改密码' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 20px; }
.two-col {
  display: grid; grid-template-columns: 1fr 1fr; gap: 20px;
}
.card {
  background: #fff; border-radius: 10px; padding: 24px;
}
.card h3 {
  font-size: 17px; margin-bottom: 20px; padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}
.form-group { margin-bottom: 16px; }
.form-group label {
  display: block; font-size: 13px; color: #555; margin-bottom: 6px;
}
.form-group input, .form-group select, .form-group textarea {
  width: 100%; padding: 9px 12px;
  border: 1px solid #ddd; border-radius: 6px; font-size: 13px;
}
.form-group input:disabled { background: #f5f5f5; color: #999; }
.form-row { display: flex; gap: 16px; }
.form-group.half { flex: 1; }
.avatar-row { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.avatar-uploader {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid #f0f0f0;
}
.avatar-uploader:hover .avatar-mask {
  opacity: 1;
}
.big-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.avatar-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}
.avatar-mask.uploading {
  opacity: 1;
  background: rgba(0, 0, 0, 0.6);
}
.avatar-mask span {
  color: #fff;
  font-size: 11px;
  text-align: center;
  line-height: 1.2;
}
.avatar-tip p {
  margin: 0;
  font-size: 13px;
  color: #555;
}
.avatar-tip .tip-small {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.btn-primary {
  height: 40px; padding: 0 28px;
  background: #ff6b35; color: #fff;
  border-radius: 6px; font-weight: 600;
}
.btn-primary:disabled { opacity: .7; cursor: not-allowed; }
@media (max-width: 900px) {
  .two-col { grid-template-columns: 1fr; }
}
</style>
