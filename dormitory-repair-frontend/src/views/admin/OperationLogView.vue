<script setup lang="ts">
import { onMounted, ref } from 'vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiInput from '../../components/ui/UiInput.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import UiSelect from '../../components/ui/UiSelect.vue'
import { getOperationLogsApi } from '../../api/log'
import { useToast } from '../../composables/useToast'
import { getPageRecords } from '../../utils/page'

const toast = useToast()

const loading = ref(false)
const logs = ref<any[]>([])
const total = ref(0)

const query = ref({
  keyword: '',
  operationType: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})

const typeOptions = [
  { label: '全部类型', value: '' },
  { label: '分类管理', value: 'CATEGORY' },
  { label: '公告管理', value: 'NOTICE' },
  { label: '用户管理', value: 'USER' },
  { label: '报修管理', value: 'REPAIR' }
]

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '成功', value: '1' },
  { label: '失败', value: '0' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = { ...query.value }
    if (!params.operationType) {
      delete params.operationType
    }
    if (params.status === '') {
      delete params.status
    }

    const data: any = await getOperationLogsApi(params)
    logs.value = getPageRecords(data)
    total.value = data.total || 0
  } catch (err: any) {
    toast.error(err.message || '获取操作日志失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.value.pageNum = 1
  fetchData()
}

const formatTime = (time: string) => {
  if (!time) {
    return '-'
  }
  return time.replace('T', ' ')
}

onMounted(fetchData)
</script>

<template>
  <div class="log-view">
    <UiSectionTitle eyebrow="操作日志" title="管理员操作审计" />

    <UiCard padding="20px" class="filter-card">
      <div class="filter-grid">
        <UiInput v-model="query.keyword" placeholder="搜索操作描述..." @keyup.enter="handleSearch" />
        <UiSelect v-model="query.operationType" :options="typeOptions" placeholder="全部类型" />
        <UiSelect v-model="query.status" :options="statusOptions" placeholder="全部状态" />
        <UiButton type="primary" @click="handleSearch">筛选</UiButton>
      </div>
    </UiCard>

    <UiCard padding="0" class="table-card">
      <div class="table-wrapper">
        <table class="mc-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>操作类型</th>
              <th>操作人</th>
              <th>描述</th>
              <th>方法</th>
              <th>请求参数</th>
              <th>IP</th>
              <th>耗时</th>
              <th>状态</th>
              <th>操作时间</th>
              <th>异常信息</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in logs" :key="log.id">
              <td>{{ log.id }}</td>
              <td>
                <span :class="['type-tag', 'is-' + (log.operationType || 'OTHER').toLowerCase()]">
                  {{ log.operationType || '-' }}
                </span>
              </td>
              <td>{{ log.operatorName || '-' }}</td>
              <td class="desc-cell">{{ log.description || '-' }}</td>
              <td class="method-cell">{{ log.methodName || '-' }}</td>
              <td class="params-cell">
                <span class="params-trunc" :title="log.requestParams">{{ log.requestParams || '-' }}</span>
              </td>
              <td>{{ log.ipAddress || '-' }}</td>
              <td>{{ log.executionTime != null ? `${log.executionTime}ms` : '-' }}</td>
              <td>
                <span :class="['status-dot', log.status === 1 ? 'is-success' : 'is-fail']"></span>
                {{ log.status === 1 ? '成功' : '失败' }}
              </td>
              <td class="time-cell">{{ formatTime(log.createTime) }}</td>
              <td class="error-cell">{{ log.errorMessage || '-' }}</td>
            </tr>
            <tr v-if="logs.length === 0 && !loading">
              <td colspan="11" class="empty-row">暂无操作日志记录</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <span>共 {{ total }} 条记录</span>
        <div class="page-btns">
          <UiButton type="secondary" :disabled="query.pageNum <= 1" @click="query.pageNum--; fetchData()">上一页</UiButton>
          <span class="current">{{ query.pageNum }}</span>
          <UiButton
            type="secondary"
            :disabled="query.pageNum * query.pageSize >= total"
            @click="query.pageNum++; fetchData()"
          >
            下一页
          </UiButton>
        </div>
      </div>
    </UiCard>
  </div>
</template>

<style scoped>
.log-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-grid {
  display: grid;
  grid-template-columns: 1fr 160px 140px 90px;
  gap: 12px;
  align-items: end;
}

.table-card {
  overflow: hidden;
}

.table-wrapper {
  overflow-x: auto;
}

.mc-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  white-space: nowrap;
}

.mc-table th {
  padding: 16px 20px;
  font-size: 11px;
  font-weight: 600;
  color: var(--mc-charcoal);
  border-bottom: 2px solid var(--mc-ink);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.mc-table td {
  padding: 20px;
  font-size: 14px;
  border-bottom: 1px solid var(--mc-hairline);
  color: var(--mc-ink);
  vertical-align: middle;
}

.mc-table tbody tr:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.type-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: var(--mc-radius-pill);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.is-repair {
  background-color: rgba(235, 0, 27, 0.1);
  color: var(--mc-signal);
}

.is-user {
  background-color: rgba(0, 0, 0, 0.04);
  color: var(--mc-charcoal);
}

.is-notice {
  background-color: rgba(207, 69, 0, 0.1);
  color: var(--mc-signal);
}

.is-category {
  background-color: rgba(0, 123, 255, 0.1);
  color: #0066cc;
}

.is-other {
  background-color: rgba(0, 0, 0, 0.04);
  color: var(--mc-muted);
}

.desc-cell {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.method-cell {
  max-width: 180px;
  font-size: 13px;
  color: var(--mc-muted);
}

.params-cell {
  max-width: 200px;
}

.params-trunc {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--mc-muted);
}

.time-cell {
  font-size: 13px;
}

.error-cell {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 12px;
  color: var(--mc-error);
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
}

.is-success {
  background-color: var(--mc-success);
}

.is-fail {
  background-color: var(--mc-error);
}

.pagination {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--mc-muted);
  border-top: 1px solid var(--mc-canvas);
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

.empty-row {
  text-align: center;
  padding: 48px;
  color: var(--mc-muted);
  font-size: 13px;
}
</style>
