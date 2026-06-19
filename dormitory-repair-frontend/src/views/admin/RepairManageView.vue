<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiSlideOver from '../../components/ui/UiSlideOver.vue'
import UiSelect from '../../components/ui/UiSelect.vue'
import UiStarDisplay from '../../components/ui/UiStarDisplay.vue'
import { getCategoriesApi, getRepairListApi, acceptRepairApi, updateRepairStatusApi, assignWorkerApi, getRepairerListApi, exportRepairOrdersApi, getRepairFeedbackApi, getRepairDetailApi } from '../../api/repair'
import { getPageRecords } from '../../utils/page'
import { REPAIR_STATUS_MAP } from '../../constants/repair'
import { useToast } from '../../composables/useToast'
import { aiRecommendWorkerApi } from '../../api/ai'
import type { AiRecommendResponse, RepairStatus } from '../../types/models'

const route = useRoute()

const toast = useToast()

const loading = ref(false)
const repairs = ref<any[]>([])
const categories = ref<any[]>([])
const total = ref(0)
const feedback = ref<any>(null)

const query = ref({
  keyword: '',
  repairStatus: '' as string | number,
  categoryId: '',
  pageNum: 1,
  pageSize: 10
})

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待受理', value: 1 },
  { label: '已派单', value: 2 },
  { label: '维修中', value: 3 },
  { label: '待确认', value: 4 },
  { label: '已完成', value: 5 },
  { label: '已取消', value: 6 }
]

const statusMap = REPAIR_STATUS_MAP

const showDetail = ref(false)
const currentRepair = ref<any>(null)
const repairerList = ref<any[]>([])
const assignWorkerId = ref<number>(0)
const updateForm = ref({
  status: 3,
  processRemark: ''
})
const updating = ref(false)
const assigning = ref(false)
const aiRecommendLoading = ref(false)
const aiRecommendResult = ref<AiRecommendResponse | null>(null)

const getConfidenceClass = (score: number) => {
  if (score >= 90) return 'is-high'
  if (score >= 70) return 'is-mid'
  return 'is-low'
}

// ===== 一键自动处理 =====
const autoProcessing = ref(false)
const autoResult = ref<{ total: number; autoAssigned: number; failed: number } | null>(null)

const handleAutoProcess = async () => {
  autoProcessing.value = true
  autoResult.value = null
  try {
    // 获取所有待受理工单
    const data: any = await getRepairListApi({ repairStatus: 1, pageSize: 999 })
    const pending = data?.records || []
    if (pending.length === 0) {
      toast.info('没有待受理工单')
      autoProcessing.value = false
      return
    }

    let autoAssigned = 0
    let failed = 0
    for (const order of pending) {
      try {
        const resp: any = await acceptRepairApi(order.id)
        if (resp?.autoAssigned) {
          autoAssigned++
        } else {
          failed++
        }
      } catch {
        failed++
      }
    }
    autoResult.value = { total: pending.length, autoAssigned, failed }
    toast.success(`自动处理完成：${autoAssigned} 个自动指派，${failed} 个需人工处理`)
    await fetchData()
  } catch (err: any) {
    toast.error(err.message || '自动处理失败')
  }
  autoProcessing.value = false
}

// ===== Dashboard 统计 =====
const pendingCount = ref(0)
const processingCount = ref(0)
const completedCount = ref(0)

const refreshStats = async () => {
  try {
    const d1: any = await getRepairListApi({ repairStatus: 1, pageSize: 1 })
    pendingCount.value = d1?.total || 0
    const d2: any = await getRepairListApi({ repairStatus: 2, pageSize: 1 })
    const d3: any = await getRepairListApi({ repairStatus: 3, pageSize: 1 })
    const d4: any = await getRepairListApi({ repairStatus: 4, pageSize: 1 })
    processingCount.value = (d2?.total || 0) + (d3?.total || 0) + (d4?.total || 0)
    const d5: any = await getRepairListApi({ repairStatus: 5, pageSize: 1 })
    completedCount.value = d5?.total || 0
  } catch { /* ignore */ }
}

const getStatusMeta = (status: number) => {
  return statusMap[status] || { label: '未知状态', color: 'var(--mc-muted)' }
}

const getRepairDescription = (repair: any) => {
  return repair?.description || repair?.content || '暂无报修说明'
}

