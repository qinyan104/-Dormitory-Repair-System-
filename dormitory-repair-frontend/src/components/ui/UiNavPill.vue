<script setup lang="ts">
import { RouterLink } from 'vue-router'

interface NavItem {
  label: string
  to: string
}

interface Props {
  items: NavItem[]
}

defineProps<Props>()
</script>

<template>
  <header class="mc-top-nav">
    <div class="nav-container">
      <div class="nav-brand">
        <div class="brand-circles">
          <div class="circle-left"></div>
          <div class="circle-right"></div>
        </div>
        <span class="brand-name">宿舍报修</span>
      </div>
      
      <nav class="nav-links">
        <RouterLink
          v-for="item in items"
          :key="item.to"
          :to="item.to"
          class="nav-link"
          active-class="is-active"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="nav-actions">
        <slot name="actions"></slot>
      </div>
    </div>
  </header>
</template>

<style scoped>
.mc-top-nav {
  position: sticky;
  top: 56px;
  z-index: 1000;
  width: 100%;
  display: flex;
  justify-content: center;
  pointer-events: none;
}

.nav-container {
  pointer-events: auto;
  background-color: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 100px; /* Perfect pill shape */
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
  padding: 10px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1000px; /* Slightly narrower for a more premium look */
  width: calc(100% - 48px);
  margin: 0 24px;
  transition: all var(--mc-transition);
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1; /* Pushes links to center */
}

.brand-circles {
  display: flex;
  align-items: center;
  width: 32px;
  height: 20px;
  position: relative;
}

.circle-left, .circle-right {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  position: absolute;
}
.circle-left {
  background-color: #EB001B;
  left: 0;
  z-index: 1;
}
.circle-right {
  background-color: #F79E1B;
  right: 0;
  z-index: 2;
  opacity: 0.8;
}

.brand-name {
  font-family: var(--mc-font-display);
  font-size: 16px;
  font-weight: 500;
  color: var(--mc-ink);
  letter-spacing: -0.02em;
}

.nav-links {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.nav-link {
  font-size: 14px;
  font-weight: 500;
  color: var(--mc-charcoal);
  padding: 8px 16px;
  border-radius: 100px;
  transition: all 0.2s ease;
  font-family: var(--mc-font-family);
  text-decoration: none;
  letter-spacing: 0.02em;
}

.nav-link:hover {
  color: var(--mc-ink);
  background-color: rgba(0, 0, 0, 0.04);
}

.nav-link.is-active {
  color: var(--mc-white);
  background-color: var(--mc-ink);
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.nav-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex: 1; /* Balances the flex layout */
  gap: 16px;
}

@media (min-width: 768px) and (max-width: 1024px) {
  .nav-container {
    padding: 12px;
  }
  .brand-name {
    display: none;
  }
  .nav-links {
    gap: 4px;
  }
  .nav-link {
    padding: 6px 12px;
    font-size: 13px;
  }
}

</style>


