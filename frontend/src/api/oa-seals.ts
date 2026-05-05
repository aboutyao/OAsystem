import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function listSeals(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/oa/seal-applies', { params: { page, size } })
}

export function getSeal(id: number) {
  return http.get<unknown, JsonObject>(`/oa/seal-applies/${id}`)
}

export function createSeal(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/oa/seal-applies', body)
}

export function updateSeal(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/oa/seal-applies/${id}`, body)
}

export function submitSeal(id: number) {
  return http.post<unknown, JsonObject>(`/oa/seal-applies/${id}/submit`)
}

export function withdrawSeal(id: number) {
  return http.post<unknown, JsonObject>(`/oa/seal-applies/${id}/withdraw`)
}

export function cancelSeal(id: number) {
  return http.post<unknown, JsonObject>(`/oa/seal-applies/${id}/cancel`)
}

export function returnSeal(id: number) {
  return http.post<unknown, JsonObject>(`/oa/seal-applies/${id}/return`)
}
