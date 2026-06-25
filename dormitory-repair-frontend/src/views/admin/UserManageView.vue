<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiInput from '../../components/ui/UiInput.vue'
import UiSelect from '../../components/ui/UiSelect.vue'
import { getUserListApi, createUserApi, updateRepairerProfileApi, updateUserStatusApi, resetUserPasswordApi } from '../../api/user'
import { getPageRecords } from '../../utils/page'
import { useToast } from '../../composables/useToast'
import { useConfirm } from '../../composables/useConfirm'
import type { UserCreateRequest, UserRole } from '../../types/models'

const toast = useToast()
const { confirm } = useConfirm()

const loading = ref(false)
const creating = ref(false)
const savingRepairer = ref(false)
const createDialogOpen = ref(false)
const repairerDialogOpen = ref(false)
const users = ref<any[]>([])
const total = ref(0)
const createErrors = ref<Record<string, string>>({})
const repairerErrors = ref<Record<string, string>>({})
const editingRepairer = ref<any | null>(null)

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

const createRoleOptions = [
  { label: '维修人员', value: 'REPAIRER' },
  { label: '学生', value: 'STUDENT' },
  { label: '管理员', value: 'ADMIN' }
]

type CreateUserForm = Omit<UserCreateRequest, 'studentNo' | 'phone' | 'dormitoryBuilding' | 'roomNo' | 'skillType' | 'serviceArea'> & {
  studentNo: string
  phone: string
  dormitoryBuilding: string
  roomNo: string
  skillType: string
  serviceArea: string
}

const createForm = ref<CreateUserForm>({
  username: '',
  password: '123456',
  realName: '',
  studentNo: '',
  phone: '',
  gender: 0,
  dormitoryBuilding: '',
  roomNo: '',
  role: 'REPAIRER',
  skillType: '',
  serviceArea: ''
})

const repairerForm = ref({
  skillType: '',
  serviceArea: ''
})

const resetCreateForm = (role: UserRole = 'REPAIRER') => {
  createForm.value = {
    username: '',
    password: '123456',
    realName: '',
    studentNo: '',
    phone: '',
    gender: 0,
    dormitoryBuilding: '',
    roomNo: '',
    role,
    skillType: role === 'REPAIRER' ? '空调维修,水电维修' : '',
    serviceArea: role === 'REPAIRER' ? '14号楼' : ''
  }
  createErrors.value = {}
}

const openCreateDialog = (role: UserRole = 'REPAIRER') => {
  resetCreateForm(role)
  createDialogOpen.value = true
}

const closeCreateDialog = () => {
  if (creating.value) return
  createDialogOpen.value = false
}

const openRepairerDialog = (user: any) => {
  editingRepairer.value = user
  repairerForm.value = {
    skillType: user.skillType || '',
    serviceArea: user.serviceArea || ''
  }
  repairerErrors.value = {}
  repairerDialogOpen.value = true
}

const closeRepairerDialog = () => {
  if (savingRepairer.value) return
  repairerDialogOpen.value = false
  editingRepairer.value = null
}

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
  if (!await confirm(`确定要重置用户 "${user.realName || user.username}" 的密码吗？`)) return

  try {
    await resetUserPasswordApi(user.id)
    toast.success('密码重置成功')
  } catch (err: any) {
    toast.error(err.message || '重置失败')
  }
}

const validateCreateForm = () => {
  const errors: Record<string, string> = {}
  if (!createForm.value.username.trim()) errors.username = '请输入用户名'
  if (!createForm.value.realName.trim()) errors.realName = '请输入真实姓名'
  if (!createForm.value.password || createForm.value.password.length < 6) errors.password = '密码至少 6 位'
  if (!createForm.value.role) errors.role = '请选择角色'
  if (createForm.value.role === 'REPAIRER' && !createForm.value.skillType?.trim()) {
    errors.skillType = '建议填写维修技能，便于 AI 派单'
  }
  if (createForm.value.role === 'REPAIRER' && !createForm.value.serviceArea?.trim()) {
    errors.serviceArea = '建议填写负责区域，便于 AI 派单'
  }
  createErrors.value = errors
  return Object.keys(errors).length === 0
}

