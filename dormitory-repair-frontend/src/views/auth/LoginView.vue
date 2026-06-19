<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import UiInput from '../../components/ui/UiInput.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiAuthShell from '../../components/ui/UiAuthShell.vue'
import { useAuthStore } from '../../stores/auth'
import { loginApi, getMeApi, getCaptchaApi } from '../../api/auth'
import { useToast } from '../../composables/useToast'

const toast = useToast()
const router = useRouter()
const authStore = useAuthStore()

const form = ref({ username: '', password: '' })
const loading = ref(false)
const errors = ref<Record<string, string>>({})
const captchaKey = ref('')
const captchaCode = ref('')
const captchaImage = ref('')

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
    const d: any = await getCaptchaApi()
    captchaKey.value = d.captchaKey
    captchaImage.value = d.captchaImage
  } catch {}
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

    const role = d.userInfo?.role || 'STUDENT'
    router.push(
      role === 'ADMIN'
        ? '/admin/dashboard'
        : role === 'REPAIRER'
          ? '/repairer/dashboard'
          : '/student/home'
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
          <span v-else class="captcha-dummy">加载中</span>
        </div>
      </div>

      <p class="auth-form-note">
        验证码点击图片即可刷新。登录成功后会自动跳转到对应身份页面。
      </p>

      <UiButton type="primary" :loading="loading" class="submit-btn">登录</UiButton>
    </form>
  </UiAuthShell>
</template>
