<script setup lang="ts">
import { ref, onMounted } from 'vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiButton from '../../components/ui/UiButton.vue'
import { getWorkerOrdersApi, workerAcceptApi } from '../../api/repair'
import { REPAIR_STATUS_MAP } from '../../constants/repair'
import { useToast } from '../../composables/useToast'

const toast = useToast()

const orders = ref<any[]>([])
const pageNum = ref(1)
const pageSize = 6
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
  } catch {
    toast.error('加载工单列表失败，请重试')
  }
  finally { loading.value = false }
}

const acceptLoading = ref<number | null>(null)

const handleAccept = async (id: number) => {
  acceptLoading.value = id
  try {
    await workerAcceptApi(id)
    toast.success('接单成功')
    fetchOrders()
  } catch (err: any) {
    toast.error(err.message || '操作失败')
  } finally {
    acceptLoading.value = null
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
  <div class="worker-orders">
    <h2 class="page-title">我的工单</h2>

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

    <div v-if="loading" class="loading-state">加载中...</div>
    <div v-else-if="orders.length === 0" class="empty-state">
      <p class="empty-text">暂无工单</p>
    </div>
    <div v-else class="order-list">
      <UiCard v-for="order in orders" :key="order.id" padding="16px 24px" class="order-item">
        <div class="order-row">
          <div class="order-info">
            <span class="order-no">{{ order.orderNo }}</span>
            <span class="order-title">{{ order.title }}</span>
            <span class="order-meta">
              {{ REPAIR_STATUS_MAP[order.repairStatus]?.label || '未知' }}
              · {{ order.dormitoryBuilding }}栋{{ order.roomNo }}室
            </span>
          </div>
          <div class="order-actions">
            <UiButton
              v-if="order.repairStatus === 2"
              type="primary"
              :loading="acceptLoading === order.id"
              @click="handleAccept(order.id)"
            >
              接单
            </UiButton>
            <UiButton
              type="secondary"
              @click="$router.push(`/repairer/orders/${order.id}`)"
            >
              详情
            </UiButton>
          </div>
        </div>
      </UiCard>
    </div>

    <div v-if="total > pageSize" class="pagination">
      <span>共 {{ total }} 条</span>
      <div class="page-btns">
        <UiButton type="secondary" :disabled="pageNum <= 1" @click="pageNum--; fetchOrders()">上一页</UiButton>
        <span class="current">{{ pageNum }}</span>
        <UiButton type="secondary" :disabled="pageNum * pageSize >= total" @click="pageNum++; fetchOrders()">下一页</UiButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.worker-orders {
  width: 100%;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--mc-ink);
  margin: 0 0 20px;
}

.status-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.tab-btn {
  padding: 8px 16px;
  border-radius: var(--mc-radius-pill);
  border: 1px solid var(--mc-hairline);
  background: var(--mc-lifted);
  color: var(--mc-muted);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.tab-btn.active {
  background: var(--mc-ink);
  color: var(--mc-white);
  border-color: var(--mc-ink);
}

.tab-btn:hover:not(.active) {
  border-color: var(--mc-ink);
  color: var(--mc-ink);
}

.loading-state, .empty-state {
  text-align: center;
  padding: 48px;
  color: var(--mc-muted);
  font-size: 14px;
}

.empty-text {
  color: var(--mc-muted);
  font-size: 15px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-item {
  transition: box-shadow 0.15s;
}

.order-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.order-no {
  font-size: 12px;
  color: var(--mc-muted);
  font-family: monospace;
}

.order-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--mc-ink);
}

.order-meta {
  font-size: 13px;
  color: var(--mc-muted);
}

.order-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  font-size: 13px;
  color: var(--mc-muted);
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
