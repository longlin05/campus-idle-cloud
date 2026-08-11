<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { chatApi, notificationApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'

const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()

// type 与后端 NotificationType 对齐：系统=2、订单=1、聊天=3
const tabs = [
  { key: 'system', label: '系统消息', icon: '🔔', type: 2 },
  { key: 'order', label: '订单消息', icon: '🛒', type: 1 },
  { key: 'chat', label: '互动消息', icon: '💬', type: 3 },
]

const activeTab = ref('system')
const loading = ref(false)
const chatConversations = ref<any[]>([])
const notifications = ref<any[]>([])

const activeTabConfig = computed(() => tabs.find(t => t.key === activeTab.value))

function getUnreadCount(tabKey: string): number {
  if (tabKey === 'system') return messageStore.systemUnread
  if (tabKey === 'order') return messageStore.orderUnread
  if (tabKey === 'chat') return messageStore.chatUnread
  return 0
}

async function loadChatConversations() {
  loading.value = true
  try {
    const res = await chatApi.getConversations()
    if (res.code === 200 && res.data) {
      chatConversations.value = res.data
      const totalUnread = res.data.reduce((sum: number, c: any) => sum + (c.unreadCount || 0), 0)
      messageStore.setChatUnread(totalUnread)
    }
  } catch (e) {
    console.error('加载会话列表失败', e)
  } finally {
    loading.value = false
  }
}

async function loadNotifications(type: number) {
  if (!userStore.userInfo?.id) return
  loading.value = true
  try {
    const res = await notificationApi.getList(userStore.userInfo.id, type)
    if (res.code === 200 && res.data) {
      notifications.value = res.data.records || []
    }
  } catch (e) {
    console.error(`加载${type === 2 ? '系统' : '订单'}消息失败`, e)
  } finally {
    loading.value = false
  }
}

function openChat(userId: number) {
  router.push({ name: 'Chat', params: { userId } })
}

async function markNotificationAsRead(notification: any) {
  if (!notification.isRead) {
    notification.isRead = 1
    try {
      await notificationApi.markReadByType(userStore.userInfo!.id, notification.type)
      if (notification.type === 2) {
        messageStore.decrementSystem()
      } else if (notification.type === 1) {
        messageStore.decrementOrder()
      }
    } catch (e) {
      console.warn('标记已读失败', e)
    }
  }
}

// 一键已读：后端 mark-all-read 会把系统/订单/聊天(type=3) 全部标记已读
async function markAllRead() {
  if (!userStore.userInfo?.id || messageStore.totalUnread === 0) return
  try {
    await notificationApi.markAllRead(userStore.userInfo.id)
    messageStore.clearAll()
    // 刷新当前 tab 列表，让已读状态立即生效
    if (activeTab.value === 'chat') {
      await loadChatConversations()
    } else {
      await loadNotifications(activeTabConfig.value?.type ?? 2)
    }
  } catch (e) {
    console.error('一键已读失败', e)
  }
}

async function handleNotificationClick(notification: any) {
  await markNotificationAsRead(notification)
  if (notification.type === 3 && notification.senderId) {
    openChat(notification.senderId)
  }
}

watch(activeTab, async (newTab) => {
  await messageStore.fetchAll()
  if (newTab === 'chat') {
    await loadChatConversations()
  } else {
    await loadNotifications(activeTabConfig.value?.type ?? 2)
  }
})

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login' })
    return
  }
  await messageStore.fetchAll()
  if (activeTab.value === 'chat') {
    await loadChatConversations()
  } else {
    await loadNotifications(activeTabConfig.value?.type ?? 2)
  }
})
</script>

<template>
  <div class="message-center">
    <h1 class="page-title">📬 消息中心</h1>
    <div class="card">
      <div class="msg-toolbar">
        <span v-if="messageStore.totalUnread > 0" class="unread-total">共 {{ messageStore.totalUnread }} 条未读</span>
        <span v-else class="unread-total">全部已读</span>
        <button class="btn-read-all" :disabled="messageStore.totalUnread === 0" @click="markAllRead">一键已读</button>
      </div>
      <div class="tabs">
        <div
          v-for="t in tabs"
          :key="t.key"
          class="tab"
          :class="{ active: activeTab === t.key }"
          @click="activeTab = t.key"
        >
          <span class="icon">{{ t.icon }}</span>
          <span>{{ t.label }}</span>
          <span v-if="getUnreadCount(t.key) > 0" class="tab-badge">{{ getUnreadCount(t.key) > 99 ? '99+' : getUnreadCount(t.key) }}</span>
        </div>
      </div>
      <div class="content">
        <!-- Loading state -->
        <div v-if="loading" class="loading">
          <div class="spinner"></div>
          <span>加载中...</span>
        </div>

        <!-- Chat messages -->
        <div v-else-if="activeTab === 'chat'">
          <div v-if="chatConversations.length === 0" class="empty">
            <div class="ph-icon">💬</div>
            <p>暂无互动消息</p>
            <p style="color:#bbb;font-size:12px;margin-top:6px">收到消息后将在这里展示</p>
          </div>
          <div v-else class="chat-list">
            <div
              v-for="c in chatConversations"
              :key="c.userId"
              class="chat-item"
              @click="openChat(c.userId)"
            >
              <img :src="c.avatar || 'https://api.dicebear.com/7.x/initials/svg?seed=' + (c.nickname || c.userId)" alt="" class="avatar">
              <div class="chat-info">
                <div class="chat-top">
                  <span class="nickname">{{ c.nickname || '用户' + c.userId }}</span>
                  <span class="time">{{ c.lastMessageTime ? new Date(c.lastMessageTime).toLocaleString() : '' }}</span>
                </div>
                <div class="last-msg">{{ c.lastMessage || '暂无消息' }}</div>
              </div>
              <span v-if="c.unreadCount > 0" class="badge">{{ c.unreadCount }}</span>
            </div>
          </div>
        </div>

        <!-- System / Order notifications -->
        <div v-else>
          <div v-if="notifications.length === 0" class="empty">
            <div class="ph-icon">{{ activeTabConfig?.icon }}</div>
            <p>暂无{{ activeTabConfig?.label }}</p>
            <p style="color:#bbb;font-size:12px;margin-top:6px">收到消息后将在这里展示</p>
          </div>
          <div v-else class="notification-list">
            <div
              v-for="n in notifications"
              :key="n.notificationId"
              class="notification-item"
              :class="{ unread: !n.isRead }"
              @click="handleNotificationClick(n)"
            >
              <div class="notif-icon">
                {{ activeTab === 'system' ? '🔔' : '🛒' }}
              </div>
              <div class="notif-content">
                <div class="notif-title">{{ n.title || (activeTab === 'system' ? '系统通知' : '订单通知') }}</div>
                <div class="notif-body">{{ n.content }}</div>
                <div class="notif-time">{{ n.createTime ? new Date(n.createTime).toLocaleString() : '' }}</div>
              </div>
              <span v-if="!n.isRead" class="unread-dot"></span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 20px; }
