<script setup lang="ts">
import { ref, onMounted } from 'vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import UiButton from '../../components/ui/UiButton.vue'
import { getStatisticsSummaryApi, getStatisticsCategoryApi } from '../../api/statistics'
import { getRepairListApi } from '../../api/repair'
import { getPageRecords } from '../../utils/page'
import { REPAIR_STATUS_MAP } from '../../constants/repair'

const summary = ref({
  total: 0,
  pending: 0,
  assigned: 0,
  repairing: 0,
  waitingConfirm: 0,
  completed: 0,
  cancelled: 0,
  today: 0
})

const categories = ref<any[]>([])
const recentRepairs = ref<any[]>([])
const loading = ref(true)

const fetchDashboardData = async () => {
  loading.value = true
  try {
    const [summaryData, categoryData, repairData]: any = await Promise.all([
      getStatisticsSummaryApi(),
      getStatisticsCategoryApi(),
      getRepairListApi({ pageNum: 1, pageSize: 8 })
    ])

    summary.value = summaryData
    categories.value = categoryData
    recentRepairs.value = getPageRecords(repairData)
  } catch (err) {
    console.error('Failed to fetch dashboard data:', err)
  } finally {
    loading.value = false
  }
}

const statusMap = REPAIR_STATUS_MAP

onMounted(() => {
  fetchDashboardData()
})
</script>

<template>
  <div class="admin-dashboard">
    <div class="bento-stats-grid">
      <!-- Hero Card (Span 2x2) -->
      <UiCard padding="32px" class="bento-card hero-stat" variant="dark" elevated>
        <div class="stat-hero-content">
          <span class="stat-eyebrow hero-eyebrow">待处理</span>
          <h2 class="stat-value huge-value">{{ summary.pending }}</h2>
          <span class="stat-label hero-label">待受理工单</span>
        </div>
        <div class="stat-hero-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke-linejoin="round" stroke-linecap="round"/>
          </svg>
        </div>
      </UiCard>

      <!-- Today Card -->
      <UiCard padding="24px" class="bento-card" variant="white" elevated>
        <div class="bento-top">
          <span class="stat-eyebrow">今日新增</span>
        </div>
        <div class="bento-bottom">
          <h2 class="stat-value">{{ summary.today }}</h2>
          <span class="stat-label">今日新增报修</span>
        </div>
      </UiCard>

      <!-- Processing (assigned + repairing) -->
      <UiCard padding="24px" class="bento-card" variant="white" elevated>
        <div class="bento-top">
          <span class="stat-eyebrow">处理中</span>
        </div>
        <div class="bento-bottom">
          <h2 class="stat-value">{{ (summary.assigned || 0) + (summary.repairing || 0) }}</h2>
          <span class="stat-label">处理中（已派单+维修中）</span>
        </div>
      </UiCard>

      <!-- Waiting Confirm -->
      <UiCard padding="24px" class="bento-card" variant="white" elevated>
        <div class="bento-top">
          <span class="stat-eyebrow" style="color: var(--mc-warning);">待确认</span>
        </div>
        <div class="bento-bottom">
          <h2 class="stat-value">{{ summary.waitingConfirm || 0 }}</h2>
          <span class="stat-label">待学生确认</span>
        </div>
      </UiCard>

      <!-- Completed -->
      <UiCard padding="24px" class="bento-card" variant="white" elevated>
        <div class="bento-top">
          <span class="stat-eyebrow" style="color: var(--mc-primary);">已完成</span>
        </div>
        <div class="bento-bottom">
          <h2 class="stat-value">{{ summary.completed }}</h2>
          <span class="stat-label">已完成工单</span>
        </div>
      </UiCard>

      <!-- Total -->
      <UiCard padding="24px" class="bento-card" variant="canvas" elevated>
        <div class="bento-top">
          <span class="stat-eyebrow">总计</span>
        </div>
        <div class="bento-bottom">
          <h2 class="stat-value">{{ summary.total }}</h2>
          <span class="stat-label">历史总报修量</span>
        </div>
      </UiCard>
    </div>

    <div class="dashboard-main-grid">
      <div class="recent-activity">
        <UiSectionTitle eyebrow="实时" title="最近工单动态" />
        <UiCard padding="0" class="table-card">
          <table class="mc-table">
            <thead>
              <tr>
                <th>单号</th>
                <th>标题</th>
                <th>分类</th>
                <th>状态</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="repair in recentRepairs" :key="repair.id" @click="$router.push(`/admin/repairs?id=${repair.id}`)">
                <td><span class="order-no">{{ repair.orderNo }}</span></td>
                <td><span class="order-title">{{ repair.title }}</span></td>
                <td>{{ repair.categoryName }}</td>
                <td>
                  <span
                    class="status-tag"
                    :style="{ color: statusMap[repair.repairStatus].color, backgroundColor: statusMap[repair.repairStatus].color + '12' }"
                  >
                    {{ statusMap[repair.repairStatus].label }}
                  </span>
                </td>
                <td class="time-col">{{ repair.submitTime?.split(' ')[0] || '-' }}</td>
              </tr>
              <tr v-if="recentRepairs.length === 0">
                <td colspan="5" class="empty-row">暂无数据</td>
              </tr>
            </tbody>
          </table>
          <div class="card-footer">
            <UiButton type="secondary" @click="$router.push('/admin/repairs')">查看全部工单</UiButton>
          </div>
        </UiCard>
      </div>

      <div class="distribution-side">
        <UiSectionTitle eyebrow="概览" title="报修分类分布" />
        <UiCard padding="24px">
          <div class="category-list">
            <div v-for="cat in categories" :key="cat.categoryId" class="category-item">
              <div class="cat-info">
                <span class="cat-name">{{ cat.categoryName }}</span>
                <span class="cat-count">{{ cat.count }} 单</span>
              </div>
              <div class="progress-bar">
                <div
                  class="progress-fill"
                  :style="{ width: `${(cat.count / (summary.total || 1)) * 100}%` }"
                ></div>
              </div>
            </div>
            <div v-if="categories.length === 0" class="empty-state">暂无分类统计</div>
          </div>
        </UiCard>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-dashboard {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.bento-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-auto-rows: minmax(150px, auto);
  gap: 24px;
}

