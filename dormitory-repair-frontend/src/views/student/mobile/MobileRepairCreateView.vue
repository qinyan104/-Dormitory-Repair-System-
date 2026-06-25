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
import { getUserProfileApi, updateUserProfileApi } from '../../../api/user'
import { aiClassifyApi } from '../../../api/ai'
import { useRepairAssistant } from '../../../composables/useRepairAssistant'
import type { AiClassifyResponse, RepairCategory } from '../../../types/models'

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
const categories = ref<RepairCategory[]>([])
const errors = ref<Record<string, string>>({})
const fileInput = ref<HTMLInputElement | null>(null)

const aiLoading = ref(false)
const aiResult = ref<AiClassifyResponse | null>(null)
const aiError = ref('')
const repairMode = ref<'manual' | 'ai'>('manual')

const {
  phase: assistantPhase,
  messages: assistantMessages,
  choices: assistantChoices,
  input: assistantInput,
  summary: assistantSummary,
  inputPlaceholder: assistantInputPlaceholder,
  classificationLoading: assistantClassifying,
  classificationError: assistantClassifyError,
  assistantSteps,
  activeQuestion,
  choiceHeading,
  progressPercent,
  collectedItems,
  chatLoading: assistantChatLoading,
  chatError: assistantChatError,
  sendInput: sendAssistantInput,
  choose: chooseAssistantOption,
  applySummaryToForm,
  reset: resetAssistant
} = useRepairAssistant({
  form,
  categories,
  getUser: () => authStore.user,
  setAiResult: (result) => { aiResult.value = result }
})

const fetchCategories = async () => {
  try {
    const data: any = await getCategoriesApi()
    categories.value = data
  } catch (err: any) {
    console.error('Failed to fetch categories:', err)
  }
}

const refreshStudentProfile = async () => {
  try {
    const latestUser: any = await getUserProfileApi()
    if (!latestUser) return
    authStore.setUser({ ...authStore.user, ...latestUser })
    if (!form.value.dormitoryBuilding && latestUser.dormitoryBuilding) {
      form.value.dormitoryBuilding = latestUser.dormitoryBuilding
    }
    if (!form.value.roomNo && latestUser.roomNo) {
      form.value.roomNo = latestUser.roomNo
    }
  } catch (err) {
    console.error('Failed to refresh student profile:', err)
  }
}

