import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export interface AssetListQuery {
  page?: number
  size?: number
  assetCategory?: string
  status?: string
  responsibleUserId?: number
  keyword?: string
}

export function listAssets(query: AssetListQuery = {}) {
  const { page = 1, size = 20, ...rest } = query
  const params: Record<string, unknown> = { page, size }
  for (const [k, v] of Object.entries(rest)) {
    if (v !== undefined && v !== null && v !== '') {
      params[k] = v
    }
  }
  return http.get<unknown, PageResponse<JsonObject>>('/assets', { params })
}

export function getAsset(id: number) {
  return http.get<unknown, JsonObject>(`/assets/${id}`)
}

export function createAsset(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/assets', body)
}

export function updateAsset(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/assets/${id}`, body)
}

export function receiveAsset(id: number, body?: { targetUserId?: number | null; reason?: string | null }) {
  return http.post<unknown, JsonObject>(`/assets/${id}/receive`, body ?? {})
}

export function returnAsset(id: number, body?: { reason?: string | null }) {
  return http.post<unknown, JsonObject>(`/assets/${id}/return`, body ?? {})
}

export function repairAsset(id: number, body?: { reason?: string | null }) {
  return http.post<unknown, JsonObject>(`/assets/${id}/repair`, body ?? {})
}

export function scrapAsset(id: number, body?: { reason?: string | null }) {
  return http.post<unknown, JsonObject>(`/assets/${id}/scrap`, body ?? {})
}

export function listAssetRecords(id: number) {
  return http.get<unknown, JsonObject[]>(`/assets/${id}/records`)
}
