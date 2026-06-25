<script setup lang="ts">
import { ref, onMounted } from 'vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import UiInput from '../../components/ui/UiInput.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiSelect from '../../components/ui/UiSelect.vue'
import UiTextarea from '../../components/ui/UiTextarea.vue'
import { useRouter } from 'vue-router'
import { getCategoriesApi, createRepairApi } from '../../api/repair'
import { uploadFileApi } from '../../api/file'
import { getUserProfileApi, updateUserProfileApi } from '../../api/user'
import { useAuthStore } from '../../stores/auth'
import { useToast } from '../../composables/useToast'
import { useRepairAssistant } from '../../composables/useRepairAssistant'
import { aiClassifyApi } from '../../api/ai'
import type { AiClassifyResponse, RepairCategory } from '../../types/models'

const toast = useToast()

const router = useRouter()
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

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleFileChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (!files || files.length === 0) return

  const file = files[0]

  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/gif'
  if (!isJpgOrPng) {
    toast.error('只能上传 JPG/PNG/GIF 格式的图片')
    return
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
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

const removeImage = (index: number) => {
  form.value.images.splice(index, 1)
}

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
      // 高置信度：自动填入
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
    loading.value = false
    toast.success('报修提交成功！')
    router.push('/student/repair/list')
  } catch (err: any) {
    loading.value = false
    toast.error(err.message || '提交失败，请重试')
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
  <div class="repair-create">
    <UiSectionTitle eyebrow="NEW REQUEST" title="我要报修" />

    <UiCard variant="white" padding="24px 32px" radius="lg" class="form-card" elevated>
      <form @submit.prevent="handleSubmit" class="repair-form">
        <!-- 智能报修助手切换 -->
        <div class="form-section switch-section">
          <div class="mode-switch">
            <button type="button" :class="['mode-btn', { active: repairMode === 'manual' }]" @click="repairMode = 'manual'">
              手动填写
            </button>
            <button type="button" :class="['mode-btn', { active: repairMode === 'ai' }]" @click="repairMode = 'ai'">
              智能报修
            </button>
          </div>
        </div>

        <!-- 智能报修模式 -->
        <div v-if="repairMode === 'ai'" class="form-section assistant-section">
          <div class="assistant-shell">
            <div class="assistant-topbar">
              <div>
                <span class="assistant-kicker">智能引导</span>
                <h4>智能报修助手</h4>
              </div>
              <button type="button" class="assistant-reset" @click="resetAssistant">重新开始</button>
            </div>

            <div class="assistant-progress" :style="{ '--progress': progressPercent + '%' }">
              <div class="assistant-progress-line"></div>
              <div
                v-for="step in assistantSteps"
                :key="step.key"
                :class="['assistant-step', `is-${step.state}`]"
              >
                <span class="step-dot"></span>
                <span class="step-label">{{ step.label }}</span>
              </div>
            </div>

            <div class="assistant-workspace">
              <aside class="assistant-side">
                <div class="side-block location-block">
                  <span class="side-label">当前宿舍</span>
                  <strong>
                    {{ form.dormitoryBuilding || authStore.user?.dormitoryBuilding || '未填写楼栋' }}
                    ·
                    {{ form.roomNo || authStore.user?.roomNo || '未填写房号' }}
                  </strong>
                </div>

                <div class="side-block">
                  <span class="side-label">已采集信息</span>
                  <div class="collected-list">
                    <div
                      v-for="item in collectedItems"
                      :key="item.label"
                      :class="['collected-item', { complete: item.complete }]"
                    >
                      <span>{{ item.label }}</span>
                      <strong>{{ item.value }}</strong>
                    </div>
                  </div>
                </div>
              </aside>

              <section class="assistant-dialog">
                <div class="assistant-dialog-header">
                  <div>
                    <span class="dialog-label">当前问题</span>
                    <h5>{{ activeQuestion.title }}</h5>
                    <p>{{ activeQuestion.description }}</p>
                  </div>
                </div>

                <div class="assistant-chat-window">
                  <div
                    v-for="message in assistantMessages"
                    :key="message.id"
                    :class="['assistant-message', `is-${message.role}`]"
                  >
                    <span class="message-avatar">{{ message.role === 'assistant' ? '助' : '我' }}</span>
                    <div class="message-bubble">
                      <span class="message-role">{{ message.role === 'assistant' ? '报修助手' : '我' }}</span>
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

                <div class="assistant-input-group">
                  <textarea
                    v-model="assistantInput"
                    class="mc-textarea assistant-textarea"
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

                <div v-if="assistantChatError" class="ai-error">
                  {{ assistantChatError }}
                </div>

                <div v-if="assistantPhase === 'review'" class="assistant-review-card">
                  <div class="review-header">
                    <span class="review-title">报修摘要</span>
                    <span class="review-note">可直接修改</span>
                  </div>
                  <textarea
                    v-model="assistantSummary"
                    class="mc-textarea summary-textarea"
                    rows="7"
                  ></textarea>

                  <div v-if="assistantClassifying" class="ai-loading">
                    <span class="ai-loader"></span>
                    <span>正在辅助判断分类...</span>
                  </div>

                  <div v-if="assistantClassifyError" class="ai-error">
                    {{ assistantClassifyError }}
                  </div>

                  <div class="result-actions">
                    <UiButton type="secondary" @click.prevent="reviewAssistantDraft">
                      填入表单检查
                    </UiButton>
                    <UiButton
                      type="primary"
                      :loading="loading"
                      :disabled="assistantClassifying"
                      @click.prevent="submitAssistantDraft"
                    >
                      确认并提交报修
                    </UiButton>
                  </div>
                </div>
              </section>
            </div>
          </div>
        </div>

        <!-- 手动填写模式 -->
        <template v-if="repairMode === 'manual'">
        <div class="form-section">
          <UiInput
            v-model="form.title"
            label="报修标题"
            placeholder="请简要描述报修内容（如：水龙头漏水）"
            :error="errors.title"
          />
        </div>

        <div class="form-grid">
          <UiSelect
            v-model="form.categoryId"
            label="报修分类"
            placeholder="请选择分类"
            :options="categories.map(c => ({ label: c.categoryName, value: c.id }))"
            :error="errors.categoryId"
          />

          <UiInput
            v-model="form.dormitoryBuilding"
            label="宿舍楼栋"
            placeholder="如：12号楼"
          />

          <UiInput
            v-model="form.roomNo"
            label="宿舍房号"
            placeholder="如：302"
          />
        </div>

        <div class="form-section">
          <UiTextarea
            v-model="form.description"
            label="详细描述"
            placeholder="请详细描述报修问题的具体情况..."
            :error="errors.description"
          />
        </div>

        <!-- Image Upload -->
        <div class="form-section">
          <label class="mc-label">图片附件（可选）</label>
          <div class="upload-container">
            <div
              v-for="(img, index) in form.images"
              :key="index"
              class="image-preview"
            >
              <img :src="img" alt="Preview" />
              <div class="image-remove" @click="removeImage(index)">&times;</div>
            </div>

            <div v-if="form.images.length < 5" class="upload-btn" @click="triggerUpload">
              <span v-if="!uploading" class="plus-icon">+</span>
              <span v-else class="upload-loader"></span>
              <span class="upload-text">{{ uploading ? '上传中...' : '上传图片' }}</span>
            </div>
            <input
              type="file"
              ref="fileInput"
              style="display: none"
              accept="image/*"
              @change="handleFileChange"
            />
          </div>
          <p class="upload-tip">支持 jpg/png/gif，单张不超过 10MB，最多 5 张</p>
        </div>

        <div class="form-section">
          <UiButton
            type="secondary"
            :disabled="aiLoading || !form.title || !form.description"
            @click.prevent="handleClassify"
          >
            {{ aiLoading ? 'AI 分析中...' : 'AI 智能分析' }}
          </UiButton>

          <div v-if="aiLoading" class="ai-loading">
            <span class="ai-loader"></span>
            <span>AI 正在分析报修内容...</span>
          </div>

          <div v-if="aiResult" class="ai-result-card">
            <div class="ai-result-header">AI 分析结果</div>
            <div class="ai-result-row">
              <span class="ai-label">建议分类</span>
              <span class="ai-value">{{ aiResult.category }}</span>
            </div>
            <div class="ai-result-row">
              <span class="ai-label">判断依据</span>
              <span class="ai-value ai-reason">{{ aiResult.reason }}</span>
            </div>
            <div v-if="aiResult.suggestion" class="ai-result-row ai-result-suggestion">
              <span class="ai-label">处理建议</span>
              <span class="ai-value">{{ aiResult.suggestion }}</span>
            </div>
          </div>

          <div v-if="aiError" class="ai-error">
            {{ aiError }}
          </div>
        </div>

        <div class="form-actions">
          <UiButton type="secondary" @click.prevent="$router.back()" :disabled="loading">取消</UiButton>
          <UiButton type="primary" :loading="loading">提交报修单</UiButton>
        </div>
      </template>
      </form>
    </UiCard>
  </div>
</template>

<style scoped>
.repair-create {
  max-width: 900px;
  margin: 0 auto;
}

.form-card {
  border: 1px solid var(--mc-hairline);
}

.repair-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.mc-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--mc-muted);
  margin-left: 4px;
  margin-bottom: 4px;
  display: block;
}