const getRepairImages = (repair: any) => {
  if (Array.isArray(repair?.images) && repair.images.length > 0) {
    return repair.images
  }
  if (typeof repair?.imageUrl === 'string' && repair.imageUrl.trim()) {
    return repair.imageUrl.split(',').map((item: string) => item.trim()).filter(Boolean)
  }
  return []
}

const getProcessRemark = (repair: any) => {
  return repair?.processRemark || repair?.remark || ''
}

const getCategoryName = (repair: any) => {
  if (repair?.categoryName) return repair.categoryName
  const match = categories.value.find((item) => item.id === repair?.categoryId)
  return match?.categoryName || '未分类'
}

const getUrgencyClass = (urgency?: string) => {
  if (!urgency) return 'is-normal'
  if (urgency.includes('紧急') || urgency.includes('高')) return 'is-urgent'
  if (urgency.includes('低')) return 'is-low'
  return 'is-normal'
}

const currentImages = computed(() => getRepairImages(currentRepair.value))

const updateStatusOptions = computed(() => {
  const status = currentRepair.value?.repairStatus
  if (status === 1) {
    return [
      { label: '受理（自己处理）', value: 2 },
      { label: '取消工单', value: 6 }
    ]
  }
  if (status === 2) {
    return [
      { label: '维修中', value: 3 },
      { label: '维修完成 (待确认)', value: 4 },
      { label: '取消工单', value: 6 }
    ]
  }
  if (status === 3) {
    return [
      { label: '维修完成 (待确认)', value: 4 },
      { label: '取消工单', value: 6 }
    ]
  }
  if (status === 4) {
    return [
      { label: '已完成（代学生确认）', value: 5 },
      { label: '取消工单', value: 6 }
    ]
  }
  return [
    { label: '已派单', value: 2 },
    { label: '维修中', value: 3 },
    { label: '维修完成 (待确认)', value: 4 },
    { label: '已完成', value: 5 },
    { label: '取消工单', value: 6 }
  ]
})

const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = { ...query.value }
    if (!params.repairStatus) delete params.repairStatus
    if (!params.categoryId) delete params.categoryId

    const data: any = await getRepairListApi(params)
    repairs.value = getPageRecords(data)
    total.value = data.total || 0
    await refreshStats()
  } catch (err) {
    console.error('Failed to fetch repairs:', err)
  } finally {
    loading.value = false
  }
}

const fetchRepairers = async () => {
  try {
    const data: any = await getRepairerListApi()
    const records = data?.records || data || []
    repairerList.value = Array.isArray(records) ? records.filter((u: any) => u.status === 1) : []
  } catch { /* ignored */ }
}

const fetchCategories = async () => {
  try {
    const data: any = await getCategoriesApi()
    categories.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.error('Failed to fetch categories:', err)
  }
}

const openDetail = async (repair: any) => {
  currentRepair.value = repair
  feedback.value = null
  updateForm.value.status = repair.repairStatus === 1 ? 2 : 3
  updateForm.value.processRemark = getProcessRemark(repair)
  assignWorkerId.value = repairerList.value[0]?.id || 0
  showDetail.value = true
  fetchRepairers()

  // Auto-fetch AI recommendation
  aiRecommendLoading.value = true
  aiRecommendResult.value = null
  try {
    const data: any = await aiRecommendWorkerApi(repair.id)
    if (data && data.rankings?.length > 0) {
      aiRecommendResult.value = data as AiRecommendResponse
      if (data.autoAssigned && data.assignedWorkerId && repair.workerId === data.assignedWorkerId) {
        toast.success(`AI 已自动指派给 ${data.rankings[0]?.workerName ?? '维修人员'}`)
        await fetchData()
        aiRecommendLoading.value = false
        return
      } else {
        assignWorkerId.value = data.assignedWorkerId || data.rankings[0].workerId
      }
    }
  } catch { /* silently fail */ }
  aiRecommendLoading.value = false

  if (repair.repairStatus >= 5) {
    try {
      feedback.value = await getRepairFeedbackApi(repair.id)
    } catch { /* ignored */ }
  }
}

const handleAssignWorker = async () => {
  if (!currentRepair.value || !assignWorkerId.value) return
  assigning.value = true
  try {
    await assignWorkerApi(currentRepair.value.id, assignWorkerId.value)
    toast.success('指派成功')
    showDetail.value = false
    await fetchData()
  } catch (err: any) {
    toast.error(err.message || '指派失败')
  }
  assigning.value = false
}

