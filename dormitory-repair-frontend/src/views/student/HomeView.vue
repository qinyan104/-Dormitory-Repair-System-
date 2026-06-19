<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiModal from '../../components/ui/UiModal.vue'
import { useAuthStore } from '../../stores/auth'
import { getMyRepairListApi } from '../../api/repair'
import { getNoticeListApi } from '../../api/notice'
import { getPageRecords } from '../../utils/page'

const authStore = useAuthStore()
const userName = authStore.user?.realName || '同学'

const loading = ref(false)
const repairs = ref<any[]>([])

const fetchMyRepairs = async () => {
  loading.value = true
  try {
    const data: any = await getMyRepairListApi({
      pageNum: 1,
      pageSize: 1000
    })
    repairs.value = getPageRecords(data)
  } catch (err) {
    console.error('Failed to fetch home stats:', err)
    repairs.value = []
  } finally {
    loading.value = false
  }
}

const stats = computed(() => {
  const total = repairs.value.length
  const pending = repairs.value.filter((item) => [1, 2, 3, 4].includes(item.repairStatus)).length
  const completed = repairs.value.filter((item) => item.repairStatus === 5).length

  return [
    { label: '报修总数', value: total, eyebrow: 'TOTAL' },
    { label: '正在处理', value: pending, eyebrow: 'PENDING' },
    { label: '已完成', value: completed, eyebrow: 'RESOLVED' }
  ]
})

const notices = ref<any[]>([])

const fetchNotices = async () => {
  try {
    const data: any = await getNoticeListApi({ pageNum: 1, pageSize: 5 })
    notices.value = getPageRecords(data)
  } catch (err) {
    console.error('Failed to fetch notices:', err)
  }
}

onMounted(() => {
  fetchMyRepairs()
  fetchNotices()
})

const activeNotice = ref<any>(null)
const showNoticeModal = ref(false)

const openNotice = (notice: any) => {
  activeNotice.value = notice
  showNoticeModal.value = true
}
</script>

<template>
  <div class="home-view">
    <!-- Hero / Welcome Card -->
    <UiCard class="hero-card" padding="40px" radius="lg">
      <div class="hero-content">
        <UiSectionTitle eyebrow="欢迎回来" :title="`你好，${userName}`" />
        <p class="hero-text">欢迎来到宿舍维修管理服务台，您可以随时发起和跟踪维修工单。</p>
      </div>
      <div class="hero-action">
        <UiButton type="primary" @click="$router.push('/student/repair/create')">立即报修</UiButton>
      </div>
    </UiCard>

    <div class="bento-grid">
      <!-- Notices Card -->
      <UiCard class="notices-card" variant="white" padding="32px" radius="lg" elevated>
        <UiSectionTitle eyebrow="NOTICES" title="最新公告" />
        <div class="notice-list">
          <div v-for="notice in notices" :key="notice.id" class="notice-row" @click="openNotice(notice)">
            <div class="notice-info">
              <span class="notice-tag" :class="{ 'is-important': notice.type === 'IMPORTANT' }">{{ notice.type === 'IMPORTANT' ? '重要' : '公告' }}</span>
              <span class="notice-title">{{ notice.title }}</span>
            </div>
            <span class="notice-date">{{ notice.publishTime }}</span>
          </div>
          <div v-if="notices.length === 0" class="empty-state">
            暂无公告
          </div>
        </div>
      </UiCard>

      <!-- Stats Card -->
      <UiCard class="stats-card" variant="white" padding="40px" radius="lg" elevated>
        <h4 class="stats-title">报修概览</h4>
        <div class="stats-combo">
          <div v-for="stat in stats" :key="stat.label" class="stat-item">
            <span class="stat-eyebrow">{{ stat.eyebrow }}</span>
            <h3 class="stat-value">{{ loading ? '-' : stat.value }}</h3>
            <span class="stat-label">{{ stat.label }}</span>
          </div>
        </div>
      </UiCard>
    </div>

    <!-- Notice Modal -->
    <UiModal :visible="showNoticeModal" :title="activeNotice?.title" @close="showNoticeModal = false">
      <div class="notice-modal-meta">
        <span class="notice-tag" :class="{ 'is-important': activeNotice?.type === 'IMPORTANT' }">
          {{ activeNotice?.type === 'IMPORTANT' ? '重要' : '公告' }}
        </span>
        <span class="notice-date">{{ activeNotice?.publishTime }}</span>
      </div>
      <div class="notice-modal-content">
        {{ activeNotice?.content || '暂无详细内容' }}
      </div>
    </UiModal>
  </div>
</template>

<style scoped>
.home-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Hero Card */
.hero-card {
  background: linear-gradient(135deg, var(--mc-canvas) 0%, var(--mc-lifted) 100%);
  border: 1px solid var(--mc-hairline);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hero-content {
  display: flex;
  flex-direction: column;
}

.hero-text {
  font-size: 15px;
  color: var(--mc-charcoal);
  margin-top: -16px;
  margin-bottom: 0;
}

/* Bento Grid */
.bento-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

/* Notices Card */
.notices-card {
  display: flex;
  flex-direction: column;
}

:deep(.mc-section-title) {
  margin-bottom: 24px !important;
}

.notice-list {
  display: flex;
  flex-direction: column;
}

.notice-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 0;
  border-bottom: 1px solid var(--mc-hairline);
  cursor: pointer;
  transition: opacity var(--mc-transition), transform var(--mc-transition);
}

.notice-row:last-child {
  border-bottom: none;
}

.notice-row:hover {
  opacity: 0.6;
}

.notice-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.notice-tag {
  padding: 4px 10px;
  background-color: var(--mc-ink);
  color: var(--mc-white);
  border-radius: var(--mc-radius-sm);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.notice-tag.is-important {
  background-color: var(--mc-signal);
}

.notice-title {
  font-size: 15px;
  color: var(--mc-ink);
  font-weight: 500;
}

.notice-date {
  font-size: 13px;
  color: var(--mc-muted);
  font-family: var(--mc-font-code);
}

/* Stats Card */
.stats-card {
  display: flex;
  flex-direction: column;
}

.stats-title {
  color: var(--mc-charcoal);
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 32px;
  font-weight: 600;
}

.stats-combo {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-eyebrow {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--mc-gold);
  margin-bottom: 6px;
  font-weight: 700;
}

.stat-value {
  font-size: 40px;
  font-family: var(--mc-font-display);
  color: var(--mc-ink);
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: var(--mc-charcoal);
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: var(--mc-gold);
  font-family: var(--mc-font-display);
  letter-spacing: 2px;
}

.notice-modal-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--mc-hairline-soft);
}

.notice-modal-content {
  font-size: 16px;
  line-height: 1.6;
  color: var(--mc-ink);
  white-space: pre-wrap;
}
</style>


