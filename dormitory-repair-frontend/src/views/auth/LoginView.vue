<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import UiInput from '../../components/ui/UiInput.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiModal from '../../components/ui/UiModal.vue'
import UiAuthShell from '../../components/ui/UiAuthShell.vue'
import ServerConfigModal from '../../components/ui/ServerConfigModal.vue'
import { useAuthStore } from '../../stores/auth'
import { loginApi, getMeApi, getCaptchaApi } from '../../api/auth'
import http from '../../api/http'
import { useToast } from '../../composables/useToast'
import { isNativeApp, getSavedServerAddress } from '../../utils/serverConfig'

const toast = useToast()
const router = useRouter()
const authStore = useAuthStore()

const form = ref({ username: '', password: '' })
const loading = ref(false)
const errors = ref<Record<string, string>>({})
const captchaKey = ref('')
const captchaCode = ref('')
const captchaImage = ref('')
const captchaError = ref('')
const showServerConfig = ref(false)
const showForgot = ref(false)
const forgotUsername = ref('')
const forgotCaptchaCode = ref('')
const forgotCaptchaKey = ref('')
const forgotCaptchaImage = ref('')
const forgotCaptchaError = ref('')
const forgotLoading = ref(false)
const forgotResult = ref('')
const forgotError = ref('')
const forgotNewPassword = ref('')
const forgotConfirmPassword = ref('')
const forgotStudentNo = ref('')

const fetchForgotCaptcha = async () => {
  try {
    forgotCaptchaError.value = ''
    forgotCaptchaImage.value = ''
    const d: any = await getCaptchaApi()
    forgotCaptchaKey.value = d.captchaKey
    forgotCaptchaImage.value = d.captchaImage
  } catch (e: any) {
    forgotCaptchaError.value = '加载失败'
  }
}

const handleForgotPassword = async () => {
  forgotError.value = ''
  forgotResult.value = ''

  if (!forgotStudentNo.value) {
    forgotError.value = '请输入学号'
    return
  }
  if (forgotNewPassword.value.length < 6) {
    forgotError.value = '密码长度不能少于6位'
    return
  }
  if (forgotNewPassword.value !== forgotConfirmPassword.value) {
    forgotError.value = '两次密码输入不一致'
    return
  }

  forgotLoading.value = true
  try {
    await http.post('/auth/forgot-password', {
      username: forgotUsername.value,
      studentNo: forgotStudentNo.value,
      captchaKey: forgotCaptchaKey.value,
      captchaCode: forgotCaptchaCode.value,
      newPassword: forgotNewPassword.value
    })
    forgotResult.value = '密码重置成功，请使用新密码登录'
    setTimeout(() => { showForgot.value = false; forgotResult.value = '' }, 3000)
  } catch (e: any) {
    forgotResult.value = ''
    forgotCaptchaError.value = e?.message || '重置失败'
    fetchForgotCaptcha()
    forgotCaptchaCode.value = ''
  } finally {
    forgotLoading.value = false
  }
}

const handleClearCache = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  sessionStorage.clear()
  toast.success('缓存已清除，请重新登录')
  window.location.reload()
}

onMounted(() => {
  // Auto-show server config on first native launch when no address is saved
  if (isNativeApp() && !getSavedServerAddress()) {
    showServerConfig.value = true
  }
})

const processSteps = [
  {
    index: '01',
    title: '提交',
    description: '填写宿舍问题'
  },
  {
    index: '02',
    title: '派单',
    description: '管理员分配人员'
  },
  {
    index: '03',
    title: '维修',
    description: '维修人员上门处理'
  },
  {
    index: '04',
    title: '确认',
    description: '学生验收并反馈'
  }
] as const

const fetchCaptcha = async () => {
  try {
    captchaError.value = ''
    captchaImage.value = ''
    const d: any = await getCaptchaApi()
    captchaKey.value = d.captchaKey
    captchaImage.value = d.captchaImage
  } catch (e: any) {
    console.error('[captcha] 验证码加载失败:', e)
    const detail = e?.message || e?.code || String(e)
    captchaError.value = '验证码加载失败: ' + detail
  }
}

fetchCaptcha()

