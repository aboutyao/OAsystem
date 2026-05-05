import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export interface HealthStatus {
  status: string
  service: string
  time: string
}

export function getHealth() {
  return http.get<unknown, HealthStatus>('/ops/health')
}

export function listOnlineUsers() {
  return http.get<unknown, JsonObject[]>('/ops/online-users')
}

export interface JobLogQuery {
  page?: number
  size?: number
  jobCode?: string
  status?: string
}

export interface ExceptionQuery {
  page?: number
  size?: number
  severity?: string
}

export interface BackupQuery {
  page?: number
  size?: number
  backupType?: string
  status?: string
}

export function listJobLogs(query: JobLogQuery = {}) {
  return http.get<unknown, PageResponse<JsonObject>>('/ops/job-logs', {
    params: { page: 1, size: 20, ...query },
  })
}

export function listExceptions(query: ExceptionQuery = {}) {
  return http.get<unknown, PageResponse<JsonObject>>('/ops/exceptions', {
    params: { page: 1, size: 20, ...query },
  })
}

export function listBackupRecords(query: BackupQuery = {}) {
  return http.get<unknown, PageResponse<JsonObject>>('/ops/backup-records', {
    params: { page: 1, size: 20, ...query },
  })
}

export function refreshCache() {
  return http.post<unknown, JsonObject>('/ops/cache/refresh', {})
}
