<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginMode = ref<'password' | 'sms'>('password')
const form = reactive({
  phone: '',
  password: '',
  code: '',
  agreement: true,
})

const message = ref('')
const sending = ref(false)
const countdown = ref(0)
const loading = ref(false)

const redirect = computed(() => route.query.redirect as string || '/')

onMounted(() => {
  if (userStore.isLoggedIn) {
    router.replace(redirect.value)
  }
})

function switchMode(mode: 'password' | 'sms') {
  loginMode.value = mode
  message.value = ''
}

async function sendCode() {
  if (!/^1\d{10}$/.test(form.phone)) {
    message.value = '请输入正确的11位手机号'
    return
  }
  if (sending.value || countdown.value > 0) return
  sending.value = true
  try {
    const res = await authApi.sendCode(form.phone)
    if (res.code === 200) {
      message.value = '验证码已发送'
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) clearInterval(timer)
      }, 1000)
    } else {
      message.value = res.message || '发送失败'
    }
  } catch (e: any) {
    message.value = e.message || '发送失败'
  } finally {
    sending.value = false
  }
}

async function handleSubmit(e: Event) {
  e.preventDefault()
  message.value = ''
  if (!form.agreement) {
    message.value = '请先阅读并同意用户协议'
    return
  }
  if (!/^1\d{10}$/.test(form.phone)) {
    message.value = '请输入正确的11位手机号'
    return
  }
  loading.value = true
  try {
    let res
    if (loginMode.value === 'password') {
      if (!form.password) {
        message.value = '请输入密码'
        return
      }
      res = await authApi.loginByPassword(form.phone, form.password)
    } else {
      if (!form.code) {
        message.value = '请输入验证码'
        return
      }
      res = await authApi.loginBySms(form.phone, form.code)
    }
    if (res.code === 200 && res.data) {
      userStore.setLogin(res.data)
      router.replace(redirect.value)
    } else {
      message.value = res.message || '登录失败'
    }
  } catch (e: any) {
    message.value = e.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <header class="auth-header">
      <div class="container">
        <router-link to="/" class="logo">
          <img src="@/assets/web-image.png" alt="大学二手交易平台">
          <span>大学二手交易平台</span>
        </router-link>
      </div>
    </header>

    <main class="auth-main">
      <div class="auth-card">
        <h1>用户登录</h1>
        <p class="auth-subtitle">登录后即可发布与购买校园二手商品</p>

        <div class="login-tabs">
          <button type="button" :class="{ active: loginMode === 'password' }" @click="switchMode('password')">密码登录</button>
          <button type="button" :class="{ active: loginMode === 'sms' }" @click="switchMode('sms')">验证码登录</button>
        </div>

        <form class="auth-form" @submit="handleSubmit" novalidate>
          <div class="form-group">
            <label for="phone">手机号<span class="required">*</span></label>
            <input
              id="phone"
              v-model="form.phone"
              type="tel"
              maxlength="11"
              placeholder="请输入11位手机号码"
              autocomplete="tel"
            >
          </div>

          <div v-if="loginMode === 'password'" class="form-panel">
            <div class="form-group">
              <label for="password">密码<span class="required">*</span></label>
              <input
                id="password"
                v-model="form.password"
                type="password"
                placeholder="请输入登录密码"
                autocomplete="current-password"
              >
            </div>
          </div>

          <div v-else class="form-panel">
            <div class="form-group">
              <label for="code">验证码<span class="required">*</span></label>
              <div class="code-row">
                <input
                  id="code"
                  v-model="form.code"
                  type="text"
                  maxlength="6"
                  placeholder="请输入短信验证码"
                  autocomplete="one-time-code"
                >
                <button type="button" class="btn-code" :disabled="sending || countdown > 0" @click="sendCode">
                  {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
                </button>
              </div>
            </div>
          </div>

          <label class="form-agree">
            <input v-model="form.agreement" type="checkbox">
            <span>我已阅读并同意<a href="#">《用户协议》</a>和<a href="#">《隐私政策》</a></span>
          </label>

          <div class="form-row">
            <router-link to="/forget-password" class="forgot-pwd">忘记密码？</router-link>
          </div>

          <button type="submit" class="btn-submit" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>

          <div v-if="message" class="form-message" role="alert">{{ message }}</div>

          <div class="auth-bottom">
            <span>还没有账号？</span>
            <router-link to="/register" class="register-link">免费注册</router-link>
          </div>
        </form>
      </div>
    </main>

    <footer class="auth-footer">
      <p>&copy; 2026 大学二手交易平台 版权所有 Lin</p>
    </footer>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff6f2 0%, #ffe8d6 50%, #ffd4b5 100%);
  display: flex;
  flex-direction: column;
}

.container {
  width: 1100px;
  margin: 0 auto;
}

.auth-header {
  background: rgba(255,255,255,0.9);
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.auth-header .container {
  display: flex;
  align-items: center;
  height: 70px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  color: #ff6b35;
}

.logo img {
  width: 36px;
  height: 36px;
  border-radius: 8px;
}

.auth-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.auth-card {
  width: 420px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 10px 40px rgba(255,107,53,0.12);
  padding: 36px 36px 28px;
}

.auth-card h1 {
  font-size: 22px;
  color: #333;
  margin-bottom: 6px;
}

.auth-subtitle {
  font-size: 13px;
  color: #999;
  margin-bottom: 20px;
}

.login-tabs {
  display: flex;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
}

.login-tabs button {
  flex: 1;
  padding: 12px 0;
  background: transparent;
  font-size: 15px;
  color: #666;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}

.login-tabs button.active {
  color: #ff6b35;
  border-bottom-color: #ff6b35;
  font-weight: 600;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 13px;
  color: #555;
  margin-bottom: 6px;
}

.form-group .required {
  color: #e74c3c;
  margin-left: 2px;
}

.form-group input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-group input:focus {
  border-color: #ff6b35;
  background: #fffaf7;
}

.code-row {
  display: flex;
  gap: 10px;
}

.code-row input {
  flex: 1;
}

.btn-code {
  height: 42px;
  padding: 0 16px;
  border-radius: 8px;
  background: #fff2eb;
  color: #ff6b35;
  font-size: 13px;
  white-space: nowrap;
}

.btn-code:disabled {
  color: #aaa;
  background: #f5f5f5;
  cursor: not-allowed;
}

.form-agree {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: #666;
  margin: 8px 0 12px;
  cursor: pointer;
}

.form-agree input {
  margin-top: 3px;
}

.form-agree a {
  color: #ff6b35;
}

.form-row {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.forgot-pwd {
  font-size: 13px;
  color: #ff6b35;
}

.btn-submit {
  width: 100%;
  height: 44px;
  background: linear-gradient(90deg, #ff6b35 0%, #f7931e 100%);
  color: #fff;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
}

.btn-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.form-message {
  margin-top: 10px;
  font-size: 13px;
  color: #e74c3c;
  text-align: center;
}

.auth-bottom {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  text-align: center;
  font-size: 13px;
  color: #888;
}

.register-link {
  color: #ff6b35;
  font-weight: 600;
  margin-left: 4px;
}

.auth-footer {
  text-align: center;
  padding: 18px;
  color: #999;
  font-size: 12px;
  background: rgba(255,255,255,0.7);
}
</style>
