import { defineStore } from 'pinia'
import type { UserInfo, UserRole } from '../types/models'

const USER_STORAGE_KEY = 'user'
const TOKEN_STORAGE_KEY = 'token'

const loadStoredUser = (): UserInfo | null => {
  const rawUser = localStorage.getItem(USER_STORAGE_KEY)

  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser) as UserInfo
  } catch {
    localStorage.removeItem(USER_STORAGE_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: loadStoredUser(),
    token: localStorage.getItem(TOKEN_STORAGE_KEY) || ''
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    role: (state): UserRole | undefined => state.user?.role
  },
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem(TOKEN_STORAGE_KEY, token)
    },
    setUser(user: UserInfo) {
      this.user = user
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      localStorage.removeItem(USER_STORAGE_KEY)
    }
  }
})
