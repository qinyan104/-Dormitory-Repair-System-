<script setup lang="ts">
interface Props {
  modelValue: string
  label?: string
  placeholder?: string
  rows?: number
  disabled?: boolean
}

defineProps<Props>()
defineEmits(['update:modelValue'])
</script>

<template>
  <div class="mc-textarea-group">
    <label v-if="label" class="mc-label">{{ label }}</label>
    <textarea
      :value="modelValue"
      :placeholder="placeholder"
      :rows="rows || 4"
      :disabled="disabled"
      class="mc-textarea"
      @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
    ></textarea>
  </div>
</template>

<style scoped>
.mc-textarea-group {
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

.mc-textarea {
  width: 100%;
  padding: 16px 24px;
  background-color: var(--mc-white);
  border: 1px solid var(--mc-hairline);
  border-radius: 20px;
  font-size: var(--mc-fz-body);
  font-family: var(--mc-font-family);
  color: var(--mc-ink);
  outline: none;
  resize: vertical;
  transition: all var(--mc-transition);
  line-height: var(--mc-lh-body);
}

.mc-textarea::placeholder {
  color: var(--mc-muted-soft);
}

.mc-textarea:focus {
  border-color: var(--mc-ink);
  box-shadow: 0 0 0 2px var(--mc-hairline);
}

.mc-textarea:disabled {
  background-color: var(--mc-lifted);
  color: var(--mc-muted);
  cursor: not-allowed;
}
</style>

