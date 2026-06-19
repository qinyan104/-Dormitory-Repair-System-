import http from './http'
import type { LoginRequest, RegisterRequest, LoginResponse, CaptchaResponse, UserInfo } from '../types/models'

export const loginApi = (data: LoginRequest) => {
  return http.post<LoginResponse>('/auth/login', data)
}

export const registerApi = (data: RegisterRequest) => {
  return http.post<void>('/auth/register', data)
}

export const getMeApi = () => {
  return http.get<UserInfo>('/auth/me')
}

export const logoutApi = () => {
  return http.post<void>('/auth/logout')
}

export const getCaptchaApi = () => {
  return http.get<CaptchaResponse>('/auth/captcha')
}
