<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { chatApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()

const targetUserId = computed(() => {
  const id = route.params.userId as string
  return id ? Number(id) : null
})

const messageInput = ref('')
const listRef = ref<HTMLDivElement | null>(null)
const loading = ref(false)

interface Message {
  notificationId: number
  senderId: number
  receiverId: number
  content: string
  createTime: string
  productId?: number
  self: boolean
}

interface Conversation {
  userId: number
  nickname: string
  avatar: string
  lastMessage: string
  lastMessageTime: string
  unreadCount: number
}

const messages = reactive<Message[]>([])
const conversations = ref<Conversation[]>([])
const activeChatId = ref<number | null>(null)
const currentChatUser = ref<{ userId: number; nickname: string; avatar: string } | null>(null)

const myId = computed(() => userStore.userInfo?.id)

async function loadConversations() {
  try {
    const res = await chatApi.getConversations()
    if (res.code === 200 && res.data) {
      conversations.value = res.data
      // 同步未读数到共享 store
      const totalUnread = res.data.reduce((sum: number, c: Conversation) => sum + (c.unreadCount || 0), 0)
      messageStore.setChatUnread(totalUnread)
    }
  } catch (e) {
    console.error('加载会话列表失败', e)
  }
}

async function openChat(userId: number) {
  if (!userId) return
  loading.value = true
  activeChatId.value = userId
  messages.length = 0
  try {
    const res = await chatApi.open(userId)
    if (res.code === 200 && res.data) {
      currentChatUser.value = res.data.otherUser
      if (res.data.messages && Array.isArray(res.data.messages)) {
        res.data.messages.forEach((m: any) => {
          messages.push({
            ...m,
            self: m.senderId === myId.value
          })
        })
      }
      // 后端已标记已读，刷新会话列表和未读数
      await loadConversations()
      scrollBottom()
    }
  } catch (e) {
    console.error('打开聊天失败', e)
  } finally {
    loading.value = false
  }
}

async function handleSend() {
  const text = messageInput.value.trim()
  if (!text || !activeChatId.value) return

  const receiverId = activeChatId.value
  const tempId = Date.now()

  messages.push({
    notificationId: tempId,
    senderId: myId.value!,
    receiverId,
    content: text,
    createTime: new Date().toISOString(),
    self: true
  })
  messageInput.value = ''
  scrollBottom()

  try {
    const res = await chatApi.send({ receiverId, content: text })
    if (res.code === 200 && res.data) {
      const idx = messages.findIndex(m => m.notificationId === tempId)
      const msg = idx !== -1 ? messages[idx] : undefined
      if (msg) {
        if (res.data.notificationId != null) msg.notificationId = res.data.notificationId
        if (res.data.createTime != null) msg.createTime = res.data.createTime
      }
      // 发送后刷新会话列表
      loadConversations()
    }
  } catch (e) {
    console.error('发送失败', e)
  }
}

function scrollBottom() {
  nextTick(() => {
    listRef.value?.scrollTo({ top: 999999, behavior: 'smooth' })
  })
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) {
    return d.toTimeString().slice(0, 5)
  }
  const diffDays = Math.floor((now.getTime() - d.getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays}天前`
  return d.toLocaleDateString()
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login' })
    return
  }
  await loadConversations()
  if (targetUserId.value) {
    openChat(targetUserId.value)
  } else {
    const first = conversations.value[0]
    if (first) {
      openChat(first.userId)
    }
  }
})

watch(targetUserId, (newId) => {
  if (newId && newId !== activeChatId.value) {
    openChat(newId)
  }
})
</script>

<template>
  <div class="chat-page">
    <h1 class="page-title">💬 私信</h1>
    <div class="chat-wrap">
      <aside class="sidebar">
        <div v-if="conversations.length === 0" class="empty-chat">
          暂无私信记录
        </div>
        <div
          v-for="c in conversations"
          :key="c.userId"
          class="chat-item"
          :class="{ active: c.userId === activeChatId }"
          @click="openChat(c.userId)"
        >
          <img :src="c.avatar || 'https://api.dicebear.com/7.x/initials/svg?seed=' + (c.nickname || c.userId)" alt="" class="avatar-img">
          <div class="info">
            <div class="top-row">
              <span class="name">{{ c.nickname || '用户' }}</span>
              <span class="time">{{ formatTime(c.lastMessageTime) }}</span>
            </div>
            <div class="last">{{ c.lastMessage || '暂无消息' }}</div>
          </div>
          <span v-if="c.unreadCount > 0" class="badge">{{ c.unreadCount }}</span>
        </div>
      </aside>
      <div class="chat-main">
        <div class="chat-header">
          {{ currentChatUser?.nickname || '聊天' }}
        </div>
        <div v-if="loading" class="loading-state">加载中...</div>
        <div v-else-if="!activeChatId" class="empty-state">选择一个会话开始聊天</div>
        <div v-else class="chat-messages" ref="listRef">
          <div v-for="m in messages" :key="m.notificationId" class="msg-wrap" :class="{ self: m.self }">
            <div class="msg-box">
              <div class="msg-text">{{ m.content }}</div>
              <div class="msg-time">{{ formatTime(m.createTime) }}</div>
            </div>
          </div>
        </div>
        <div v-if="activeChatId" class="chat-input">
          <textarea
            v-model="messageInput"
            placeholder="输入消息..."
            rows="2"
            @keyup.ctrl.enter="handleSend"
          ></textarea>
          <button @click="handleSend">发送</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 20px; }
.chat-wrap {
  background: #fff; border-radius: 10px; overflow: hidden;
  display: flex; height: 600px;
}
.sidebar {
  width: 260px; border-right: 1px solid #f0f0f0; overflow-y: auto;
}
.chat-item {
  display: flex; gap: 10px; padding: 14px; cursor: pointer;
  border-bottom: 1px solid #f8f8f8; position: relative;
  transition: background .15s;
}
.chat-item:hover, .chat-item.active {
  background: #fff6f2;
}
.avatar {
  width: 40px; height: 40px; border-radius: 50%;
  background: #f5f5f5; display: flex; align-items: center; justify-content: center;
  font-size: 22px;
}
.avatar-img {
  width: 40px; height: 40px; border-radius: 50%;
  object-fit: cover;
}
.info { flex: 1; min-width: 0; }
.top-row {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 14px; color: #333; margin-bottom: 4px;
}
.time { font-size: 11px; color: #bbb; }
.last {
  font-size: 12px; color: #999;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.badge {
  position: absolute; right: 10px; top: 14px;
  min-width: 18px; height: 18px; padding: 0 5px;
  background: #e74c3c; color: #fff; border-radius: 9px;
  font-size: 11px; display: flex; align-items: center; justify-content: center;
}
.chat-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.chat-header {
  padding: 14px 20px; border-bottom: 1px solid #f0f0f0;
  font-size: 15px; font-weight: 600; color: #333;
}
.chat-messages {
  flex: 1; overflow-y: auto; padding: 20px; background: #fafafa;
  display: flex; flex-direction: column; gap: 14px;
}
.msg-wrap { display: flex; }
.msg-wrap.self { justify-content: flex-end; }
.msg-box {
  max-width: 70%; padding: 10px 14px; border-radius: 12px;
  background: #fff; box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}
.msg-wrap.self .msg-box {
  background: #ff6b35; color: #fff;
}
.msg-text { font-size: 14px; line-height: 1.5; margin-bottom: 4px; word-break: break-all; }
.msg-time {
  font-size: 10px; color: #bbb;
}
.msg-wrap.self .msg-time { color: rgba(255,255,255,0.7); }
.chat-input {
  border-top: 1px solid #f0f0f0; padding: 14px;
  display: flex; gap: 10px;
}
.chat-input textarea {
  flex: 1; resize: none; border: 1px solid #e0e0e0;
  border-radius: 8px; padding: 10px; font-size: 14px;
}
.chat-input button {
  align-self: flex-end;
  height: 40px; padding: 0 24px;
  background: #ff6b35; color: #fff; border-radius: 8px; font-weight: 600;
}
.empty-chat {
  padding: 40px 20px;
  text-align: center;
  color: #999;
  font-size: 14px;
}
.loading-state, .empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 14px;
  background: #fafafa;
}
</style>
