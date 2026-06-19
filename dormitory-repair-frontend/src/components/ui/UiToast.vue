<script setup lang="ts">
import { useToast } from '../../composables/useToast'

const { toasts } = useToast()
</script>

<template>
  <div class="mc-toast-container">
    <TransitionGroup name="toast">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        :class="['mc-toast', `mc-toast--${toast.type}`]"
      >
        <span class="mc-toast-icon">
          <template v-if="toast.type === 'success'">&#10003;</template>
          <template v-else-if="toast.type === 'error'">&#10007;</template>
          <template v-else>&#8505;</template>
        </span>
        <span class="mc-toast-msg">{{ toast.message }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.mc-toast-container {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}

.mc-toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  border-radius: var(--mc-radius-md);
  background-color: var(--mc-surface-dark);
  color: var(--mc-on-dark);
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  pointer-events: auto;
  max-width: 360px;
}

.mc-toast--success { border-left: 3px solid var(--mc-success); }
.mc-toast--error   { border-left: 3px solid var(--mc-error); }
.mc-toast--info    { border-left: 3px solid var(--mc-primary); }

.mc-toast-icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  border-radius: 50%;
}

.mc-toast--success .mc-toast-icon { background: var(--mc-success); color: #fff; }
.mc-toast--error   .mc-toast-icon { background: var(--mc-error); color: #fff; }
.mc-toast--info    .mc-toast-icon { background: var(--mc-primary); color: #fff; }

.mc-toast-msg { line-height: 1.4; }

.toast-enter-active { transition: all 0.25s ease; }
.toast-leave-active { transition: all 0.2s ease; }
.toast-enter-from   { opacity: 0; transform: translateX(40px); }
.toast-leave-to     { opacity: 0; transform: translateX(40px); }
</style>