.bento-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: transform var(--mc-transition), box-shadow var(--mc-transition);
}

.bento-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
}

.hero-stat {
  grid-column: span 2;
  grid-row: span 2;
  flex-direction: row;
  justify-content: space-between;
  align-items: flex-end;
  background: linear-gradient(135deg, var(--mc-ink) 0%, #1A1C20 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.stat-hero-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  justify-content: flex-end;
}

.hero-eyebrow {
  color: var(--mc-gold) !important;
  margin-bottom: auto !important; /* Pushes the eyebrow to the top */
}

.hero-label {
  color: rgba(255, 255, 255, 0.6) !important;
}

.huge-value {
  font-size: 84px !important;
  line-height: 1;
  margin-bottom: 8px !important;
  color: var(--mc-white) !important;
}

.stat-hero-icon {
  color: var(--mc-gold);
  opacity: 0.15;
  margin-bottom: 12px;
  transform: scale(2);
  transform-origin: bottom right;
}

.bento-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.bento-bottom {
  display: flex;
  flex-direction: column;
}

.stat-eyebrow {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--mc-muted);
  display: block;
  margin-bottom: 6px;
  font-weight: 700;
}

.stat-value {
  font-size: 44px;
  font-weight: 400;
  margin-bottom: 4px;
  color: var(--mc-ink);
  font-family: var(--mc-font-display);
  letter-spacing: -0.02em;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: var(--mc-charcoal);
  font-weight: 500;
}

.dashboard-main-grid {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 24px;
  align-items: start;
}

.table-card {
  overflow: hidden;
}

.mc-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.mc-table th {
  padding: 12px 20px;
  font-size: 12px;
  font-weight: 500;
  color: var(--mc-muted);
  border-bottom: 1px solid var(--mc-canvas);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.mc-table td {
  padding: 12px 20px;
  font-size: 13px;
  border-bottom: 1px solid var(--mc-canvas);
}

.mc-table tbody tr {
  cursor: pointer;
  transition: background-color var(--mc-transition);
}

.mc-table tbody tr:hover {
  background-color: var(--mc-canvas);
}

.order-no {
  font-family: monospace;
  color: var(--mc-muted);
  font-size: 12px;
}

.order-title {
  font-weight: 500;
  color: var(--mc-ink);
}

.status-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--mc-radius-pill);
}

.time-col {
  color: var(--mc-muted);
  white-space: nowrap;
  font-size: 12px;
}

.card-footer {
  padding: 12px 20px;
  display: flex;
  justify-content: center;
  border-top: 1px solid var(--mc-canvas);
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.cat-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.cat-name {
  font-size: 13px;
  font-weight: 500;
}

.cat-count {
  font-size: 12px;
  color: var(--mc-muted);
}

.progress-bar {
  height: 4px;
  background-color: var(--mc-canvas);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: var(--mc-signal);
  border-radius: 2px;
  transition: width 0.3s ease;
}

.empty-state,
.empty-row {
  text-align: center;
  color: var(--mc-muted);
  padding: 32px;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .dashboard-main-grid {
    grid-template-columns: 1fr;
  }
  .bento-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>