.mc-error-text {
  font-size: 12px;
  color: var(--mc-error);
  margin-top: 8px;
  margin-left: 4px;
}

.upload-container {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 12px;
}

.image-preview {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: var(--mc-radius-md);
  overflow: hidden;
  border: 1px solid var(--mc-hairline-soft);
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  background-color: var(--mc-surface-dark);
  color: var(--mc-on-dark);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  opacity: 0.8;
  transition: opacity var(--mc-transition);
}

.image-remove:hover {
  opacity: 1;
}

.upload-btn {
  width: 80px;
  height: 80px;
  border: 1.5px solid var(--mc-hairline);
  border-radius: var(--mc-radius-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--mc-transition);
  background-color: var(--mc-canvas);
}

.upload-btn:hover {
  border-color: var(--mc-ink);
  background-color: var(--mc-white);
  box-shadow: 0 0 0 3px var(--mc-hairline-soft);
  transform: scale(0.96);
}

.plus-icon {
  font-size: 24px;
  color: var(--mc-muted);
}

.upload-loader {
  width: 20px;
  height: 20px;
  border: 2px solid var(--mc-ink);
  border-bottom-color: transparent;
  border-radius: 50%;
  animation: rotation 1s linear infinite;
  margin-bottom: 4px;
}

.upload-text {
  font-size: 12px;
  color: var(--mc-muted);
  font-weight: 500;
}

