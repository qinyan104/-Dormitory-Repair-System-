import http from './http'
import type {
  UserInfo,
  UserProfileUpdateRequest,
  UserQueryParams,
  ChangePasswordRequest,
  PageResult
} from '../types/models'

export const getUserProfileApi = () => {
  return http.get<UserInfo>('/user/profile')
}

export const updateUserProfileApi = (data: UserProfileUpdateRequest) => {
  return http.put<UserInfo>('/user/profile', data)
}

export const getUserListApi = (params: UserQueryParams) => {
  return http.get<PageResult<UserInfo>>('/user/page', { params })
}

export const updateUserStatusApi = (id: number | string, status: number) => {
  return http.put<void>(`/user/status/${id}`, { status })
}

export const resetUserPasswordApi = (id: number | string) => {
  return http.post<void>(`/user/${id}/reset-password`)
}

export const changePasswordApi = (data: ChangePasswordRequest) => {
  return http.post<void>('/user/change-password', data)
}
