import { ref, onMounted, onUnmounted } from 'vue'

const MOBILE_BREAKPOINT = 768

/**
 * One-shot mobile detection for route guards.
 * Returns true if viewport width < 768px.
 * Falls back to false in SSR / test environments.
 */
export function isMobileDevice(): boolean {
  if (typeof window === 'undefined') return false
  return window.innerWidth < MOBILE_BREAKPOINT
}

/**
 * Composable for reactive mobile detection inside components.
 * Returns a reactive ref that updates on window resize.
 */
/**
 * Get the correct repair detail route for current device.
 * Desktop: /student/repair/detail/:id  |  Mobile: /student/m/detail/:id
 */
export function repairDetailPath(id: number | string): string {
  return isMobileDevice() ? `/student/m/detail/${id}` : `/student/repair/detail/${id}`
}

/**
 * Get the correct repair feedback route for current device.
 * Desktop: /student/repair/feedback/:id  |  Mobile: /student/m/feedback/:id
 */
export function repairFeedbackPath(id: number | string): string {
  return isMobileDevice() ? `/student/m/feedback/${id}` : `/student/repair/feedback/${id}`
}

/**
 * Get the correct worker order detail route for current device.
 * Desktop: /repairer/orders/:id  |  Mobile: /repairer/m/orders/:id
 */
export function workerOrderDetailPath(id: number | string): string {
  return isMobileDevice() ? `/repairer/m/orders/${id}` : `/repairer/orders/${id}`
}

/**
 * Get the correct worker order list route for current device.
 * Desktop: /repairer/orders  |  Mobile: /repairer/m/orders
 */
export function workerOrderListPath(): string {
  return isMobileDevice() ? '/repairer/m/orders' : '/repairer/orders'
}

export function useIsMobile() {
  const isMobile = ref(typeof window !== 'undefined' && window.innerWidth < MOBILE_BREAKPOINT)

  const onResize = () => {
    isMobile.value = window.innerWidth < MOBILE_BREAKPOINT
  }

  onMounted(() => window.addEventListener('resize', onResize))
  onUnmounted(() => window.removeEventListener('resize', onResize))

  return { isMobile }
}
