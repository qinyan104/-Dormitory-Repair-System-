<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import McCard from '../../components/mc/McCard.vue'
import McSectionTitle from '../../components/mc/McSectionTitle.vue'
import McButton from '../../components/mc/McButton.vue'
import { getCategoriesApi, getRepairDetailApi, cancelRepairApi } from '../../api/repair'
import { useAuthStore } from '../../stores/auth'
import { normalizeRepairOrder } from '../../utils/repair'

const route = useRoute()
const router = useRouter()
const orderId = route.params.id as string
const authStore = useAuthStore()

const order = ref<any>(null)
const loading = ref(true)
const cancelling = ref(false)

const statusMap: Record<number, { label: string, color: string }> = {
  1: { label: '待受理', color: 'var(--mc-muted)' },
  2: { label: '处理中', color: 'var(--mc-signal-light)' },
  3: { label: '已完成', color: '#10b981' },
  4: { label: '已取消', color: '#9ca3af' }
}

const fetchDetail = async () => {
  loading.value = true
  try {
    const [data, categories]: any = await Promise.all([
      getRepairDetailApi(orderId),
      getCategoriesApi()
    ])
    order.value = normalizeRepairOrder(data, categories, authStore.user ?? undefined)
  } catch (err: any) {
    alert(err.message || '获取详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const handleCancel = async () => {
  if (!confirm('确定要取消这条报修申请吗？')) return

  cancelling.value = true
  try {
    await cancelRepairApi(orderId)
    alert('已成功取消报修')
    fetchDetail()
  } catch (err: any) {
    alert(err.message || '取消失败')
  } finally {
    cancelling.value = false
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="repair-detail-view">
    <div class="page-header">
      <button class="back-btn" @click="router.back()">返回列表</button>
      <McSectionTitle eyebrow="工单详情" title="报修单详情" />
    </div>

    <div v-if="loading" class="loading-state">
      <div class="mc-loader"></div>
      <p>正在获取详情...</p>
    </div>

    <div v-else-if="order" class="detail-content">
      <div class="detail-main">
        <McCard padding="32px" class="status-card">
          <div class="status-header">
            <span class="order-no">#{{ order.orderNo }}</span>
            <span
              class="status-tag"
              :style="{ color: statusMap[order.repairStatus].color, backgroundColor: statusMap[order.repairStatus].color + '10' }"
            >
              {{ statusMap[order.repairStatus].label }}
            </span>
          </div>
          <h2 class="detail-title">{{ order.title }}</h2>
          <div class="submit-time">提交时间：{{ order.submitTime }}</div>
        </McCard>

        <McCard padding="32px" class="info-card">
          <h4 class="info-section-title">报修信息</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">报修分类</span>
              <span class="value">{{ order.categoryName || '未分类' }}</span>
            </div>
            <div class="info-item">
              <span class="label">宿舍地点</span>
              <span class="value">{{ order.dormitoryBuilding || '-' }} 楼 {{ order.roomNo || '-' }} 室</span>
            </div>
          </div>

          <div class="info-section">
            <span class="label">详细描述</span>
            <p class="description-text">{{ order.description || '暂无描述' }}</p>
          </div>

          <div v-if="order.images && order.images.length > 0" class="info-section">
            <span class="label">图片附件</span>
            <div class="image-gallery">
              <div v-for="(img, idx) in order.images" :key="idx" class="detail-image">
                <img :src="img" alt="Repair" />
              </div>
            </div>
          </div>
        </McCard>

        <McCard v-if="order.processRemark" padding="32px" class="remark-card">
          <h4 class="info-section-title">处理反馈</h4>
          <p class="remark-text">{{ order.processRemark }}</p>
          <div v-if="order.finishTime" class="finish-time">完成时间：{{ order.finishTime }}</div>
        </McCard>
      </div>

      <div class="detail-side">
        <McCard padding="32px" class="action-card">
          <h4 class="info-section-title">操作</h4>
          <div class="action-buttons">
            <McButton
              v-if="order.repairStatus === 1"
              type="signal"
              :loading="cancelling"
              @click="handleCancel"
              class="full-width"
            >
              取消报修
            </McButton>
            <McButton
              v-if="order.repairStatus === 3"
              type="primary"
              class="full-width"
              @click="$router.push(`/student/repair/feedback/${order.id}`)"
            >
              评价工单
            </McButton>
            <p v-if="order.repairStatus === 2" class="action-tip">工单正在处理中，暂不可操作</p>
            <p v-if="order.repairStatus === 4" class="action-tip">该工单已取消</p>
            <McButton type="secondary" class="full-width" @click="fetchDetail">刷新进度</McButton>
          </div>
        </McCard>
      </div>
    </div>
  </div>
</template>

<style scoped>
.repair-detail-view {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.back-btn {
  background: none;
  border: none;
  color: var(--mc-muted);
  font-size: 14px;
  cursor: pointer;
  margin-bottom: 8px;
  padding: 0;
}

.back-btn:hover {
  color: var(--mc-ink);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100px 0;
  gap: 16px;
  color: var(--mc-muted);
}

.detail-content {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 24px;
  align-items: start;
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.status-card .status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.order-no {
  font-size: 14px;
  color: var(--mc-muted);
  font-family: monospace;
}

.status-tag {
  font-size: 14px;
  font-weight: 600;
  padding: 4px 16px;
  border-radius: var(--mc-radius-pill);
}

.detail-title {
  font-size: 28px;
  font-weight: 500;
  color: var(--mc-ink);
  margin-bottom: 8px;
}

.submit-time {
  font-size: 14px;
  color: var(--mc-muted);
}

.info-section-title {
  font-size: 18px;
  font-weight: 500;
  margin-bottom: 24px;
  color: var(--mc-ink);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 32px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.label {
  font-size: 14px;
  color: var(--mc-muted);
}

.value {
  font-size: 16px;
  font-weight: 500;
  color: var(--mc-ink);
}

.info-section {
  margin-bottom: 32px;
}

.description-text {
  margin-top: 8px;
  font-size: 16px;
  color: var(--mc-ink);
  line-height: 1.6;
  white-space: pre-wrap;
}

.image-gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 12px;
}

.detail-image {
  width: 120px;
  height: 120px;
  border-radius: var(--mc-radius-md);
  overflow: hidden;
  border: 1px solid var(--mc-canvas);
}

.detail-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remark-text {
  font-size: 16px;
  color: var(--mc-ink);
  background-color: var(--mc-canvas);
  padding: 20px;
  border-radius: var(--mc-radius-md);
  margin-bottom: 12px;
}

.finish-time {
  font-size: 14px;
  color: var(--mc-muted);
  text-align: right;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.full-width {
  width: 100%;
}

.action-tip {
  font-size: 14px;
  color: var(--mc-muted);
  text-align: center;
  padding: 8px 0;
}

.mc-loader {
  width: 32px;
  height: 32px;
  border: 3px solid var(--mc-canvas);
  border-bottom-color: var(--mc-ink);
  border-radius: 50%;
  animation: rotation 1s linear infinite;
}

@keyframes rotation {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@media (max-width: 900px) {
  .detail-content {
    grid-template-columns: 1fr;
  }
}
</style>