.upload-tip {
  font-size: 12px;
  color: var(--mc-muted-soft);
  margin-top: 12px;
}

.ai-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 14px;
  color: var(--mc-muted);
}

.ai-loader {
  width: 16px;
  height: 16px;
  border: 2px solid var(--mc-hairline);
  border-top-color: var(--mc-ink);
  border-radius: 50%;
  animation: ai-spin 0.8s linear infinite;
}

@keyframes ai-spin {
  to { transform: rotate(360deg); }
}

.ai-result-card {
  margin-top: 12px;
  padding: 16px;
  border-radius: var(--mc-radius-md);
  border: 1px solid var(--mc-hairline);
  background: var(--mc-canvas);
  animation: ai-fade-in 0.3s ease;
}

@keyframes ai-fade-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.ai-result-header {
  font-size: 13px;
  font-weight: 600;
  color: var(--mc-ink);
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--mc-hairline-soft);
}

.ai-result-row {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 14px;
}

.ai-label {
  color: var(--mc-muted);
  min-width: 70px;
  flex-shrink: 0;
}

.ai-value {
  color: var(--mc-ink);
  font-weight: 500;
}

.ai-reason {
  font-weight: 400;
  color: var(--mc-muted);
}

.ai-error {
  margin-top: 12px;
  padding: 8px 12px;
  border-radius: var(--mc-radius-sm);
  background: var(--mc-error-bg, #fef2f2);
  color: var(--mc-error);
  font-size: 13px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 8px;
  border-top: 1px solid var(--mc-hairline-soft);
  padding-top: 16px;
}

/* Guided repair mode styles */
.switch-section { margin-bottom: 16px; }
.mode-switch { display: flex; gap: 0; border: 1px solid var(--mc-hairline); border-radius: var(--mc-radius-md); overflow: hidden; }
.mode-btn { flex: 1; padding: 10px 16px; border: none; background: var(--mc-white); font-size: 14px; cursor: pointer; transition: all 0.2s; color: var(--mc-muted); }
.mode-btn.active { background: var(--mc-ink); color: var(--mc-on-dark); font-weight: 600; }
.mode-btn:not(.active):hover { background: var(--mc-canvas); }

.assistant-section {
  border-radius: 22px;
  background: linear-gradient(180deg, var(--mc-lifted) 0%, var(--mc-white) 100%);
  border: 1px solid var(--mc-hairline);
  overflow: hidden;
}

.assistant-shell {
  padding: 20px;
}

.assistant-topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.assistant-kicker {
  display: block;
  margin-bottom: 5px;
  color: var(--mc-signal);
  font-size: 12px;
  font-weight: 700;
}

.assistant-topbar h4 {
  margin: 0;
  color: var(--mc-ink);
  font-size: 22px;
  line-height: 1.2;
}

.assistant-reset {
  border: 1px solid var(--mc-hairline);
  border-radius: var(--mc-radius-sm);
  background: var(--mc-white);
  color: var(--mc-muted);
  font-size: 13px;
  padding: 8px 12px;
  cursor: pointer;
  transition: all var(--mc-transition);
}

.assistant-reset:hover {
  color: var(--mc-ink);
  border-color: var(--mc-ink);
}

.assistant-progress {
  position: relative;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-bottom: 18px;
  padding: 6px 0 2px;
}

.assistant-progress-line {
  position: absolute;
  top: 15px;
  left: 8px;
  right: 8px;
  height: 2px;
  background: linear-gradient(90deg, var(--mc-ink) var(--progress), var(--mc-hairline) var(--progress));
}

.assistant-step {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--mc-muted-soft);
  font-size: 12px;
  text-align: center;
}

