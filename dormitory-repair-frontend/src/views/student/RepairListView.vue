<script setup lang="ts">
import { ref, onMounted } from 'vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import { getMyRepairListApi } from '../../api/repair'
import { REPAIR_STATUS_MAP } from '../../constants/repair'
import { useToast } from '../../composables/useToast'
import { getPageRecords } from '../../utils/page'
import { repairDetailPath } from '../../utils/device'

const toast = useToast()

const repairs = ref<any[]>([])
const pageNum = ref(1)
const pageSize = 8
const total = ref(0)
const statusFilter = ref<number>(0)
const loading = ref(false)

const statusTabs = [
  { label: '全部', value: 0 },
  { label: '待受理', value: 1 },
  { label: '已派单', value: 2 },
  { label: '维修中', value: 3 },
  { label: '待确认', value: 4 },
  { label: '已完成', value: 5 },
  { label: '已取消', value: 6 }
]

const fetchRepairs = async () => {
  loading.value = true
  try {
    const params: any = { pageNum: pageNum.value, pageSize }
    if (statusFilter.value > 0) params.repairStatus = statusFilter.value
    const data: any = await getMyRepairListApi(params)
    repairs.value = getPageRecords(data)
    total.value = data.total || 0
  } catch (err: any) {
    toast.error(err.message || '获取报修列表失败')
  } finally {
    loading.value = false
  }
}

const handleFilter = (val: number) => {
  statusFilter.value = val
  pageNum.value = 1
  fetchRepairs()
}

onMounted(fetchRepairs)
</script>

<template>
  <div class="repair-list-view">
    <UiSectionTitle eyebrow="我的工单" title="我的报修" />

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
    <div v-else-if="repairs.length === 0" class="empty-state">暂无报修记录</div>
    <div v-else class="repair-cards">
      <UiCard v-for="item in repairs" :key="item.id" padding="20px 24px" class="repair-item">
        <div class="item-header">
          <span class="item-no">{{ item.orderNo }}</span>
          <span class="item-status" :style="{ color: REPAIR_STATUS_MAP[item.repairStatus]?.color }">
            {{ REPAIR_STATUS_MAP[item.repairStatus]?.label }}
          </span>
        </div>
        <h3 class="item-title">{{ item.title }}</h3>
        <p class="item-meta">{{ item.categoryName || '未分类' }} · {{ item.submitTime?.replace('T', ' ').slice(0, 16) }}</p>
        <div class="item-actions">
          <UiButton type="secondary" @click="$router.push(repairDetailPath(item.id))">查看详情</UiButton>
        </div>
      </UiCard>
    </div>

    <div v-if="total > pageSize" class="pagination">
      <span>共 {{ total }} 条</span>
      <div class="page-btns">
        <UiButton type="secondary" :disabled="pageNum <= 1" @click="pageNum--; fetchRepairs()">上一页</UiButton>
        <span class="current">{{ pageNum }}</span>
        <UiButton type="secondary" :disabled="pageNum * pageSize >= total" @click="pageNum++; fetchRepairs()">下一页</UiButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.repair-list-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.status-tabs {
  display: flex;
  gap: 8px;
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

.repair-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.repair-item {
  transition: box-shadow 0.15s;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.item-no {
  font-size: 12px;
  color: var(--mc-muted-soft);
  font-family: monospace;
}

.item-status {
  font-size: 13px;
  font-weight: 600;
}

.item-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--mc-ink);
  margin: 0 0 8px;
}

.item-meta {
  font-size: 13px;
  color: var(--mc-muted);
  margin: 0 0 12px;
}

.item-actions {
  display: flex;
  justify-content: flex-end;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

@media (max-width: 768px) {
  .status-tabs {
    overflow-x: auto;
    flex-wrap: nowrap;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    padding-bottom: 4px;
  }
  .status-tabs::-webkit-scrollbar {
    display: none;
  }
  .tab-btn {
    flex-shrink: 0;
    min-height: 44px;
  }
  .pagination {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
    text-align: center;
  }
  .page-btns {
    justify-content: center;
  }
}
</style>
