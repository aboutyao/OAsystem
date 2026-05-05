import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function listContracts(page = 1, size = 20, ownerId?: number) {
  return http.get<unknown, PageResponse<JsonObject>>('/contracts', {
    params: { page, size, ...(ownerId != null ? { ownerId } : {}) },
  })
}

export function getContract(id: number) {
  return http.get<unknown, JsonObject>(`/contracts/${id}`)
}

export function createContract(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/contracts', body)
}

export function updateContract(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/contracts/${id}`, body)
}

export function submitContract(id: number) {
  return http.post<unknown, JsonObject>(`/contracts/${id}/submit`)
}

export function signContract(id: number) {
  return http.post<unknown, JsonObject>(`/contracts/${id}/sign`)
}

export function terminateContract(id: number) {
  return http.post<unknown, JsonObject>(`/contracts/${id}/terminate`)
}

export function renewContract(id: number) {
  return http.post<unknown, JsonObject>(`/contracts/${id}/renew`)
}

export function contractVersions(id: number) {
  return http.get<unknown, JsonObject[]>(`/contracts/${id}/versions`)
}
