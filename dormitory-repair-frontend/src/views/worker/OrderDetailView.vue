<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UiCard from '../../components/ui/UiCard.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiTextarea from '../../components/ui/UiTextarea.vue'
import UiTimeline from '../../components/ui/UiTimeline.vue'
import UiStarDisplay from '../../components/ui/UiStarDisplay.vue'
import { getRepairDetailApi, workerCompleteApi, getRepairFeedbackApi } from '../../api/repair'
import { REPAIR_STATUS_MAP } from '../../constants/repair'
import { useToast } from '../../composables/useToast'
import { workerOrderListPath } from '../../utils/device'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const order = ref<any>(null)
const feedback = ref<any>(null)
const remark = ref('')
const submitting = ref(false)

const fetchOrder = async () => {
  try {
    order.value = await getRepairDetailApi(route.params.id as string)
    if (order.value.repairStatus >= 5) {
      const fbData: any = await getRepairFeedbackApi(route.params.id as string)
      feedback.value = fbData
    }
  } catch { /* handled */ }
}

const timelineEvents = computed(() => {
  if (!order.value) return []
  const events = []

  events.push({
    label: '提交报修',
    time: order.value.submitTime?.slice(0, 16),
    status: 'success' as const
  })

  if (order.value.assignTime) {
    events.push({
      label: '管理员指派',
      time: order.value.assignTime?.slice(0, 16),
      status: 'success' as const
    })
  }

  if (order.value.workerAcceptTime) {
    events.push({
      label: '已接单',
      time: order.value.workerAcceptTime?.slice(0, 16),
      status: 'success' as const
    })
  }

  if (order.value.workerCompleteTime) {
    events.push({
      label: '维修完成',
      time: order.value.workerCompleteTime?.slice(0, 16),
      status: 'success' as const
    })
  }

  if (order.value.studentConfirmTime) {
    events.push({
      label: '学生确认',
      time: order.value.studentConfirmTime?.slice(0, 16),
      status: 'success' as const
    })
  }

  if (order.value.cancelTime) {
    events.push({
      label: '已取消',
      time: order.value.cancelTime?.slice(0, 16),
      status: 'error' as const
    })
  }

  return events
})

const handleComplete = async () => {
  submitting.value = true
  try {
    await workerCompleteApi(order.value.id, remark.value)
    toast.success('维修完成标记成功')
    router.push(workerOrderListPath())
  } catch (err: any) {
    toast.error(err.message || '操作失败')
  }
  submitting.value = false
}

onMounted(fetchOrder)
</script>