const handleLogin = async () => {
  loading.value = true
  errors.value = {}

  if (!form.value.username) errors.value.username = '请输入用户名'
  if (!form.value.password) errors.value.password = '请输入密码'
  if (!captchaCode.value) errors.value.captcha = '请输入验证码'

  if (Object.keys(errors.value).length) {
    loading.value = false
    return
  }

  try {
    const d: any = await loginApi({
      ...form.value,
      captchaKey: captchaKey.value,
      captchaCode: captchaCode.value
    })

    authStore.setToken(d.token)
    authStore.setUser(await getMeApi() as any)

    const role = authStore.user?.role || 'STUDENT'
    router.push(
      role === 'ADMIN'
        ? '/admin/dashboard'
        : role === 'REPAIRER'
          ? '/repairer/dashboard'
          : '/student'
    )
  } catch (e: any) {
    fetchCaptcha()
    captchaCode.value = ''
    toast.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <UiAuthShell
    mode="login"
    eyebrow="Dormitory Repair Dispatch"
    title="一次报修"
    accent="每一步都有回应"
    subtitle="从问题提交到维修确认，所有角色都在同一条清晰的处理链路上协作。"
    :steps="processSteps"
    panel-tag="统一身份入口"
    panel-title="欢迎回来"
    panel-description="使用学号或用户名登录，系统会自动进入与你身份对应的工作台。"
    footer-text="还没有账号？"
    footer-link-text="立即注册"
    footer-to="/register"
  >
    <form class="auth-form" @submit.prevent="handleLogin" @keyup.enter="handleLogin">
      <UiInput
        v-model="form.username"
        label="用户名"
        placeholder="学号或用户名"
        :error="errors.username"
      />

      <UiInput
        v-model="form.password"
        label="密码"
        type="password"
        placeholder="输入登录密码"
        :error="errors.password"
      />

      <div class="captcha-row">
        <UiInput
          v-model="captchaCode"
          label="验证码"
          placeholder="输入图片验证码"
          :error="errors.captcha"
          class="captcha-input"
        />

        <div class="captcha-box" @click="fetchCaptcha" title="点击刷新验证码">
          <img v-if="captchaImage" :src="captchaImage" alt="验证码图片" class="captcha-img" />
          <span v-else class="captcha-dummy">{{ captchaError || '加载中' }}</span>
        </div>
      </div>

      <p class="auth-form-note">
        验证码点击图片即可刷新。登录成功后会自动跳转到对应身份页面。
      </p>

      <UiButton type="primary" :loading="loading" class="submit-btn">登录</UiButton>
      <p class="forgot-pwd">
        <a href="javascript:void(0)" @click="showForgot = true">忘记密码？</a>
      </p>
    </form>

    <button
      v-if="isNativeApp()"
      class="server-config-btn"
      @click="showServerConfig = true"
      title="设置服务器地址"
    >
      ⚙️ 服务器设置
    </button>

    <ServerConfigModal
      :visible="showServerConfig"
      @close="showServerConfig = false"
      @saved="fetchCaptcha"
    />

    <UiModal :visible="showForgot" title="重置密码" max-width="400px" @close="showForgot = false">
      <form @submit.prevent="handleForgotPassword">
        <UiInput
          v-model="forgotUsername"
          label="用户名"
          placeholder="输入你的用户名"
        />
        <UiInput
          v-model="forgotStudentNo"
          label="学号"
          placeholder="输入学号验证身份"
        />
        <div class="captcha-row" style="margin-bottom:12px">
          <UiInput
            v-model="forgotCaptchaCode"
            label="验证码"
            placeholder="输入图片验证码"
            class="captcha-input"
          />
          <div class="captcha-box" @click="fetchForgotCaptcha" title="点击刷新验证码">
            <img v-if="forgotCaptchaImage" :src="forgotCaptchaImage" alt="验证码" class="captcha-img" />
            <span v-else class="captcha-dummy">{{ forgotCaptchaError || '加载中' }}</span>
          </div>
        </div>
        <UiInput
          v-model="forgotNewPassword"
          label="新密码"
          type="password"
          placeholder="不少于6位"
        />
        <UiInput
          v-model="forgotConfirmPassword"
          label="确认密码"
          type="password"
          placeholder="再次输入新密码"
        />
        <p v-if="forgotError" style="color:var(--mc-error);font-size:13px;margin-bottom:8px">{{ forgotError }}</p>
        <p v-if="forgotResult" style="color:var(--mc-success);font-size:13px;margin-bottom:8px">{{ forgotResult }}</p>
        <UiButton type="primary" :loading="forgotLoading" html-type="submit" style="width:100%">
          重置密码
        </UiButton>
      </form>
    </UiModal>

    <!-- 清除缓存按钮 -->
    <div class="clear-cache-section">
      <p class="clear-cache-hint">登录遇到问题？</p>
      <button class="clear-cache-btn" @click="handleClearCache">
        🗑️ 清除浏览器缓存
      </button>
    </div>
  </UiAuthShell>
</template>

<style scoped>
.server-config-btn {
  display: block;
  margin: 16px auto 0;
  padding: 6px 12px;
  background: none;
  border: 1px solid var(--mc-hairline);
  border-radius: var(--mc-radius-sm);
  color: var(--mc-muted);
  font-size: 12px;
  cursor: pointer;
  transition: color var(--mc-transition), border-color var(--mc-transition);
}

.server-config-btn:hover {
  color: var(--mc-ink);
  border-color: var(--mc-muted);
}

.forgot-pwd {
  text-align: center;
  margin-top: 10px;
}
.forgot-pwd a {
  color: var(--mc-muted);
  font-size: 13px;
  text-decoration: none;
  transition: color 0.2s;
}
.forgot-pwd a:hover {
  color: var(--mc-signal);
}

/* 清除缓存按钮样式 */
.clear-cache-section {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--mc-hairline);
  text-align: center;
}

.clear-cache-hint {
  font-size: 13px;
  color: var(--mc-muted);
  margin-bottom: 8px;
}

.clear-cache-btn {
  background: none;
  border: 1px solid var(--mc-hairline);
  border-radius: var(--mc-radius-sm);
  padding: 6px 12px;
  font-size: 12px;
  color: var(--mc-muted);
  cursor: pointer;
  transition: all var(--mc-transition);
}

.clear-cache-btn:hover {
  color: var(--mc-signal);
  border-color: var(--mc-signal);
  background-color: rgba(0, 0, 0, 0.02);
}
</style>
