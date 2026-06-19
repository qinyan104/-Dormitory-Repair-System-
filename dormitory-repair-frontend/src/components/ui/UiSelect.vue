<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

interface Option {
  label: string
  value: string | number
}

interface Props {
  modelValue: string | number
  options: Option[]
  label?: string
  placeholder?: string
  disabled?: boolean
  error?: string
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue'])

const isOpen = ref(false)
const selectRef = ref<HTMLElement | null>(null)
const dropdownStyle = ref({})

const updateDropdownPosition = () => {
  if (!selectRef.value) return
  const rect = selectRef.value.getBoundingClientRect()
  dropdownStyle.value = {
    position: 'fixed',
    top: `${rect.bottom + 8}px`,
    left: `${rect.left}px`,
    width: `${rect.width}px`,
    zIndex: 9999
  }
}

const toggleDropdown = () => {
  if (props.disabled) return
  if (!isOpen.value) {
    updateDropdownPosition()
    window.addEventListener('resize', updateDropdownPosition)
    document.addEventListener('scroll', updateDropdownPosition, true)
  } else {
    window.removeEventListener('resize', updateDropdownPosition)
    document.removeEventListener('scroll', updateDropdownPosition, true)
  }
  isOpen.value = !isOpen.value
}

const selectOption = (option: Option) => {
  emit('update:modelValue', option.value)
  closeDropdown()
}

const closeDropdown = () => {
  if (isOpen.value) {
    isOpen.value = false
    window.removeEventListener('resize', updateDropdownPosition)
    document.removeEventListener('scroll', updateDropdownPosition, true)
  }
}

const selectedLabel = computed(() => {
  const option = props.options.find(opt => opt.value === props.modelValue)
  return option ? option.label : ''
})

const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as Node
  // Check if click is outside both the select wrapper AND the teleported dropdown
  const dropdownEl = document.querySelector('.mc-select-dropdown.is-active-dropdown')
  if (
    selectRef.value && !selectRef.value.contains(target) &&
    (!dropdownEl || !dropdownEl.contains(target))
  ) {
    closeDropdown()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="mc-select-group" :class="{ 'has-error': error }" ref="selectRef">
    <label v-if="label" class="mc-label">{{ label }}</label>
    
    <div class="mc-select-wrapper" @click="toggleDropdown">
      <div class="mc-select-trigger" :class="{ 'is-open': isOpen, 'is-disabled': disabled }">
        <span v-if="!modelValue" class="placeholder">{{ placeholder || '请选择' }}</span>
        <span v-else class="selected-text">{{ selectedLabel }}</span>
        
        <svg class="mc-select-chevron" :class="{ 'is-rotated': isOpen }" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="6 9 12 15 18 9"/>
        </svg>
      </div>

      <Teleport to="body">
        <Transition name="dropdown">
          <div v-if="isOpen" class="mc-select-dropdown is-active-dropdown" :style="dropdownStyle">
            <ul class="mc-select-options">
              <li 
                v-for="opt in options" 
                :key="opt.value"
                class="mc-select-option"
                :class="{ 'is-active': opt.value === modelValue }"
                @click.stop="selectOption(opt)"
              >
                {{ opt.label }}
                <svg v-if="opt.value === modelValue" class="check-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
              </li>
            </ul>
          </div>
        </Transition>
      </Teleport>
    </div>
    <span v-if="error" class="mc-error-text">{{ error }}</span>
  </div>
</template>

<style scoped>
.mc-select-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  position: relative;
}

.mc-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--mc-muted);
  margin-left: 2px;
  font-family: var(--mc-font-family);
}

.mc-select-wrapper {
  position: relative;
}

.mc-select-trigger {
  width: 100%;
  padding: 10px 48px 10px 24px;
  height: 48px;
  background-color: var(--mc-white);
  border: 1px solid var(--mc-hairline);
  border-radius: var(--mc-radius-pill);
  font-size: var(--mc-fz-body);
  font-family: var(--mc-font-family);
  color: var(--mc-ink);
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: all var(--mc-transition);
  user-select: none;
}

.mc-select-trigger:hover:not(.is-disabled) {
  border-color: var(--mc-ink);
}

.mc-select-trigger.is-open {
  border-color: var(--mc-ink);
  box-shadow: 0 0 0 3px var(--mc-hairline-soft);
}

.mc-select-trigger.is-disabled {
  background-color: var(--mc-lifted);
  color: var(--mc-muted);
  cursor: not-allowed;
}

.placeholder {
  color: var(--mc-muted-soft);
}

.selected-text {
  color: var(--mc-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mc-select-chevron {
  position: absolute;
  right: 24px;
  color: var(--mc-muted);
  transition: transform var(--mc-transition);
}

.mc-select-chevron.is-rotated {
  transform: rotate(180deg);
}

/* Dropdown Menu - now teleported so positioned fixed */
.mc-select-dropdown {
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: var(--mc-radius-md);
  box-shadow: var(--mc-shadow-card);
  border: 1px solid var(--mc-hairline-soft);
  overflow: hidden;
}

.mc-select-options {
  list-style: none;
  margin: 0;
  padding: 8px;
  max-height: 240px;
  overflow-y: auto;
}

.mc-select-options::-webkit-scrollbar {
  width: 6px;
}

.mc-select-options::-webkit-scrollbar-thumb {
  background-color: var(--mc-hairline);
  border-radius: 4px;
}

.mc-select-option {
  padding: 12px 16px;
  border-radius: var(--mc-radius-sm);
  font-size: 14px;
  color: var(--mc-charcoal);
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all var(--mc-transition);
}

.mc-select-option:hover {
  background-color: var(--mc-canvas);
  color: var(--mc-ink);
}

.mc-select-option.is-active {
  background-color: var(--mc-ink);
  color: var(--mc-white);
  font-weight: 500;
}

.check-icon {
  color: var(--mc-white);
}

/* Transitions */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  transform-origin: top;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px) scaleY(0.95);
}

.has-error .mc-select-trigger {
  border-color: var(--mc-error);
}

.has-error .mc-select-trigger.is-open {
  box-shadow: 0 0 0 3px rgba(235, 0, 27, 0.15);
}

.mc-error-text {
  font-size: 12px;
  color: var(--mc-error);
  margin-top: 6px;
  margin-left: 2px;
}
</style>

