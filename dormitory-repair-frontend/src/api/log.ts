import http from './http'

export const getOperationLogsApi = (params: Record<string, any>) => {
  return http.get('/log/page', { params })
}
