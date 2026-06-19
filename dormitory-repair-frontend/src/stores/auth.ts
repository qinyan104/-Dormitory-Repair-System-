import { defineStore } from 'pinia'
import type { UserInfo, UserRole } from '../types/models'

const USER_STORAGE_KEY = 'user'
const TOKEN_STORAGE_KEY = 'token'

const loadStoredUser = (): UserInfo | null => {
  const rawUser = sessionStorage.getItem(USER_STORAGE_KEY)

  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser) as UserInfo
  } catch {
    sessionStorage.removeItem(USER_STORAGE_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: loadStoredUser(),
    token: sessionStorage.getItem(TOKEN_STORAGE_KEY) || ''
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    role: (state): UserRole | undefined => state.user?.role
  },
  actions: {
    setToken(token: string) {
      this.token = token
      sessionStorage.setItem(TOKEN_STORAGE_KEY, token)
    },
    setUser(user: UserInfo) {
      this.user = user
      sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      sessionStorage.removeItem(TOKEN_STORAGE_KEY)
      sessionStorage.removeItem(USER_STORAGE_KEY)
    }
  }
})
