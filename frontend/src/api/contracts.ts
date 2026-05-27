import { http } from './http'
import type { JsonObject, PageResponse } from './types'
import { createCrudApi } from './crud'

const crud = createCrudApi('contracts')

export const getContract = crud.get
export const createContract = crud.create
export const updateContract = crud.update
export const submitContract = crud.submit
export const withdrawContract = crud.withdraw
export const cancelContract = crud.cancel

export function listContracts(page = 1, size = 20, ownerId?: number) {
  return http.get<unknown, PageResponse<JsonObject>>('/contracts', {
    params: { page, size, ...(ownerId != null ? { ownerId } : {}) },
  })
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