const handleCreateUser = async () => {
  if (!validateCreateForm()) return

  creating.value = true
  try {
    await createUserApi({
      ...createForm.value,
      username: createForm.value.username.trim(),
      realName: createForm.value.realName.trim(),
      phone: createForm.value.phone?.trim(),
      studentNo: createForm.value.studentNo?.trim(),
      dormitoryBuilding: createForm.value.dormitoryBuilding?.trim(),
      roomNo: createForm.value.roomNo?.trim(),
      skillType: createForm.value.role === 'REPAIRER' ? createForm.value.skillType?.trim() : '',
      serviceArea: createForm.value.role === 'REPAIRER' ? createForm.value.serviceArea?.trim() : ''
    })
    toast.success(createForm.value.role === 'REPAIRER' ? '维修人员创建成功' : '用户创建成功')
    createDialogOpen.value = false
    query.value.role = createForm.value.role || ''
    query.value.pageNum = 1
    await fetchData()
  } catch (err: any) {
    toast.error(err.message || '创建失败')
  } finally {
    creating.value = false
  }
}

const validateRepairerForm = () => {
  const errors: Record<string, string> = {}
  if (repairerForm.value.skillType.length > 200) errors.skillType = '维修技能不能超过 200 个字符'
  if (repairerForm.value.serviceArea.length > 200) errors.serviceArea = '负责区域不能超过 200 个字符'
  repairerErrors.value = errors
  return Object.keys(errors).length === 0
}

const handleSaveRepairer = async () => {
  if (!editingRepairer.value || !validateRepairerForm()) return

  savingRepairer.value = true
  try {
    await updateRepairerProfileApi(editingRepairer.value.id, {
      skillType: repairerForm.value.skillType.trim(),
      serviceArea: repairerForm.value.serviceArea.trim()
    })
    toast.success('维修人员信息已更新')
    repairerDialogOpen.value = false
    editingRepairer.value = null
    await fetchData()
  } catch (err: any) {
    toast.error(err.message || '保存失败')
  } finally {
    savingRepairer.value = false
  }
}

