<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
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

const menuItems = [
  { label: '控制台', to: '/admin/dashboard', icon: '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="9"></rect><rect x="14" y="3" width="7" height="5"></rect><rect x="14" y="12" width="7" height="9"></rect><rect x="3" y="16" width="7" height="5"></rect></svg>' },
  { label: '工单管理', to: '/admin/repairs', icon: '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>' },
  { label: '公告发布', to: '/admin/notices', icon: '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>' },
  { label: '用户管理', to: '/admin/users', icon: '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>' },
  { label: '分类设置', to: '/admin/categories', icon: '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 2 7 12 12 22 7 12 2"></polygon><polyline points="2 17 12 22 22 17"></polyline><polyline points="2 12 12 17 22 12"></polyline></svg>' },
  { label: '操作日志', to: '/admin/logs', icon: '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line></svg>' },
  { label: 'AI 洞察', to: '/admin/ai-insights', icon: '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>' }
]


const handleLogout = async () => {
  if (await confirm('确定要退出管理后台吗？')) {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <div class="brand-circles">
          <div class="circle-left"></div>
          <div class="circle-right"></div>
        </div>
        <span class="brand-text">管理系统</span>
      </div>

      <nav class="sidebar-nav">
        <RouterLink
          v-for="item in menuItems"
          :key="item.to"
          :to="item.to"
          class="nav-item"
          active-class="is-active"
        >
          <span class="nav-icon" v-html="item.icon"></span>
          <span class="nav-label">{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <button class="logout-btn" @click="handleLogout">
          <span class="nav-icon"><svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg></span>
          <span class="nav-label">退出登录</span>
        </button>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-header">
        <h2 class="page-title">{{ ($route.meta.title as string) || '系统管理' }}</h2>
        <div class="header-actions">
          <div class="header-bell" @click="router.push('/admin/notifications')">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
            </svg>
            <span v-if="unreadCount > 0" class="bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          </div>
          <div class="admin-user">
            <span class="admin-name">{{ authStore.user?.realName || '管理员' }}</span>
            <div class="admin-avatar">管</div>
          </div>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>

  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background-color: var(--mc-canvas);
}

.admin-sidebar {
  width: 260px;
  background-color: var(--mc-ink);
  color: var(--mc-white);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 100;
}

.sidebar-brand {
  padding: 32px 24px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-text {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 0.02em;
  font-family: var(--mc-font-display);
}

.brand-circles {
  display: flex;
  align-items: center;
  width: 28px;
  height: 18px;
  position: relative;
}

.circle-left, .circle-right {
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

.sidebar-nav {
  flex: 1;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--mc-radius-md);
  color: rgba(255, 255, 255, 0.5);
  text-decoration: none;
  transition: all var(--mc-transition);
  font-size: 15px;
  font-weight: 500;
}

.nav-item:hover {
  color: rgba(255, 255, 255, 0.9);
  background-color: rgba(255, 255, 255, 0.05);
}

.nav-item.is-active {
  background-color: rgba(255, 255, 255, 0.1);
  color: var(--mc-white);
  font-weight: 600;
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color var(--mc-transition);
}

.nav-item.is-active .nav-icon {
  color: var(--mc-signal);
}

.sidebar-footer {
  padding: 24px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.logout-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--mc-radius-md);
  color: var(--mc-error);
  background: none;
  border: none;
  cursor: pointer;
  transition: all var(--mc-transition);
  text-align: left;
  font-size: 14px;
  font-weight: 500;
}

.logout-btn:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.admin-main {
  flex: 1;
  margin-left: 260px;
  display: flex;
  flex-direction: column;
}

.admin-header {
  height: 80px;
  background-color: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  position: sticky;
  top: 0;
  z-index: 90;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--mc-ink);
  font-family: var(--mc-font-display);
  letter-spacing: 0.02em;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-bell {
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

.header-bell:hover {
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

.admin-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--mc-ink);
}

.admin-avatar {
  width: 36px;
  height: 36px;
  background-color: var(--mc-lifted);
  color: var(--mc-ink);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
}

.admin-content {
  padding: 40px;
  max-width: 1400px;
  width: 100%;
}
</style>


