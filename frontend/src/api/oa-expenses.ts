import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function listExpenses(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/oa/expenses', { params: { page, size } })
}

export function getExpense(id: number) {
  return http.get<unknown, JsonObject>(`/oa/expenses/${id}`)
}

export function createExpense(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/oa/expenses', body)
}

export function updateExpense(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/oa/expenses/${id}`, body)
}

export function submitExpense(id: number) {
  return http.post<unknown, JsonObject>(`/oa/expenses/${id}/submit`)
}

export function withdrawExpense(id: number) {
  return http.post<unknown, JsonObject>(`/oa/expenses/${id}/withdraw`)
}

export function cancelExpense(id: number) {
  return http.post<unknown, JsonObject>(`/oa/expenses/${id}/cancel`)
}

export function markPaidExpense(id: number) {
  return http.post<unknown, JsonObject>(`/oa/expenses/${id}/mark-paid`)
}
