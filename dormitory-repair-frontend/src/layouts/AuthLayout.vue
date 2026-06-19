<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <RouterView v-slot="{ Component, route: currentRoute }">
    <Transition name="auth-route" mode="out-in">
      <component :is="Component" :key="currentRoute.path" />
    </Transition>
  </RouterView>
</template>

<style scoped>
.auth-route-enter-active,
.auth-route-leave-active {
  transition: opacity 0.24s ease;
}

.auth-route-enter-from,
.auth-route-leave-to {
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
  .auth-route-enter-active,
  .auth-route-leave-active {
    transition: none;
  }
}
</style>
