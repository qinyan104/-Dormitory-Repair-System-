<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import UiCard from '../../../components/ui/UiCard.vue'
import UiButton from '../../../components/ui/UiButton.vue'
import UiModal from '../../../components/ui/UiModal.vue'
import { useAuthStore } from '../../../stores/auth'
import { getMyRepairListApi } from '../../../api/repair'
import { getNoticeListApi } from '../../../api/notice'
import { getPageRecords } from '../../../utils/page'

const router = useRouter()
const authStore = useAuthStore()
const userName = authStore.user?.realName || '同学'

const loading = ref(false)
const repairs = ref<any[]>([])

const fetchMyRepairs = async () => {
  loading.value = true
  try {
    const data: any = await getMyRepairListApi({ pageNum: 1, pageSize: 1000 })
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
    { label: '报修总数', value: total },
    { label: '处理中', value: pending },
    { label: '已完成', value: completed }
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
  <div class="mobile-home">
    <!-- Welcome -->
    <div class="welcome-card">
      <div class="welcome-text">
        <span class="welcome-eyebrow">欢迎回来</span>
        <h2 class="welcome-name">你好，{{ userName }}</h2>
      </div>
      <UiButton type="primary" @click="router.push('/student/m/create')">立即报修</UiButton>
    </div>

    <!-- Stats Row -->
    <div class="stats-row">
      <template v-if="loading">
        <div v-for="i in 3" :key="i" class="stat-item skeleton-card">
          <div class="skeleton-num"></div>
          <div class="skeleton-label-sm"></div>
        </div>
      </template>
      <template v-else>
        <div v-for="stat in stats" :key="stat.label" class="stat-item">
          <span class="stat-value">{{ stat.value }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
      </template>
    </div>

    <!-- Notices -->
    <div class="section-header">
      <h3 class="section-title">最新公告</h3>
    </div>

    <div v-if="notices.length === 0" class="empty-state">暂无公告</div>
    <div v-else class="notice-list">
      <UiCard
        v-for="notice in notices"
        :key="notice.id"
        padding="14px 16px"
        radius="sm"
        class="notice-card"
        @click="openNotice(notice)"
      >
        <div class="notice-row">
          <div class="notice-info">
            <span class="notice-tag" :class="{ 'is-important': notice.type === 'IMPORTANT' }">
              {{ notice.type === 'IMPORTANT' ? '重要' : '公告' }}
            </span>
            <span class="notice-title">{{ notice.title }}</span>
          </div>
          <span class="notice-date">{{ notice.publishTime?.slice(5, 10) }}</span>
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
.mobile-home {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Welcome Card */
.welcome-card {
  background: linear-gradient(135deg, var(--mc-lifted, #FCFBFA) 0%, var(--mc-white, #fff) 100%);
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  border-radius: var(--mc-radius-md, 16px);
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.welcome-eyebrow {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--mc-gold, #D4AF37);
  font-weight: 700;
}

.welcome-name {
  font-family: var(--mc-font-display, sans-serif);
  font-size: 20px;
  font-weight: 600;
  color: var(--mc-ink, #141413);
  margin: 4px 0 0;
}

/* Stats */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-item {
  background: var(--mc-white, #fff);
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  border-radius: var(--mc-radius-md, 16px);
  padding: 16px 12px;
  text-align: center;
  animation: fadeInUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.stat-item:nth-child(1) { animation-delay: 0.05s; }
.stat-item:nth-child(2) { animation-delay: 0.1s; }
.stat-item:nth-child(3) { animation-delay: 0.15s; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.stat-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
  font-family: var(--mc-font-display, sans-serif);
  color: var(--mc-ink, #141413);
  line-height: 1.1;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: var(--mc-muted, #696969);
  margin-top: 4px;
  font-weight: 500;
}

/* Section */
.section-header {
  margin-top: 4px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--mc-ink, #141413);
  margin: 0;
}

/* Notices */
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-card {
  cursor: pointer;
  transition: opacity 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.notice-card:active {
  opacity: 0.7;
}

.notice-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.notice-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.notice-tag {
  padding: 2px 8px;
  background-color: var(--mc-ink, #141413);
  color: var(--mc-white, #fff);
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.05em;
  flex-shrink: 0;
}

.notice-tag.is-important {
  background-color: var(--mc-signal, #CF4500);
}

.notice-title {
  font-size: 14px;
  color: var(--mc-ink, #141413);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-date {
  font-size: 12px;
  color: var(--mc-muted, #696969);
  font-family: monospace;
  flex-shrink: 0;
}

.empty-state {
  text-align: center;
  padding: 32px;
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

.skeleton-num {
  width: 48px;
  height: 32px;
  margin: 0 auto;
  border-radius: 6px;
  background: linear-gradient(90deg, var(--mc-hairline, rgba(20,20,19,0.08)) 25%, rgba(20,20,19,0.04) 50%, var(--mc-hairline, rgba(20,20,19,0.08)) 75%);
  background-size: 400px 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-label-sm {
  width: 40px;
  height: 14px;
  margin: 8px auto 0;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--mc-hairline, rgba(20,20,19,0.08)) 25%, rgba(20,20,19,0.04) 50%, var(--mc-hairline, rgba(20,20,19,0.08)) 75%);
  background-size: 400px 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

/* Notice Modal */
.notice-modal-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--mc-hairline-soft, rgba(20,20,19,0.05));
}

.notice-modal-content {
  font-size: 15px;
  line-height: 1.6;
  color: var(--mc-ink, #141413);
  white-space: pre-wrap;
}
</style>
