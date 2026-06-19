<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import UiNavPill from '../components/ui/UiNavPill.vue'
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

const navItems = [
  { label: '首页', to: '/student/home' },
  { label: '我要报修', to: '/student/repair/create' },
  { label: '我的报修', to: '/student/repair/list' },
  { label: '个人中心', to: '/student/profile' }
]

</script>

<template>
  <div class="student-layout">
    <header class="layout-header">
      <UiNavPill :items="navItems">
        <template #actions>
          <div class="nav-bell" @click="$router.push('/student/notifications')">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
            </svg>
            <span v-if="unreadCount > 0" class="bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </div>
          <div class="nav-user-avatar" @click="$router.push('/student/profile')">
            <img v-if="authStore.user?.avatar" :src="authStore.user?.avatar" alt="Avatar" class="avatar-img" />
            <span v-else>{{ authStore.user?.realName?.charAt(0) || 'U' }}</span>
          </div>
        </template>
      </UiNavPill>
    </header>
    <main class="layout-content">
      <router-view />
    </main>

  </div>
</template>

<style scoped>
.student-layout {
  min-height: 100vh;
  padding-bottom: var(--mc-sp-section);
  background-color: var(--mc-canvas);
}

.layout-header {
  padding-top: 56px;
}

.layout-content {
  margin-top: 100px;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
  padding: 0 24px;
}

.nav-user-avatar {
  width: 36px;
  height: 36px;
  background-color: var(--mc-ink);
  color: var(--mc-white);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-family: var(--mc-font-display);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
}

.nav-user-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.nav-bell {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  color: var(--mc-ink);
  transition: background-color 0.15s;
}

.nav-bell:hover {
  background-color: rgba(0, 0, 0, 0.06);
}

.bell-badge {
  position: absolute;
  top: 0;
  right: 0;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background-color: var(--mc-signal);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

</style>