const handleUpdateStatus = async () => {
  if (!currentRepair.value) return

  updating.value = true
  try {
    await updateRepairStatusApi(currentRepair.value.id, {
      repairStatus: updateForm.value.status as RepairStatus,
      remark: updateForm.value.processRemark
    })
    toast.success('状态更新成功')
    showDetail.value = false
    await fetchData()
  } catch (err: any) {
    toast.error(err.message || '状态更新失败')
  } finally {
    updating.value = false
  }
}

const handleSearch = () => {
  query.value.pageNum = 1
  fetchData()
}

const handleExport = async () => {
  try {
    const params: Record<string, any> = {
      keyword: query.value.keyword,
      repairStatus: query.value.repairStatus,
      categoryId: query.value.categoryId
    }
    if (!params.keyword) delete params.keyword
    if (!params.repairStatus && params.repairStatus !== 0) delete params.repairStatus
    if (!params.categoryId) delete params.categoryId
    await exportRepairOrdersApi(params)
    toast.success('导出成功')
  } catch (err: any) {
    toast.error(err.message || '导出失败')
  }
}

onMounted(async () => {
  fetchCategories()
  await fetchData()
  
  const queryOrderId = route.query.orderId
  if (queryOrderId) {
    const orderId = Number(queryOrderId)
    const match = repairs.value.find(r => r.id === orderId)
    if (match) {
      openDetail(match)
    } else {
      try {
        const detail = await getRepairDetailApi(orderId)
        if (detail) {
          openDetail(detail)
        }
      } catch (err: any) {
        console.error('Failed to auto-open order detail:', err)
      }
    }
  }
})

watch([() => query.value.repairStatus, () => query.value.categoryId], () => {
  query.value.pageNum = 1
  fetchData()
})
</script>

