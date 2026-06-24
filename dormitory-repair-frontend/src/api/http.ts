import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
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

let isRedirecting = false

// Request Interceptor: Inject Token
http.interceptors.request.use(
  (config) => {
    // Dynamically resolve base URL so server address changes take effect immediately
    config.baseURL = getApiBaseUrl()
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
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
        if (isRedirecting) return Promise.reject(new Error(res.message || 'Unauthorized'))
        isRedirecting = true
        const authStore = useAuthStore()
        authStore.logout()
        router.push('/login').finally(() => { isRedirecting = false })
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }

    return res.data
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      if (isRedirecting) return Promise.reject(error)
      isRedirecting = true
      const authStore = useAuthStore()
      authStore.logout()
      router.push('/login').finally(() => { isRedirecting = false })
    }
    return Promise.reject(error)
  }
)

export default http
