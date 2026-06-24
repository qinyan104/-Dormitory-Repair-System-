<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import UiCard from '../../components/ui/UiCard.vue'
import UiSectionTitle from '../../components/ui/UiSectionTitle.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiInput from '../../components/ui/UiInput.vue'
import UiModal from '../../components/ui/UiModal.vue'
import UiSelect from '../../components/ui/UiSelect.vue'
import { useAuthStore } from '../../stores/auth'
import { updateUserProfileApi, getUserProfileApi, changePasswordApi } from '../../api/user'
import { uploadFileApi } from '../../api/file'
import { useToast } from '../../composables/useToast'
import { useConfirm } from '../../composables/useConfirm'
import { useWebSocket } from '../../composables/useWebSocket'

const toast = useToast()
const { confirm } = useConfirm()
const { disconnect } = useWebSocket()

const router = useRouter()
const authStore = useAuthStore()

const user = ref({
  realName: authStore.user?.realName || '',
  username: authStore.user?.username || '',
  phone: authStore.user?.phone || '',
  gender: authStore.user?.gender ?? 0,
  avatar: authStore.user?.avatar || ''
})

const isEditing = ref(false)
const loading = ref(false)

const genderOptions = [
  { label: '保密', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 }
]

const handleLogout = async () => {
  if (await confirm('确定要退出维修后台吗？')) {
    disconnect()          // 先断开WebSocket连接
    authStore.logout()    // 再清理认证状态
    router.push('/login')
  }
}

const handleSave = async () => {
  loading.value = true
  try {
    await updateUserProfileApi({
      realName: user.value.realName,
      phone: user.value.phone,
      gender: user.value.gender,
      avatar: user.value.avatar
    })
    const userInfo: any = await getUserProfileApi()
    authStore.setUser(userInfo)
    isEditing.value = false
    toast.success('个人信息已更新')
  } catch (err: any) {
    toast.error(err.message || '更新失败')
  } finally {
    loading.value = false
  }
}

const fileInput = ref<HTMLInputElement | null>(null)
const uploadingAvatar = ref(false)

const triggerAvatarUpload = () => {
  if (!isEditing.value) return
  fileInput.value?.click()
}

