<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { useConfirm } from '../../composables/useConfirm'
import { useWebSocket } from '../../composables/useWebSocket'

const router = useRouter()
const authStore = useAuthStore()
const { confirm } = useConfirm()
const { disconnect } = useWebSocket()

const handleLogout = async () => {
  if (await confirm('确定要退出管理系统吗？')) {
    disconnect()          // 先断开WebSocket连接
    authStore.logout()    // 再清理认证状态
    router.push('/login')
  }
}

const menuItems = [
  { label: '公告发布', route: '/admin/notices', icon: '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>' },
  { label: '分类设置', route: '/admin/categories', icon: '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 2 7 12 12 22 7 12 2"></polygon><polyline points="2 17 12 22 22 17"></polyline><polyline points="2 12 12 17 22 12"></polyline></svg>' },
  { label: '操作日志', route: '/admin/logs', icon: '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line></svg>' }
]
</script>

<template>
  <div class="profile-view">
    <div class="profile-card">
      <div class="user-info">
        <div class="avatar">管</div>
        <div class="details">
          <h2 class="name">{{ authStore.user?.realName || '管理员' }}</h2>
          <p class="role">系统管理员</p>
        </div>
      </div>
      
      <div class="menu-list">
        <div v-for="item in menuItems" :key="item.route" class="menu-item" @click="router.push(item.route)">
          <span class="menu-icon" v-html="item.icon"></span>
          <span class="menu-label">{{ item.label }}</span>
          <span class="arrow">&rarr;</span>
        </div>
        
        <div class="menu-divider"></div>
        
        <div class="menu-item logout-item" @click="handleLogout">
          <span class="menu-icon logout-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
          </span>
          <span class="menu-label logout-text">退出登录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-view {
  max-width: 600px;
  margin: 0 auto;
}

.profile-card {
  background-color: var(--mc-white);
  border-radius: var(--mc-radius-lg);
  padding: 24px;
  border: 1px solid var(--mc-hairline);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--mc-hairline);
}

.avatar {
  width: 64px;
  height: 64px;
  background-color: var(--mc-lifted);
  color: var(--mc-ink);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 600;
}

.name {
  font-size: 20px;
  font-weight: 600;
  color: var(--mc-ink);
  margin-bottom: 4px;
}

.role {
  font-size: 14px;
  color: var(--mc-muted);
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background-color: var(--mc-canvas);
  border-radius: var(--mc-radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
}

.menu-item:active {
  background-color: var(--mc-hairline);
}

.menu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--mc-ink);
}

.menu-label {
  flex: 1;
  font-size: 16px;
  font-weight: 500;
  color: var(--mc-ink);
}

.arrow {
  color: var(--mc-muted-soft);
  font-size: 18px;
}

.menu-divider {
  height: 1px;
  background-color: var(--mc-hairline);
  margin: 16px 0;
}

.logout-icon {
  color: var(--mc-error);
}

.logout-text {
  color: var(--mc-error);
}

.logout-item:active {
  background-color: rgba(235, 0, 27, 0.05);
}
</style>
