<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import UiCard from '../../components/ui/UiCard.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import UiInput from '../../components/ui/UiInput.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiSelect from '../../components/ui/UiSelect.vue'
import UiTextarea from '../../components/ui/UiTextarea.vue'
import { useRouter } from 'vue-router'
import { getCategoriesApi, createRepairApi } from '../../api/repair'
import { uploadFileApi } from '../../api/file'
import { updateUserProfileApi } from '../../api/user'
import { useAuthStore } from '../../stores/auth'
import { useToast } from '../../composables/useToast'
import { aiClassifyApi, aiNaturalRepairApi } from '../../api/ai'
import type { AiClassifyResponse, NaturalRepairResponse } from '../../types/models'

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
const categories = ref<any[]>([])
const errors = ref<Record<string, string>>({})
const fileInput = ref<HTMLInputElement | null>(null)

const aiLoading = ref(false)
const aiResult = ref<AiClassifyResponse | null>(null)
const aiError = ref('')

const repairMode = ref<'manual' | 'ai'>('manual')
const naturalText = ref('')
const nlResult = ref<NaturalRepairResponse | null>(null)
const nlError = ref('')
const nlLoading = ref(false)

const nlSubmitting = ref(false)

const handleNaturalRepair = async () => {
  if (!naturalText.value.trim()) {
    nlError.value = '请输入报修描述'
    return
  }
  nlLoading.value = true
  nlError.value = ''
  nlResult.value = null
  try {
    const result = await aiNaturalRepairApi(naturalText.value)
    if (!result) {
      nlError.value = 'AI 暂未返回结果，请稍后重试'
      return
    }
    nlResult.value = result
  } catch (e: any) {
    nlError.value = e?.message || 'AI 识别失败，请重试'
  } finally {
    nlLoading.value = false
  }
}

const applyNaturalResult = async () => {
  if (!nlResult.value || nlSubmitting.value) return
  nlSubmitting.value = true
  // Auto-fill the form with AI extracted data
  form.value.title = nlResult.value.repairType + (nlResult.value.description ? ' - ' + nlResult.value.description : '')
  form.value.categoryId = String(nlResult.value.categoryId || '')
  form.value.description = nlResult.value.description || naturalText.value
  if (nlResult.value.building) form.value.dormitoryBuilding = nlResult.value.building
  if (nlResult.value.room) form.value.roomNo = nlResult.value.room

  // Set AI result for submit
  aiResult.value = {
    category: nlResult.value.categoryName || '',
    categoryId: nlResult.value.categoryId || 0,
    categoryName: nlResult.value.categoryName || '',
    urgency: nlResult.value.urgencyLevel || '',
    priorityScore: nlResult.value.priorityScore || 5,
    impactScope: 5,
    confidence: nlResult.value.confidence || 0.5,
    reason: nlResult.value.reason || '',
    suggestion: '',
    autoApplied: true
  }

  // Switch to manual and submit
  repairMode.value = 'manual'
  await nextTick()
  handleSubmit()
  nlSubmitting.value = false
}

const fetchCategories = async () => {
  try {
    const data: any = await getCategoriesApi()
    categories.value = data
  } catch (err: any) {
    console.error('Failed to fetch categories:', err)
  }
}

