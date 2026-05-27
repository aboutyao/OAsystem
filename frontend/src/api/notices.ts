import { http } from './http'
import type { JsonObject, PageResponse } from './types'
import { createCrudApi } from './crud'

const crud = createCrudApi('notices')

export const getNotice = crud.get
export const createNotice = crud.create
export const updateNotice = crud.update

export function listNotices(page = 1, size = 20, mine?: boolean, status?: string, category?: string) {
  return http.get<unknown, PageResponse<JsonObject>>('/notices', {
    params: {
      page,
      size,
      ...(mine === true ? { mine: true } : {}),
      ...(status ? { status } : {}),
      ...(category ? { category } : {}),
    },
  })
}

export function publishNotice(id: number) {
  return http.post<unknown, JsonObject>(`/notices/${id}/publish`)
}

export const withdrawNotice = crud.withdraw

export function markNoticeRead(id: number) {
  return http.post<unknown, JsonObject>(`/notices/${id}/read`)
}

export function confirmNoticeRead(id: number) {
  return http.post<unknown, JsonObject>(`/notices/${id}/confirm`)
}

export function noticeReadStats(id: number) {
  return http.get<unknown, JsonObject>(`/notices/${id}/read-stats`)
}
