<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import UiInput from '../../../components/ui/UiInput.vue'
import UiSelect from '../../../components/ui/UiSelect.vue'
import UiTextarea from '../../../components/ui/UiTextarea.vue'
import UiButton from '../../../components/ui/UiButton.vue'
import { useAuthStore } from '../../../stores/auth'
import { useToast } from '../../../composables/useToast'
import { getCategoriesApi, createRepairApi } from '../../../api/repair'
import { uploadFileApi } from '../../../api/file'
import { updateUserProfileApi } from '../../../api/user'
import { aiClassifyApi } from '../../../api/ai'
import type { AiClassifyResponse } from '../../../types/models'

const router = useRouter()
const toast = useToast()
const authStore = useAuthStore()

const form = ref({
  title: '',
  categoryId: '',
  dormitoryBuilding: authStore.user?.dormitoryBuilding || '',
  roomNo: authStore.user?.roomNo || '',
  description: '',
  images: [] as string[]
})

const loading = ref(false)
const uploading = ref(false)
const categories = ref<any[]>([])
const errors = ref<Record<string, string>>({})
const fileInput = ref<HTMLInputElement | null>(null)

const aiLoading = ref(false)
const aiResult = ref<AiClassifyResponse | null>(null)
const aiError = ref('')

const fetchCategories = async () => {
  try {
    const data: any = await getCategoriesApi()
    categories.value = data
  } catch (err: any) {
    console.error('Failed to fetch categories:', err)
  }
}

onMounted(() => { fetchCategories() })

const triggerUpload = () => { fileInput.value?.click() }

const handleFileChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (!files || files.length === 0) return
  const file = files[0]

  if (!['image/jpeg', 'image/png', 'image/gif'].includes(file.type)) {
    toast.error('只能上传 JPG/PNG/GIF 格式的图片')
    return
  }
  if (file.size / 1024 / 1024 >= 10) {
    toast.error('图片大小不能超过 10MB')
    return
  }

  uploading.value = true
  try {
    const data: any = await uploadFileApi(file)
    form.value.images.push(data.url)
  } catch (err: any) {
    toast.error(err.message || '图片上传失败')
  } finally {
    uploading.value = false
    target.value = ''
  }
}

const removeImage = (index: number) => { form.value.images.splice(index, 1) }

const handleClassify = async () => {
  if (!form.value.title || !form.value.description) {
    toast.info('请先填写报修标题和描述')
    return
  }
  aiLoading.value = true
  aiError.value = ''
  aiResult.value = null
  try {
    const data: any = await aiClassifyApi({
      title: form.value.title,
      description: form.value.description
    })
    if (data) {
      aiResult.value = data
      if (data.autoApplied) {
        form.value.categoryId = data.categoryId
        toast.success('AI 已自动填写分类')
      }
    } else {
      aiError.value = 'AI 暂未返回结果'
    }
  } catch (err: any) {
    aiError.value = err.message || 'AI 分析暂不可用'
  } finally {
    aiLoading.value = false
  }
}

