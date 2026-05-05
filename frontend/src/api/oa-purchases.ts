import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function listPurchases(page = 1, size = 20, params?: { status?: string }) {
  return http.get<unknown, PageResponse<JsonObject>>('/oa/purchases', { params: { page, size, ...params } })
}

export function getPurchase(id: number) {
  return http.get<unknown, JsonObject>(`/oa/purchases/${id}`)
}

export function createPurchase(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/oa/purchases', body)
}

export function updatePurchase(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/oa/purchases/${id}`, body)
}

export function submitPurchase(id: number) {
  return http.post<unknown, JsonObject>(`/oa/purchases/${id}/submit`)
}

export function withdrawPurchase(id: number) {
  return http.post<unknown, JsonObject>(`/oa/purchases/${id}/withdraw`)
}

export function cancelPurchase(id: number) {
  return http.post<unknown, JsonObject>(`/oa/purchases/${id}/cancel`)
}

export function confirmPurchaseArrival(id: number) {
  return http.post<unknown, JsonObject>(`/oa/purchases/${id}/confirm-arrival`)
}

export function acceptPurchase(id: number) {
  return http.post<unknown, JsonObject>(`/oa/purchases/${id}/accept`)
}
