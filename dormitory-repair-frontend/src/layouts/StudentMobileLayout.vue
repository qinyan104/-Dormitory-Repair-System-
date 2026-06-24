<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import UiTabBar from '../components/ui/UiTabBar.vue'
import { useAuthStore } from '../stores/auth'
import { useWebSocket } from '../composables/useWebSocket'
import { useNotification } from '../composables/useNotification'

const authStore = useAuthStore()
const { connect, disconnect } = useWebSocket()
const { unreadCount, fetchUnreadCount } = useNotification()

onMounted(() => {
  connect()
  fetchUnreadCount()
})
onUnmounted(() => disconnect())

const tabItems = [
  {
    label: '首页',
    to: '/student/m/home',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>'
  },
  {
    label: '报修',
    to: '/student/m/create',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="12" y1="18" x2="12" y2="12"/><line x1="9" y1="15" x2="15" y2="15"/></svg>'
  },
  {
    label: '工单',
    to: '/student/m/list',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>'
  },
  {
    label: '我的',
    to: '/student/m/profile',
    icon: '<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>'
  }
]
</script>

<template>
  <div class="student-mobile-layout">
    <header class="mobile-header">
      <div class="header-brand">
        <div class="brand-circles">
          <div class="circle-left"></div>
          <div class="circle-right"></div>
        </div>
        <span class="brand-name">宿舍报修</span>
      </div>
      <div class="header-actions">
        <div class="header-bell" @click="$router.push('/student/m/notifications')">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
          <span v-if="unreadCount > 0" class="bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </div>
        <div class="header-avatar" @click="$router.push('/student/m/profile')">
          <img v-if="authStore.user?.avatar" :src="authStore.user.avatar" alt="Avatar" class="avatar-img" />
          <span v-else>{{ authStore.user?.realName?.charAt(0) || 'U' }}</span>
        </div>
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
.student-mobile-layout {
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

.header-brand {
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

.brand-name {
  font-family: var(--mc-font-display, sans-serif);
  font-size: 16px;
  font-weight: 600;
  color: var(--mc-ink, #141413);
  letter-spacing: -0.02em;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
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

.header-avatar {
  width: 36px;
  height: 36px;
  background-color: var(--mc-ink, #141413);
  color: var(--mc-white, #fff);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
