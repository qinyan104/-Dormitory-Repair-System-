<script setup lang="ts">
interface Props {
  visible: boolean
  title?: string
  maxWidth?: string
}

defineProps<Props>()
const emit = defineEmits(['close'])
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="mc-modal-mask" @click.self="emit('close')">
        <div class="mc-modal-container" :style="{ maxWidth: maxWidth || '560px' }">
          <div class="mc-modal-header" v-if="title || $slots.header">
            <slot name="header">
              <h3>{{ title }}</h3>
            </slot>
            <button class="mc-modal-close" @click="emit('close')">&times;</button>
          </div>
          <div class="mc-modal-body">
            <slot />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.mc-modal-mask {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.mc-modal-container {
  width: 90%;
  max-height: 85vh;
  overflow-y: auto;
  background-color: var(--mc-white);
  border-radius: var(--mc-radius-lg);
  padding: 40px;
  border: 1px solid var(--mc-hairline);
}

.mc-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.mc-modal-header h3 {
  font-family: var(--mc-font-display);
  font-size: var(--mc-fz-display-sm);
  font-weight: 400;
  letter-spacing: var(--mc-ls-display-sm);
  color: var(--mc-ink);
  margin: 0;
}

.mc-modal-close {
  font-size: 22px;
  line-height: 1;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--mc-muted);
  padding: 4px;
  transition: color var(--mc-transition);
}

.mc-modal-close:hover {
  color: var(--mc-ink);
}

.mc-modal-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.modal-enter-active { transition: opacity 0.2s ease; }
.modal-leave-active { transition: opacity 0.15s ease; }
.modal-enter-from,
.modal-leave-to { opacity: 0; }
</style>

