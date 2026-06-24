<script setup lang="ts">
import { ref, onMounted } from 'vue'
import UiModal from './UiModal.vue'
import UiInput from './UiInput.vue'
import UiButton from './UiButton.vue'
import { getSavedServerAddress, saveServerAddress, clearServerAddress } from '../../utils/serverConfig'

interface Props {
  visible: boolean
}

defineProps<Props>()
const emit = defineEmits(['close', 'saved'])

const address = ref('')
const error = ref('')
const saved = ref(false)

onMounted(() => {
  const existing = getSavedServerAddress()
  if (existing) address.value = existing
})

const handleSave = () => {
  const val = address.value.trim()
  if (!val) {
    error.value = '请输入服务器地址'
    return
  }
  // Basic validation: must look like host:port
  if (!/^[a-zA-Z0-9.\-]+:\d+$/.test(val)) {
    error.value = '格式应为 IP:端口，例如 192.168.1.5:8080'
    return
  }
  error.value = ''
  saveServerAddress(val)
  saved.value = true
  setTimeout(() => {
    emit('saved')
    emit('close')
  }, 800)
}

const handleReset = () => {
  clearServerAddress()
  address.value = ''
  error.value = ''
}
</script>

<template>
  <UiModal :visible="visible" title="服务器地址设置" max-width="440px" @close="emit('close')">
    <p class="server-hint">
      手机 App 需要通过局域网 IP 连接电脑上的后端服务。
      在电脑的终端中运行 <code>ipconfig</code> 查看 IPv4 地址。
    </p>

    <UiInput
      v-model="address"
      label="服务器地址"
      placeholder="例如 192.168.1.5:8080"
      :error="error"
    />

    <div v-if="saved" class="server-saved">✓ 地址已保存</div>

    <div class="server-actions">
      <UiButton type="secondary" @click="handleReset">重置为默认</UiButton>
      <UiButton type="primary" @click="handleSave">保存</UiButton>
    </div>

    <p class="server-note">
      默认地址（模拟器）：10.0.2.2:8080<br />
      修改后无需重新构建，立即生效。
    </p>
  </UiModal>
</template>

<style scoped>
.server-hint {
  font-size: 13px;
  color: var(--mc-muted);
  line-height: 1.6;
  margin: 0;
}

.server-hint code {
  background: var(--mc-bg-subtle);
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 12px;
}

.server-saved {
  text-align: center;
  color: #22c55e;
  font-size: 14px;
  font-weight: 500;
}

.server-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 4px;
}

.server-note {
  font-size: 12px;
  color: var(--mc-muted);
  margin: 0;
  line-height: 1.6;
}
</style>
