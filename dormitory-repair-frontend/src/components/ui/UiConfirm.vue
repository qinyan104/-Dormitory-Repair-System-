<script setup lang="ts">
import { useConfirm } from '../../composables/useConfirm'
import UiButton from './UiButton.vue'

const { confirmState, handleConfirm, handleCancel } = useConfirm()
</script>

<template>
  <Teleport to="body">
    <Transition name="confirm">
      <div v-if="confirmState.visible" class="mc-confirm-mask" @click.self="handleCancel">
        <div class="mc-confirm-box">
          <h4 class="mc-confirm-title">{{ confirmState.title }}</h4>
          <p class="mc-confirm-msg">{{ confirmState.message }}</p>
          <div class="mc-confirm-actions">
            <UiButton type="secondary" @click="handleCancel">取消</UiButton>
            <UiButton type="primary" @click="handleConfirm">确认</UiButton>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.mc-confirm-mask {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 300;
}

.mc-confirm-box {
  width: 90%;
  max-width: 380px;
  background-color: var(--mc-white);
  border: 1px solid var(--mc-hairline);
  border-radius: var(--mc-radius-lg);
  padding: 32px;
}

.mc-confirm-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--mc-ink);
  margin: 0 0 12px;
}

.mc-confirm-msg {
  font-size: 14px;
  color: var(--mc-muted);
  line-height: 1.6;
  margin: 0 0 28px;
}

.mc-confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.confirm-enter-active { transition: opacity 0.2s ease; }
.confirm-leave-active { transition: opacity 0.15s ease; }
.confirm-enter-from,
.confirm-leave-to { opacity: 0; }
</style>

