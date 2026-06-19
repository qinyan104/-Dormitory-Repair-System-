<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import UiCard from '../../components/ui/UiCard.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import UiButton from '../../components/ui/UiButton.vue'
import {
  getNotificationPageApi,
  markNotificationReadApi,
  markAllNotificationsReadApi
} from '../../api/notification'
import { useToast } from '../../composables/useToast'
import { useNotification } from '../../composables/useNotification'
import { useAuthStore } from '../../stores/auth'
import { getPageRecords } from '../../utils/page'

const router = useRouter()
const toast = useToast()
const { unreadCount } = useNotification()
const authStore = useAuthStore()

const loading = ref(false)
const notifications = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 15

const fetchNotifications = async () => {
  loading.value = true
  try {
    const data: any = await getNotificationPageApi({ pageNum: pageNum.value, pageSize })
    notifications.value = getPageRecords(data)
    total.value = data.total || 0
  } catch (err: any) {
    toast.error(err.message || '获取通知失败')
  } finally {
    loading.value = false
  }
}

const handleRead = async (item: any) => {
  if (item.isRead === 0) {
    try {
      await markNotificationReadApi(item.id)
      item.isRead = 1
      if (unreadCount.value > 0) unreadCount.value--
    } catch { /* ignore */ }
  }
  if (item.orderId) {
    const role = authStore.user?.role || 'STUDENT'
    if (role === 'STUDENT') {
      router.push(`/student/repair/detail/${item.orderId}`)
    } else if (role === 'REPAIRER') {
      router.push(`/repairer/orders/${item.orderId}`)
    } else if (role === 'ADMIN') {
      router.push(`/admin/repairs?orderId=${item.orderId}`)
    }
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllNotificationsReadApi()
    notifications.value.forEach(n => { n.isRead = 1 })
    unreadCount.value = 0
    toast.success('全部已标为已读')
  } catch (err: any) {
    toast.error(err.message || '操作失败')
  }
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ')
}

onMounted(fetchNotifications)
</script>

<template>
  <div class="notification-view">
    <div class="header-row">
      <UiSectionTitle eyebrow="消息中心" title="我的通知" />
      <UiButton
        v-if="unreadCount > 0"
        type="secondary"
        @click="handleMarkAllRead"
      >
        全部已读 ({{ unreadCount }})
      </UiButton>
    </div>

    <UiCard padding="0" class="notification-card">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="notifications.length === 0" class="empty-state">
        暂无通知
      </div>
      <div v-else class="notification-list">
        <div
          v-for="item in notifications"
          :key="item.id"
          :class="['notification-item', { 'is-unread': item.isRead === 0 }]"
          @click="handleRead(item)"
        >
          <div class="noti-dot" v-if="item.isRead === 0"></div>
          <div class="noti-body">
            <div class="noti-title">{{ item.title }}</div>
            <div class="noti-content">{{ item.content }}</div>
            <div class="noti-time">{{ formatTime(item.createTime) }}</div>
          </div>
        </div>
      </div>

      <div v-if="total > pageSize" class="pagination">
        <span>共 {{ total }} 条</span>
        <div class="page-btns">
          <UiButton type="secondary" :disabled="pageNum <= 1" @click="pageNum--; fetchNotifications()">上一页</UiButton>
          <span class="current">{{ pageNum }}</span>
          <UiButton type="secondary" :disabled="pageNum * pageSize >= total" @click="pageNum++; fetchNotifications()">下一页</UiButton>
        </div>
      </div>
    </UiCard>
  </div>
</template>

<style scoped>
.notification-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.notification-card {
  overflow: hidden;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 48px;
  color: var(--mc-muted);
  font-size: 14px;
}

.notification-list {
  display: flex;
  flex-direction: column;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--mc-hairline);
  cursor: pointer;
  transition: background-color 0.15s;
}

.notification-item:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.notification-item.is-unread {
  background-color: rgba(235, 0, 27, 0.03);
}

.noti-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--mc-signal);
  margin-top: 6px;
  flex-shrink: 0;
}

.noti-body {
  flex: 1;
  min-width: 0;
}

.noti-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--mc-ink);
  margin-bottom: 4px;
}

.noti-content {
  font-size: 13px;
  color: var(--mc-charcoal);
  line-height: 1.5;
}

.noti-time {
  font-size: 12px;
  color: var(--mc-muted);
  margin-top: 6px;
}

.pagination {
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--mc-muted);
  border-top: 1px solid var(--mc-canvas);
}

.page-btns {
  display: flex;
  align-items: center;
  gap: 12px;
}

.current {
  font-weight: 500;
  color: var(--mc-ink);
}
</style>
