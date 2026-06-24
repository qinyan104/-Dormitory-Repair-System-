<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UiCard from '../../components/ui/UiCard.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import UiButton from '../../components/ui/UiButton.vue'
import { getCategoriesApi, getRepairDetailApi } from '../../api/repair'
import http from '../../api/http'
import { normalizeRepairOrder } from '../../utils/repair'
import { useToast } from '../../composables/useToast'

const toast = useToast()

const route = useRoute()
const router = useRouter()
const orderId = route.params.id as string

const order = ref<any>(null)
const loading = ref(true)
const submitting = ref(false)

const feedback = ref({
  score: 5,
  content: ''
})

const ratingHints = ['非常差', '较差', '一般', '满意', '非常满意']

const fetchDetail = async () => {
  loading.value = true
  try {
    const [data, categories]: any = await Promise.all([
      getRepairDetailApi(orderId),
      getCategoriesApi()
    ])
    if (data.repairStatus !== 5) {
      toast.error('只有已完成的工单才能评价')
      router.back()
      return
    }
    order.value = normalizeRepairOrder(data, categories)
  } catch (err: any) {
    toast.error(err.message || '获取详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (feedback.value.score < 1) {
    toast.error('请先为服务评分')
    return
  }

  submitting.value = true
  try {
    await http.post('/repair-feedback', {
      repairOrderId: Number(orderId),
      score: feedback.value.score,
      content: feedback.value.content
    })
    toast.success('评价提交成功，感谢你的反馈！')
    router.push('/student/repair/list')
  } catch (err: any) {
    toast.error(err.message || '提交评价失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <div class="repair-feedback-view">
    <div class="page-header">
      <button class="back-btn" @click="router.back()">返回详情</button>
      <UiSectionTitle eyebrow="意见反馈" title="评价工单" />
    </div>

    <div v-if="loading" class="loading-state">
      <div class="mc-loader"></div>
    </div>

    <UiCard v-else-if="order" padding="40px" class="feedback-card">
      <div class="order-summary">
        <span class="order-no">#{{ order.orderNo }}</span>
        <h3 class="order-title">{{ order.title }}</h3>
        <p class="order-meta">{{ order.categoryName || '未分类' }} | 完成人：{{ order.finishTime || '近期' }}</p>
      </div>

      <div class="feedback-form">
        <form @submit.prevent="handleSubmit">
          <div class="rating-section">
            <label class="mc-label">服务评分</label>
            <div class="stars">
              <span
                v-for="star in 5"
                :key="star"
                class="star"
                :class="{ 'is-active': star <= feedback.score }"
                @click="feedback.score = star"
              >
                ⭐
              </span>
            </div>
            <p class="rating-hint">{{ ratingHints[feedback.score - 1] }}</p>
          </div>

          <div class="content-section">
            <label class="mc-label" for="feedback-content">评价内容</label>
            <textarea
              id="feedback-content"
              v-model="feedback.content"
              class="mc-textarea"
              placeholder="请分享你的维修体验，帮助我们继续改进服务。"
              rows="5"
            ></textarea>
          </div>

          <div class="form-actions">
            <UiButton type="secondary" @click="router.back()">以后再说</UiButton>
            <UiButton type="primary" :loading="submitting" html-type="submit">提交评价</UiButton>
          </div>
        </form>
      </div>
    </UiCard>
  </div>
</template>

<style scoped>
.repair-feedback-view {
  max-width: 600px;
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
  justify-content: center;
  padding: 100px 0;
}

.order-summary {
  text-align: center;
  margin-bottom: 40px;
  border-bottom: 1px solid var(--mc-canvas);
  padding-bottom: 24px;
}

.order-no {
  font-size: 12px;
  color: var(--mc-muted);
  font-family: monospace;
}

.order-title {
  font-size: 20px;
  font-weight: 500;
  margin: 8px 0;
  color: var(--mc-ink);
}

.order-meta {
  font-size: 14px;
  color: var(--mc-muted);
}

.feedback-form {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.mc-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--mc-muted);
  margin-bottom: 12px;
  display: block;
}

.rating-section {
  text-align: center;
}

.stars {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 12px;
}

.star {
  font-size: 40px;
  cursor: pointer;
  transition: all var(--mc-transition);
  opacity: 0.3;
}

.star.is-active {
  opacity: 1;
}

.star:hover {
  transform: scale(1.1);
}

.rating-hint {
  font-size: 14px;
  color: var(--mc-muted);
  font-weight: 500;
}

.mc-textarea {
  width: 100%;
  padding: 16px 20px;
  background-color: var(--mc-white);
  border: 1px solid var(--mc-canvas);
  border-radius: var(--mc-radius-md);
  font-size: 16px;
  font-family: inherit;
  outline: none;
  transition: border-color var(--mc-transition);
  resize: vertical;
}

.mc-textarea:focus {
  border-color: var(--mc-ink);
}

.form-actions {
  display: flex;
  gap: 16px;
}

.form-actions button {
  flex: 1;
}

.mc-loader {
  width: 32px;
  height: 32px;
  border: 3px solid var(--mc-canvas);
  border-bottom-color: var(--mc-ink);
  border-radius: 50%;
  animation: rotation 1s linear infinite;
}

@media (max-width: 600px) {
  .order-title {
    font-size: 15px;
  }
  .order-summary {
    margin-bottom: 16px;
    padding-bottom: 12px;
  }
  .order-meta {
    font-size: 12px;
  }
  .star {
    font-size: 28px;
  }
  .stars {
    gap: 4px;
  }
  .rating-hint {
    font-size: 12px;
  }
  .form-actions {
    flex-direction: column;
  }
  .form-actions button {
    width: 100%;
  }
  .mc-textarea {
    font-size: 14px;
    padding: 10px 12px;
  }
  .mc-label {
    font-size: 12px;
    margin-bottom: 6px;
  }
  .feedback-form {
    gap: 16px;
  }
  .rating-section {
    margin-bottom: 4px;
  }
}
</style>