onMounted(() => {
  fetchCategories()
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
        form.value.categoryId = data.categoryId
        toast.success('✅ AI 已自动填写分类和紧急度')
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
              <span class="mode-icon">✏️</span>
              手动填写
            </button>
            <button type="button" :class="['mode-btn', { active: repairMode === 'ai' }]" @click="repairMode = 'ai'">
              <span class="mode-icon">✨</span>
              智能报修
            </button>
          </div>
        </div>

        <!-- 智能报修模式 -->
        <div v-if="repairMode === 'ai'" class="form-section ai-mode-section">
          <div class="ai-mode-header">
            <div class="ai-mode-icon">🤖</div>
            <div class="ai-mode-text">
              <h4>智能报修助手</h4>
              <p>只需一句话描述问题，AI 自动提取关键信息</p>
            </div>
          </div>
          
          <div class="ai-input-group">
            <label class="mc-label">问题描述</label>
            <textarea
              v-model="naturalText"
              class="mc-textarea ai-textarea"
              placeholder="例如：3号楼502空调不制冷，晚上热得睡不着，帮我报修。"
              rows="4"
            ></textarea>
          </div>
          
          <div class="ai-actions">
            <UiButton 
              type="primary" 
              :loading="nlLoading" 
              :disabled="!naturalText.trim()"
              @click="handleNaturalRepair"
              class="ai-analyze-btn"
            >
              <span class="btn-icon">🔍</span>
              {{ nlLoading ? 'AI 分析中...' : '开始智能分析' }}
            </UiButton>
          </div>
          
          <div v-if="nlError" class="ai-error-card">
            <div class="error-icon">⚠️</div>
            <div class="error-content">
              <div class="error-title">分析失败</div>
              <div class="error-message">{{ nlError }}</div>
            </div>
          </div>
          
          <div v-if="nlResult" class="ai-result-card">
            <div class="result-header">
              <div class="result-icon">✅</div>
              <div class="result-title">AI 分析结果</div>
            </div>
            
            <div class="result-grid">
              <div class="result-item">
                <span class="result-label">楼栋</span>
                <span class="result-value" :class="{ 'not-recognized': !nlResult.building }">
                  {{ nlResult.building || '未识别' }}
                </span>
              </div>
              <div class="result-item">
                <span class="result-label">房号</span>
                <span class="result-value" :class="{ 'not-recognized': !nlResult.room }">
                  {{ nlResult.room || '未识别' }}
                </span>
              </div>
              <div class="result-item">
                <span class="result-label">故障类型</span>
                <span class="result-value" :class="{ 'not-recognized': !nlResult.repairType }">
                  {{ nlResult.repairType || '未识别' }}
                </span>
              </div>
              <div class="result-item">
                <span class="result-label">建议分类</span>
                <span class="result-value category">{{ nlResult.categoryName || '未识别' }}</span>
              </div>
              <div class="result-item">
                <span class="result-label">紧急程度</span>
                <span class="result-value urgency" :class="nlResult.urgencyLevel">
                  {{ nlResult.urgencyLevel || '未识别' }}
                </span>
              </div>
              <div class="result-item">
                <span class="result-label">置信度</span>
                <span class="result-value confidence">
                  {{ (nlResult.confidence * 100).toFixed(0) }}%
                </span>
              </div>
            </div>
            
            <div class="result-reason">
              <div class="reason-label">判断依据</div>
              <div class="reason-text">{{ nlResult.reason }}</div>
            </div>
            
            <div class="result-actions">
              <UiButton 
                type="secondary" 
                @click="nlResult = null"
                class="re-analyze-btn"
              >
                重新分析
              </UiButton>
              <UiButton 
                type="primary" 
                :loading="nlSubmitting" 
                @click="applyNaturalResult"
                class="submit-btn"
              >
                <span class="btn-icon">📝</span>
                确认并提交报修
              </UiButton>
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
            @click="handleClassify"
          >
            {{ aiLoading ? 'AI 分析中...' : '🤖 AI 智能分析' }}
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
          <UiButton type="secondary" @click="$router.back()" :disabled="loading">取消</UiButton>
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

/* Natural repair mode styles */
.switch-section { margin-bottom: 16px; }
.mode-switch { display: flex; gap: 0; border: 1px solid var(--mc-hairline); border-radius: var(--mc-radius-md); overflow: hidden; }
.mode-btn { flex: 1; padding: 10px 16px; border: none; background: var(--mc-white); font-size: 14px; cursor: pointer; transition: all 0.2s; color: var(--mc-muted); }
.mode-btn.active { background: var(--mc-ink); color: var(--mc-on-dark); font-weight: 600; }
.mode-btn:not(.active):hover { background: var(--mc-canvas); }
.natural-actions { margin-top: 8px; }
.nl-result-card { margin-top: 12px; padding: 16px; border-radius: var(--mc-radius-md); border: 1px solid var(--mc-success); background: var(--mc-canvas); animation: ai-fade-in 0.3s ease; }
.nl-result-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.nl-label { font-size: 12px; color: var(--mc-muted); display: block; }
.nl-value { font-size: 14px; font-weight: 600; color: var(--mc-ink); }
.nl-reason { margin-top: 10px; font-size: 13px; color: var(--mc-muted); padding-top: 10px; border-top: 1px solid var(--mc-hairline-soft); }
</style>


