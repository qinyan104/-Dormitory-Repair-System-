<script setup lang="ts">
import { useRoute } from 'vue-router'

interface TabItem {
  label: string
  to: string
  icon: string  // SVG markup
}

defineProps<{
  items: TabItem[]
}>()

const route = useRoute()
</script>

<template>
  <nav class="ui-tab-bar">
    <RouterLink
      v-for="item in items"
      :key="item.to"
      :to="item.to"
      :class="['tab-item', { 'is-active': route.path === item.to || route.path.startsWith(item.to + '/') }]"
    >
      <span class="tab-icon" v-html="item.icon"></span>
      <span class="tab-label">{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.ui-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  display: flex;
  align-items: stretch;
  height: 64px;
  padding-bottom: env(safe-area-inset-bottom, 0px);
  background-color: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 -2px 16px rgba(0, 0, 0, 0.04);
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  text-decoration: none;
  color: var(--mc-muted, #696969);
  transition: color 0.2s ease;
  -webkit-tap-highlight-color: transparent;
  padding: 8px 0;
  min-height: 48px;
}

.tab-item:active {
  opacity: 0.7;
}

.tab-item.is-active {
  color: var(--mc-ink, #141413);
  position: relative;
}

.tab-item.is-active::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  border-radius: 2px;
  background-color: var(--mc-signal, #CF4500);
  animation: tabIndicatorIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes tabIndicatorIn {
  from { opacity: 0; transform: translateX(-50%) scaleX(0); }
  to { opacity: 1; transform: translateX(-50%) scaleX(1); }
}

.tab-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

.tab-icon :deep(svg) {
  width: 22px;
  height: 22px;
  transition: transform 0.2s ease;
}

.tab-item.is-active .tab-icon :deep(svg) {
  transform: scale(1.08);
}

.tab-label {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.02em;
  line-height: 1;
  font-family: var(--mc-font-family, sans-serif);
}

.tab-item.is-active .tab-label {
  font-weight: 700;
}
</style>
