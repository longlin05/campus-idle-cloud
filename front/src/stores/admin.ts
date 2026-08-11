import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface AdminInfo {
  id?: number
  username?: string
  nickname?: string
  avatar?: string
  role?: number | string
  phone?: string
  email?: string
  [k: string]: any
}

export const useAdminStore = defineStore('admin', () => {
  const token = ref<string>(localStorage.getItem('admin_token') || '')
  const adminInfo = ref<AdminInfo | null>(() => {
    const saved = localStorage.getItem('adminInfo')
    return saved ? JSON.parse(saved) : null
  })

  const isLoggedIn = computed(() => !!token.value)

  function setLogin(data: { token: string; admin?: AdminInfo; user?: AdminInfo }) {
    token.value = data.token
    const info = data.admin || data.user || {}
    adminInfo.value = info
    localStorage.setItem('admin_token', data.token)
    localStorage.setItem('adminInfo', JSON.stringify(info))
  }

  function logout() {
    token.value = ''
    adminInfo.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('adminInfo')
  }

  return {
    token,
    adminInfo,
    isLoggedIn,
    setLogin,
    logout,
  }
})
