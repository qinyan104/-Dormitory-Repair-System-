import http from './http'
import type {
  RepairOrder,
  RepairOrderCreateRequest,
  RepairOrderQueryParams,
  RepairOrderStatusRequest,
  RepairCategory,
  CategorySaveRequest,
  RepairFeedback,
  RepairFeedbackCreateRequest,
  PageResult,
  UserInfo
} from '../types/models'

// ==================== 学生端 ====================

export const createRepairApi = (data: RepairOrderCreateRequest) => {
  return http.post<RepairOrder>('/repair-order', data)
}

export const getRepairListApi = (params: RepairOrderQueryParams) => {
  return http.get<PageResult<RepairOrder>>('/repair-order/page', { params })
}

export const getMyRepairListApi = (params: RepairOrderQueryParams) => {
  return http.get<PageResult<RepairOrder>>('/repair-order/my-page', { params })
}

export const getRepairDetailApi = (id: number | string) => {
  return http.get<RepairOrder>(`/repair-order/${id}`)
}

export const cancelRepairApi = (id: number | string) => {
  return http.put<void>(`/repair-order/cancel/${id}`)
}

export const studentConfirmApi = (id: number | string) => {
  return http.put<void>(`/repair-order/student-confirm/${id}`)
}

export const getRepairFeedbackApi = (id: number | string) => {
  return http.get<RepairFeedback>(`/repair-feedback/${id}`)
}

export const createFeedbackApi = (data: RepairFeedbackCreateRequest) => {
  return http.post<RepairFeedback>('/repair-feedback', data)
}

// ==================== 分类管理 ====================

export const getCategoriesApi = () => {
  return http.get<RepairCategory[]>('/category/list')
}

export const getCategoryPageApi = (params: { pageNum?: number; pageSize?: number; categoryName?: string; status?: number }) => {
  return http.get<PageResult<RepairCategory>>('/category/page', { params })
}

export const createCategoryApi = (data: CategorySaveRequest) => {
  return http.post<RepairCategory>('/category', data)
}

export const updateCategoryApi = (_id: number | string, data: CategorySaveRequest) => {
  return http.put<RepairCategory>('/category', data)
}

export const deleteCategoryApi = (id: number | string) => {
  return http.delete<void>(`/category/${id}`)
}

export const updateCategoryStatusApi = (id: number | string, status: number) => {
  return http.put<void>(`/category/${id}/status?status=${status}`)
}

// ==================== 管理员端 ====================

export const acceptRepairApi = (id: number | string) => {
  return http.put<{ autoAssigned?: boolean; workerName?: string; rankings?: any[] }>(`/repair-order/accept/${id}`)
}

export const updateRepairStatusApi = (id: number | string, data: RepairOrderStatusRequest) => {
  return http.put<void>(`/repair-order/status/${id}`, {
    repairStatus: data.repairStatus,
    remark: data.remark
  })
}

export const assignWorkerApi = (id: number | string, workerId: number) => {
  return http.put<void>(`/repair-order/assign/${id}`, { workerId })
}

export const getRepairerListApi = () => {
  return http.get<PageResult<UserInfo>>('/user/page', { params: { role: 'REPAIRER', pageSize: 100 } })
}

// ==================== 维修人员端 ====================

export const getWorkerOrdersApi = (params: RepairOrderQueryParams) => {
  return http.get<PageResult<RepairOrder>>('/repair-order/worker-page', { params })
}

export const workerAcceptApi = (id: number | string) => {
  return http.put<void>(`/repair-order/worker-accept/${id}`)
}

export const workerCompleteApi = (id: number | string, remark?: string) => {
  return http.put<void>(`/repair-order/worker-complete/${id}`, { remark })
}

// ==================== Excel 导出 ====================

export const exportRepairOrdersApi = async (params: RepairOrderQueryParams) => {
  const response = await http.get<Blob, Blob>('/repair-order/export', {
    params,
    responseType: 'blob'
  })
  const url = window.URL.createObjectURL(new Blob([response]))
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', '报修工单.xlsx')
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
