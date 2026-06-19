<script setup lang="ts">
interface Props {
  variant?: 'canvas' | 'lifted' | 'white' | 'dark'
  radius?: 'lg' | 'pill' | 'circle' | 'sm'
  padding?: string
  elevated?: boolean
}

withDefaults(defineProps<Props>(), {
  variant: 'white',
  radius: 'lg',
  padding: '40px',
  elevated: false
})
</script>

<template>
  <div class="mc-card" :class="[`mc-card--${variant}`, `mc-card--radius-${radius}`, { 'is-elevated': elevated }]" :style="{ padding }">
    <slot></slot>
  </div>
</template>

<style scoped>
.mc-card {
  overflow: hidden;
  position: relative;
  border: 1px solid transparent;
}

.mc-card--radius-lg { border-radius: var(--mc-radius-lg); }
.mc-card--radius-pill { border-radius: var(--mc-radius-pill); }
.mc-card--radius-circle { border-radius: var(--mc-radius-circle); }
.mc-card--radius-sm { border-radius: var(--mc-radius-sm); }

.is-elevated {
  box-shadow: var(--mc-shadow-card);
  border: none;
}

.mc-card--white { background-color: var(--mc-white); }
.mc-card--canvas { background-color: var(--mc-canvas); }
.mc-card--lifted { background-color: var(--mc-lifted); }

.mc-card--dark {
  background-color: var(--mc-ink);
  color: var(--mc-white);
}
</style>

