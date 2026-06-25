import http from './http'
import type {
  AiClassifyRequest, AiClassifyResponse,
  AiRecommendResponse, EvaluationResponse,
  InsightRequest, InsightResponse,
  NaturalRepairResponse,
  RepairChatRequest,
  RepairChatResponse
} from '../types/models'

export const aiClassifyApi = (data: AiClassifyRequest) => {
  return http.post<AiClassifyResponse, AiClassifyResponse>('/ai/classify', data)
}

export const aiRecommendWorkerApi = (orderId: number) => {
  return http.get<AiRecommendResponse, AiRecommendResponse>('/ai/recommend-worker', { params: { orderId } })
}

export const aiEvaluateCompletionApi = (orderId: number) => {
  return http.post<EvaluationResponse, EvaluationResponse>('/ai/evaluate-completion', null, { params: { orderId } })
}

export const aiInsightsApi = (data: InsightRequest) => {
  return http.post<InsightResponse, InsightResponse>('/ai/insights', data)
}

export const aiNaturalRepairApi = (text: string) => {
  return http.post<NaturalRepairResponse, NaturalRepairResponse>('/ai/natural-repair', { text })
}

export const aiRepairChatApi = (data: RepairChatRequest) => {
  return http.post<RepairChatResponse, RepairChatResponse>('/ai/repair-chat', data)
}
