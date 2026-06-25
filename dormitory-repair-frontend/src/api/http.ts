import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '../stores/auth'
import { getApiBaseUrl } from '../utils/serverConfig'
import router from '../router'

const http: AxiosInstance = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

interface AuthAwareRequestConfig extends InternalAxiosRequestConfig {
  _authToken?: string
}

let isRedirecting = false

const isPublicAuthRequest = (url?: string) => {
  if (!url) return false
  return [
    '/auth/login',
    '/auth/register',
    '/auth/captcha',
    '/auth/forgot-password'
  ].some(path => url.includes(path))
}

const handleUnauthorized = (config?: AuthAwareRequestConfig) => {
  if (isPublicAuthRequest(config?.url)) return

  const authStore = useAuthStore()
  const requestToken = config?._authToken
  if (requestToken && authStore.token && requestToken !== authStore.token) {
    return
  }

  if (isRedirecting) return
  isRedirecting = true
  authStore.logout()
  router.push('/login').finally(() => { isRedirecting = false })
}

// Request Interceptor: Inject Token
http.interceptors.request.use(
  (config) => {
    // Dynamically resolve base URL so server address changes take effect immediately
    config.baseURL = getApiBaseUrl()
    const authStore = useAuthStore()
    const token = authStore.token
    ;(config as AuthAwareRequestConfig)._authToken = token
    if (token && !isPublicAuthRequest(config.url)) {
      config.headers.Authorization = `Bearer ${token}`
    } else {
      delete config.headers.Authorization
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response Interceptor: Handle unwrap and errors
http.interceptors.response.use(
  (response: AxiosResponse) => {
    const responseType = response.config.responseType
    if (responseType === 'blob' || responseType === 'arraybuffer') {
      return response.data
    }

    const res = response.data

    if (res.code !== 200) {
      if (res.code === 401) {
        handleUnauthorized(response.config as AuthAwareRequestConfig)
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }

    return res.data
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      handleUnauthorized(error.config as AuthAwareRequestConfig | undefined)
    }
    return Promise.reject(error)
  }
)

export default http
