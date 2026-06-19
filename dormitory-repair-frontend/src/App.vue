<script setup lang="ts">
import { RouterView } from 'vue-router'
import UiToast from './components/ui/UiToast.vue'
import UiConfirm from './components/ui/UiConfirm.vue'
</script>

<template>
  <RouterView />
  <UiToast />
  <UiConfirm />
</template>

<style>
/* Global styles are imported in main.ts */
</style>