onMounted(() => {
  resetCreateForm()
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

    <div class="user-actions">
      <div>
        <strong>维修人员配置</strong>
        <span>建议至少维护 3 位不同技能/区域的维修人员，AI 派单才有比较依据。</span>
      </div>
      <div class="user-action-buttons">
        <UiButton type="secondary" @click="openCreateDialog('STUDENT')">新增学生</UiButton>
        <UiButton type="primary" @click="openCreateDialog('REPAIRER')">新增维修人员</UiButton>
      </div>
    </div>

    <UiCard padding="20px" class="filter-card">
      <div class="filter-grid">
        <UiInput v-model="query.username" placeholder="搜索用户名..." @keyup.enter="handleSearch" />
        <UiInput v-model="query.realName" placeholder="搜索真实姓名..." @keyup.enter="handleSearch" />
        <UiInput v-model="query.studentNo" placeholder="搜索学号..." @keyup.enter="handleSearch" />

        <UiSelect v-model="query.role" :options="roleOptions" placeholder="全部角色" />

        <UiButton type="primary" @click="handleSearch">筛选</UiButton>
      </div>
    </UiCard>

    <Teleport to="body">
      <div v-if="createDialogOpen" class="modal-backdrop" @click.self="closeCreateDialog">
        <div class="create-dialog">
          <div class="dialog-header">
            <div>
              <span class="dialog-eyebrow">新增账号</span>
              <h3>{{ createForm.role === 'REPAIRER' ? '新增维修人员' : createForm.role === 'ADMIN' ? '新增管理员' : '新增学生' }}</h3>
            </div>
            <button type="button" class="dialog-close" @click="closeCreateDialog">×</button>
          </div>

          <div class="create-form">
            <UiSelect
              v-model="createForm.role"
              label="账号角色"
              :options="createRoleOptions"
              :error="createErrors.role"
              placeholder="选择角色"
            />
            <UiInput v-model="createForm.username" label="用户名" placeholder="如 repairer02" :error="createErrors.username" />
            <UiInput v-model="createForm.password" label="初始密码" type="password" placeholder="至少 6 位" :error="createErrors.password" />
            <UiInput v-model="createForm.realName" label="真实姓名" placeholder="如 李师傅" :error="createErrors.realName" />
            <UiInput v-model="createForm.phone" label="联系电话" placeholder="可选" />

            <template v-if="createForm.role === 'STUDENT'">
              <UiInput v-model="createForm.studentNo" label="学号" placeholder="可选" />
              <UiInput v-model="createForm.dormitoryBuilding" label="宿舍楼栋" placeholder="如 14号楼" />
              <UiInput v-model="createForm.roomNo" label="宿舍房号" placeholder="如 703" />
            </template>

            <template v-if="createForm.role === 'REPAIRER'">
              <UiInput
                v-model="createForm.skillType"
                label="维修技能"
                placeholder="如 空调维修,水电维修"
                :error="createErrors.skillType"
              />
              <UiInput
                v-model="createForm.serviceArea"
                label="负责区域"
                placeholder="如 14号楼,15号楼"
                :error="createErrors.serviceArea"
              />
            </template>
          </div>

          <div class="dialog-actions">
            <UiButton type="secondary" :disabled="creating" @click="closeCreateDialog">取消</UiButton>
            <UiButton type="primary" :loading="creating" @click="handleCreateUser">创建账号</UiButton>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="repairerDialogOpen" class="modal-backdrop" @click.self="closeRepairerDialog">
        <div class="create-dialog repairer-dialog">
          <div class="dialog-header">
            <div>
              <span class="dialog-eyebrow">维修人员资料</span>
              <h3>{{ editingRepairer?.realName || editingRepairer?.username || '维修人员' }}</h3>
            </div>
            <button type="button" class="dialog-close" @click="closeRepairerDialog">×</button>
          </div>

          <div class="repairer-form">
            <UiInput
              v-model="repairerForm.skillType"
              label="维修技能"
              placeholder="如 空调维修,水电维修"
              :error="repairerErrors.skillType"
            />
            <UiInput
              v-model="repairerForm.serviceArea"
              label="负责区域"
              placeholder="如 14号楼,15号楼"
              :error="repairerErrors.serviceArea"
            />
          </div>

          <div class="dialog-actions">
            <UiButton type="secondary" :disabled="savingRepairer" @click="closeRepairerDialog">取消</UiButton>
            <UiButton type="primary" :loading="savingRepairer" @click="handleSaveRepairer">保存信息</UiButton>
          </div>
        </div>
      </div>
    </Teleport>

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
                <div v-if="user.role === 'REPAIRER' && (user.skillType || user.serviceArea)" class="repairer-info">
                  <div v-if="user.skillType" class="info-line">技能: {{ user.skillType }}</div>
                  <div v-if="user.serviceArea" class="info-line">区域: {{ user.serviceArea }}</div>
                </div>
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
                  <button v-if="user.role === 'REPAIRER'" class="text-btn" @click="openRepairerDialog(user)">编辑资料</button>
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

.user-actions {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px 20px;
  border: 1px solid var(--mc-hairline);
  border-radius: var(--mc-radius-md);
  background: var(--mc-white);
}

.user-actions strong {
  display: block;
  color: var(--mc-ink);
  font-size: 15px;
  margin-bottom: 4px;
}

.user-actions span {
  color: var(--mc-muted);
  font-size: 13px;
  line-height: 1.5;
}

.user-action-buttons {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
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

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 8000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(20, 20, 19, 0.42);
}

.create-dialog {
  width: min(720px, 100%);
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  border-radius: 18px;
  border: 1px solid var(--mc-hairline);
  background: var(--mc-white);
  box-shadow: 0 28px 70px rgba(20, 20, 19, 0.22);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 22px 24px 16px;
  border-bottom: 1px solid var(--mc-hairline-soft);
}

.dialog-eyebrow {
  display: block;
  margin-bottom: 4px;
  color: var(--mc-signal);
  font-size: 12px;
  font-weight: 700;
}

.dialog-header h3 {
  margin: 0;
  color: var(--mc-ink);
  font-size: 22px;
  line-height: 1.25;
}

.dialog-close {
  width: 34px;
  height: 34px;
  border: 1px solid var(--mc-hairline);
  border-radius: 50%;
  background: var(--mc-white);
  color: var(--mc-ink);
  cursor: pointer;
  font-size: 22px;
  line-height: 1;
}

.create-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding: 20px 24px;
}

.repairer-dialog {
  width: min(560px, 100%);
}

.repairer-form {
  display: grid;
  gap: 14px;
  padding: 20px 24px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px 22px;
  border-top: 1px solid var(--mc-hairline-soft);
}

@media (max-width: 1000px) {
  .filter-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .user-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .user-action-buttons,
  .dialog-actions {
    width: 100%;
  }

  .user-action-buttons > *,
  .dialog-actions > * {
    flex: 1;
  }

  .create-form {
    grid-template-columns: 1fr;
  }
}
</style>