const handleAvatarUpload = async (e: Event) => {
  const target = e.target as HTMLInputElement
  if (!target.files?.length) return
  const file = target.files[0]
  if (!file.type.startsWith('image/')) {
    toast.error('只能上传图片文件')
    return
  }
  uploadingAvatar.value = true
  try {
    const res: any = await uploadFileApi(file)
    user.value.avatar = res.url || res
    toast.success('头像上传成功')
  } catch (err: any) {
    toast.error(err.message || '头像上传失败')
  } finally {
    uploadingAvatar.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

const showPasswordModal = ref(false)
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordLoading = ref(false)

const handlePasswordSubmit = async () => {
  if (!passwordForm.value.oldPassword) return toast.error('请输入旧密码')
  if (!passwordForm.value.newPassword) return toast.error('请输入新密码')
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    return toast.error('两次输入的新密码不一致')
  }
  
  passwordLoading.value = true
  try {
    await changePasswordApi({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    toast.success('密码修改成功，请重新登录')
    showPasswordModal.value = false
    authStore.logout()
    router.push('/login')
  } catch (err: any) {
    toast.error(err.message || '密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}
</script>

<template>
  <div class="profile-view">
    <UiSectionTitle eyebrow="账号设置" title="个人中心" />

    <div class="profile-grid">
      <UiCard variant="white" padding="24px 40px" class="info-card" elevated>
        <div class="user-header">
          <div class="user-avatar" :class="{ 'is-editable': isEditing }" @click="triggerAvatarUpload">
            <img v-if="user.avatar" :src="user.avatar" alt="Avatar" class="avatar-img" />
            <span v-else>{{ user.realName.charAt(0) || '修' }}</span>
            <div v-if="isEditing" class="avatar-overlay">
              <span v-if="uploadingAvatar" class="uploading-text">上传中</span>
              <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"/>
              </svg>
            </div>
            <input type="file" ref="fileInput" class="hidden-input" accept="image/*" @change="handleAvatarUpload" />
          </div>
          <div class="user-meta">
            <h3 class="user-name">{{ user.realName || '维修人员' }}</h3>
            <span class="user-role">维修工</span>
          </div>
        </div>

        <div class="info-list">
          <div class="info-item">
            <span class="info-label">用户名</span>
            <div class="info-content"><span class="info-value">{{ user.username }}</span></div>
          </div>
          <div class="info-item">
            <span class="info-label">真实姓名</span>
            <div class="info-content">
              <span v-if="!isEditing" class="info-value">{{ user.realName }}</span>
              <UiInput v-else v-model="user.realName" placeholder="输入姓名" />
            </div>
          </div>
          <div class="info-item">
            <span class="info-label">性别</span>
            <div class="info-content">
              <span v-if="!isEditing" class="info-value">{{ user.gender === 1 ? '男' : user.gender === 2 ? '女' : '保密' }}</span>
              <UiSelect v-else v-model="user.gender" :options="genderOptions" />
            </div>
          </div>
          <div class="info-item">
            <span class="info-label">联系电话</span>
            <div class="info-content">
              <span v-if="!isEditing" class="info-value">{{ user.phone || '未设置' }}</span>
              <UiInput v-else v-model="user.phone" placeholder="输入手机号" />
            </div>
          </div>
        </div>

        <div class="info-actions">
          <UiButton v-if="!isEditing" type="secondary" @click="isEditing = true">编辑资料</UiButton>
          <template v-else>
            <UiButton type="secondary" @click="isEditing = false">取消</UiButton>
            <UiButton type="primary" :loading="loading" @click="handleSave">保存修改</UiButton>
          </template>
        </div>
      </UiCard>

      <div class="side-options">
        <UiCard variant="canvas" padding="32px" class="option-card">
          <h4 class="option-title">系统安全</h4>
          <p class="option-desc">保护您的账号安全，定期更换密码。</p>
          <UiButton type="secondary" class="full-width" @click="showPasswordModal = true">修改密码</UiButton>
        </UiCard>

        <UiCard variant="canvas" padding="32px" class="option-card logout-card">
          <h4 class="option-title">会话管理</h4>
          <p class="option-desc">退出当前账号并返回登录界面。</p>
          <UiButton type="signal" class="full-width" @click="handleLogout">退出登录</UiButton>
        </UiCard>
      </div>
    </div>

    <UiModal :visible="showPasswordModal" @close="showPasswordModal = false" title="修改密码" maxWidth="400px">
      <div class="password-form">
        <div class="form-group">
          <label class="mc-label">旧密码</label>
          <UiInput v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" />
        </div>
        <div class="form-group">
          <label class="mc-label">新密码</label>
          <UiInput v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" />
        </div>
        <div class="form-group">
          <label class="mc-label">确认新密码</label>
          <UiInput v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
        </div>
        <div class="password-footer">
          <UiButton type="secondary" @click="showPasswordModal = false">取消</UiButton>
          <UiButton type="primary" :loading="passwordLoading" @click="handlePasswordSubmit">确认修改</UiButton>
        </div>
      </div>
    </UiModal>
  </div>
</template>

<style scoped>
.profile-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 32px;
  align-items: start;
}

.user-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 40px;
}

.user-avatar {
  width: 80px;
  height: 80px;
  background-color: var(--mc-signal);
  color: var(--mc-white);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-family: var(--mc-font-display);
  font-weight: 400;
  overflow: hidden;
  position: relative;
}

.user-avatar.is-editable {
  cursor: pointer;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transition: opacity 0.2s;
}

.user-avatar.is-editable:hover .avatar-overlay {
  opacity: 1;
}

.hidden-input {
  display: none;
}

.uploading-text {
  font-size: 12px;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-name {
  font-size: var(--mc-fz-display-sm);
  font-weight: 400;
  color: var(--mc-ink);
  font-family: var(--mc-font-display);
  letter-spacing: var(--mc-ls-display-sm);
}

.user-role {
  font-size: 14px;
  color: var(--mc-charcoal);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-list {
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--mc-hairline);
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  width: 120px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--mc-charcoal);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-content {
  flex: 1;
  display: flex;
  align-items: center;
}

.info-value {
  font-size: 16px;
  font-weight: 500;
  color: var(--mc-ink);
}

.info-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  border-top: 1px solid var(--mc-hairline-soft);
  padding-top: 16px;
}

.side-options {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.option-card {
  border: 1px solid var(--mc-hairline);
}

.option-title {
  font-size: var(--mc-fz-title-md);
  font-weight: 400;
  color: var(--mc-ink);
  margin-bottom: 12px;
  font-family: var(--mc-font-display);
}

.option-desc {
  font-size: 14px;
  color: var(--mc-ink);
  font-weight: 500;
  margin-bottom: 20px;
}

.full-width {
  width: 100%;
}

.password-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 10px 0;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mc-label {
  font-size: 14px;
  color: var(--mc-charcoal);
  font-weight: 500;
}

.password-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

@media (max-width: 768px) {
  .profile-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  .info-card {
    padding: 20px 16px !important;
  }
  .user-header {
    gap: 16px;
    margin-bottom: 24px;
  }
  .user-avatar {
    width: 64px;
    height: 64px;
    font-size: 24px;
  }
  .user-name {
    font-size: 20px;
  }
  .info-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  .info-label {
    width: auto;
  }
  .info-actions {
    flex-direction: column;
  }
  .info-actions :deep(.mc-button) {
    width: 100%;
    min-height: 48px;
  }
  .password-footer {
    flex-direction: column;
  }
  .password-footer :deep(.mc-button) {
    width: 100%;
    min-height: 48px;
  }
}

</style>
