<script setup lang="ts">
import { ref, onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import UiInput from '../../components/ui/UiInput.vue'
import UiButton from '../../components/ui/UiButton.vue'
import UiSelect from '../../components/ui/UiSelect.vue'
import UiAuthShell from '../../components/ui/UiAuthShell.vue'
import ServerConfigModal from '../../components/ui/ServerConfigModal.vue'
import { registerApi, getCaptchaApi } from '../../api/auth'
import { useToast } from '../../composables/useToast'
import { isNativeApp, getSavedServerAddress } from '../../utils/serverConfig'

const toast = useToast()
const router = useRouter()

const form = ref({
  username: '',
  password: '',
  realName: '',
  studentNo: '',
  phone: '',
  gender: '1',
  dormitoryBuilding: '',
  roomNo: ''
})
const loading = ref(false)
const errors = ref<Record<string, string>>({})
const captchaKey = ref('')
const captchaCode = ref('')
const captchaImage = ref('')
const captchaError = ref('')
const showServerConfig = ref(false)
let captchaRequestId = 0

onMounted(() => {
  if (isNativeApp() && !getSavedServerAddress()) {
    showServerConfig.value = true
  }
  fetchCaptcha()
})

onBeforeUnmount(() => {
  captchaRequestId++
})

const setupSteps = [
  {
    index: '01',
    title: '建立账户',
    description: '设置登录信息与真实姓名'
  },
  {
    index: '02',
    title: '绑定宿舍',
    description: '补充楼栋、房间和联系方式'
  },
  {
    index: '03',
    title: '开始报修',
    description: '登录后即可提交和跟踪工单'
  }
] as const

const genderOptions = [
  { label: '男', value: '1' },
  { label: '女', value: '0' }
]

const fetchCaptcha = async () => {
  const requestId = ++captchaRequestId
  try {
    captchaError.value = ''
    captchaImage.value = ''
    const d: any = await getCaptchaApi()
    if (requestId !== captchaRequestId) return
    captchaKey.value = d.captchaKey
    captchaImage.value = d.captchaImage
  } catch {
    if (requestId !== captchaRequestId) return
    captchaError.value = '点击刷新'
  }
}

const handleRegister = async () => {
  if (loading.value) return
  loading.value = true
  errors.value = {}

  if (!form.value.username) errors.value.username = '请输入用户名'
  if (!form.value.password) errors.value.password = '请输入密码'
  if (!form.value.realName) errors.value.realName = '请输入真实姓名'
  if (!form.value.studentNo) errors.value.studentNo = '请输入学号'
  if (!captchaCode.value) errors.value.captcha = '请输入验证码'

  if (Object.keys(errors.value).length) {
    loading.value = false
    return
  }

  try {
    await registerApi({
      ...form.value,
      gender: form.value.gender ? Number(form.value.gender) : undefined,
      captchaKey: captchaKey.value,
      captchaCode: captchaCode.value
    })

    toast.success('注册成功，请登录')
    router.push('/login')
  } catch (e: any) {
    fetchCaptcha()
    captchaCode.value = ''
    toast.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <UiAuthShell
    mode="register"
    eyebrow="Student Account Setup"
    title="先建立档案"
    accent="以后报修少填一步"
    subtitle="身份和宿舍信息只需录入一次，后续工单会自动关联位置与联系人。"
    :steps="setupSteps"
    panel-tag="学生账号创建"
    panel-title="创建学生账号"
    panel-description="请填写真实资料。带有校验提示的字段将用于登录和确认学生身份。"
    footer-text="已经有账号了？"
    footer-link-text="返回登录"
    footer-to="/login"
  >
    <form class="auth-form" @submit.prevent="handleRegister">
      <section class="auth-fieldset">
        <div class="auth-section-header">
          <span class="auth-section-label">账户信息</span>
          <span class="auth-section-note">这些字段将用于登录与身份校验</span>
        </div>

        <div class="auth-grid auth-grid--two">
          <UiInput
            v-model="form.username"
            label="用户名"
            placeholder="用于登录系统"
            :error="errors.username"
          />
          <UiInput
            v-model="form.password"
            label="密码"
            type="password"
            placeholder="不少于 6 位"
            :error="errors.password"
          />
          <UiInput
            v-model="form.realName"
            label="真实姓名"
            placeholder="填写本人姓名"
            :error="errors.realName"
          />
          <UiInput
            v-model="form.studentNo"
            label="学号"
            placeholder="输入学号"
            :error="errors.studentNo"
          />
        </div>
      </section>

      <section class="auth-fieldset">
        <div class="auth-section-header">
          <span class="auth-section-label">宿舍资料</span>
          <span class="auth-section-note">后续提交工单时可直接复用</span>
        </div>

        <div class="auth-grid auth-grid--two">
          <UiInput
            v-model="form.dormitoryBuilding"
            label="宿舍楼"
            placeholder="例如 12 号楼"
          />
          <UiInput
            v-model="form.roomNo"
            label="房间号"
            placeholder="例如 302"
          />
          <UiInput
            v-model="form.phone"
            label="手机号"
            placeholder="选填"
          />
          <UiSelect
            v-model="form.gender"
            label="性别"
            :options="genderOptions"
          />
        </div>
      </section>

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
        注册完成后请直接使用新账号登录。验证码若不清楚，点击图片即可刷新。
      </p>

      <UiButton type="primary" :loading="loading" class="submit-btn">创建账号</UiButton>
      <p class="auth-form-note" style="margin-top:12px;text-align:center">
        维修人员请联系管理员创建账号
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
</style>
