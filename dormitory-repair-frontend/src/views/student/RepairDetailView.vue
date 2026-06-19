<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UiCard from '../../components/ui/UiCard.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiTimeline from '../../components/ui/UiTimeline.vue'
import UiStarDisplay from '../../components/ui/UiStarDisplay.vue'
import { getCategoriesApi, getRepairDetailApi, cancelRepairApi, studentConfirmApi, getRepairFeedbackApi } from '../../api/repair'
import { useAuthStore } from '../../stores/auth'
import { normalizeRepairOrder } from '../../utils/repair'
import { REPAIR_STATUS_MAP } from '../../constants/repair'
import { useToast } from '../../composables/useToast'
import { useConfirm } from '../../composables/useConfirm'

const toast = useToast()
const { confirm } = useConfirm()

const route = useRoute()
const router = useRouter()
const orderId = route.params.id as string
const authStore = useAuthStore()

const order = ref<any>(null)
const feedback = ref<any>(null)
const loading = ref(true)
const cancelling = ref(false)
const confirming = ref(false)

const statusMap = REPAIR_STATUS_MAP

const fetchDetail = async () => {
  loading.value = true
  try {
    const [data, categories]: any = await Promise.all([
      getRepairDetailApi(orderId),
      getCategoriesApi()
    ])
    order.value = normalizeRepairOrder(data, categories, authStore.user ?? undefined)
    
    if (order.value.repairStatus >= 5) {
      const fbData: any = await getRepairFeedbackApi(orderId)
      feedback.value = fbData
    }
  } catch (err: any) {
    toast.error(err.message || '获取详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const timelineEvents = computed(() => {
  if (!order.value) return []
  const events = []

  // 1. Submit
  events.push({
    label: '提交报修',
    time: order.value.submitTime,
    status: 'success' as const,
    description: '报修单已提交，等待受理'
  })

  // 2. Assign/Accept
  if (order.value.assignTime) {
    events.push({
      label: '已派单',
      time: order.value.assignTime,
      status: 'success' as const,
      description: `系统已指派维修人员：${order.value.workerName || '老师傅'}`
    })
  }

  // 3. Worker Accept
  if (order.value.workerAcceptTime) {
    events.push({
      label: '维修人员接单',
      time: order.value.workerAcceptTime,
      status: 'success' as const,
      description: '维修人员已出发，请保持电话畅通'
    })
  }

  // 4. Worker Complete
  if (order.value.workerCompleteTime) {
    events.push({
      label: '维修完成',
      time: order.value.workerCompleteTime,
      status: 'success' as const,
      description: order.value.processRemark || '维修任务已处理完毕，请核地'
    })
  }

  // 5. Student Confirm
  if (order.value.studentConfirmTime) {
    events.push({
      label: '确认完成',
      time: order.value.studentConfirmTime,
      status: 'success' as const,
      description: '工单已结案，感谢你的使用'
    })
  }

  // 6. Cancelled
  if (order.value.cancelTime) {
    events.push({
      label: '工单已取消',
      time: order.value.cancelTime,
      status: 'error' as const,
      description: '该报修单已撤销'
    })
  }

  // Current active status if not finished
  if (order.value.repairStatus < 5 && !order.value.cancelTime) {
    const currentStatus = statusMap[order.value.repairStatus]
    // Check if latest event matches current status to avoid duplicates
    const latestEvent = events[events.length - 1]
    if (latestEvent.label !== currentStatus.label) {
      events.push({
        label: currentStatus.label,
        status: 'active' as any,
        description: '正在处理中'
      })
    } else {
      latestEvent.status = 'active' as any
    }
  }

  return events
})

const handleCancel = async () => {
  if (!await confirm('确定要取消这条报修申请吗？')) return

  cancelling.value = true
  try {
    await cancelRepairApi(orderId)
    toast.success('已成功取消报修')
    fetchDetail()
  } catch (err: any) {
    toast.error(err.message || '取消失败')
  } finally {
    cancelling.value = false
  }
}

const handleStudentConfirm = async () => {
  if (!await confirm('确认维修已完成？确认后将无法撤销。')) return

  confirming.value = true
  try {
    await studentConfirmApi(orderId)
    toast.success('确认完成成功')
    fetchDetail()
  } catch (err: any) {
    toast.error(err.message || '确认失败')
  } finally {
    confirming.value = false
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
      <UiSectionTitle eyebrow="工单详情" title="报修单详情" />
    </div>

    <div v-if="loading" class="loading-state">
      <div class="mc-loader"></div>
      <p>正在获取详情...</p>
    </div>

    <div v-else-if="order" class="detail-content">
      <div class="detail-main">
        <UiCard variant="white" padding="40px" class="status-card">
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
        </UiCard>

        <UiCard variant="white" padding="40px" class="info-card">
          <h4 class="info-section-title">工单进度</h4>
          <UiTimeline :events="timelineEvents" />
        </UiCard>

        <UiCard variant="white" padding="40px" class="info-card">
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
        </UiCard>

        <UiCard v-if="feedback" variant="white" padding="40px" class="feedback-display-card">
          <h4 class="info-section-title">服务评价</h4>
          <div class="feedback-score">
            <UiStarDisplay :score="feedback.score" show-label size="large" />
          </div>
          <div class="feedback-content" v-if="feedback.content">
            <p class="feedback-text">{{ feedback.content }}</p>
          </div>
          <div class="feedback-time">评价于：{{ feedback.createTime?.replace('T', ' ') }}</div>
        </UiCard>

        <UiCard v-if="order.processRemark && !feedback" variant="white" padding="40px" class="remark-card">
          <h4 class="info-section-title">维修备注</h4>
          <p class="remark-text">{{ order.processRemark }}</p>
          <div v-if="order.finishTime" class="finish-time">完成时间：{{ order.finishTime }}</div>
        </UiCard>
      </div>

      <div class="detail-side">
        <UiCard variant="canvas" padding="32px" class="action-card">
          <h4 class="info-section-title">操作</h4>
          <div class="action-buttons">
            <UiButton
              v-if="order.repairStatus === 1 || order.repairStatus === 2"
              type="signal"
              :loading="cancelling"
              @click="handleCancel"
              class="full-width"
            >
              取消报修
            </UiButton>
            <UiButton
              v-if="order.repairStatus === 4"
              type="primary"
              :loading="confirming"
              @click="handleStudentConfirm"
              class="full-width"
            >
              确认维修完成
            </UiButton>
            <UiButton
              v-if="order.repairStatus === 5"
              type="primary"
              class="full-width"
              @click="$router.push(`/student/repair/feedback/${order.id}`)"
            >
              评价工单
            </UiButton>
            <p v-if="order.repairStatus === 2" class="action-tip">已指派维修人员，等待接单处理中...</p>
            <p v-if="order.repairStatus === 3" class="action-tip">维修人员正在处理中，暂不可操作</p>
            <p v-if="order.repairStatus === 6" class="action-tip">该工单已取消</p>
            <UiButton type="secondary" class="full-width" @click="fetchDetail">刷新进度</UiButton>
          </div>
        </UiCard>
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
  font-weight: 500;
  padding: 4px 16px;
  border-radius: var(--mc-radius-pill);
}

.detail-title {
  font-size: var(--mc-fz-display-md);
  font-weight: 400;
  color: var(--mc-ink);
  margin-bottom: 8px;
  letter-spacing: var(--mc-ls-display-md);
  font-family: var(--mc-font-display);
}

.submit-time {
  font-size: 14px;
  color: var(--mc-muted);
  font-weight: 400;
}

.info-section-title {
  font-family: var(--mc-font-display);
  font-size: var(--mc-fz-title-lg);
  font-weight: 400;
  margin-bottom: 32px;
  color: var(--mc-ink);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  margin-bottom: 40px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-bottom: 1px solid var(--mc-hairline-soft);
  padding-bottom: 16px;
}

.label {
  font-size: 13px;
  color: var(--mc-muted);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.value {
  font-size: 16px;
  font-weight: 400;
  color: var(--mc-body-strong);
}

.info-section {
  margin-bottom: 40px;
}

.description-text {
  margin-top: 12px;
  font-size: 16px;
  color: var(--mc-body);
  line-height: 1.6;
  white-space: pre-wrap;
}

.image-gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 16px;
}

.detail-image {
  width: 140px;
  height: 140px;
  border-radius: var(--mc-radius-md);
  overflow: hidden;
  border: 1px solid var(--mc-hairline);
}

.detail-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remark-text {
  font-size: 16px;
  color: var(--mc-body);
  background-color: var(--mc-canvas);
  padding: 24px;
  border-radius: var(--mc-radius-md);
  border: 1px solid var(--mc-hairline);
  line-height: 1.6;
  margin-bottom: 16px;
}

.finish-time {
  font-size: 14px;
  color: var(--mc-muted);
  text-align: right;
  font-weight: 400;
}

.action-card {
  border: 1px solid var(--mc-hairline);
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.full-width {
  width: 100%;
}

.action-tip {
  font-size: 14px;
  color: var(--mc-muted);
  text-align: center;
  padding: 12px 0;
  font-weight: 400;
}

.feedback-display-card {
  border-left: 4px solid var(--mc-accent-amber);
}

.feedback-score {
  margin-bottom: 24px;
}

.feedback-content {
  background-color: var(--mc-canvas);
  padding: 20px;
  border-radius: var(--mc-radius-md);
  margin-bottom: 16px;
  border: 1px solid var(--mc-hairline);
}

.feedback-text {
  font-size: 15px;
  color: var(--mc-body);
  line-height: 1.6;
}

.feedback-time {
  font-size: 12px;
  color: var(--mc-muted);
  text-align: right;
}

.mc-loader {
  width: 32px;
  height: 32px;
  border: 3px solid var(--mc-hairline);
  border-bottom-color: var(--mc-primary);
  border-radius: 50%;
  animation: rotation 1s linear infinite;
}

</style>


