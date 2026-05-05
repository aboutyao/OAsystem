import { http } from './http'
import type { JsonObject } from './types'

export function workflowEfficiency(params?: { from?: string; to?: string }) {
  return http.get<unknown, JsonObject>('/reports/workflow-efficiency', { params })
}
export function todoSummary() {
  return http.get<unknown, JsonObject>('/reports/todo-summary')
}
export function leaveSummary(params?: { from?: string; to?: string }) {
  return http.get<unknown, JsonObject>('/reports/leave-summary', { params })
}
export function expenseSummary(params?: { from?: string; to?: string }) {
  return http.get<unknown, JsonObject>('/reports/expense-summary', { params })
}
export function contractSummary(params?: { from?: string; to?: string }) {
  return http.get<unknown, JsonObject>('/reports/contract-summary', { params })
}
export function assetSummary() {
  return http.get<unknown, JsonObject>('/reports/asset-summary')
}
export function userSummary() {
  return http.get<unknown, JsonObject>('/reports/user-summary')
}
