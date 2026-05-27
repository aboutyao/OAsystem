import { http } from './http'
import type { JsonObject, PageResponse } from './types'
import { createCrudApi } from './crud'

const crud = createCrudApi('oa/purchases')

export const getPurchase = crud.get
export const createPurchase = crud.create
export const updatePurchase = crud.update
export const submitPurchase = crud.submit
export const withdrawPurchase = crud.withdraw
export const cancelPurchase = crud.cancel

export function listPurchases(page = 1, size = 20, params?: { status?: string }) {
  return http.get<unknown, PageResponse<JsonObject>>('/oa/purchases', { params: { page, size, ...params } })
}

export function confirmPurchaseArrival(id: number) {
  return http.post<unknown, JsonObject>(`/oa/purchases/${id}/confirm-arrival`)
}

export function acceptPurchase(id: number) {
  return http.post<unknown, JsonObject>(`/oa/purchases/${id}/accept`)
}
