<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiInput from '../../components/ui/UiInput.vue'
import UiSelect from '../../components/ui/UiSelect.vue'
import { getUserListApi, updateUserStatusApi, resetUserPasswordApi } from '../../api/user'
import { getPageRecords } from '../../utils/page'
import { useToast } from '../../composables/useToast'
import { useConfirm } from '../../composables/useConfirm'

const toast = useToast()
const { confirm } = useConfirm()

const loading = ref(false)
const users = ref<any[]>([])
const total = ref(0)

const query = ref({
  username: '',
  realName: '',
  studentNo: '',
  role: '',
  pageNum: 1,
  pageSize: 10
})

const roleOptions = [
  { label: '全部角色', value: '' },
  { label: '学生', value: 'STUDENT' },
  { label: '维修人员', value: 'REPAIRER' },
  { label: '管理员', value: 'ADMIN' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = { ...query.value }
    if (!params.role) delete params.role

    const data: any = await getUserListApi(params)
    users.value = getPageRecords(data)
    total.value = data.total || 0
  } catch (err) {
    console.error('Failed to fetch users:', err)
  } finally {
    loading.value = false
  }
}

const handleStatusChange = async (user: any) => {
  const newStatus = user.status === 1 ? 0 : 1
  const actionText = newStatus === 1 ? '启用' : '禁用'

  if (!await confirm(`确定要${actionText}用户 "${user.realName || user.username}" 吗？`)) return

  try {
    await updateUserStatusApi(user.id, newStatus)
    toast.success(`${actionText}成功`)
    fetchData()
  } catch (err: any) {
    toast.error(err.message || '操作失败')
  }
}

const handleResetPassword = async (user: any) => {
  if (!await confirm(`确定要重置用户 "${user.realName || user.username}" 的密码吗？重置后密码通常为 123456。`)) return

  try {
    await resetUserPasswordApi(user.id)
    toast.success('密码重置成功')
  } catch (err: any) {
    toast.error(err.message || '重置失败')
  }
}

onMounted(() => {
  fetchData()
})

const handleSearch = () => {
  query.value.pageNum = 1
  fetchData()
}

watch(() => query.value.role, handleSearch)
</script>

<template>
  <div class="user-manage">
    <UiSectionTitle eyebrow="用户管理" title="用户账号管理" />

    <UiCard padding="20px" class="filter-card">
      <div class="filter-grid">
        <UiInput v-model="query.username" placeholder="搜索用户名..." @keyup.enter="handleSearch" />
        <UiInput v-model="query.realName" placeholder="搜索真实姓名..." @keyup.enter="handleSearch" />
        <UiInput v-model="query.studentNo" placeholder="搜索学号..." @keyup.enter="handleSearch" />

        <UiSelect v-model="query.role" :options="roleOptions" placeholder="全部角色" />

        <UiButton type="primary" @click="handleSearch">筛选</UiButton>
      </div>
    </UiCard>

    <UiCard padding="0" class="table-card">
      <div class="table-wrapper">
        <table class="mc-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>姓名</th>
              <th>学号</th>
              <th>宿舍信息</th>
              <th>角色</th>
              <th>状态</th>
              <th class="action-col">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.id }}</td>
              <td><span class="username-text">{{ user.username }}</span></td>
              <td class="name-cell">{{ user.realName || '-' }}</td>
              <td>{{ user.studentNo || '-' }}</td>
              <td>
                <span v-if="user.dormitoryBuilding" class="dorm-text">
                  {{ user.dormitoryBuilding }}#{{ user.roomNo }}
                </span>
                <span v-else>-</span>
              </td>
              <td>
                <span :class="['role-tag', user.role === 'ADMIN' ? 'is-admin' : user.role === 'REPAIRER' ? 'is-repairer' : 'is-student']">
                  {{ user.role === 'ADMIN' ? '管理员' : user.role === 'REPAIRER' ? '维修人员' : '学生' }}
                </span>
              </td>
              <td>
                <span :class="['status-dot', user.status === 1 ? 'is-active' : 'is-disabled']"></span>
                {{ user.status === 1 ? '正常' : '禁用' }}
              </td>
              <td class="action-col">
                <div class="action-btns">
                  <button class="text-btn" @click="handleStatusChange(user)">
                    {{ user.status === 1 ? '禁用' : '启用' }}
                  </button>
                  <button class="text-btn" @click="handleResetPassword(user)">重置密码</button>
                </div>
              </td>
            </tr>
            <tr v-if="users.length === 0 && !loading">
              <td colspan="8" class="empty-row">未找到匹配的用户</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <span>共 {{ total }} 位用户</span>
        <div class="page-btns">
          <UiButton type="secondary" :disabled="query.pageNum <= 1" @click="query.pageNum--; fetchData()">上一页</UiButton>
          <span class="current">{{ query.pageNum }}</span>
          <UiButton type="secondary" :disabled="query.pageNum * query.pageSize >= total" @click="query.pageNum++; fetchData()">下一页</UiButton>
        </div>
      </div>
    </UiCard>
  </div>
</template>

<style scoped>
.user-manage {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 140px 90px;
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

.mc-table tbody tr {
  transition: background-color var(--mc-transition);
}

.mc-table tbody tr:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.username-text {
  font-weight: 500;
  color: var(--mc-ink);
}

.name-cell {
  color: var(--mc-ink);
}

.role-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: var(--mc-radius-pill);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.is-admin {
  background-color: rgba(235, 0, 27, 0.1);
  color: var(--mc-signal);
}

.is-student {
  background-color: rgba(0, 0, 0, 0.04);
  color: var(--mc-charcoal);
}

.is-repairer {
  background-color: rgba(207, 69, 0, 0.1);
  color: var(--mc-signal);
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
}

.is-active { background-color: var(--mc-success); }
.is-disabled { background-color: var(--mc-error); }

.action-col {
  text-align: right;
}

.action-btns {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.text-btn {
  background: none;
  border: none;
  font-weight: 500;
  font-size: 12px;
  color: var(--mc-signal);
  cursor: pointer;
}

.text-btn:hover { text-decoration: underline; }

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

@media (max-width: 1000px) {
  .filter-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>


