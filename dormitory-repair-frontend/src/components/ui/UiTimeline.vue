<script setup lang="ts">
interface TimelineEvent {
  label: string
  time?: string
  description?: string
  status?: 'pending' | 'active' | 'success' | 'error'
}

defineProps<{
  events: TimelineEvent[]
}>()
</script>

<template>
  <div class="ui-timeline">
    <div v-for="(event, idx) in events" :key="idx" class="timeline-item" :class="[event.status ? `is-${event.status}` : '']">
      <div class="timeline-line" v-if="idx !== events.length - 1"></div>
      <div class="timeline-dot"></div>
      <div class="timeline-content">
        <div class="timeline-header">
          <span class="timeline-label">{{ event.label }}</span>
          <span class="timeline-time" v-if="event.time">{{ event.time }}</span>
        </div>
        <div class="timeline-desc" v-if="event.description">{{ event.description }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ui-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.timeline-item {
  position: relative;
  display: flex;
  gap: 20px;
  padding-bottom: 24px;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-line {
  position: absolute;
  left: 5px;
  top: 12px;
  bottom: -12px;
  width: 2px;
  background-color: var(--mc-hairline);
  z-index: 1;
}

.timeline-dot {
  position: relative;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: var(--mc-hairline);
  margin-top: 6px;
  z-index: 2;
  flex-shrink: 0;
  border: 2px solid var(--mc-white);
}

.timeline-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.timeline-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--mc-ink);
}

.timeline-time {
  font-size: 12px;
  color: var(--mc-muted);
}

.timeline-desc {
  font-size: 13px;
  color: var(--mc-muted);
  line-height: 1.4;
}

/* Status variants */
.is-active .timeline-dot {
  background-color: var(--mc-ink);
  box-shadow: 0 0 0 4px rgba(0, 0, 0, 0.05);
}

.is-success .timeline-dot {
  background-color: var(--mc-success);
}

.is-pending .timeline-dot {
  background-color: var(--mc-muted-soft);
}

.is-success .timeline-label {
  color: var(--mc-success);
}
</style>
