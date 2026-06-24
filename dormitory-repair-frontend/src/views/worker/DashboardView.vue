<script setup lang="ts">
import { ref, onMounted } from 'vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import { getWorkerOrdersApi } from '../../api/repair'
import { REPAIR_STATUS_MAP } from '../../constants/repair'
import { useToast } from '../../composables/useToast'

const toast = useToast()

const stats = ref({
  assigned: 0,
  repairing: 0,
  waitingConfirm: 0,
  completedToday: 0
})

const recentOrders = ref<any[]>([])

const fetchData = async () => {
  try {
    const allOrders: any = await getWorkerOrdersApi({ pageNum: 1, pageSize: 100 })
    const orders = allOrders?.records || allOrders || []
    recentOrders.value = (orders as any[]).slice(0, 8)

    const today = new Date().toISOString().slice(0, 10)
    stats.value.assigned = (orders as any[]).filter((o: any) => o.repairStatus === 2).length
    stats.value.repairing = (orders as any[]).filter((o: any) => o.repairStatus === 3).length
    stats.value.waitingConfirm = (orders as any[]).filter((o: any) => o.repairStatus === 4).length
    stats.value.completedToday = (orders as any[]).filter((o: any) => o.repairStatus === 5 && o.studentConfirmTime?.startsWith(today)).length
  } catch {
    toast.error('加载数据失败，请下拉刷新重试')
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="worker-dashboard">
    <UiSectionTitle eyebrow="维修工作台" title="今日概览" />

    <div class="stats-grid">
      <UiCard class="stat-card">
        <div class="stat-number">{{ stats.assigned }}</div>
        <div class="stat-label">待接单</div>
        <div class="stat-desc">管理员已指派给你的工单</div>
      </UiCard>
      <UiCard class="stat-card">
        <div class="stat-number">{{ stats.repairing }}</div>
        <div class="stat-label">维修中</div>
        <div class="stat-desc">你正在处理的工单</div>
      </UiCard>
      <UiCard class="stat-card">
        <div class="stat-number">{{ stats.waitingConfirm }}</div>
        <div class="stat-label">待学生确认</div>
        <div class="stat-desc">已完成维修，等待学生确认</div>
      </UiCard>
      <UiCard class="stat-card">
        <div class="stat-number">{{ stats.completedToday }}</div>
        <div class="stat-label">今日完成</div>
        <div class="stat-desc">今日学生确认完成的工单</div>
      </UiCard>
    </div>

    <UiSectionTitle title="最近工单" style="margin-top: 40px" />
    <div class="recent-list" v-if="recentOrders.length > 0">
      <div v-for="order in recentOrders" :key="order.id" class="recent-item"
           @click="$router.push(`/repairer/orders/${order.id}`)">
        <div class="recent-left">
          <span class="recent-no">{{ order.orderNo }}</span>
          <span class="recent-title">{{ order.title }}</span>
        </div>
        <div class="recent-right">
          <span class="recent-status" :style="{ color: REPAIR_STATUS_MAP[order.repairStatus]?.color }">
            {{ REPAIR_STATUS_MAP[order.repairStatus]?.label || '未知' }}
          </span>
        </div>
      </div>
    </div>
    <UiCard v-else padding="40px" class="empty-card">
      <p class="empty-text">暂无工单</p>
    </UiCard>
  </div>
</template>

<style scoped>
.worker-dashboard {
  width: 100%;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.stat-card {
  text-align: center;
  padding: 32px 24px;
}

.stat-number {
  font-size: 48px;
  font-weight: 700;
  color: var(--mc-ink);
  font-family: var(--mc-font-display);
  line-height: 1.1;
}

.stat-label {
  font-size: 16px;
  font-weight: 600;
  color: var(--mc-ink);
  margin-top: 8px;
}

.stat-desc {
  font-size: 13px;
  color: var(--mc-muted);
  margin-top: 4px;
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  background: var(--mc-hairline);
  border-radius: var(--mc-radius-md);
  overflow: hidden;
}

.recent-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: var(--mc-white);
  cursor: pointer;
  transition: background 0.15s ease;
}

.recent-item:hover {
  background: var(--mc-lifted);
}

.recent-left {
  display: flex;
  gap: 16px;
  align-items: center;
}

.recent-no {
  font-size: 13px;
  color: var(--mc-muted);
  font-family: monospace;
}

.recent-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--mc-ink);
}

.recent-status {
  font-size: 14px;
  font-weight: 600;
}

.empty-card {
  text-align: center;
}

.empty-text {
  color: var(--mc-muted);
  font-size: 15px;
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .stat-card {
    padding: 20px 12px;
  }
  .stat-number {
    font-size: 32px;
  }
  .stat-label {
    font-size: 14px;
  }
  .stat-desc {
    display: none;
  }
  .recent-item {
    padding: 12px 16px;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .recent-right {
    width: 100%;
    display: flex;
    justify-content: flex-end;
  }
  .recent-left {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