.assistant-step.is-current,
.assistant-step.is-done {
  color: var(--mc-ink);
}

.step-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid var(--mc-hairline);
  background: var(--mc-white);
}

.assistant-step.is-current .step-dot,
.assistant-step.is-done .step-dot {
  background: var(--mc-ink);
  border-color: var(--mc-ink);
}

.assistant-workspace {
  display: grid;
  grid-template-columns: minmax(210px, 0.38fr) minmax(0, 1fr);
  gap: 16px;
}

.assistant-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-block {
  padding: 14px;
  border-radius: 14px;
  background: var(--mc-canvas);
  border: 1px solid var(--mc-hairline-soft);
}

.side-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--mc-muted);
  font-weight: 600;
}

.location-block strong {
  display: block;
  color: var(--mc-ink);
  font-size: 16px;
  line-height: 1.4;
}

.collected-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.collected-item {
  padding: 9px 10px;
  border-radius: 10px;
  background: var(--mc-white);
  border: 1px solid var(--mc-hairline-soft);
}

.collected-item span {
  display: block;
  color: var(--mc-muted);
  font-size: 12px;
  margin-bottom: 3px;
}

.collected-item strong {
  display: block;
  color: var(--mc-muted-soft);
  font-size: 13px;
  line-height: 1.45;
  font-weight: 600;
}

.collected-item.complete strong {
  color: var(--mc-ink);
}

.assistant-dialog {
  min-width: 0;
  border-radius: 18px;
  border: 1px solid var(--mc-hairline);
  background: var(--mc-white);
  box-shadow: 0 18px 40px rgba(20, 20, 19, 0.08);
  overflow: hidden;
}

.assistant-dialog-header {
  padding: 16px 18px;
  border-bottom: 1px solid var(--mc-hairline-soft);
  background: var(--mc-canvas);
}

