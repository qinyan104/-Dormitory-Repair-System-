<script setup lang="ts">
import { ref, onMounted } from 'vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiInput from '../../components/ui/UiInput.vue'
import UiSlideOver from '../../components/ui/UiSlideOver.vue'
import { getCategoryPageApi, createCategoryApi, updateCategoryApi, deleteCategoryApi, updateCategoryStatusApi } from '../../api/repair'
import { getPageRecords } from '../../utils/page'
import { useToast } from '../../composables/useToast'
import { useConfirm } from '../../composables/useConfirm'

const toast = useToast()
const { confirm } = useConfirm()

const loading = ref(false)
const categories = ref<any[]>([])
const total = ref(0)

const query = ref({
  pageNum: 1,
  pageSize: 10
})

const fetchData = async () => {
  loading.value = true
  try {
    const data: any = await getCategoryPageApi(query.value)
    categories.value = getPageRecords(data)
    total.value = data.total || 0
  } catch (err) {
    console.error('Failed to fetch categories:', err)
  } finally {
    loading.value = false
  }
}

const showModal = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const form = ref({
  id: undefined as number | undefined,
  categoryName: '',
  sortNum: 1,
  status: 1
})

const openAdd = () => {
  isEdit.value = false
  form.value = {
    id: undefined,
    categoryName: '',
    sortNum: total.value + 1,
    status: 1
  }
  showModal.value = true
}

const openEdit = (cat: any) => {
  isEdit.value = true
  form.value = { ...cat }
  showModal.value = true
}

const handleSubmit = async () => {
  if (!form.value.categoryName) {
    toast.error('请输入分类名称')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value && form.value.id) {
      await updateCategoryApi(form.value.id, form.value)
    } else {
      await createCategoryApi(form.value)
    }
    toast.success('操作成功')
    showModal.value = false
    fetchData()
  } catch (err: any) {
    toast.error(err.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (cat: any) => {
  const newStatus = cat.status === 1 ? 0 : 1
  try {
    await updateCategoryStatusApi(cat.id, newStatus)
    fetchData()
  } catch (err: any) {
    toast.error(err.message || '更新状态失败')
  }
}

const handleDelete = async (id: number) => {
  if (!await confirm('确定要删除该分类吗？删除分类可能影响历史工单显示，建议优先使用“停用”功能。')) return
  try {
    await deleteCategoryApi(id)
    toast.success('删除成功')
    fetchData()
  } catch (err: any) {
    toast.error(err.message || '删除失败，该分类可能已被工单引用')
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="category-manage-view">
    <div class="view-header">
      <UiSectionTitle eyebrow="基础设置" title="报修分类设置" />
      <UiButton type="primary" @click="openAdd">+ 新增分类</UiButton>
    </div>

    <UiCard padding="16px 20px" class="info-banner">
      <p class="info-text">提示：停用分类后，学生发起新报修时将无法选择该项，但不影响历史工单正常显示。</p>
    </UiCard>

    <UiCard padding="0" class="table-card">
      <div class="table-wrapper">
        <table class="mc-table">
          <thead>
            <tr>
              <th width="60">ID</th>
              <th>分类名称</th>
              <th width="100">排序号</th>
              <th width="100">当前状态</th>
              <th width="160">创建时间</th>
              <th width="160" class="action-col">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="cat in categories" :key="cat.id">
              <td>{{ cat.id }}</td>
              <td><span class="cat-name-text">{{ cat.categoryName }}</span></td>
              <td><span class="sort-badge">{{ cat.sortNum }}</span></td>
              <td>
                <span :class="['status-pill', cat.status === 1 ? 'is-enabled' : 'is-disabled']">
                  {{ cat.status === 1 ? '启用中' : '已停用' }}
                </span>
              </td>
              <td class="time-cell">{{ cat.createTime || '-' }}</td>
              <td class="action-col">
                <div class="action-btns">
                  <button class="text-btn" @click="handleToggleStatus(cat)">
                    {{ cat.status === 1 ? '停用' : '启用' }}
                  </button>
                  <button class="text-btn" @click="openEdit(cat)">编辑</button>
                  <button class="text-btn danger" @click="handleDelete(cat.id)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="categories.length === 0 && !loading">
              <td colspan="6" class="empty-row">暂无分类数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <span>共 {{ total }} 条</span>
        <div class="page-btns">
          <UiButton type="secondary" :disabled="query.pageNum <= 1" @click="query.pageNum--; fetchData()">上一页</UiButton>
          <span class="current">{{ query.pageNum }}</span>
          <UiButton type="secondary" :disabled="query.pageNum * query.pageSize >= total" @click="query.pageNum++; fetchData()">下一页</UiButton>
        </div>
      </div>
    </UiCard>

    <UiSlideOver :visible="showModal" :title="isEdit ? '编辑分类' : '新增报修分类'" max-width="460px" @close="showModal = false">
      <form @submit.prevent="handleSubmit" class="modal-form">
        <UiInput
          v-model="form.categoryName"
          label="分类名称"
          placeholder="如：水电维修、木工维修"
        />

        <UiInput
          v-model.number="form.sortNum"
          type="number"
          label="排序号"
          placeholder="数字越小越靠前"
        />

        <div class="input-group">
          <label class="mc-label">启用状态</label>
          <div class="toggle-group">
            <label class="radio-label">
              <input type="radio" v-model="form.status" :value="1" /> 启用
            </label>
            <label class="radio-label">
              <input type="radio" v-model="form.status" :value="0" /> 停用
            </label>
          </div>
        </div>

        <div class="form-footer">
          <UiButton type="secondary" @click="showModal = false">取消</UiButton>
          <UiButton type="primary" :loading="submitting">保存设置</UiButton>
        </div>
      </form>
    </UiSlideOver>
  </div>
</template>

<style scoped>
.category-manage-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.info-banner {
  background-color: var(--mc-white);
  border-left: 3px solid var(--mc-signal);
}

.info-text {
  font-size: 13px;
  color: var(--mc-muted);
}

.table-card {
  overflow: hidden;
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

.cat-name-text {
  font-weight: 500;
  color: var(--mc-ink);
}

.sort-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background-color: var(--mc-canvas);
  color: var(--mc-ink);
  border-radius: 4px;
  font-weight: 500;
  font-size: 12px;
}

.status-pill {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: var(--mc-radius-pill);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.is-enabled {
  background-color: rgba(0, 0, 0, 0.04);
  color: var(--mc-charcoal);
}

.is-disabled {
  background-color: rgba(235, 0, 27, 0.1);
  color: var(--mc-signal);
}

.time-cell {
  font-size: 12px;
  color: var(--mc-muted);
}

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

.text-btn:hover {
  text-decoration: underline;
}

.text-btn.danger {
  color: var(--mc-error);
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-group {
  display: flex;
  flex-direction: column;
}

.mc-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--mc-muted);
  display: block;
  margin-bottom: 6px;
}

.toggle-group {
  display: flex;
  gap: 20px;
  padding: 6px 0;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
}

.form-footer {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.form-footer button {
  flex: 1;
}

.empty-row {
  text-align: center;
  padding: 48px;
  color: var(--mc-muted);
  font-size: 13px;
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
</style>