const handleSubmit = async () => {
  loading.value = true
  errors.value = {}

  if (!form.value.title) errors.value.title = '请输入报修标题'
  if (!form.value.categoryId) errors.value.categoryId = '请选择报修分类'
  if (!form.value.description) errors.value.description = '请填写问题描述'

  if (Object.keys(errors.value).length > 0) {
    loading.value = false
    return
  }

  try {
    const shouldSyncDormitory =
      form.value.dormitoryBuilding !== (authStore.user?.dormitoryBuilding || '') ||
      form.value.roomNo !== (authStore.user?.roomNo || '')

    if (shouldSyncDormitory && authStore.user) {
      const updatedUser: any = await updateUserProfileApi({
        realName: authStore.user.realName,
        phone: authStore.user.phone || '',
        gender: authStore.user.gender,
        dormitoryBuilding: form.value.dormitoryBuilding,
        roomNo: form.value.roomNo,
        avatar: authStore.user.avatar || ''
      })
      authStore.setUser({ ...authStore.user, ...updatedUser })
    }

    await createRepairApi({
      title: form.value.title,
      categoryId: Number(form.value.categoryId),
      content: form.value.description,
      imageUrl: form.value.images.join(','),
      urgency: aiResult.value?.urgency || undefined,
      aiPriorityScore: aiResult.value?.priorityScore,
      aiImpactScope: aiResult.value?.impactScope
    })
    toast.success('报修提交成功！')
    router.push('/student/m/list')
  } catch (err: any) {
    toast.error(err.message || '提交失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mobile-repair-create">
    <h2 class="page-title">我要报修</h2>

    <form @submit.prevent="handleSubmit" class="repair-form">
      <UiInput
        v-model="form.title"
        label="报修标题"
        placeholder="简要描述报修内容"
        :error="errors.title"
      />

      <UiSelect
        v-model="form.categoryId"
        label="报修分类"
        placeholder="请选择分类"
        :options="categories.map(c => ({ label: c.categoryName, value: c.id }))"
        :error="errors.categoryId"
      />

      <div class="form-row">
        <UiInput
          v-model="form.dormitoryBuilding"
          label="楼栋"
          placeholder="如：12号楼"
        />
        <UiInput
          v-model="form.roomNo"
          label="房号"
          placeholder="如：302"
        />
      </div>

      <UiTextarea
        v-model="form.description"
        label="详细描述"
        placeholder="请详细描述报修问题..."
        :error="errors.description"
        :rows="4"
      />

      <!-- Image Upload -->
      <div class="form-field">
        <label class="field-label">图片附件（可选）</label>
        <div class="upload-scroll">
          <div
            v-for="(img, index) in form.images"
            :key="index"
            class="image-thumb"
          >
            <img :src="img" alt="Preview" />
            <div class="thumb-remove" @click="removeImage(index)">&times;</div>
          </div>
          <div v-if="form.images.length < 5" class="upload-btn" @click="triggerUpload">
            <span v-if="!uploading" class="plus-icon">+</span>
            <span v-else class="upload-spinner"></span>
          </div>
          <input type="file" ref="fileInput" style="display: none" accept="image/*" @change="handleFileChange" />
        </div>
        <p class="upload-tip">jpg/png/gif，最大 10MB，最多 5 张</p>
      </div>

      <!-- AI Classify -->
      <div class="form-field">
        <UiButton
          type="secondary"
          :disabled="aiLoading || !form.title || !form.description"
          @click="handleClassify"
        >
          {{ aiLoading ? 'AI 分析中...' : 'AI 智能分析' }}
        </UiButton>

        <div v-if="aiLoading" class="ai-loading">
          <span class="ai-spinner"></span>
          <span>正在分析...</span>
        </div>

        <div v-if="aiResult" class="ai-result">
          <div class="ai-row">
            <span class="ai-label">建议分类</span>
            <span class="ai-value">{{ aiResult.category }}</span>
          </div>
          <div class="ai-row">
            <span class="ai-label">依据</span>
            <span class="ai-value">{{ aiResult.reason }}</span>
          </div>
        </div>

        <div v-if="aiError" class="ai-error">{{ aiError }}</div>
      </div>

      <!-- Sticky Submit -->
      <div class="form-actions">
        <UiButton type="secondary" @click="router.back()" :disabled="loading">取消</UiButton>
        <UiButton type="primary" :loading="loading">提交报修</UiButton>
      </div>
    </form>
  </div>
</template>

<style scoped>
.mobile-repair-create {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-title {
  font-family: var(--mc-font-display, sans-serif);
  font-size: 22px;
  font-weight: 600;
  color: var(--mc-ink, #141413);
  margin: 0;
}

.repair-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: var(--mc-white, #fff);
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  border-radius: var(--mc-radius-md, 16px);
  padding: 20px 16px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--mc-muted, #696969);
  margin-left: 2px;
}

/* Image upload horizontal scroll */
.upload-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 4px;
  scroll-snap-type: x mandatory;
}

.upload-scroll::-webkit-scrollbar {
  display: none;
}

.image-thumb {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: var(--mc-radius-sm, 6px);
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  scroll-snap-align: start;
}

.image-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-remove {
  position: absolute;
  top: 0;
  right: 0;
  width: 28px;
  height: 28px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  /* Expand tap area beyond visual size */
  box-shadow: 0 0 0 8px transparent;
  transition: background 0.15s;
}

.thumb-remove:active {
  background: rgba(0, 0, 0, 0.85);
}

.upload-btn {
  width: 72px;
  height: 72px;
  border: 1.5px dashed var(--mc-taupe, #D1CDC7);
  border-radius: var(--mc-radius-sm, 6px);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  -webkit-tap-highlight-color: transparent;
  transition: border-color 0.15s;
}

.upload-btn:active {
  border-color: var(--mc-ink, #141413);
}

.plus-icon {
  font-size: 28px;
  color: var(--mc-muted, #696969);
  line-height: 1;
}

.upload-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--mc-hairline, rgba(20,20,19,0.1));
  border-top-color: var(--mc-ink, #141413);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.upload-tip {
  font-size: 12px;
  color: var(--mc-muted-soft, #A3A3A3);
  margin: 0;
}

/* AI */
.ai-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--mc-muted, #696969);
}

.ai-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--mc-hairline, rgba(20,20,19,0.1));
  border-top-color: var(--mc-ink, #141413);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.ai-result {
  padding: 12px;
  border-radius: var(--mc-radius-sm, 6px);
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  background: var(--mc-canvas, #F3F0EE);
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.ai-row {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 13px;
}

.ai-row:last-child {
  margin-bottom: 0;
}

.ai-label {
  color: var(--mc-muted, #696969);
  min-width: 56px;
  flex-shrink: 0;
}

.ai-value {
  color: var(--mc-ink, #141413);
  font-weight: 500;
}

.ai-error {
  padding: 8px 10px;
  border-radius: var(--mc-radius-xs, 4px);
  background: var(--mc-error-bg, #FEE2E2);
  color: var(--mc-error, #EB001B);
  font-size: 13px;
}

/* Submit */
.form-actions {
  display: flex;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid var(--mc-hairline-soft, rgba(20,20,19,0.05));
}

.form-actions :deep(.mc-button) {
  flex: 1;
  min-height: 48px;
}
</style>