.card {
  background: #fff; border-radius: 10px; overflow: hidden;
}
.msg-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}
.unread-total {
  font-size: 13px;
  color: #999;
}
.btn-read-all {
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  background: #ff6b35;
  border-radius: 6px;
  transition: opacity .15s;
}
.btn-read-all:disabled {
  opacity: .55;
  cursor: not-allowed;
}
.tabs {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 16px;
}
.tab {
  display: flex; align-items: center; gap: 8px;
  padding: 16px 24px; font-size: 14px; color: #666;
  cursor: pointer; border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: color 0.2s, border-color 0.2s;
}
.tab:hover { color: #ff6b35; }
.tab.active {
  color: #ff6b35; font-weight: 600; border-bottom-color: #ff6b35;
}
.tab-badge {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: #e74c3c;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 20px;
  text-align: center;
  border-radius: 10px;
  box-shadow: 0 2px 6px rgba(231, 76, 60, 0.4);
  animation: tab-badge-pulse 1.6s ease-in-out infinite;
  white-space: nowrap;
}

@keyframes tab-badge-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.12); }
}
.content {
  padding: 20px;
  min-height: 400px;
}

/* Loading state */
.loading {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 60px 20px; color: #999;
}
.spinner {
  width: 32px; height: 32px; border: 3px solid #f0f0f0;
  border-top-color: #ff6b35; border-radius: 50%;
  animation: spin 0.8s linear infinite; margin-bottom: 12px;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Empty state */
.empty {
  text-align: center; color: #999; padding: 60px 20px;
}
.ph-icon {
  font-size: 60px; margin-bottom: 14px; opacity: 0.5;
}

/* Chat list */
.chat-list {
  display: flex; flex-direction: column;
}
.chat-item {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px; cursor: pointer;
  border-bottom: 1px solid #f8f8f8;
  transition: background 0.15s;
}
.chat-item:hover { background: #fff6f2; }
.chat-item:last-child { border-bottom: none; }
.avatar {
  width: 48px; height: 48px; border-radius: 50%;
  object-fit: cover; background: #f5f5f5;
}
.chat-info {
  flex: 1; min-width: 0;
}
.chat-top {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 4px;
}
.nickname {
  font-size: 15px; font-weight: 600; color: #333;
}
.time {
  font-size: 11px; color: #bbb;
}
.last-msg {
  font-size: 13px; color: #999;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.badge {
  min-width: 20px; height: 20px; padding: 0 6px;
  background: #e74c3c; color: #fff; border-radius: 10px;
  font-size: 11px; display: flex; align-items: center; justify-content: center;
}

/* Notification list */
.notification-list {
  display: flex; flex-direction: column;
}
.notification-item {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 14px 16px; cursor: pointer;
  border-bottom: 1px solid #f8f8f8;
  transition: background 0.15s;
  position: relative;
}
.notification-item:hover { background: #fafafa; }
.notification-item:last-child { border-bottom: none; }
.notification-item.unread { background: #fffbf8; }
.notif-icon {
  font-size: 24px; flex-shrink: 0;
}
.notif-content {
  flex: 1; min-width: 0;
}
.notif-title {
  font-size: 14px; font-weight: 600; color: #333; margin-bottom: 4px;
}
.notif-body {
  font-size: 13px; color: #666; margin-bottom: 4px;
  overflow: hidden; text-overflow: ellipsis; display: -webkit-box;
  -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}
.notif-time {
  font-size: 11px; color: #bbb;
}
.unread-dot {
  width: 8px; height: 8px; background: #ff6b35; border-radius: 50%;
  flex-shrink: 0; margin-top: 6px;
}
</style>
