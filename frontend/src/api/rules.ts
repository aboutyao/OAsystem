import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export interface RuleCreateRequest {
  groupCode: string
  ruleCode: string
  ruleName: string
  ruleType: string
  businessType: string
  description?: string
}

export interface RuleVersionCreateRequest {
  ruleContentJson: string
  naturalLanguage?: string
  changeReason?: string
}

export interface SimulateRequest {
  businessType: string
  context: Record<string, unknown>
}

export function listRules(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/rules', {
    params: { page, size },
  })
}

export function getRule(id: number) {
  return http.get<unknown, JsonObject>(`/rules/${id}`)
}

export function createRule(payload: RuleCreateRequest) {
  return http.post<unknown, JsonObject>('/rules', payload)
}

export function createRuleVersion(ruleId: number, payload: RuleVersionCreateRequest) {
  return http.post<unknown, JsonObject>(`/rules/${ruleId}/versions`, payload)
}

export function publishRuleVersion(versionId: number) {
  return http.post<unknown, JsonObject>(`/rule-versions/${versionId}/publish`, {})
}

export function simulateRuleVersion(versionId: number, payload: SimulateRequest) {
  return http.post<unknown, JsonObject>(`/rule-versions/${versionId}/simulate`, payload)
}

export function updateRule(id: number, body: { ruleName?: string; description?: string }) {
  return http.put<unknown, JsonObject>(`/rules/${id}`, body)
}

export function deleteRule(id: number) {
  return http.delete<unknown, JsonObject>(`/rules/${id}`)
}

export function listRuleGroups() {
  return http.get<unknown, JsonObject[]>('/rule-groups')
}

export function createRuleGroup(body: { groupCode: string; groupName: string; description?: string; status?: string }) {
  return http.post<unknown, JsonObject>('/rule-groups', body)
}

export function updateRuleGroup(id: number, body: { groupCode: string; groupName: string; description?: string; status?: string }) {
  return http.put<unknown, JsonObject>(`/rule-groups/${id}`, body)
}

export function deleteRuleGroup(id: number) {
  return http.delete<unknown, JsonObject>(`/rule-groups/${id}`)
}
