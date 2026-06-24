<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import UiTabBar from '../components/ui/UiTabBar.vue'
import { useAuthStore } from '../stores/auth'
import { useConfirm } from '../composables/useConfirm'
import { useWebSocket } from '../composables/useWebSocket'
import { useNotification } from '../composables/useNotification'

const router = useRouter()
const authStore = useAuthStore()
const { confirm } = useConfirm()
const { connect, disconnect } = useWebSocket()
const { unreadCount, fetchUnreadCount } = useNotification()

onMounted(() => {
  connect()
  fetchUnreadCount()
})
onUnmounted(() => disconnect())

const handleLogout = async () => {
  if (await confirm('确定要退出维修后台吗？')) {
    disconnect()          // 先断开WebSocket连接
    authStore.logout()    // 再清理认证状态
    router.push('/login')
  }
}

const tabItems = [
  {
    label: '工作台',
    to: '/repairer/m/dashboard',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="9"/><rect x="14" y="3" width="7" height="5"/><rect x="14" y="12" width="7" height="9"/><rect x="3" y="16" width="7" height="5"/></svg>'
  },
  {
    label: '工单',
    to: '/repairer/m/orders',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>'
  },
  {
    label: '我的',
    to: '/repairer/m/profile',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>'
  }
]
</script>

<template>
  <div class="worker-mobile-layout">
    <header class="mobile-header">
      <div class="header-left">
        <div class="brand-circles">
          <div class="circle-left"></div>
          <div class="circle-right"></div>
        </div>
        <h1 class="header-title">{{ ($route.meta.title as string) || '维修后台' }}</h1>
      </div>
      <div class="header-actions">
        <div class="header-bell" @click="router.push('/repairer/m/notifications')">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
          <span v-if="unreadCount > 0" class="bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </div>
        <button class="logout-btn" @click="handleLogout">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
        </button>
      </div>
    </header>

    <main class="mobile-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <UiTabBar :items="tabItems" />
  </div>
</template>

<style scoped>
.worker-mobile-layout {
  min-height: 100dvh;
  background-color: var(--mc-canvas, #F3F0EE);
}

.mobile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background-color: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  position: sticky;
  top: 0;
  z-index: 900;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  padding-top: calc(12px + env(safe-area-inset-top, 0px));
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-circles {
  display: flex;
  align-items: center;
  width: 28px;
  height: 18px;
  position: relative;
}

.circle-left,
.circle-right {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  position: absolute;
}

.circle-left {
  background-color: #EB001B;
  left: 0;
  z-index: 1;
}

.circle-right {
  background-color: #F79E1B;
  right: 0;
  z-index: 2;
  opacity: 0.8;
}

.header-title {
  font-family: var(--mc-font-display, sans-serif);
  font-size: 17px;
  font-weight: 600;
  color: var(--mc-ink, #141413);
  margin: 0;
  letter-spacing: -0.01em;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-bell {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  cursor: pointer;
  color: var(--mc-ink, #141413);
  -webkit-tap-highlight-color: transparent;
}

.header-bell:active {
  background-color: rgba(0, 0, 0, 0.06);
}

.bell-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background-color: var(--mc-signal, #CF4500);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: none;
  background: none;
  color: var(--mc-muted, #696969);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: color 0.15s ease;
}

.logout-btn:active {
  color: var(--mc-error, #EB001B);
  background-color: rgba(0, 0, 0, 0.04);
}

.mobile-content {
  padding: 16px;
  padding-bottom: calc(80px + env(safe-area-inset-bottom, 0px));
}

/* Page transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
