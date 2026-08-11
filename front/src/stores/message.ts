import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi, chatApi } from '@/api'
import { useUserStore } from './user'

export const useMessageStore = defineStore('message', () => {
  const systemUnread = ref(0)
  const orderUnread = ref(0)
  const chatUnread = ref(0)

  const totalUnread = computed(() =>
    systemUnread.value + orderUnread.value + chatUnread.value
  )

  async function fetchAll() {
    const userStore = useUserStore()
    const userId = userStore.userInfo?.id
    if (!userId) {
      systemUnread.value = 0
      orderUnread.value = 0
      chatUnread.value = 0
      return
    }
    try {
      const [byTypeRes, chatRes] = await Promise.all([
        notificationApi.unreadCountByType(userId),
        chatApi.unreadCount(userId)
      ])
      if (byTypeRes?.code === 200 && byTypeRes.data) {
        // 后端 count-by-type 返回 {0,1,2}：0=无类型兜底、1=订单、2=系统；聊天(3)走 chatApi
        systemUnread.value = byTypeRes.data['2'] || 0
        orderUnread.value = byTypeRes.data['1'] || 0
      }
      if (chatRes?.code === 200 && typeof chatRes.data === 'number') {
        if (chatRes.data > 0 || chatUnread.value === 0) {
          chatUnread.value = chatRes.data
        }
      }
    } catch (e) {
      console.warn('加载未读数失败', e)
    }
  }

  function decrementSystem() {
    if (systemUnread.value > 0) systemUnread.value--
  }

  function decrementOrder() {
    if (orderUnread.value > 0) orderUnread.value--
  }

  function decrementChat(count = 1) {
    chatUnread.value = Math.max(0, chatUnread.value - count)
  }

  function setChatUnread(count: number) {
    chatUnread.value = Math.max(0, count)
  }

  function clearAll() {
    systemUnread.value = 0
    orderUnread.value = 0
    chatUnread.value = 0
  }

  return {
    systemUnread,
    orderUnread,
    chatUnread,
    totalUnread,
    fetchAll,
    decrementSystem,
    decrementOrder,
    decrementChat,
    setChatUnread,
    clearAll,
  }
})
