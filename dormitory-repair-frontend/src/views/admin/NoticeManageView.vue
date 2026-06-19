<script setup lang="ts">
import { ref, onMounted } from 'vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiInput from '../../components/ui/UiInput.vue'
import UiSlideOver from '../../components/ui/UiSlideOver.vue'
import UiSelect from '../../components/ui/UiSelect.vue'
import { getNoticeListApi, createNoticeApi, updateNoticeApi, deleteNoticeApi } from '../../api/notice'
import { getPageRecords } from '../../utils/page'
import { useToast } from '../../composables/useToast'
import { useConfirm } from '../../composables/useConfirm'

const toast = useToast()
const { confirm } = useConfirm()

const loading = ref(false)
const notices = ref<any[]>([])
const total = ref(0)

const query = ref({
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

const fetchData = async () => {
  loading.value = true
  try {
    const data: any = await getNoticeListApi(query.value)
    notices.value = getPageRecords(data)
    total.value = data.total || 0
  } catch (err) {
    console.error('Failed to fetch notices:', err)
  } finally {
    loading.value = false
  }
}

const showModal = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const form = ref({
  id: undefined as number | undefined,
  title: '',
  content: '',
  type: 'GENERAL',
  status: 1,
  isTop: 0
})

const typeOptions = [
  { label: '普通公告', value: 'GENERAL' },
  { label: '重要紧急', value: 'IMPORTANT' }
]

const statusOptions = [
  { label: '直接发布', value: 1 },
  { label: '保存草稿', value: 0 }
]

const openAdd = () => {
  isEdit.value = false
  form.value = {
    id: undefined,
    title: '',
    content: '',
    type: 'GENERAL',
    status: 1,
    isTop: 0
  }
  showModal.value = true
}

const openEdit = (notice: any) => {
  isEdit.value = true
  form.value = { ...notice }
  showModal.value = true
}

const handleSubmit = async () => {
  if (!form.value.title || !form.value.content) {
    toast.error('请填写完整标题和内容')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value && form.value.id) {
      await updateNoticeApi(form.value.id, form.value)
    } else {
      await createNoticeApi(form.value)
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

const handleDelete = async (id: number) => {
  if (!await confirm('确定要删除该公告吗？此操作不可撤销。')) return
  try {
    await deleteNoticeApi(id)
    toast.success('删除成功')
    fetchData()
  } catch (err: any) {
    toast.error(err.message || '删除失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="notice-manage-view">
    <div class="view-header">
      <UiSectionTitle eyebrow="公告发布" title="公告发布管理" />
      <UiButton type="primary" @click="openAdd">+ 发布新公告</UiButton>
    </div>

    <UiCard padding="20px" class="filter-card">
      <div class="filter-flex">
        <UiInput
          v-model="query.keyword"
          placeholder="搜索公告标题..."
          class="search-input"
          @keyup.enter="fetchData"
        />
        <UiButton type="secondary" @click="fetchData">查询</UiButton>
      </div>
    </UiCard>

    <UiCard padding="0" class="table-card">
      <div class="table-wrapper">
        <table class="mc-table">
          <thead>
            <tr>
              <th width="60">ID</th>
              <th>标题</th>
              <th width="100">类型</th>
              <th width="80">置顶</th>
              <th width="100" class="status-col">状态</th>
              <th width="160">发布时间</th>
              <th width="120" class="action-col">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in notices" :key="item.id">
              <td>{{ item.id }}</td>
              <td class="title-cell">
                <span v-if="item.isTop" class="top-tag">置顶</span>
                <span class="title-text">{{ item.title }}</span>
              </td>
              <td>
                <span class="type-badge">{{ item.type === 'IMPORTANT' ? '重要' : '普通' }}</span>
              </td>
              <td>{{ item.isTop ? '是' : '否' }}</td>
              <td class="status-col">
                <span :class="['status-dot', item.status === 1 ? 'is-published' : 'is-draft']"></span>
                {{ item.status === 1 ? '已发布' : '草稿' }}
              </td>
              <td class="time-cell">{{ item.publishTime || '-' }}</td>
              <td class="action-col">
                <div class="action-btns">
                  <button class="text-btn" @click="openEdit(item)">编辑</button>
                  <button class="text-btn danger" @click="handleDelete(item.id)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="notices.length === 0 && !loading">
              <td colspan="7" class="empty-row">暂无公告数据</td>
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

    <UiSlideOver :visible="showModal" :title="isEdit ? '编辑公告' : '发布新公告'" max-width="500px" @close="showModal = false">
      <form @submit.prevent="handleSubmit" class="modal-form">
        <UiInput v-model="form.title" label="公告标题" placeholder="输入醒目的标题" />

        <div class="form-row">
          <UiSelect v-model="form.type" label="类型" :options="typeOptions" />
          <UiSelect v-model="form.status" label="状态" :options="statusOptions" />
        </div>

        <div class="input-group">
          <label class="mc-label">是否置顶</label>
          <div class="toggle-group">
            <label class="radio-label">
              <input type="radio" v-model="form.isTop" :value="1" /> 是
            </label>
            <label class="radio-label">
              <input type="radio" v-model="form.isTop" :value="0" /> 否
            </label>
          </div>
        </div>

        <div class="input-group">
          <label class="mc-label">详细内容</label>
          <textarea
            v-model="form.content"
            class="mc-textarea"
            placeholder="在此输入公告正文内容..."
            rows="6"
          ></textarea>
        </div>

        <div class="form-footer">
          <UiButton type="secondary" @click="showModal = false">取消</UiButton>
          <UiButton type="primary" :loading="submitting">确认保存</UiButton>
        </div>
      </form>
    </UiSlideOver>
  </div>
</template>

<style scoped>
.notice-manage-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.filter-flex {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  flex: 1;
  max-width: 360px;
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

.title-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.top-tag {
  background-color: rgba(235, 0, 27, 0.1);
  color: var(--mc-signal);
  font-size: 10px;
  padding: 2px 8px;
  border-radius: var(--mc-radius-pill);
  font-weight: 600;
  flex-shrink: 0;
  text-transform: uppercase;
}

.title-text {
  font-weight: 500;
  color: var(--mc-ink);
}

.type-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  background-color: rgba(0, 0, 0, 0.04);
  border-radius: var(--mc-radius-pill);
  color: var(--mc-charcoal);
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
}

.is-published { background-color: var(--mc-success); }
.is-draft { background-color: var(--mc-muted); }

.time-cell {
  font-size: 12px;
  color: var(--mc-muted);
}

.action-col {
  text-align: right;
}

.status-col {
  white-space: nowrap;
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
.text-btn.danger { color: var(--mc-error); }

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

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.mc-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--mc-muted);
  display: block;
  margin-bottom: 6px;
}

.mc-textarea {
  width: 100%;
  padding: 10px 14px;
  border-radius: var(--mc-radius-md);
  border: 1px solid var(--mc-canvas);
  background-color: var(--mc-white);
  font-family: inherit;
  font-size: 13px;
  outline: none;
}

.mc-textarea:focus {
  border-color: var(--mc-ink);
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

.form-footer button { flex: 1; }

.empty-row {
  text-align: center;
  padding: 48px;
  color: var(--mc-muted);
  font-size: 13px;
}
</style>


