/** 用户角色 */
export type UserRole = 'STUDENT' | 'ADMIN' | 'REPAIRER'

/** 用户状态: 1=启用 0=禁用 */
export type UserStatus = 0 | 1

/** 报修状态: 1待受理→2已派单→3维修中→4待确认→5已完成→6已取消 */
export type RepairStatus = 1 | 2 | 3 | 4 | 5 | 6

// ==================== 用户 ====================

export interface UserInfo {
  id: number
  username: string
  realName: string
  studentNo?: string
  phone?: string
  gender?: number
  dormitoryBuilding?: string
  roomNo?: string
  avatar?: string
  role: UserRole
  status?: UserStatus
}

export interface LoginRequest {
  username: string
  password: string
  captchaKey: string
  captchaCode: string
}

export interface LoginResponse {
  token: string
  userInfo: UserInfo
}

export interface RegisterRequest {
  username: string
  password: string
  realName: string
  studentNo?: string
  phone?: string
  gender?: number
  dormitoryBuilding?: string
  roomNo?: string
  captchaKey: string
  captchaCode: string
}

export interface CaptchaResponse {
  captchaKey: string
  captchaImage: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

export interface UserProfileUpdateRequest {
  realName: string
  phone?: string
  gender?: number
  dormitoryBuilding?: string
  roomNo?: string
  avatar?: string
}

export interface UserQueryParams {
  pageNum?: number
  pageSize?: number
  realName?: string
  studentNo?: string
  status?: UserStatus
  role?: UserRole
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

// ==================== 报修工单 ====================

export interface RepairOrder {
  id: number
  orderNo: string
  userId: number
  categoryId: number
  handlerId?: number
  workerId?: number
  title: string
  content: string
  description?: string
  imageUrl?: string
  repairStatus: RepairStatus
  urgency?: string
  submitTime: string
  acceptTime?: string
  assignTime?: string
  workerAcceptTime?: string
  handleTime?: string
  workerCompleteTime?: string
  finishTime?: string
  studentConfirmTime?: string
  cancelTime?: string
  remark?: string
  processRemark?: string
  createTime?: string
  updateTime?: string
  // 关联字段（由后端填充）
  categoryName?: string
  username?: string
  realName?: string
  studentNo?: string
  phone?: string
  dormitoryBuilding?: string
  roomNo?: string
  avatar?: string
  handlerName?: string
  workerName?: string
  workerPhone?: string
}

export interface RepairOrderCreateRequest {
  categoryId: number
  title: string
  content: string
  imageUrl?: string
  urgency?: string
  aiPriorityScore?: number
  aiImpactScope?: number
}

export interface RepairOrderQueryParams {
  pageNum?: number
  pageSize?: number
  categoryId?: number
  repairStatus?: RepairStatus
  keyword?: string
  submitTime?: string
}

export interface RepairOrderStatusRequest {
  repairStatus: RepairStatus
  remark?: string
}

// ==================== 报修分类 ====================

export interface RepairCategory {
  id: number
  categoryName: string
  description?: string
  sortNum: number
  status: number
  createTime?: string
}

export interface CategorySaveRequest {
  id?: number
  categoryName: string
  description?: string
  sortNum: number
  status: number
}

// ==================== 评价反馈 ====================

export interface RepairFeedback {
  id: number
  repairOrderId: number
  userId: number
  score: number
  content?: string
  createTime?: string
  // 关联字段
  orderTitle?: string
  orderNo?: string
  realName?: string
  username?: string
}

export interface RepairFeedbackCreateRequest {
  repairOrderId: number
  score: number
  content?: string
}

// ==================== 通知公告 ====================

export interface Notice {
  id: number
  title: string
  content: string
  status: number
  createTime?: string
  updateTime?: string
}

export interface NoticeSaveRequest {
  id?: number
  title: string
  content: string
  status: number
}

// ==================== AI ====================

export interface AiClassifyRequest {
  title: string
  description: string
}

export interface AiClassifyResponse {
  category: string
  categoryId: number
  categoryName: string
  urgency: string
  priorityScore: number
  impactScope: number
  confidence: number
  reason: string
  suggestion?: string
  autoApplied: boolean
}

export interface WorkerRanking {
  workerId: number
  workerName: string
  matchScore: number
  breakdown: Record<string, number>
  reason: string
}

export interface AiRecommendResponse {
  autoAssigned: boolean
  assignedWorkerId?: number
  rankings: WorkerRanking[]
}

export interface RiskFactor {
  signal: string
  score: number
  detail: string
}

export interface EvaluationResponse {
  riskScore: number
  riskLevel: string
  decision: 'AUTO_ACCEPT' | 'NOTIFY_STUDENT' | 'REQUIRE_REVIEW'
  riskFactors: RiskFactor[]
  suggestion: string
}

export interface NaturalRepairResponse {
  building: string
  room: string
  repairType: string
  categoryId: number
  categoryName: string
  description: string
  urgencyLevel: string
  priorityScore: number
  reason: string
  confidence: number
}

export interface InsightRequest {
  timeRange: 'today' | 'this_week' | 'this_month'
  dimensions?: string[]
}

export interface InsightHighlight {
  type: 'anomaly' | 'trend' | 'efficiency' | 'bottleneck' | 'good'
  severity: 'warning' | 'info' | 'good'
  title: string
  detail: string
  relatedOrderIds?: number[]
}

export interface InsightResponse {
  summary: string
  highlights: InsightHighlight[]
  charts: Record<string, any>
}

// ==================== 统计 ====================

export interface StatisticsOverview {
  totalOrders: number
  pendingOrders: number
  processingOrders: number
  completedOrders: number
  cancelledOrders: number
}

// ==================== 操作日志 ====================

export interface OperationLog {
  id: number
  operatorId: number
  operatorName: string
  operationType: string
  description: string
  methodName: string
  requestParams: string
  ipAddress: string
  executionTime: number
  status: number
  errorMessage?: string
  createTime: string
}
