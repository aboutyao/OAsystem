import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function listMessages(
  page = 1,
  size = 20,
  readStatus?: string,
  archiveStatus?: string,
) {
  return http.get<unknown, PageResponse<JsonObject>>('/messages', {
    params: {
      page,
      size,
      ...(readStatus ? { readStatus } : {}),
      ...(archiveStatus ? { archiveStatus } : {}),
    },
  })
}

export function getUnreadMessageCount() {
  return http.get<unknown, { count: number }>('/messages/unread-count')
}

export function markMessageRead(id: number) {
  return http.patch<unknown, JsonObject>(`/messages/${id}/read`, {})
}

export function batchMarkMessagesRead(ids: number[]) {
  return http.patch<unknown, JsonObject>('/messages/batch-read', { ids })
}

export function archiveMessage(id: number) {
  return http.patch<unknown, JsonObject>(`/messages/${id}/archive`, {})
}

export function deleteMessage(id: number) {
  return http.delete<unknown, JsonObject>(`/messages/${id}`)
}
