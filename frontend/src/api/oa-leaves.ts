import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function listLeaves(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/oa/leaves', { params: { page, size } })
}

export function getLeave(id: number) {
  return http.get<unknown, JsonObject>(`/oa/leaves/${id}`)
}

export function createLeave(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/oa/leaves', body)
}

export function updateLeave(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/oa/leaves/${id}`, body)
}

export function submitLeave(id: number) {
  return http.post<unknown, JsonObject>(`/oa/leaves/${id}/submit`)
}

export function withdrawLeave(id: number) {
  return http.post<unknown, JsonObject>(`/oa/leaves/${id}/withdraw`)
}

export function cancelLeave(id: number) {
  return http.post<unknown, JsonObject>(`/oa/leaves/${id}/cancel`)
}

export function calculateLeaveDuration(startAt: string, endAt: string) {
  return http.get<unknown, JsonObject>('/oa/leaves/calculate-duration', {
    params: { startAt, endAt },
  })
}
