<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import UiButton from '../../../components/ui/UiButton.vue'
import { getWorkerOrdersApi, workerAcceptApi } from '../../../api/repair'
import { REPAIR_STATUS_MAP } from '../../../constants/repair'
import { useToast } from '../../../composables/useToast'

const router = useRouter()
const toast = useToast()

const orders = ref<any[]>([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const statusFilter = ref<number>(0)
const loading = ref(false)

const statusTabs = [
  { label: '全部', value: 0 },
  { label: '待接单', value: 2 },
  { label: '维修中', value: 3 },
  { label: '待确认', value: 4 },
  { label: '已完成', value: 5 }
]

const fetchOrders = async () => {
  loading.value = true
  try {
    const params: any = { pageNum: pageNum.value, pageSize }
    if (statusFilter.value > 0) params.repairStatus = statusFilter.value
    const data: any = await getWorkerOrdersApi(params)
    orders.value = data?.records || data || []
    total.value = data?.total || 0
  } catch { /* handled */ }
  finally { loading.value = false }
}

const handleAccept = async (id: number) => {
  try {
    await workerAcceptApi(id)
    toast.success('接单成功')
    fetchOrders()
  } catch (err: any) {
    toast.error(err.message || '操作失败')
  }
}

const handleFilter = (val: number) => {
  statusFilter.value = val
  pageNum.value = 1
  fetchOrders()
}

onMounted(fetchOrders)
</script>

<template>
  <div class="mobile-orders">
    <h2 class="page-title">我的工单</h2>

    <!-- Scrollable Tabs -->
    <div class="status-tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.value"
        :class="['tab-btn', { active: statusFilter === tab.value }]"
        @click="handleFilter(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="loading" class="order-list">
      <div v-for="i in 4" :key="i" class="order-card skeleton-card">
        <div class="skeleton-line skeleton-line--sm"></div>
        <div class="skeleton-line skeleton-line--md"></div>
        <div class="skeleton-line skeleton-line--xs"></div>
        <div class="skeleton-actions">
          <div class="skeleton-btn"></div>
          <div class="skeleton-btn"></div>
        </div>
      </div>
    </div>
    <div v-else-if="orders.length === 0" class="empty-state">暂无工单</div>
    <div v-else class="order-list">
      <div
        v-for="order in orders"
        :key="order.id"
        class="order-card"
      >
        <div class="order-header">
          <span class="order-no">{{ order.orderNo }}</span>
          <span class="order-status" :style="{ color: REPAIR_STATUS_MAP[order.repairStatus]?.color }">
            {{ REPAIR_STATUS_MAP[order.repairStatus]?.label || '未知' }}
          </span>
        </div>
        <h4 class="order-title">{{ order.title }}</h4>
        <span class="order-meta">{{ order.dormitoryBuilding }}栋{{ order.roomNo }}室</span>
        <div class="order-actions">
          <UiButton
            v-if="order.repairStatus === 2"
            type="primary"
            @click.stop="handleAccept(order.id)"
          >
            接单
          </UiButton>
          <UiButton
            type="secondary"
            @click="router.push(`/repairer/m/orders/${order.id}`)"
          >
            详情
          </UiButton>
        </div>
      </div>
    </div>

    <div v-if="total > pageSize" class="load-more">
      <button
        class="load-more-btn"
        :disabled="pageNum * pageSize >= total"
        @click="pageNum++; fetchOrders()"
      >
        {{ pageNum * pageSize >= total ? '没有更多了' : '加载更多' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.mobile-orders {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-title {
  font-family: var(--mc-font-display, sans-serif);
  font-size: 22px;
  font-weight: 600;
  color: var(--mc-ink, #141413);
  margin: 0;
}

/* Horizontal scrollable tabs */
.status-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 4px;
  scrollbar-width: none;
  scroll-snap-type: x mandatory;
}

.status-tabs::-webkit-scrollbar {
  display: none;
}

.tab-btn {
  padding: 8px 16px;
  border-radius: var(--mc-radius-pill, 9999px);
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  background: var(--mc-lifted, #FCFBFA);
  color: var(--mc-muted, #696969);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  flex-shrink: 0;
  min-height: 44px;
  -webkit-tap-highlight-color: transparent;
  scroll-snap-align: start;
}

.tab-btn.active {
  background: var(--mc-ink, #141413);
  color: var(--mc-white, #fff);
  border-color: var(--mc-ink, #141413);
}

/* Order Cards */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-card {
  background: var(--mc-white, #fff);
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  border-radius: var(--mc-radius-md, 16px);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.order-header {
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
  font-weight: 600;
  color: var(--mc-ink, #141413);
  margin: 0;
  line-height: 1.3;
}

.order-meta {
  font-size: 12px;
  color: var(--mc-muted, #696969);
}

.order-actions {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}

.order-actions :deep(.mc-button) {
  min-height: 44px;
  flex: 1;
}

/* Load More */
.load-more {
  display: flex;
  justify-content: center;
  padding-top: 4px;
}

.load-more-btn {
  background: none;
  border: none;
  color: var(--mc-muted, #696969);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  padding: 12px 24px;
  -webkit-tap-highlight-color: transparent;
}

.load-more-btn:disabled {
  color: var(--mc-muted-soft, #A3A3A3);
  cursor: default;
}

.load-more-btn:not(:disabled):active {
  color: var(--mc-ink, #141413);
}

/* States */
.loading-state,
.empty-state {
  text-align: center;
  padding: 40px;
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

.skeleton-line {
  border-radius: 4px;
  background: linear-gradient(90deg, var(--mc-hairline, rgba(20,20,19,0.08)) 25%, rgba(20,20,19,0.04) 50%, var(--mc-hairline, rgba(20,20,19,0.08)) 75%);
  background-size: 400px 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-line--sm { width: 80px; height: 12px; margin-bottom: 8px; }
.skeleton-line--md { width: 100%; height: 16px; margin-bottom: 6px; }
.skeleton-line--xs { width: 120px; height: 12px; margin-bottom: 10px; }

.skeleton-actions {
  display: flex;
  gap: 8px;
}

.skeleton-btn {
  flex: 1;
  height: 44px;
  border-radius: var(--mc-radius-btn, 20px);
  background: linear-gradient(90deg, var(--mc-hairline, rgba(20,20,19,0.08)) 25%, rgba(20,20,19,0.04) 50%, var(--mc-hairline, rgba(20,20,19,0.08)) 75%);
  background-size: 400px 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}
</style>