<template>
  <div class="order-detail" v-if="order">
    <button class="back-btn" @click="$router.push(workerOrderListPath())">
      &larr; 返回我的工单
    </button>

    <div class="detail-grid">
      <div class="detail-main">
        <UiCard>
          <div class="order-header">
            <span class="order-no">{{ order.orderNo }}</span>
            <span class="order-status" :style="{ color: REPAIR_STATUS_MAP[order.repairStatus]?.color }">
              {{ REPAIR_STATUS_MAP[order.repairStatus]?.label || '未知' }}
            </span>
          </div>
          <h2 class="order-title">{{ order.title }}</h2>

          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">分类</span>
              <span class="info-value">{{ order.categoryName || '未知' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">宿舍</span>
              <span class="info-value">{{ order.dormitoryBuilding }}栋{{ order.roomNo }}室</span>
            </div>
            <div class="info-item">
              <span class="info-label">报修人</span>
              <span class="info-value">{{ order.realName }} ({{ order.phone }})</span>
            </div>
            <div class="info-item">
              <span class="info-label">提交时间</span>
              <span class="info-value">{{ order.submitTime?.slice(0, 16) }}</span>
            </div>
          </div>

          <div class="section">
            <h4 class="section-title">问题描述</h4>
            <p class="section-content">{{ order.description || order.content }}</p>
          </div>

          <div class="section" v-if="order.imageUrl">
            <h4 class="section-title">现场图片</h4>
            <img :src="order.imageUrl" alt="现场图片" class="detail-image" v-if="!order.imageUrl.includes(',')" />
            <div class="image-grid" v-else>
              <img v-for="(url, i) in order.imageUrl.split(',')" :key="i" :src="url" alt="现场图片" class="detail-image" />
            </div>
          </div>

          <div class="section" v-if="order.remark">
            <h4 class="section-title">处理备注</h4>
            <p class="section-content">{{ order.remark }}</p>
          </div>

          <div class="section" v-if="feedback">
            <h4 class="section-title">学生评价</h4>
            <div class="feedback-box">
              <UiStarDisplay :score="feedback.score" show-label size="medium" />
              <p class="feedback-text" v-if="feedback.content">{{ feedback.content }}</p>
              <span class="feedback-date">{{ feedback.createTime?.replace('T', ' ') }}</span>
            </div>
          </div>
        </UiCard>
      </div>

      <div class="detail-side">
        <UiCard>
          <h3 class="side-title">操作</h3>

          <div v-if="order.repairStatus === 3" class="action-section">
            <UiTextarea
              v-model="remark"
              label="维修备注"
              placeholder="请填写维修处理说明..."
              :rows="4"
            />
            <UiButton type="primary" :loading="submitting" style="width: 100%; margin-top: 16px" @click="handleComplete">
              标记维修完成
            </UiButton>
          </div>

          <div v-else-if="order.repairStatus === 2" class="action-hint">
            请先在工单列表页面点击"接单"按钮开始处理。
          </div>

          <div v-else class="action-hint">
            当前工单状态为"{{ REPAIR_STATUS_MAP[order.repairStatus]?.label }}"，无需操作。
          </div>
        </UiCard>

        <UiCard style="margin-top: 16px">
          <h3 class="side-title">时间轴</h3>
          <UiTimeline :events="timelineEvents" />
        </UiCard>
      </div>
    </div>
  </div>
</template>

<style scoped>
.order-detail {
  width: 100%;
}

.feedback-box {
  background-color: var(--mc-lifted);
  padding: 16px;
  border-radius: var(--mc-radius-md);
  border: 1px solid var(--mc-hairline);
  margin-top: 12px;
}

.feedback-text {
  margin-top: 12px;
  font-size: 14px;
  color: var(--mc-ink);
  line-height: 1.5;
}

.feedback-date {
  display: block;
  margin-top: 8px;
  font-size: 11px;
  color: var(--mc-muted);
  text-align: right;
}

.back-btn {
  background: none;
  border: none;
  color: var(--mc-muted);
  font-size: 14px;
  cursor: pointer;
  padding: 0;
  margin-bottom: 20px;
  transition: color 0.15s ease;
}

.back-btn:hover {
  color: var(--mc-ink);
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 24px;
  align-items: start;
}

.order-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.order-no {
  font-size: 12px;
  color: var(--mc-muted-soft);
  font-family: monospace;
}

.order-status {
  font-size: 14px;
  font-weight: 600;
}

.order-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--mc-ink);
  margin: 0 0 24px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: var(--mc-muted-soft);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-value {
  font-size: 15px;
  color: var(--mc-ink);
  font-weight: 500;
}

.section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--mc-hairline);
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--mc-muted);
  margin: 0 0 8px;
}

.section-content {
  font-size: 15px;
  color: var(--mc-body);
  line-height: 1.6;
}

.detail-image {
  max-width: 100%;
  border-radius: var(--mc-radius-md);
  margin-top: 8px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 8px;
}

.side-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--mc-ink);
  margin: 0 0 16px;
}

.action-section {
  display: flex;
  flex-direction: column;
}

.action-hint {
  font-size: 14px;
  color: var(--mc-muted);
  line-height: 1.6;
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
  .info-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .detail-image {
    width: 100%;
  }
  .image-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .order-title {
    font-size: 18px;
  }
  .order-detail {
    padding: 0;
  }
  .info-grid {
    gap: 8px;
  }
  .detail-image {
    width: 100%;
    max-width: 200px;
  }
  .image-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .side-title {
    font-size: 15px;
  }
}
</style>
