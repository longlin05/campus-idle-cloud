<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const toast = useToast()
const step = ref(1)

const form = reactive({
  phone: '',
  code: '',
  password: '',
  confirm: '',
})

const message = ref('')
const countdown = ref(0)
const sending = ref(false)
const loading = ref(false)

async function sendCode() {
  if (!/^1\d{10}$/.test(form.phone)) {
    message.value = '请输入正确的手机号'
    return
  }
  sending.value = true
  try {
    const res = await authApi.sendCode(form.phone)
    if (res.code === 200) {
      message.value = '验证码已发送'
      countdown.value = 60
      const t = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) clearInterval(t)
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

async function handleStep1(e: Event) {
  e.preventDefault()
  if (!/^1\d{10}$/.test(form.phone)) { message.value = '请输入正确的手机号'; return }
  if (!form.code) { message.value = '请输入验证码'; return }
  loading.value = true
  try {
    // 模拟验证码校验（实际后端处理）
    step.value = 2
    message.value = ''
  } finally {
    loading.value = false
  }
}

async function handleStep2(e: Event) {
  e.preventDefault()
  if (!form.password || form.password.length < 6) {
    message.value = '密码至少6位'; return
  }
  if (form.password !== form.confirm) {
    message.value = '两次输入的密码不一致'; return
  }
  loading.value = true
  try {
    const res = await authApi.resetPassword(form.phone, form.code, form.password)
    if (res.code === 200) {
      toast.success('密码重置成功，请使用新密码登录')
      router.replace({ name: 'Login', query: { phone: form.phone } })
    } else {
      message.value = res.message || '重置失败'
    }
  } catch (e: any) {
    message.value = e.message || '重置失败'
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
          <img src="@/assets/web-image.png" alt="">
          <span>大学二手交易平台</span>
        </router-link>
      </div>
    </header>

    <main class="auth-main">
      <div class="auth-card">
        <h1>找回密码</h1>
        <p class="auth-subtitle">通过手机验证码重置密码</p>

        <div class="steps">
          <div class="step" :class="{ active: step >= 1, done: step > 1 }">1. 身份验证</div>
          <div class="line" :class="{ done: step > 1 }"></div>
          <div class="step" :class="{ active: step >= 2 }">2. 重置密码</div>
        </div>

        <form v-if="step === 1" class="auth-form" @submit="handleStep1" novalidate>
          <div class="form-group">
            <label for="phone">手机号<span class="required">*</span></label>
            <input id="phone" v-model="form.phone" type="tel" maxlength="11" placeholder="请输入注册手机号">
          </div>
          <div class="form-group">
            <label for="code">验证码<span class="required">*</span></label>
            <div class="code-row">
              <input id="code" v-model="form.code" type="text" maxlength="6" placeholder="短信验证码">
              <button type="button" class="btn-code" :disabled="sending || countdown > 0" @click="sendCode">
                {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
              </button>
            </div>
          </div>
          <button type="submit" class="btn-submit" :disabled="loading">{{ loading ? '验证中...' : '下一步' }}</button>
          <div v-if="message" class="form-message">{{ message }}</div>
          <div class="auth-bottom">
            <span>想起密码了？</span>
            <router-link to="/login" class="register-link">立即登录</router-link>
          </div>
        </form>

        <form v-else class="auth-form" @submit="handleStep2" novalidate>
          <div class="form-group">
            <label for="pwd">新密码<span class="required">*</span></label>
            <input id="pwd" v-model="form.password" type="password" placeholder="至少6位">
          </div>
          <div class="form-group">
            <label for="cpwd">确认新密码<span class="required">*</span></label>
            <input id="cpwd" v-model="form.confirm" type="password" placeholder="再次输入新密码">
          </div>
          <button type="button" class="btn-back" @click="step = 1">← 上一步</button>
          <button type="submit" class="btn-submit" :disabled="loading">{{ loading ? '提交中...' : '重置密码' }}</button>
          <div v-if="message" class="form-message">{{ message }}</div>
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
.container { width: 1100px; margin: 0 auto; }
.auth-header {
  background: rgba(255,255,255,0.9);
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.auth-header .container { display: flex; align-items: center; height: 70px; }
.logo { display: flex; align-items: center; gap: 10px; font-size: 18px; font-weight: 700; color: #ff6b35; }
.logo img { width: 36px; height: 36px; border-radius: 8px; }
.auth-main {
  flex: 1; display: flex; align-items: center; justify-content: center;
  padding: 40px 20px;
}
.auth-card {
  width: 420px; background: #fff; border-radius: 14px;
  box-shadow: 0 10px 40px rgba(255,107,53,0.12);
  padding: 36px 36px 28px;
}
.auth-card h1 { font-size: 22px; color: #333; margin-bottom: 6px; }
.auth-subtitle { font-size: 13px; color: #999; margin-bottom: 20px; }

.steps { display: flex; align-items: center; margin-bottom: 24px; }
.step {
  flex: 0 0 auto; padding: 6px 14px; border-radius: 16px;
  background: #f0f0f0; color: #999; font-size: 13px;
}
.step.active { background: #fff2eb; color: #ff6b35; font-weight: 600; }
.step.done { background: #eafaf0; color: #27ae60; }
.line {
  flex: 1; height: 2px; margin: 0 8px;
  background: #eee;
}
.line.done { background: #27ae60; }

.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; color: #555; margin-bottom: 6px; }
.form-group .required { color: #e74c3c; margin-left: 2px; }
.form-group input {
  width: 100%; height: 42px; padding: 0 14px;
  border: 1px solid #ddd; border-radius: 8px; font-size: 14px;
}
.form-group input:focus {
  border-color: #ff6b35; background: #fffaf7;
}
.code-row { display: flex; gap: 10px; }
.code-row input { flex: 1; }
.btn-code {
  height: 42px; padding: 0 16px;
  background: #fff2eb; color: #ff6b35;
  border-radius: 8px; font-size: 13px; white-space: nowrap;
}
.btn-code:disabled { color: #aaa; background: #f5f5f5; cursor: not-allowed; }

.btn-submit, .btn-back {
  height: 44px; border-radius: 8px;
  font-size: 16px; font-weight: 600;
}
.btn-submit {
  width: 100%;
  background: linear-gradient(90deg, #ff6b35 0%, #f7931e 100%);
  color: #fff;
}
.btn-submit:disabled { opacity: 0.7; cursor: not-allowed; }
.btn-back {
  width: auto; padding: 0 20px;
  background: #f0f0f0; color: #666;
  margin-bottom: 12px;
}
.form-message {
  margin-top: 10px; font-size: 13px; color: #e74c3c; text-align: center;
}
.auth-bottom {
  margin-top: 20px; padding-top: 16px; border-top: 1px solid #f0f0f0;
  text-align: center; font-size: 13px; color: #888;
}
.register-link { color: #ff6b35; font-weight: 600; margin-left: 4px; }
.auth-footer {
  text-align: center; padding: 18px; color: #999; font-size: 12px;
  background: rgba(255,255,255,0.7);
}
</style>
