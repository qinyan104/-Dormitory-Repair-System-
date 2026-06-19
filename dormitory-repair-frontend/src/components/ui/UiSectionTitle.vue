<script setup lang="ts">
interface Props {
  eyebrow?: string
  title: string
}

defineProps<Props>()
</script>

<template>
  <div class="mc-section-title">
    <div v-if="eyebrow" class="mc-eyebrow">
      <span class="mc-eyebrow-dot">&bull;</span>
      <span class="mc-eyebrow-text">{{ eyebrow }}</span>
    </div>
    <h2 class="mc-h2">{{ title }}</h2>
  </div>
</template>

<style scoped>
.mc-section-title {
  margin-bottom: 32px;
}

.mc-eyebrow {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.mc-eyebrow-dot {
  color: var(--mc-signal-light);
  font-size: 16px;
  line-height: 1;
}

.mc-eyebrow-text {
  font-size: var(--mc-fz-eyebrow);
  text-transform: uppercase;
  letter-spacing: var(--mc-ls-eyebrow);
  color: var(--mc-slate);
  font-weight: var(--mc-fw-eyebrow);
  font-family: var(--mc-font-family);
  line-height: 1;
}

.mc-h2 {
  font-size: var(--mc-fz-display-md);
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--mc-ink);
  line-height: var(--mc-lh-display-md);
  font-family: var(--mc-font-display);
}
</style>