<template>
  <div class="repair-manage-view">
    <UiSectionTitle eyebrow="工单处理" title="报修工单管理" />

    <!-- 自动化面板 — Double-Bezel 架构 -->
    <div class="dashboard-bento">
      <!-- 统计卡片组 — Bento Grid -->
      <div class="stats-bento">
        <div class="stat-bento-card stat-primary">
          <div class="stat-card-shell">
            <div class="stat-card-core">
              <span class="stat-eyebrow">总工单</span>
              <span class="stat-number">{{ total }}</span>
            </div>
          </div>
        </div>
        <div class="stat-bento-card stat-urgent">
          <div class="stat-card-shell">
            <div class="stat-card-core">
              <span class="stat-eyebrow">待受理</span>
              <span class="stat-number">{{ pendingCount }}</span>
              <div class="stat-glow"></div>
            </div>
          </div>
        </div>
        <div class="stat-bento-card stat-active">
          <div class="stat-card-shell">
            <div class="stat-card-core">
              <span class="stat-eyebrow">处理中</span>
              <span class="stat-number">{{ processingCount }}</span>
            </div>
          </div>
        </div>
        <div class="stat-bento-card stat-done">
          <div class="stat-card-shell">
            <div class="stat-card-core">
              <span class="stat-eyebrow">已完成</span>
              <span class="stat-number">{{ completedCount }}</span>
            </div>
          </div>
        </div>
        <div class="stat-bento-card stat-action-card">
          <div class="stat-card-shell">
            <div class="stat-card-core">
              <button
                class="auto-process-btn group"
                @click="handleAutoProcess"
                :disabled="pendingCount === 0 || autoProcessing"
              >
                <span class="btn-label">{{ autoProcessing ? '处理中…' : `一键自动处理 ${pendingCount}单` }}</span>
                <span class="btn-arrow-wrap">
                  <svg class="btn-arrow" viewBox="0 0 16 16" fill="none"><path d="M3 8h10M9 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                </span>
              </button>
              <div v-if="autoResult" class="auto-result-inline">
                <span class="ar-ok">{{ autoResult.autoAssigned }}个自动指派</span>
                <span v-if="autoResult.failed" class="ar-miss">{{ autoResult.failed }}个需人工</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 — 悬浮玻璃面板 -->
    <div class="filter-glass">
      <div class="filter-row">
        <div class="search-wrap">
          <svg class="search-icon" viewBox="0 0 16 16" fill="none"><circle cx="7" cy="7" r="5.5" stroke="currentColor" stroke-width="1.2"/><path d="M11 11l3.5 3.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
          <input
            v-model="query.keyword"
            placeholder="搜索工单号或报修标题"
            @keyup.enter="handleSearch"
            class="search-input"
          />
        </div>
        <button class="glass-btn" @click="handleSearch">筛选</button>
        <button class="glass-btn glass-btn-ghost" @click="handleExport">导出 Excel</button>
      </div>

      <!-- 状态标签页 -->
      <div class="status-tabs">
        <button
          v-for="s in statusOptions"
          :key="s.value"
          :class="['status-tab', { active: query.repairStatus === s.value }]"
          @click="query.repairStatus = s.value; handleSearch()"
        >
          {{ s.label }}
        </button>
      </div>

      <!-- 分类标签页 -->
      <div class="category-tabs">
        <button
          :class="['cat-tab', { active: query.categoryId === '' }]"
          @click="query.categoryId = ''; handleSearch()"
        >
          全部 ({{ total }})
        </button>
        <button
          v-for="cat in categories"
          :key="cat.id"
          :class="['cat-tab', { active: query.categoryId === cat.id }]"
          @click="query.categoryId = cat.id; handleSearch()"
        >
          {{ cat.categoryName }}
        </button>
      </div>
    </div>

    <!-- 工单表格 — 双层嵌套卡片 -->
    <div class="table-shell">
      <div class="table-core">
        <table class="mc-table">
          <thead>
            <tr>
              <th>工单号</th>
              <th>标题</th>
              <th>分类</th>
              <th>紧急程度</th>
              <th>学生</th>
              <th>维修人员</th>
              <th>宿舍</th>
              <th>状态</th>
              <th>提交时间</th>
              <th class="action-col">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="repair in repairs" :key="repair.id">
              <td><span class="order-no">{{ repair.orderNo }}</span></td>
              <td class="title-cell">{{ repair.title }}</td>
              <td>{{ getCategoryName(repair) }}</td>
              <td>
                <span :class="['urgency-tag', getUrgencyClass(repair.urgency)]">
                  {{ repair.urgency || '一般' }}
                </span>
              </td>
              <td>{{ repair.realName }}</td>
              <td><span class="worker-name-cell">{{ repair.workerName || '-' }}</span></td>
              <td>{{ repair.dormitoryBuilding }}#{{ repair.roomNo }}</td>
              <td>
                <span
                  class="status-tag"
                  :style="{
                    color: getStatusMeta(repair.repairStatus).color,
                    backgroundColor: `${getStatusMeta(repair.repairStatus).color}12`
                  }"
                >
                  {{ getStatusMeta(repair.repairStatus).label }}
                </span>
              </td>
              <td class="time-cell">{{ repair.submitTime }}</td>
              <td class="action-col">
                <button class="text-btn" @click="openDetail(repair)">详情 / 处理</button>
              </td>
            </tr>
            <tr v-if="repairs.length === 0 && !loading">
              <td colspan="10" class="empty-row">暂无相关工单</td>
            </tr>
          </tbody>
        </table>

        <div class="pagination">
          <span>共 {{ total }} 条数据</span>
          <div class="page-btns">
            <button class="glass-btn glass-btn-ghost" :disabled="query.pageNum <= 1" @click="query.pageNum--; fetchData()">上一页</button>
            <span class="current-page">{{ query.pageNum }}</span>
            <button class="glass-btn glass-btn-ghost" :disabled="query.pageNum * query.pageSize >= total" @click="query.pageNum++; fetchData()">下一页</button>
          </div>
        </div>
      </div>
    </div>

    <UiSlideOver :visible="showDetail" title="工单详情与处理" max-width="500px" @close="showDetail = false">
      <div v-if="currentRepair" class="modal-body">
        <div class="detail-section">
          <div class="section-label">基础信息</div>
          <div class="detail-grid">
            <div class="item"><span class="l">工单号：</span>{{ currentRepair.orderNo }}</div>
            <div class="item"><span class="l">状态：</span>{{ getStatusMeta(currentRepair.repairStatus).label }}</div>
            <div class="item"><span class="l">学生：</span>{{ currentRepair.realName }} ({{ currentRepair.username }})</div>
            <div class="item"><span class="l">分类：</span>{{ getCategoryName(currentRepair) }}</div>
            <div class="item"><span class="l">紧急程度：</span>
              <span :class="['urgency-tag', getUrgencyClass(currentRepair.urgency)]">
                {{ currentRepair.urgency || '一般' }}
              </span>
            </div>
            <div class="item"><span class="l">维修人员：</span>{{ currentRepair.workerName || '未指派' }}</div>
            <div class="item"><span class="l">宿舍：</span>{{ currentRepair.dormitoryBuilding }}#{{ currentRepair.roomNo }}</div>
            <div class="item"><span class="l">提交时间：</span>{{ currentRepair.submitTime }}</div>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-label">报修内容</div>
          <p class="desc-text">{{ getRepairDescription(currentRepair) }}</p>
          <div v-if="currentImages.length > 0" class="image-list">
            <img v-for="(img, idx) in currentImages" :key="idx" :src="img" class="thumb-img" />
          </div>
        </div>

        <div v-if="currentRepair.repairStatus < 5" class="process-section">
          <div class="section-label">处理操作</div>

          <!-- 指派维修人员 (仅状态1) -->
          <div v-if="currentRepair.repairStatus === 1" class="assign-section">
            <label class="mc-label">指派维修人员</label>
            <div class="assign-row">
              <select v-model="assignWorkerId" class="mc-select">
                <option :value="0" disabled>-- 请选择维修人员 --</option>
                <option v-for="r in repairerList" :key="r.id" :value="r.id">{{ r.realName }} ({{ r.username }})</option>
              </select>
              <UiButton @click="handleAssignWorker" :loading="assigning" :disabled="!assignWorkerId">指派</UiButton>
            </div>

            <div v-if="aiRecommendLoading" class="ai-recommend-loading">
              <span class="ai-loader"></span>
              <span>AI 正在推荐维修人员...</span>
            </div>

            <div v-if="aiRecommendResult && aiRecommendResult.rankings?.length" class="ai-recommend-card">
                <div class="ai-recommend-header">
                  🤖 AI 推荐
                  <span v-if="aiRecommendResult.autoAssigned" class="ai-auto-tag">高置信推荐</span>
                </div>
              <div class="ai-recommend-body">
                <div v-for="(r, i) in aiRecommendResult.rankings.slice(0, 3)" :key="r.workerId"
                  class="ai-ranking-row" :class="{ 'ai-top': i === 0 }"
                  @click="assignWorkerId = r.workerId">
                  <span class="ai-rank">#{{ i + 1 }}</span>
                  <span class="ai-worker-name">{{ r.workerName }}</span>
                  <span class="ai-confidence-badge" :class="getConfidenceClass(r.matchScore)">
                    {{ r.matchScore }}% 匹配度
                  </span>
                  <span class="ai-reason">{{ r.reason }}</span>
                </div>
              </div>
            </div>

            <p class="assign-hint">指派后工单状态将变为"已派单"，维修人员可在其后台接单处理。</p>
          </div>

          <div class="process-form" style="margin-top: 16px">
            <UiSelect
              v-model="updateForm.status"
              label="直接更新状态"
              :options="updateStatusOptions"
            />
            <div class="mc-input-group">
              <label class="mc-label">处理备注（学生可见）</label>
              <textarea
                v-model="updateForm.processRemark"
                class="mc-textarea"
                placeholder="填写处理进度、原因说明或结果说明"
                rows="3"
              ></textarea>
            </div>
            <UiButton type="primary" class="full-width" :loading="updating" @click="handleUpdateStatus">
              确认更新状态
            </UiButton>
          </div>
        </div>

        <div v-else class="process-section">
          <div class="section-label">处理备注</div>
          <p class="desc-text">{{ getProcessRemark(currentRepair) || '暂无处理备注' }}</p>

          <div class="feedback-section" v-if="feedback" style="margin-top: 24px; padding-top: 24px; border-top: 1px solid var(--mc-hairline);">
            <div class="section-label">学生评价</div>
            <div class="admin-feedback-card">
              <UiStarDisplay :score="feedback.score" show-label size="medium" />
              <p class="feedback-text" v-if="feedback.content">{{ feedback.content }}</p>
              <div class="feedback-footer">
                <span>评价于：{{ feedback.createTime?.replace('T', ' ') }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </UiSlideOver>
  </div>
</template>

<style scoped>
/* ============================================
   HIGH-END DESIGN — Ethereal Glass × Bento
   ============================================ */
:root { --ease-spring: cubic-bezier(0.32,0.72,0,1); }
:root { --ease-out-expo: cubic-bezier(0.16,1,0.3,1); }

.repair-manage-view {
  display: flex; flex-direction: column; gap: 24px;
  animation: fadeUpIn 0.6s var(--ease-out-expo);
}
@keyframes fadeUpIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== Bento Stats ===== */
.stats-bento { display: grid; grid-template-columns: 2fr 1fr 1fr 1fr 2fr; gap: 10px; align-items: stretch; }

/* Double-Bezel Card */
.stat-card-shell {
  background: rgba(207,69,0,0.06); border: 1px solid rgba(207,69,0,0.08);
  border-radius: 20px; padding: 2px; height: 100%;
  transition: all 0.4s var(--ease-spring);
}
.stat-card-shell:hover { background: rgba(207,69,0,0.1); transform: translateY(-1px); }

.stat-card-core {
  background: var(--mc-white); border-radius: 18px;
  padding: 18px 22px; height: 100%;
  display: flex; flex-direction: column; justify-content: center; gap: 4px;
  position: relative; overflow: hidden;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.6);
}

