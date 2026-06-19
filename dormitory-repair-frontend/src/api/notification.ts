import http from './http'

export const getNotificationPageApi = (params: { pageNum?: number; pageSize?: number }) => {
  return http.get('/notification/page', { params })
}

export const getUnreadCountApi = () => {
  return http.get('/notification/unread-count')
}

export const markNotificationReadApi = (id: number) => {
  return http.put(`/notification/read/${id}`)
}

export const markAllNotificationsReadApi = () => {
  return http.put('/notification/read-all')
}
