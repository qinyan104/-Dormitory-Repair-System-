<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  score: number
  showLabel?: boolean
  size?: 'small' | 'medium' | 'large'
}>()

const moodMap: Record<number, { emoji: string; label: string; color: string }> = {
  1: { emoji: '😠', label: '非常差', color: '#EB001B' },
  2: { emoji: '😞', label: '较差', color: '#FF5F00' },
  3: { emoji: '😐', label: '一般', color: '#F79E1B' },
  4: { emoji: '😊', label: '满意', color: '#0070FF' },
  5: { emoji: '😍', label: '非常满意', color: '#00AA6E' }
}

const currentMood = computed(() => moodMap[props.score] || moodMap[3])
</script>

<template>
  <div class="ui-star-display" :class="[`is-${size || 'medium'}`]">
    <div class="stars">
      <span
        v-for="star in 5"
        :key="star"
        class="star"
        :class="{ 'is-active': star <= score }"
      >
        ★
      </span>
    </div>
    <div class="mood-badge" v-if="showLabel">
      <span class="emoji">{{ currentMood.emoji }}</span>
      <span class="label">{{ currentMood.label }}</span>
    </div>
  </div>
</template>

<style scoped>
.ui-star-display {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.stars {
  display: flex;
  gap: 2px;
}

.star {
  color: var(--mc-hairline);
  line-height: 1;
}

.star.is-active {
  color: var(--mc-accent-amber);
}

.mood-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  background-color: var(--mc-canvas);
  padding: 4px 10px;
  border-radius: var(--mc-radius-pill);
  font-size: 13px;
  font-weight: 600;
  color: var(--mc-ink);
}

/* Sizes */
.is-small .star { font-size: 14px; }
.is-small .mood-badge { font-size: 11px; padding: 2px 6px; }

.is-medium .star { font-size: 20px; }

.is-large .star { font-size: 32px; }
.is-large .mood-badge { font-size: 16px; padding: 6px 16px; }
</style>