.stat-eyebrow { font-size: 10px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.08em; color: var(--mc-muted); }
.stat-number { font-family: var(--mc-font-display); font-size: 36px; font-weight: 700; color: var(--mc-ink); line-height: 1; letter-spacing: -1px; }

.stat-urgent .stat-number { color: var(--mc-signal); }
.stat-urgent .stat-glow { position: absolute; top: -20px; right: -20px; width: 120px; height: 120px; background: radial-gradient(circle,rgba(207,69,0,0.08),transparent 70%); pointer-events: none; }
.stat-active .stat-card-shell { background: rgba(59,130,246,0.06); border-color: rgba(59,130,246,0.1); }
.stat-active .stat-number { color: #3b82f6; }
.stat-done .stat-card-shell { background: rgba(22,163,74,0.06); border-color: rgba(22,163,74,0.1); }
.stat-done .stat-number { color: #16a34a; }

/* Action Card */
.stat-action-card .stat-card-core { align-items: flex-start; gap: 8px; }

.auto-process-btn {
  display: flex; align-items: center; gap: 10px; width: 100%;
  padding: 12px 18px; background: var(--mc-signal); color: #fff;
  border: none; border-radius: 14px; font-size: 14px; font-weight: 600;
  cursor: pointer; transition: all 0.35s var(--ease-spring); font-family: inherit;
}
.auto-process-btn:hover:not(:disabled) { transform: scale(1.01); box-shadow: 0 8px 24px rgba(207,69,0,0.25); }
.auto-process-btn:active:not(:disabled) { transform: scale(0.98); }
.auto-process-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-label { flex: 1; text-align: left; }

.btn-arrow-wrap {
  width: 28px; height: 28px; border-radius: 50%;
  background: rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center;
  transition: all 0.35s var(--ease-spring);
}
.group:hover .btn-arrow-wrap { transform: translateX(2px) translateY(-1px); background: rgba(255,255,255,0.3); }
.btn-arrow { width: 14px; height: 14px; }
.auto-result-inline { display: flex; gap: 12px; font-size: 12px; }
.ar-ok { color: #16a34a; font-weight: 600; }
.ar-miss { color: var(--mc-signal); }

/* Glass Filter */
.filter-glass {
  background: rgba(255,255,255,0.7); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(0,0,0,0.06); border-radius: 20px; padding: 18px 22px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
}
.filter-row { display: flex; gap: 10px; align-items: center; }
.search-wrap { flex: 1; display: flex; align-items: center; gap: 10px; background: var(--mc-white); border: 1px solid rgba(0,0,0,0.06); border-radius: 14px; padding: 0 14px; transition: border-color 0.2s, box-shadow 0.2s; }
.search-wrap:focus-within { border-color: var(--mc-signal); box-shadow: 0 0 0 3px rgba(207,69,0,0.06); }
.search-icon { width: 16px; height: 16px; color: var(--mc-muted); flex-shrink: 0; }
.search-input { flex: 1; border: none; outline: none; background: transparent; font-size: 14px; color: var(--mc-ink); padding: 10px 0; font-family: inherit; }
.search-input::placeholder { color: var(--mc-muted-soft); }

.glass-btn {
  padding: 10px 20px; background: var(--mc-ink); color: #fff;
  border: none; border-radius: 14px; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.35s var(--ease-spring); font-family: inherit; white-space: nowrap;
}
.glass-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.glass-btn:active { transform: scale(0.97); }
.glass-btn-ghost { background: transparent; color: var(--mc-ink); border: 1px solid rgba(0,0,0,0.1); }
.glass-btn-ghost:hover { background: rgba(0,0,0,0.03); border-color: rgba(0,0,0,0.2); }
.glass-btn:disabled { opacity: 0.4; cursor: not-allowed; transform: none; box-shadow: none; }

/* Tabs */
.status-tabs { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(0,0,0,0.05); }
.status-tab { padding: 6px 18px; border: none; border-radius: 10px; background: transparent; font-size: 13px; font-weight: 500; color: var(--mc-muted); cursor: pointer; transition: all 0.3s var(--ease-spring); font-family: inherit; }
.status-tab:hover { background: rgba(207,69,0,0.06); color: var(--mc-signal); }
.status-tab.active { background: var(--mc-signal); color: #fff; font-weight: 600; box-shadow: 0 2px 8px rgba(207,69,0,0.2); }

.category-tabs { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 12px; }
.cat-tab { padding: 5px 14px; border: 1px solid rgba(0,0,0,0.06); border-radius: 20px; background: var(--mc-white); font-size: 12px; color: var(--mc-muted); cursor: pointer; transition: all 0.3s var(--ease-spring); font-family: inherit; }
.cat-tab:hover { border-color: var(--mc-taupe); color: var(--mc-ink); }
.cat-tab.active { background: var(--mc-ink); color: #fff; border-color: var(--mc-ink); font-weight: 500; }

/* Double-Bezel Table */
.table-shell { background: rgba(0,0,0,0.03); border: 1px solid rgba(0,0,0,0.05); border-radius: 22px; padding: 2px; }
.table-core { background: var(--mc-white); border-radius: 20px; overflow: hidden; box-shadow: inset 0 1px 0 rgba(255,255,255,0.8); }

.mc-table { width: 100%; border-collapse: separate; border-spacing: 0; }
.mc-table th { padding: 14px 20px; font-size: 10px; font-weight: 600; color: var(--mc-muted); background: rgba(0,0,0,0.012); border-bottom: 1px solid rgba(0,0,0,0.04); text-transform: uppercase; letter-spacing: 0.06em; }
.mc-table th:first-child, .mc-table td:first-child { padding-left: 24px; }
.mc-table th:last-child, .mc-table td:last-child { padding-right: 24px; }
.mc-table td { padding: 16px 20px; font-size: 13px; border-bottom: 1px solid rgba(0,0,0,0.03); color: var(--mc-ink); vertical-align: middle; }
.mc-table tbody tr { transition: background 0.15s; }
.mc-table tbody tr:hover { background: rgba(207,69,0,0.025); }
.mc-table tbody tr:last-child td { border-bottom: none; }

.order-no { font-family: 'SF Mono','Cascadia Code',monospace; font-size: 11px; color: var(--mc-muted); background: rgba(0,0,0,0.025); padding: 2px 8px; border-radius: 5px; }
.title-cell { max-width: 180px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.status-tag { font-size: 11px; font-weight: 600; padding: 4px 12px; border-radius: 20px; letter-spacing: 0.02em; }
.time-cell { font-size: 12px; color: var(--mc-muted); }
.action-col { text-align: right; white-space: nowrap; }
.urgency-tag { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; }
.urgency-tag.is-urgent { background: rgba(235,0,27,0.07); color: var(--mc-error); }
.urgency-tag.is-normal { background: rgba(0,0,0,0.04); color: var(--mc-charcoal); }
.urgency-tag.is-low { background: rgba(93,184,114,0.08); color: var(--mc-success-text); }
.text-btn { color: var(--mc-signal); font-weight: 600; font-size: 12px; background: none; border: none; cursor: pointer; padding: 4px 12px; border-radius: 8px; transition: background 0.2s; }
.text-btn:hover { background: rgba(207,69,0,0.05); }

.pagination { padding: 14px 24px; display: flex; justify-content: space-between; align-items: center; font-size: 13px; color: var(--mc-muted); border-top: 1px solid rgba(0,0,0,0.04); }
.page-btns { display: flex; align-items: center; gap: 10px; }
.current-page { font-weight: 600; color: var(--mc-ink); font-size: 14px; }

/* Detail Panel */
.modal-body { display: flex; flex-direction: column; gap: 28px; }
.section-label { font-size: 10px; text-transform: uppercase; letter-spacing: 0.08em; color: var(--mc-muted-soft); margin-bottom: 12px; font-weight: 600; }
.detail-grid { display: grid; grid-template-columns: 1fr; gap: 14px; font-size: 14px; }
.detail-grid .l { color: var(--mc-muted); font-size: 12px; }
.desc-text { font-size: 14px; line-height: 1.7; color: var(--mc-ink); background: var(--mc-lifted); padding: 18px; border-radius: 12px; border: 1px solid rgba(0,0,0,0.04); }
.image-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.thumb-img { width: 64px; height: 64px; border-radius: 12px; object-fit: cover; border: 1px solid var(--mc-canvas); }
.process-form { display: flex; flex-direction: column; gap: 16px; }
.mc-label { display: block; margin-bottom: 6px; font-size: 12px; font-weight: 500; color: var(--mc-ink); }
.mc-textarea { width: 100%; padding: 12px 14px; background: var(--mc-canvas); border: 1px solid rgba(0,0,0,0.06); border-radius: 12px; font-size: 13px; outline: none; font-family: inherit; resize: vertical; transition: border-color 0.2s,background 0.2s; }
.mc-textarea:focus { border-color: var(--mc-ink); background: var(--mc-white); }
.full-width { width: 100%; }
.assign-section { margin-bottom: 4px; }
.assign-row { display: flex; gap: 10px; align-items: center; }
.mc-select { flex: 1; height: 42px; padding: 0 14px; background: var(--mc-canvas); border: 1px solid rgba(0,0,0,0.06); border-radius: 12px; font-size: 13px; color: var(--mc-ink); outline: none; font-family: inherit; transition: border-color 0.2s,background 0.2s; }
.mc-select:focus { border-color: var(--mc-ink); background: var(--mc-white); }
.assign-hint { font-size: 12px; color: var(--mc-muted); margin-top: 8px; line-height: 1.5; }
.worker-name-cell { font-size: 13px; color: var(--mc-muted); }
.empty-row { text-align: center; padding: 64px; color: var(--mc-muted-soft); font-size: 14px; }

/* AI Section */
.ai-recommend-loading { display: flex; align-items: center; gap: 8px; margin-top: 12px; font-size: 13px; color: var(--mc-muted); }
.ai-loader { width: 16px; height: 16px; border: 2px solid rgba(0,0,0,0.06); border-top-color: var(--mc-signal); border-radius: 50%; animation: ai-spin 0.8s linear infinite; }
@keyframes ai-spin { to { transform: rotate(360deg); } }
.ai-recommend-card { margin-top: 16px; padding: 16px; border-radius: 16px; border: 1px solid rgba(0,0,0,0.06); background: linear-gradient(135deg,rgba(207,69,0,0.02),var(--mc-white)); }
.ai-recommend-header { font-size: 13px; font-weight: 600; margin-bottom: 12px; color: var(--mc-ink); display: flex; align-items: center; gap: 8px; }
.ai-auto-tag { font-size: 10px; font-weight: 600; color: #fff; background: #16a34a; padding: 2px 8px; border-radius: 10px; }
.ai-ranking-row { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 10px; cursor: pointer; transition: all 0.2s; font-size: 13px; }
.ai-ranking-row:hover { background: rgba(207,69,0,0.04); }
.ai-ranking-row.ai-top { background: rgba(207,69,0,0.03); border: 1px solid rgba(207,69,0,0.08); }
.ai-rank { font-weight: 700; color: var(--mc-ink); width: 28px; font-size: 14px; }
.ai-worker-name { font-weight: 600; min-width: 64px; }
.ai-confidence-badge { font-weight: 700; font-size: 11px; padding: 3px 8px; border-radius: 6px; letter-spacing: 0.02em; display: inline-flex; align-items: center; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
.ai-confidence-badge.is-high { color: #16a34a; background: #f0fdf4; border: 1px solid #bbf7d0; }
.ai-confidence-badge.is-mid { color: #d97706; background: #fffbeb; border: 1px solid #fde68a; }
.ai-confidence-badge.is-low { color: #dc2626; background: #fef2f2; border: 1px solid #fecaca; }
.ai-reason { flex: 1; color: var(--mc-muted); font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.admin-feedback-card { background: var(--mc-lifted); padding: 18px; border-radius: 16px; border: 1px solid rgba(0,0,0,0.06); margin-top: 12px; }
.feedback-text { margin-top: 14px; font-size: 14px; color: var(--mc-ink); line-height: 1.6; }
.feedback-footer { margin-top: 14px; font-size: 11px; color: var(--mc-muted); }
</style>


