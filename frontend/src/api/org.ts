import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function getDeptTree() {
  return http.get<unknown, JsonObject[]>('/org/depts/tree')
}

export function createDept(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/org/depts', body)
}

export function updateDept(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/org/depts/${id}`, body)
}

export function listUsers(page = 1, size = 20, keyword?: string) {
  return http.get<unknown, PageResponse<JsonObject>>('/org/users', {
    params: { page, size, ...(keyword ? { keyword } : {}) },
  })
}

export function createUser(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/org/users', body)
}

export function updateUser(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/org/users/${id}`, body)
}

export function enableUser(id: number) {
  return http.patch<unknown, JsonObject>(`/org/users/${id}/enable`)
}

export function disableUser(id: number) {
  return http.patch<unknown, JsonObject>(`/org/users/${id}/disable`)
}

export function resignUser(id: number) {
  return http.patch<unknown, JsonObject>(`/org/users/${id}/resign`)
}

export function resetPassword(id: number) {
  return http.post<unknown, JsonObject>(`/org/users/${id}/reset-password`)
}

export function listPositions() {
  return http.get<unknown, JsonObject[]>('/org/positions')
}

export function createPosition(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/org/positions', body)
}

export function updatePosition(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/org/positions/${id}`, body)
}

export function deletePosition(id: number) {
  return http.delete<unknown, JsonObject>(`/org/positions/${id}`)
}

export function listRanks() {
  return http.get<unknown, JsonObject[]>('/org/ranks')
}

export function createRank(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/org/ranks', body)
}

export function updateRank(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/org/ranks/${id}`, body)
}

export function deleteRank(id: number) {
  return http.delete<unknown, JsonObject>(`/org/ranks/${id}`)
}

export function listChangeLogs(page = 1, size = 20, targetType?: string, changeType?: string) {
  return http.get<unknown, PageResponse<JsonObject>>('/org/change-logs', {
    params: {
      page,
      size,
      ...(targetType ? { targetType } : {}),
      ...(changeType ? { changeType } : {}),
    },
  })
}
