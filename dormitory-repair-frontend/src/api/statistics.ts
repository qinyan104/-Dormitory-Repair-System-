import http from './http'

export const getStatisticsSummaryApi = () => {
  return http.get('/statistics/summary')
}

export const getStatisticsCategoryApi = () => {
  return http.get('/statistics/category')
}

export const getStatisticsStatusApi = () => {
  return http.get('/statistics/status')
}
