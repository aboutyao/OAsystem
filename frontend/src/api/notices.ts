import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function listNotices(page = 1, size = 20, mine?: boolean, status?: string) {
  return http.get<unknown, PageResponse<JsonObject>>('/notices', {
    params: {
      page,
      size,
      ...(mine === true ? { mine: true } : {}),
      ...(status ? { status } : {}),
    },
  })
}

export function getNotice(id: number) {
  return http.get<unknown, JsonObject>(`/notices/${id}`)
}

export function createNotice(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/notices', body)
}

export function updateNotice(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/notices/${id}`, body)
}

export function publishNotice(id: number) {
  return http.post<unknown, JsonObject>(`/notices/${id}/publish`)
}

export function withdrawNotice(id: number) {
  return http.post<unknown, JsonObject>(`/notices/${id}/withdraw`)
}

export function markNoticeRead(id: number) {
  return http.post<unknown, JsonObject>(`/notices/${id}/read`)
}

export function confirmNoticeRead(id: number) {
  return http.post<unknown, JsonObject>(`/notices/${id}/confirm`)
}

export function noticeReadStats(id: number) {
  return http.get<unknown, JsonObject>(`/notices/${id}/read-stats`)
}
