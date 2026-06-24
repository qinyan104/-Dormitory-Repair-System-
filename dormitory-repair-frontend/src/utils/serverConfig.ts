import { Capacitor } from '@capacitor/core'

const STORAGE_KEY = 'DORM_SERVER_ADDRESS'

/** Default address from build-time env (only used as fallback in native mode) */
const ENV_BASE = import.meta.env.VITE_API_BASE_URL || 'http://10.0.2.2:8080/api'
const ENV_WS = import.meta.env.VITE_WS_URL || 'http://10.0.2.2:8080/ws'

/**
 * Read the user-configured server address from localStorage.
 * Returns e.g. "192.168.1.5:8080" or null if not set.
 */
export function getSavedServerAddress(): string | null {
  const val = localStorage.getItem(STORAGE_KEY)
  return val && val.trim() ? val.trim() : null
}

/**
 * Save the server address to localStorage.
 * @param address e.g. "192.168.1.5:8080" or "10.0.2.2:8080"
 */
export function saveServerAddress(address: string): void {
  localStorage.setItem(STORAGE_KEY, address.trim())
}

/**
 * Clear saved address (revert to .env defaults).
 */
export function clearServerAddress(): void {
  localStorage.removeItem(STORAGE_KEY)
}

/**
 * Build the full API base URL.
 * - Browser: always "/api" (Vite proxy handles it)
 * - Native: user-configured address, or .env fallback
 */
export function getApiBaseUrl(): string {
  if (!Capacitor.isNativePlatform()) return '/api'

  const saved = getSavedServerAddress()
  if (saved) return `http://${saved}/api`

  return ENV_BASE
}

/**
 * Build the full WebSocket URL.
 * - Browser: always "/ws" (Vite proxy handles it)
 * - Native: user-configured address, or .env fallback
 */
export function getWsUrl(): string {
  if (!Capacitor.isNativePlatform()) return '/ws'

  const saved = getSavedServerAddress()
  if (saved) return `http://${saved}/ws`

  return ENV_WS
}

/**
 * Whether the app is running inside Capacitor (Android/iOS).
 */
export function isNativeApp(): boolean {
  return Capacitor.isNativePlatform()
}
