import { http } from './http'
import type { JsonObject } from './types'

// ==================== 智能审批 ====================
export function getSmartApproverRecommendation(roleCode: string, businessType?: string, amount?: number) {
  const params = new URLSearchParams({ roleCode })
  if (businessType) params.append('businessType', businessType)
  if (amount) params.append('amount', amount.toString())
  return http.get<unknown, JsonObject[]>(`/workflow/smart-approvals/recommend?${params}`)
}

// ==================== 异常检测 ====================
export function detectAnomalies(userId: number) {
  return http.get<unknown, JsonObject[]>(`/workflow/anomalies?userId=${userId}`)
}

export function detectMyAnomalies() {
  return http.get<unknown, JsonObject[]>('/workflow/anomalies/self')
}

// ==================== 流程分析 ====================
export function getBottleneckAnalysis(startTime?: string, endTime?: string) {
  const params = new URLSearchParams()
  if (startTime) params.append('startTime', startTime)
  if (endTime) params.append('endTime', endTime)
  return http.get<unknown, JsonObject>(`/workflow/analytics/bottlenecks?${params}`)
}

export function getDepartmentEfficiency(days: number = 30) {
  return http.get<unknown, JsonObject>(`/workflow/analytics/efficiency?days=${days}`)
}

// ==================== 智能表单 ====================
export function getSupplierSuggestions(userId: number, keyword?: string) {
  const params = new URLSearchParams({ userId: userId.toString() })
  if (keyword) params.append('keyword', keyword)
  return http.get<unknown, JsonObject[]>(`/smart-form/suppliers?${params}`)
}

export function getCategorySuggestions(userId: number, keyword?: string) {
  const params = new URLSearchParams({ userId: userId.toString() })
  if (keyword) params.append('keyword', keyword)
  return http.get<unknown, string[]>(`/smart-form/categories?${params}`)
}

export function getExpenseTemplates(userId: number) {
  return http.get<unknown, JsonObject[]>(`/smart-form/templates?userId=${userId}`)
}

// ==================== 摘要生成 ====================
export function generateWorkflowSummary(wfInstanceId: number) {
  return http.get<unknown, string>(`/workflow/summary/${wfInstanceId}`)
}

// ==================== 任务依赖 ====================
export function createTaskDependency(taskId: number, dependsOnTaskId: number, dependencyType?: string) {
  const params = new URLSearchParams({
    taskId: taskId.toString(),
    dependsOnTaskId: dependsOnTaskId.toString(),
  })
  if (dependencyType) params.append('dependencyType', dependencyType)
  return http.post<unknown, void>(`/workflow/task-dependencies?${params}`)
}

export function canTaskStart(taskId: number) {
  return http.get<unknown, boolean>(`/workflow/task-dependencies/${taskId}/can-start`)
}

export function getDownstreamTasks(taskId: number) {
  return http.get<unknown, JsonObject[]>(`/workflow/task-dependencies/${taskId}/downstream`)
}

// ==================== 供应商画像 ====================
export function getSupplierProfile(supplierName: string) {
  return http.get<unknown, JsonObject>(`/suppliers/profile?name=${encodeURIComponent(supplierName)}`)
}

export function getSupplierSummaries() {
  return http.get<unknown, JsonObject[]>('/suppliers/summaries')
}

// ==================== Webhook ====================
export function listWebhooks() {
  return http.get<unknown, JsonObject[]>('/webhooks')
}

export function createWebhook(data: { name: string; url: string; eventType: string; secret?: string }) {
  return http.post<unknown, JsonObject>('/webhooks', data)
}

export function deleteWebhook(id: number) {
  return http.delete<unknown, void>(`/webhooks/${id}`)
}

// ==================== 操作回放 ====================
export function getOperationReplay(entityType: string, entityId: number) {
  return http.get<unknown, JsonObject[]>(`/audit/replay?entityType=${entityType}&entityId=${entityId}`)
}

// ==================== 知识图谱 ====================
export function getEntityRelations(entityType: string, entityId: number) {
  return http.get<unknown, JsonObject>(`/knowledge-graph/relations/${entityType}/${entityId}`)
}

export function searchKnowledgeGraph(keyword: string, entityType?: string) {
  const params = new URLSearchParams({ keyword })
  if (entityType) params.append('entityType', entityType)
  return http.get<unknown, JsonObject[]>(`/knowledge-graph/search?${params}`)
}

// ==================== 合同风险 ====================
export function getContractRisks(contractId: number) {
  return http.get<unknown, JsonObject[]>(`/contracts/${contractId}/risks`)
}

export function getContractRiskReport() {
  return http.get<unknown, JsonObject>('/contracts/risks/report')
}

// ==================== 智能催办 ====================
export function analyzeReminderTiming(taskId: number) {
  return http.get<unknown, JsonObject>(`/workflow/smart-reminder/analyze/${taskId}`)
}

export function sendSmartReminder(taskId: number) {
  return http.post<unknown, JsonObject>(`/workflow/smart-reminder/send/${taskId}`)
}

// ==================== 流程版本 ====================
export function listProcessVersions(templateId: number) {
  return http.get<unknown, JsonObject[]>(`/workflow/versions/template/${templateId}`)
}

export function createProcessVersion(templateId: number, config: string, changeDescription: string) {
  return http.post<unknown, JsonObject>(`/workflow/versions/template/${templateId}`, { config, changeDescription })
}

export function publishProcessVersion(versionId: number) {
  return http.post<unknown, JsonObject>(`/workflow/versions/${versionId}/publish`)
}

export function rollbackProcessVersion(versionId: number) {
  return http.post<unknown, JsonObject>(`/workflow/versions/${versionId}/rollback`)
}

// ==================== 智能日程 ====================
export function checkMeetingConflicts(userIds: number[], startTime: string, endTime: string, excludeBookingId?: number) {
  return http.post<unknown, JsonObject>('/meetings/smart-schedule/check-conflicts', {
    userIds, startTime, endTime, excludeBookingId,
  })
}

export function recommendMeetingTime(userIds: number[], durationMinutes: number, preferredDate: string) {
  return http.post<unknown, JsonObject[]>('/meetings/smart-schedule/recommend', {
    userIds, durationMinutes, preferredDate,
  })
}

// ==================== 报表 ====================
export function getCostPrediction(year: number, quarter: number) {
  return http.get<unknown, JsonObject>(`/reports/cost-prediction/quarterly?year=${year}&quarter=${quarter}`)
}

export function getCostTrend(year: number, months: number = 12) {
  return http.get<unknown, JsonObject[]>(`/reports/cost-prediction/trend?year=${year}&months=${months}`)
}

export function getWorkloadRanking() {
  return http.get<unknown, JsonObject[]>('/reports/workload/ranking')
}

export function getDepartmentWorkload() {
  return http.get<unknown, JsonObject[]>('/reports/workload/department')
}

export function getUserWorkload(userId: number) {
  return http.get<unknown, JsonObject>(`/reports/workload/user/${userId}`)
}

export function getOverloadWarnings() {
  return http.get<unknown, JsonObject[]>('/reports/workload/overload-warnings')
}

export function getDepartmentHealth(deptId: number) {
  return http.get<unknown, JsonObject>(`/reports/department-health/${deptId}`)
}

export function getDepartmentHealthRanking() {
  return http.get<unknown, JsonObject[]>('/reports/department-health/ranking')
}
