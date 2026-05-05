import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function listLoginLogs(params: {
  page?: number
  size?: number
  username?: string
  result?: string
}) {
  return http.get<unknown, PageResponse<JsonObject>>('/audit/login-logs', { params })
}

export function listOperationLogs(params: {
  page?: number
  size?: number
  businessType?: string
  result?: string
  operatorId?: number
}) {
  return http.get<unknown, PageResponse<JsonObject>>('/audit/operation-logs', { params })
}
