<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '@/api'
import { useAdminStore } from '@/stores/admin'

const router = useRouter()
const adminStore = useAdminStore()

const form = ref({ username: '', password: '', captcha: '' })
const message = ref('')
const loading = ref(false)

onMounted(() => {
  if (adminStore.isLoggedIn) router.replace('/admin/dashboard')
})

async function handleSubmit(e: Event) {
  e.preventDefault()
  message.value = ''
  if (!form.value.username || !form.value.password) {
    message.value = '请输入账号和密码'
    return
  }
  loading.value = true
  try {
    const res = await adminApi.login(form.value.username, form.value.password)
    if (res.code === 200 && res.data) {
      adminStore.setLogin(res.data)
      router.replace('/admin/dashboard')
    } else {
      message.value = res.message || '登录失败'
    }
  } catch (e: any) {
    message.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="admin-login">
    <div class="login-box">
      <div class="logo-box">
        <img src="@/assets/web-image.png" alt="">
        <h1>管理后台</h1>
        <p>校园闲置物品交易平台 - 管理系统</p>
      </div>
      <form class="form" @submit="handleSubmit" novalidate>
        <div class="form-group">
          <span class="prefix">👤</span>
          <input
            v-model="form.username"
            type="text"
            placeholder="管理员账号"
            autocomplete="username"
          >
        </div>
        <div class="form-group">
          <span class="prefix">🔒</span>
          <input
            v-model="form.password"
            type="password"
            placeholder="登录密码"
            autocomplete="current-password"
          >
        </div>
        <div class="form-group">
          <span class="prefix">🔑</span>
          <input
            v-model="form.captcha"
            type="text"
            placeholder="验证码（可留空）"
          >
        </div>
        <button type="submit" class="btn-submit" :disabled="loading">
          {{ loading ? '登录中...' : '立即登录' }}
        </button>
        <div v-if="message" class="form-message">{{ message }}</div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.admin-login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 50%, #667eea 100%);
}

.login-box {
  width: 400px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  padding: 36px 36px 28px;
}

.logo-box {
  text-align: center;
  margin-bottom: 28px;
}

.logo-box img {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  margin-bottom: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.logo-box h1 {
  font-size: 20px;
  color: #1e3c72;
  margin-bottom: 4px;
}

.logo-box p {
  font-size: 12px;
  color: #999;
}

.form-group {
  position: relative;
  margin-bottom: 16px;
}

.form-group .prefix {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 15px;
  opacity: 0.7;
}

.form-group input {
  width: 100%;
  height: 44px;
  padding: 0 14px 0 40px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-group input:focus {
  border-color: #2a5298;
  background: #f8fbff;
}

.btn-submit {
  width: 100%;
  height: 44px;
  background: linear-gradient(90deg, #1e3c72 0%, #2a5298 100%);
  color: #fff;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  margin-top: 8px;
}

.btn-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.form-message {
  margin-top: 12px;
  font-size: 13px;
  color: #e74c3c;
  text-align: center;
}
</style>
