<script setup lang="ts">
interface Props {
  modelValue: string | number
  label?: string
  placeholder?: string
  type?: string
  error?: string
  disabled?: boolean
}

defineProps<Props>()
defineEmits(['update:modelValue'])
</script>

<template>
  <div class="mc-input-group" :class="{ 'has-error': error }">
    <label v-if="label" class="mc-label">{{ label }}</label>
    <div class="mc-input-wrapper">
      <input
        :type="type || 'text'"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        class="mc-input"
        @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      />
    </div>
    <span v-if="error" class="mc-error-text">{{ error }}</span>
  </div>
</template>

<style scoped>
.mc-input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.mc-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--mc-muted);
  margin-left: 2px;
  font-family: var(--mc-font-family);
}

.mc-input-wrapper {
  position: relative;
}

.mc-input {
  width: 100%;
  padding: 10px 24px;
  height: 48px;
  background-color: var(--mc-white);
  border: 1px solid var(--mc-hairline);
  border-radius: var(--mc-radius-pill);
  font-size: var(--mc-fz-body);
  font-family: var(--mc-font-family);
  color: var(--mc-ink);
  transition: all var(--mc-transition);
  outline: none;
}

.mc-input::placeholder {
  color: var(--mc-muted-soft);
}

.mc-input:focus {
  border-color: var(--mc-ink);
  box-shadow: 0 0 0 3px var(--mc-hairline-soft);
}

.mc-input:disabled {
  background-color: var(--mc-lifted);
  color: var(--mc-muted);
  cursor: not-allowed;
}

.has-error .mc-input {
  border-color: var(--mc-error);
}

.has-error .mc-input:focus {
  box-shadow: 0 0 0 2px rgba(235, 0, 27, 0.15);
}

.mc-error-text {
  font-size: 12px;
  color: var(--mc-error);
  margin-left: 2px;
}
</style>

