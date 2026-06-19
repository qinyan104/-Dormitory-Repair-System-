<script setup lang="ts">
interface Props {
  type?: 'primary' | 'secondary' | 'signal' | 'dark' | 'circle'
  disabled?: boolean
  loading?: boolean
}

withDefaults(defineProps<Props>(), {
  type: 'primary',
  disabled: false,
  loading: false
})
</script>

<template>
  <button
    class="mc-button"
    :class="[`mc-button--${type}`, { 'is-loading': loading }]"
    :disabled="disabled || loading"
  >
    <span v-if="loading" class="mc-button__loader"></span>
    <slot v-else></slot>
  </button>
</template>

<style scoped>
.mc-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--mc-font-family);
  transition: transform 0.15s ease, opacity 0.15s ease, box-shadow 0.15s ease;
  cursor: pointer;
  white-space: nowrap;
}

.mc-button:active:not(:disabled) {
  transform: scale(0.98);
}

.mc-button--primary {
  background-color: var(--mc-ink);
  color: var(--mc-canvas);
  border: 1.5px solid var(--mc-ink);
  border-radius: var(--mc-radius-btn);
  padding: 6px 24px;
  font-size: var(--mc-fz-btn);
  font-weight: var(--mc-fw-btn);
  letter-spacing: var(--mc-ls-btn);
  height: auto;
  min-height: 40px;
}

.mc-button--secondary {
  background-color: var(--mc-white);
  color: var(--mc-ink);
  border: 1.5px solid var(--mc-ink);
  border-radius: var(--mc-radius-btn);
  padding: 6px 24px;
  font-size: var(--mc-fz-btn);
  font-weight: var(--mc-fw-body); /* 450/400 */
  line-height: 20.8px;
  height: auto;
  min-height: 40px;
}

.mc-button--signal {
  background-color: var(--mc-signal);
  color: var(--mc-white);
  border: none;
  border-radius: var(--mc-radius-consent);
  padding: 1px 30px;
  font-size: 13px;
  font-weight: var(--mc-fw-body);
  letter-spacing: 0.13px;
  min-height: 32px;
}

.mc-button--dark {
  background-color: var(--mc-ink);
  color: var(--mc-white);
  border: 1.5px solid var(--mc-white);
  border-radius: var(--mc-radius-btn);
  padding: 6px 24px;
  font-size: var(--mc-fz-btn);
  font-weight: var(--mc-fw-btn);
  min-height: 40px;
}

.mc-button--circle {
  background-color: var(--mc-white);
  color: var(--mc-ink);
  border: 1px solid var(--mc-ink);
  border-radius: var(--mc-radius-circle);
  width: 48px;
  height: 48px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.mc-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
}

.mc-button__loader {
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-bottom-color: transparent;
  border-radius: 50%;
  animation: rotation 1s linear infinite;
}
</style>

