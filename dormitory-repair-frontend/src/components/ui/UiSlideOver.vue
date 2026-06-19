<script setup lang="ts">
import { watch, onUnmounted } from 'vue'

const props = defineProps({
  visible: Boolean,
  title: String,
  maxWidth: { type: String, default: '500px' }
})

const emit = defineEmits(['close'])

const close = () => {
  emit('close')
}

// Lock body scroll when open
watch(() => props.visible, (newVal) => {
  if (newVal) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

onUnmounted(() => {
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition name="slide-fade">
      <div v-if="visible" class="mc-slide-over">
        <div class="slide-overlay" @click="close"></div>
        <div class="slide-panel" :style="{ maxWidth: maxWidth }">
          <div class="slide-header">
            <h3 class="slide-title">{{ title }}</h3>
            <button class="slide-close" @click="close">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M18 6L6 18M6 6l12 12" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
          <div class="slide-body">
            <slot></slot>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.mc-slide-over {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  justify-content: flex-end;
}

.slide-overlay {
  position: absolute;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
}

.slide-panel {
  position: relative;
  width: 100%;
  height: 100vh;
  background-color: var(--mc-canvas);
  box-shadow: -10px 0 40px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.slide-header {
  height: 80px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--mc-hairline);
  background-color: var(--mc-white);
  flex-shrink: 0;
}

.slide-title {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
  color: var(--mc-ink);
  font-family: var(--mc-font-display);
}

.slide-close {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--mc-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  margin-right: -8px;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.slide-close:hover {
  background-color: rgba(0, 0, 0, 0.05);
  color: var(--mc-ink);
}

.slide-body {
  flex: 1;
  overflow-y: auto;
  padding: 32px;
}

/* Transitions */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.slide-fade-enter-active .slide-panel,
.slide-fade-leave-active .slide-panel {
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
}
.slide-fade-enter-from .slide-panel,
.slide-fade-leave-to .slide-panel {
  transform: translateX(100%);
}
</style>

