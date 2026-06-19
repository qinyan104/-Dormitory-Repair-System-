import http from './http'

export const getNoticeListApi = (params: any) => {
  return http.get('/notice/page', { params })
}

export const getNoticeDetailApi = (id: number | string) => {
  return http.get(`/notice/${id}`)
}

export const createNoticeApi = (data: any) => {
  return http.post('/notice', data)
}

export const updateNoticeApi = (_id: number | string, data: any) => {
  return http.put('/notice', data)
}

export const deleteNoticeApi = (id: number | string) => {
  return http.delete(`/notice/${id}`)
}
