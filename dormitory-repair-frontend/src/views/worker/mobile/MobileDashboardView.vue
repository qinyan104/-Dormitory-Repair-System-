<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getWorkerOrdersApi } from '../../../api/repair'
import { REPAIR_STATUS_MAP } from '../../../constants/repair'

const router = useRouter()

const stats = ref({
  assigned: 0,
  repairing: 0,
  waitingConfirm: 0,
  completedToday: 0
})

const recentOrders = ref<any[]>([])
const loading = ref(true)

const fetchData = async () => {
  loading.value = true
  try {
    const allOrders: any = await getWorkerOrdersApi({ pageNum: 1, pageSize: 100 })
    const orders = allOrders?.records || allOrders || []
    recentOrders.value = (orders as any[]).slice(0, 6)

    const today = new Date().toISOString().slice(0, 10)
    stats.value.assigned = (orders as any[]).filter((o: any) => o.repairStatus === 2).length
    stats.value.repairing = (orders as any[]).filter((o: any) => o.repairStatus === 3).length
    stats.value.waitingConfirm = (orders as any[]).filter((o: any) => o.repairStatus === 4).length
    stats.value.completedToday = (orders as any[]).filter((o: any) => o.repairStatus === 5 && o.studentConfirmTime?.startsWith(today)).length
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}

onMounted(fetchData)
</script>

<template>
  <div class="mobile-dashboard">
    <h2 class="page-title">今日概览</h2>

    <div class="stats-grid">
      <template v-if="loading">
        <div v-for="i in 4" :key="i" class="stat-card skeleton-card">
          <div class="skeleton-num"></div>
          <div class="skeleton-label"></div>
        </div>
      </template>
      <template v-else>
        <div class="stat-card">
          <span class="stat-number">{{ stats.assigned }}</span>
          <span class="stat-label">待接单</span>
        </div>
        <div class="stat-card">
          <span class="stat-number">{{ stats.repairing }}</span>
          <span class="stat-label">维修中</span>
        </div>
        <div class="stat-card">
          <span class="stat-number">{{ stats.waitingConfirm }}</span>
          <span class="stat-label">待确认</span>
        </div>
        <div class="stat-card">
          <span class="stat-number">{{ stats.completedToday }}</span>
          <span class="stat-label">今日完成</span>
        </div>
      </template>
    </div>

    <h3 class="section-title">最近工单</h3>

    <div v-if="loading" class="order-list">
      <div v-for="i in 3" :key="i" class="order-card skeleton-card">
        <div class="skeleton-line skeleton-line--sm"></div>
        <div class="skeleton-line skeleton-line--md"></div>
        <div class="skeleton-line skeleton-line--xs"></div>
      </div>
    </div>
    <div v-else-if="recentOrders.length === 0" class="empty-state">暂无工单</div>
    <div v-else class="order-list">
      <div
        v-for="order in recentOrders"
        :key="order.id"
        class="order-card"
        @click="router.push(`/repairer/m/orders/${order.id}`)"
      >
        <div class="order-top">
          <span class="order-no">{{ order.orderNo }}</span>
          <span class="order-status" :style="{ color: REPAIR_STATUS_MAP[order.repairStatus]?.color }">
            {{ REPAIR_STATUS_MAP[order.repairStatus]?.label || '未知' }}
          </span>
        </div>
        <span class="order-title">{{ order.title }}</span>
        <span class="order-meta">{{ order.dormitoryBuilding }}栋{{ order.roomNo }}室</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mobile-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-title {
  font-family: var(--mc-font-display, sans-serif);
  font-size: 22px;
  font-weight: 600;
  color: var(--mc-ink, #141413);
  margin: 0;
}

/* 2x2 Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stat-card {
  background: var(--mc-white, #fff);
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  border-radius: var(--mc-radius-md, 16px);
  padding: 18px 16px;
  text-align: center;
  animation: fadeInUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.stat-card:nth-child(1) { animation-delay: 0.05s; }
.stat-card:nth-child(2) { animation-delay: 0.1s; }
.stat-card:nth-child(3) { animation-delay: 0.15s; }
.stat-card:nth-child(4) { animation-delay: 0.2s; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.stat-number {
  display: block;
  font-size: 36px;
  font-weight: 700;
  font-family: var(--mc-font-display, sans-serif);
  color: var(--mc-ink, #141413);
  line-height: 1.1;
}

.stat-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--mc-ink, #141413);
  margin-top: 6px;
}

/* Recent Orders */
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--mc-ink, #141413);
  margin: 4px 0 0;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-card {
  background: var(--mc-white, #fff);
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  border-radius: var(--mc-radius-md, 16px);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  cursor: pointer;
  transition: opacity 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.order-card:active {
  opacity: 0.7;
}

.order-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-no {
  font-size: 11px;
  color: var(--mc-muted-soft, #A3A3A3);
  font-family: monospace;
}

.order-status {
  font-size: 13px;
  font-weight: 600;
}

.order-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--mc-ink, #141413);
}

.order-meta {
  font-size: 12px;
  color: var(--mc-muted, #696969);
}

.empty-state {
  text-align: center;
  padding: 32px;
  color: var(--mc-muted, #696969);
  font-size: 14px;
}

/* Skeleton loading */
@keyframes shimmer {
  0% { background-position: -200px 0; }
  100% { background-position: 200px 0; }
}

.skeleton-card {
  position: relative;
  overflow: hidden;
}

.skeleton-num {
  width: 48px;
  height: 36px;
  margin: 0 auto;
  border-radius: 6px;
  background: linear-gradient(90deg, var(--mc-hairline, rgba(20,20,19,0.08)) 25%, rgba(20,20,19,0.04) 50%, var(--mc-hairline, rgba(20,20,19,0.08)) 75%);
  background-size: 400px 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-label {
  width: 40px;
  height: 14px;
  margin: 8px auto 0;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--mc-hairline, rgba(20,20,19,0.08)) 25%, rgba(20,20,19,0.04) 50%, var(--mc-hairline, rgba(20,20,19,0.08)) 75%);
  background-size: 400px 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-line {
  border-radius: 4px;
  background: linear-gradient(90deg, var(--mc-hairline, rgba(20,20,19,0.08)) 25%, rgba(20,20,19,0.04) 50%, var(--mc-hairline, rgba(20,20,19,0.08)) 75%);
  background-size: 400px 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-line--sm { width: 80px; height: 12px; }
.skeleton-line--md { width: 100%; height: 16px; }
.skeleton-line--xs { width: 120px; height: 12px; }
</style>