onMounted(async () => {
  await Promise.all([fetchCategories(), refreshStudentProfile()])
  resetAssistant()
})

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
        form.value.categoryId = String(data.categoryId)
        toast.success('AI 已自动填写分类和紧急度')
      }
    } else {
      aiError.value = 'AI 暂未返回结果，请稍后再试'
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

const reviewAssistantDraft = () => {
  applySummaryToForm()
  repairMode.value = 'manual'
}

const submitAssistantDraft = async () => {
  applySummaryToForm()
  if (!form.value.categoryId) {
    repairMode.value = 'manual'
    toast.info('请先确认报修分类')
    return
  }
  await handleSubmit()
}
</script>

<template>
  <div class="mobile-repair-create">
    <h2 class="page-title">我要报修</h2>

    <form @submit.prevent="handleSubmit" class="repair-form">
      <div class="mode-switch">
        <button type="button" :class="['mode-btn', { active: repairMode === 'manual' }]" @click="repairMode = 'manual'">
          手动填写
        </button>
        <button type="button" :class="['mode-btn', { active: repairMode === 'ai' }]" @click="repairMode = 'ai'">
          智能报修
        </button>
      </div>

      <div v-if="repairMode === 'ai'" class="assistant-section">
        <div class="assistant-card">
          <div class="assistant-header">
            <div>
              <span class="assistant-kicker">智能引导</span>
              <h3>智能报修助手</h3>
            </div>
            <button type="button" class="assistant-reset" @click="resetAssistant">重来</button>
          </div>

          <div class="assistant-progress" :style="{ '--progress': progressPercent + '%' }">
            <span class="progress-track"></span>
            <span class="progress-fill"></span>
          </div>

          <div class="assistant-steps">
            <span
              v-for="step in assistantSteps"
              :key="step.key"
              :class="['assistant-step', `is-${step.state}`]"
            >
              {{ step.label }}
            </span>
          </div>
        </div>

        <div class="student-location-card">
          <span>当前宿舍</span>
          <strong>
            {{ form.dormitoryBuilding || authStore.user?.dormitoryBuilding || '未填写楼栋' }}
            ·
            {{ form.roomNo || authStore.user?.roomNo || '未填写房号' }}
          </strong>
        </div>

        <div class="collected-strip">
          <div
            v-for="item in collectedItems"
            :key="item.label"
            :class="['collected-chip', { complete: item.complete }]"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>

        <section class="assistant-dialog">
          <div class="assistant-dialog-header">
            <span>当前问题</span>
            <strong>{{ activeQuestion.title }}</strong>
            <p>{{ activeQuestion.description }}</p>
          </div>

          <div class="assistant-chat">
            <div
              v-for="message in assistantMessages"
              :key="message.id"
              :class="['assistant-message', `is-${message.role}`]"
            >
              <span class="message-avatar">{{ message.role === 'assistant' ? '助' : '我' }}</span>
              <div class="message-bubble">
                <span>{{ message.role === 'assistant' ? '报修助手' : '我' }}</span>
                <p>{{ message.text }}</p>
              </div>
            </div>
          </div>

          <div v-if="assistantChoices.length" class="assistant-choice-panel">
            <span class="choice-heading">{{ choiceHeading }}</span>
            <div class="assistant-options">
              <button
                v-for="choice in assistantChoices"
                :key="choice.label"
                type="button"
                class="assistant-option"
                :disabled="assistantChatLoading"
                @click="chooseAssistantOption(choice)"
              >
                <span>{{ choice.label }}</span>
                <small v-if="choice.hint">{{ choice.hint }}</small>
              </button>
            </div>
          </div>

          <div class="assistant-input-row">
            <textarea
              v-model="assistantInput"
              class="assistant-textarea"
              :placeholder="assistantInputPlaceholder"
              rows="2"
              @keydown.enter.exact.prevent="sendAssistantInput"
            ></textarea>
            <button
              type="button"
              class="assistant-send"
              :disabled="!assistantInput.trim() || assistantClassifying || assistantChatLoading"
              @click="sendAssistantInput"
            >
              {{ assistantChatLoading ? '思考中' : '发送' }}
            </button>
          </div>

          <div v-if="assistantChatError" class="ai-error assistant-chat-error">
            {{ assistantChatError }}
          </div>
        </section>

        <div v-if="assistantPhase === 'review'" class="assistant-review">
          <div class="review-header">
            <strong>报修摘要</strong>
            <span>可直接修改</span>
          </div>
          <textarea v-model="assistantSummary" class="summary-textarea" rows="7"></textarea>

          <div v-if="assistantClassifying" class="ai-loading">
            <span class="ai-spinner"></span>
            <span>正在辅助判断分类...</span>
          </div>

          <div v-if="assistantClassifyError" class="ai-error">{{ assistantClassifyError }}</div>

          <div class="assistant-actions">
            <UiButton type="secondary" @click.prevent="reviewAssistantDraft">填入表单检查</UiButton>
            <UiButton
              type="primary"
              :loading="loading"
              :disabled="assistantClassifying"
              @click.prevent="submitAssistantDraft"
            >
              确认提交
            </UiButton>
          </div>
        </div>
      </div>

      <template v-if="repairMode === 'manual'">
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
          @click.prevent="handleClassify"
        >
          {{ aiLoading ? 'AI 分析中...' : 'AI 智能分析' }}
        </UiButton>

        <div v-if="aiLoading" class="ai-loading">
          <span class="ai-spinner"></span>
          <span>正在分析...</span>
        </div>

        <div v-if="aiResult" class="ai-result">
          <div class="ai-result-header">AI 分析结果</div>
          <div class="ai-row">
            <span class="ai-label">建议分类</span>
            <span class="ai-value">{{ aiResult.category }}</span>
          </div>
          <div class="ai-row">
            <span class="ai-label">依据</span>
            <span class="ai-value ai-reason">{{ aiResult.reason }}</span>
          </div>
          <div v-if="aiResult.suggestion" class="ai-row ai-suggestion">
            <span class="ai-label">处理建议</span>
            <span class="ai-value">{{ aiResult.suggestion }}</span>
          </div>
        </div>

        <div v-if="aiError" class="ai-error">{{ aiError }}</div>
      </div>

      <!-- Sticky Submit -->
      <div class="form-actions">
        <UiButton type="secondary" @click.prevent="router.back()" :disabled="loading">取消</UiButton>
        <UiButton type="primary" :loading="loading">提交报修</UiButton>
      </div>
      </template>
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

.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  overflow: hidden;
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  border-radius: var(--mc-radius-sm, 6px);
}

