import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export interface SupplyListQuery {
  page?: number
  size?: number
  category?: string
  status?: string
  keyword?: string
}

export function listSupplies(query: SupplyListQuery = {}) {
  const { page = 1, size = 20, ...rest } = query
  const params: Record<string, unknown> = { page, size }
  for (const [k, v] of Object.entries(rest)) {
    if (v !== undefined && v !== null && v !== '') {
      params[k] = v
    }
  }
  return http.get<unknown, PageResponse<JsonObject>>('/supplies', { params })
}

export function createSupply(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/supplies', body)
}

export function updateSupply(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/supplies/${id}`, body)
}

export interface SupplyMovementBody {
  quantity: number
  userId?: number | null
  reason?: string | null
}

export function stockInSupply(id: number, body: SupplyMovementBody) {
  return http.post<unknown, JsonObject>(`/supplies/${id}/stock-in`, body)
}

export function stockOutSupply(id: number, body: SupplyMovementBody) {
  return http.post<unknown, JsonObject>(`/supplies/${id}/stock-out`, body)
}

export function returnSupply(id: number, body: SupplyMovementBody) {
  return http.post<unknown, JsonObject>(`/supplies/${id}/return`, body)
}

export function adjustSupply(id: number, body: { quantity: number; reason?: string | null }) {
  return http.post<unknown, JsonObject>(`/supplies/${id}/adjust`, body)
}

export function listSupplyRecords(supplyId?: number) {
  if (supplyId != null) {
    return http.get<unknown, JsonObject[]>(`/supplies/${supplyId}/records`)
  }
  return http.get<unknown, JsonObject[]>('/supplies/records')
}
