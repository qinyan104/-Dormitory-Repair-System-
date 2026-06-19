<template>
  <div class="ai-insights-page">
    <div class="page-header">
      <h2>AI 数据洞察</h2>
      <div class="time-selector">
        <button
          v-for="t in timeOptions"
          :key="t.value"
          :class="{ active: timeRange === t.value }"
          @click="timeRange = t.value; fetchInsights()"
        >
          {{ t.label }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">AI 正在分析数据...</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>

    <div v-else-if="insights" class="insights-content">
      <div class="summary-banner">{{ insights.summary }}</div>

      <div class="highlights-grid">
        <div
          v-for="h in insights.highlights"
          :key="`${h.type}-${h.title}`"
          :class="['highlight-card', `severity-${h.severity}`]"
        >
          <div class="hl-icon">{{ typeIcon(h.type) }}</div>
          <div class="hl-body">
            <div class="hl-title">{{ h.title }}</div>
            <div class="hl-detail">{{ h.detail }}</div>
          </div>
        </div>
      </div>

      <div v-if="insights.charts" class="charts-section">
        <h3>数据概览</h3>
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-value">{{ insights.charts.totalOrders }}</div>
            <div class="stat-label">{{ insights.charts.period }}工单</div>
            <div class="stat-change">{{ insights.charts.changeRate }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ insights.charts.completionRate }}</div>
            <div class="stat-label">完成率</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ insights.charts.completedOrders }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      AI 洞察暂不可用，请稍后重试
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { aiInsightsApi } from '../../api/ai'
import type { InsightResponse } from '../../types/models'

const timeRange = ref<'today' | 'this_week' | 'this_month'>('this_week')
const loading = ref(false)
const insights = ref<InsightResponse | null>(null)
const error = ref('')

const timeOptions = [
  { label: '今日', value: 'today' as const },
  { label: '本周', value: 'this_week' as const },
  { label: '本月', value: 'this_month' as const }
]

const typeIcon = (type: string) => {
  const icons: Record<string, string> = {
    anomaly: '!',
    trend: '↗',
    efficiency: 'E',
    bottleneck: '×',
    good: '✓'
  }
  return icons[type] || '•'
}

const fetchInsights = async () => {
  loading.value = true
  error.value = ''
  insights.value = null
  try {
    const data = await aiInsightsApi({ timeRange: timeRange.value })
    insights.value = data || null
    if (!data) {
      error.value = 'AI 洞察暂不可用'
    }
  } catch (_err) {
    error.value = 'AI 洞察暂不可用'
  } finally {
    loading.value = false
  }
}

onMounted(fetchInsights)
</script>

<style scoped>
.ai-insights-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 8px 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--mc-ink);
  letter-spacing: -0.3px;
}

.time-selector {
  display: flex;
  gap: 4px;
  background: var(--mc-white);
  border-radius: var(--mc-radius-btn);
  border: 1px solid var(--mc-hairline);
  padding: 3px;
}

.time-selector button {
  padding: 7px 20px;
  border: none;
  border-radius: var(--mc-radius-btn);
  background: transparent;
  font-size: 13px;
  font-weight: 500;
  color: var(--mc-muted);
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}

.time-selector button:hover {
  color: var(--mc-ink);
}

.time-selector button.active {
  background: var(--mc-signal);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(207, 69, 0, 0.2);
}

.summary-banner {
  background: var(--mc-signal);
  color: #fff;
  padding: 22px 28px;
  border-radius: var(--mc-radius-md);
  font-size: 15px;
  line-height: 1.7;
  margin-bottom: 28px;
  letter-spacing: -0.2px;
  box-shadow: 0 4px 16px rgba(207, 69, 0, 0.18);
}

.highlights-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 32px;
}

.highlight-card {
  display: flex;
  gap: 14px;
  padding: 18px;
  border-radius: var(--mc-radius-md);
  border: 1px solid var(--mc-hairline);
  background: var(--mc-white);
  border-left: 4px solid;
  transition: box-shadow 0.15s;
}

.highlight-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.highlight-card.severity-warning {
  background: #fffaf5;
  border-color: var(--mc-signal);
}

.highlight-card.severity-info {
  background: #f8fafd;
  border-color: #3b82f6;
}

.highlight-card.severity-good {
  background: #f8fdf9;
  border-color: #16a34a;
}

.hl-icon {
  font-size: 22px;
  flex-shrink: 0;
  line-height: 1;
}

.hl-body {
  flex: 1;
}

.hl-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--mc-ink);
  margin-bottom: 6px;
}

.hl-detail {
  font-size: 13px;
  color: var(--mc-muted);
  line-height: 1.6;
}

.charts-section {
  margin-bottom: 32px;
}

.charts-section h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--mc-ink);
  margin-bottom: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 14px;
}

.stat-card {
  text-align: center;
  padding: 24px;
  background: var(--mc-white);
  border: 1px solid var(--mc-hairline);
  border-radius: var(--mc-radius-md);
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--mc-signal);
  border-radius: var(--mc-radius-md) var(--mc-radius-md) 0 0;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--mc-ink);
  line-height: 1.1;
  letter-spacing: -0.5px;
}

.stat-label {
  font-size: 12px;
  color: var(--mc-muted);
  margin-top: 6px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.stat-change {
  font-size: 12px;
  color: #16a34a;
  font-weight: 500;
  margin-top: 4px;
}

.loading-state,
.error-state,
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--mc-muted);
  font-size: 15px;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.loading-state::before {
  content: '';
  width: 18px;
  height: 18px;
  border: 2px solid var(--mc-hairline);
  border-top-color: var(--mc-signal);
  border-radius: 50%;
  animation: ai-spin 0.8s linear infinite;
}

@keyframes ai-spin {
  to {
    transform: rotate(360deg);
  }
}

.error-state {
  color: var(--mc-error);
}
</style>