.mode-btn {
  min-height: 42px;
  border: none;
  background: var(--mc-white, #fff);
  color: var(--mc-muted, #696969);
  font-size: 14px;
  font-weight: 600;
}

.mode-btn.active {
  background: var(--mc-ink, #141413);
  color: var(--mc-white, #fff);
}

.assistant-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.assistant-card,
.assistant-dialog,
.assistant-review {
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  border-radius: 14px;
  background: var(--mc-white, #fff);
}

.assistant-card {
  padding: 14px;
  background: linear-gradient(180deg, var(--mc-lifted, #FCFBFA) 0%, var(--mc-white, #fff) 100%);
}

.assistant-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.assistant-kicker {
  display: block;
  margin-bottom: 4px;
  color: var(--mc-signal, #CF4500);
  font-size: 12px;
  font-weight: 700;
}

.assistant-header h3 {
  margin: 0;
  color: var(--mc-ink, #141413);
  font-size: 18px;
  line-height: 1.2;
}

.assistant-reset {
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  border-radius: var(--mc-radius-sm, 6px);
  background: var(--mc-white, #fff);
  color: var(--mc-muted, #696969);
  font-size: 13px;
  padding: 7px 10px;
}

.assistant-progress {
  position: relative;
  height: 4px;
  margin-top: 14px;
  border-radius: 999px;
  overflow: hidden;
}

.progress-track,
.progress-fill {
  position: absolute;
  inset: 0;
}

.progress-track {
  background: var(--mc-hairline, rgba(20,20,19,0.1));
}

.progress-fill {
  width: var(--progress);
  background: var(--mc-ink, #141413);
}

.assistant-steps {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 4px;
  margin-top: 9px;
}

.assistant-step {
  font-size: 10px;
  color: var(--mc-muted-soft, #A3A3A3);
  text-align: center;
  line-height: 1.2;
}

.assistant-step.is-current,
.assistant-step.is-done {
  color: var(--mc-ink, #141413);
  font-weight: 700;
}

.student-location-card {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  border-radius: var(--mc-radius-sm, 6px);
  color: var(--mc-muted, #696969);
  font-size: 13px;
}

.collected-strip {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.collected-chip {
  padding: 9px;
  border-radius: var(--mc-radius-sm, 6px);
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  background: var(--mc-canvas, #F3F0EE);
}

.collected-chip span {
  display: block;
  color: var(--mc-muted, #696969);
  font-size: 11px;
  margin-bottom: 2px;
}

.collected-chip strong {
  display: block;
  color: var(--mc-muted-soft, #A3A3A3);
  font-size: 12px;
  line-height: 1.35;
}

.collected-chip.complete strong {
  color: var(--mc-ink, #141413);
}

.student-location-card strong {
  color: var(--mc-ink, #141413);
  font-weight: 700;
  text-align: right;
}

.assistant-dialog {
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(20, 20, 19, 0.08);
}

.assistant-dialog-header {
  padding: 12px;
  background: var(--mc-canvas, #F3F0EE);
  border-bottom: 1px solid var(--mc-hairline-soft, rgba(20,20,19,0.05));
}

.assistant-dialog-header span {
  display: block;
  margin-bottom: 3px;
  color: var(--mc-signal, #CF4500);
  font-size: 11px;
  font-weight: 700;
}

.assistant-dialog-header strong {
  display: block;
  color: var(--mc-ink, #141413);
  font-size: 16px;
}

.assistant-dialog-header p {
  margin: 4px 0 0;
  color: var(--mc-muted, #696969);
  font-size: 12px;
  line-height: 1.45;
}

.assistant-chat {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 310px;
  overflow-y: auto;
  padding: 12px;
  background: linear-gradient(180deg, rgba(243,240,238,0.74), rgba(255,255,255,0.9));
}

.assistant-message {
  display: flex;
  gap: 8px;
}

.assistant-message.is-student {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 28px;
  height: 28px;
  border-radius: 9px;
  background: var(--mc-ink, #141413);
  color: var(--mc-white, #fff);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 700;
}

.assistant-message.is-student .message-avatar {
  background: var(--mc-signal, #CF4500);
}

.message-bubble {
  max-width: calc(100% - 36px);
  padding: 9px 11px;
  border-radius: 12px;
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  background: var(--mc-white, #fff);
  color: var(--mc-ink, #141413);
}

.assistant-message.is-student .message-bubble {
  background: var(--mc-signal, #CF4500);
  color: var(--mc-white, #fff);
  border-color: var(--mc-signal, #CF4500);
}

.assistant-message.is-student .message-bubble span,
.assistant-message.is-student .message-bubble p {
  color: var(--mc-white, #fff);
}

.message-bubble span {
  display: block;
  margin-bottom: 2px;
  font-size: 11px;
  opacity: 0.72;
  font-weight: 700;
}

.message-bubble p {
  margin: 0;
  white-space: pre-wrap;
  font-size: 13px;
  line-height: 1.55;
}

.assistant-choice-panel {
  padding: 12px;
  border-top: 1px solid var(--mc-hairline-soft, rgba(20,20,19,0.05));
}

.choice-heading {
  display: block;
  margin-bottom: 8px;
  color: var(--mc-muted, #696969);
  font-size: 12px;
  font-weight: 700;
}

.assistant-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.assistant-option {
  min-height: 48px;
  padding: 8px 9px;
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  border-radius: 10px;
  background: var(--mc-white, #fff);
  color: var(--mc-ink, #141413);
  text-align: left;
}

.assistant-option span {
  display: block;
  font-size: 13px;
  font-weight: 700;
}

.assistant-option small {
  display: block;
  margin-top: 2px;
  color: var(--mc-muted, #696969);
  font-size: 11px;
  line-height: 1.25;
}

.assistant-option:active {
  background: var(--mc-canvas, #F3F0EE);
}

.assistant-option:disabled {
  opacity: 0.56;
  cursor: wait;
}

.assistant-input-row {
  display: grid;
  grid-template-columns: 1fr 64px;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid var(--mc-hairline-soft, rgba(20,20,19,0.05));
}

.assistant-textarea,
.summary-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--mc-hairline, rgba(20,20,19,0.1));
  border-radius: var(--mc-radius-sm, 6px);
  color: var(--mc-ink, #141413);
  font-family: var(--mc-font-family, sans-serif);
  font-size: 14px;
  line-height: 1.5;
  resize: vertical;
}

.assistant-send {
  border: 1px solid var(--mc-ink, #141413);
  border-radius: 10px;
  background: var(--mc-ink, #141413);
  color: var(--mc-white, #fff);
  font-weight: 700;
}

.assistant-send:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.assistant-chat-error {
  margin: 0 12px 12px;
}

.assistant-review {
  padding: 12px;
  border-color: rgba(207, 69, 0, 0.22);
  background: #fffaf6;
}

.review-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: var(--mc-ink, #141413);
}

.review-header span {
  color: var(--mc-muted, #696969);
  font-size: 12px;
}

.assistant-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 12px;
}

.assistant-actions :deep(.mc-button) {
  width: 100%;
  min-height: 44px;
  padding-inline: 10px;
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

.ai-result-header {
  margin-bottom: 9px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--mc-hairline-soft, rgba(20,20,19,0.05));
  color: var(--mc-ink, #141413);
  font-size: 13px;
  font-weight: 700;
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

.ai-reason {
  color: var(--mc-muted, #696969);
  font-weight: 400;
  line-height: 1.45;
}

.ai-suggestion {
  padding-top: 4px;
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