.dialog-label {
  display: block;
  margin-bottom: 4px;
  color: var(--mc-signal);
  font-size: 12px;
  font-weight: 700;
}

.assistant-dialog-header h5 {
  margin: 0;
  color: var(--mc-ink);
  font-size: 18px;
}

.assistant-dialog-header p {
  margin: 5px 0 0;
  color: var(--mc-muted);
  font-size: 13px;
  line-height: 1.5;
}

.assistant-chat-window {
  min-height: 230px;
  max-height: 360px;
  overflow-y: auto;
  padding: 18px;
  background:
    linear-gradient(180deg, rgba(243, 240, 238, 0.7) 0%, rgba(255, 255, 255, 0.7) 100%);
}

.assistant-message {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.assistant-message.is-student {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--mc-ink);
  color: var(--mc-white);
  font-size: 12px;
  font-weight: 700;
}

.assistant-message.is-student .message-avatar {
  background: var(--mc-signal);
}

.message-bubble {
  max-width: min(620px, 82%);
  padding: 11px 13px;
  border-radius: 14px;
  background: var(--mc-white);
  border: 1px solid var(--mc-hairline);
  color: var(--mc-ink);
  box-shadow: 0 8px 18px rgba(20, 20, 19, 0.05);
}

.assistant-message.is-student .message-bubble {
  background: var(--mc-signal);
  color: var(--mc-white);
  border-color: var(--mc-signal);
}

.assistant-message.is-student .message-bubble span,
.assistant-message.is-student .message-bubble p {
  color: var(--mc-white);
}

.message-role {
  display: block;
  font-size: 12px;
  opacity: 0.72;
  font-weight: 700;
  margin-bottom: 4px;
}

.message-bubble p {
  margin: 0;
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.65;
}

.assistant-choice-panel {
  padding: 14px 18px 4px;
  border-top: 1px solid var(--mc-hairline-soft);
}

.choice-heading {
  display: block;
  margin-bottom: 10px;
  color: var(--mc-muted);
  font-size: 12px;
  font-weight: 700;
}

.assistant-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 8px;
}

.assistant-option {
  min-height: 54px;
  border: 1px solid var(--mc-hairline);
  background: var(--mc-white);
  color: var(--mc-ink);
  border-radius: 12px;
  padding: 9px 11px;
  text-align: left;
  cursor: pointer;
  transition: transform var(--mc-transition), border-color var(--mc-transition), background var(--mc-transition);
}

.assistant-option:hover {
  border-color: var(--mc-ink);
  background: var(--mc-canvas);
  transform: translateY(-1px);
}

.assistant-option:disabled {
  opacity: 0.56;
  cursor: wait;
  transform: none;
}

.assistant-option span {
  display: block;
  font-size: 13px;
  font-weight: 700;
}

.assistant-option small {
  display: block;
  margin-top: 3px;
  color: var(--mc-muted);
  font-size: 12px;
  line-height: 1.35;
}

.assistant-input-group {
  display: grid;
  grid-template-columns: 1fr 84px;
  gap: 10px;
  align-items: stretch;
  padding: 14px 18px 18px;
}

.assistant-textarea,
.summary-textarea {
  width: 100%;
  border-radius: 14px;
  resize: vertical;
}

.assistant-textarea {
  min-height: 54px;
}

.assistant-send {
  border: 1.5px solid var(--mc-ink);
  border-radius: 16px;
  background: var(--mc-ink);
  color: var(--mc-on-dark);
  font-weight: 600;
  cursor: pointer;
}

.assistant-send:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.assistant-dialog > .ai-error {
  margin: 0 18px 12px;
}

.assistant-review-card {
  margin: 0 18px 18px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid rgba(207, 69, 0, 0.22);
  background: #fffaf6;
  animation: ai-fade-in 0.3s ease;
}

.review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.review-title {
  font-weight: 600;
  color: var(--mc-ink);
}

.review-note {
  font-size: 12px;
  color: var(--mc-muted);
}

.result-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 14px;
}
</style>


