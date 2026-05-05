import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function listConfigs(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/system/configs', { params: { page, size } })
}

export function updateConfig(key: string, configValue: string) {
  return http.put<unknown, JsonObject>(`/system/configs/${encodeURIComponent(key)}`, { configValue })
}

export function listDictTypes(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/system/dict-types', { params: { page, size } })
}

export function listDictItems(code: string) {
  return http.get<unknown, JsonObject[]>(`/system/dict-types/${encodeURIComponent(code)}/items`)
}

export function listNumberRules(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/system/number-rules', { params: { page, size } })
}

export function createNumberRule(body: {
  ruleCode: string
  businessType: string
  prefix: string
  datePattern?: string | null
  seqLength: number
  seqReset: string
  description?: string | null
}) {
  return http.post<unknown, JsonObject>('/system/number-rules', body)
}

export function previewNumber(ruleCode: string) {
  return http.post<unknown, JsonObject>(`/system/number-rules/${encodeURIComponent(ruleCode)}/preview`)
}

export function listWorkCalendar(params: { from?: string; to?: string; page?: number; size?: number }) {
  return http.get<unknown, PageResponse<JsonObject>>('/system/work-calendar', { params })
}

export function upsertWorkCalendar(body: { calDate: string; dayType: string; description?: string | null }) {
  return http.post<unknown, JsonObject>('/system/work-calendar', body)
}

export function deleteWorkCalendar(date: string) {
  return http.delete<unknown, JsonObject>(`/system/work-calendar/${date}`)
}

export function countWorkdays(from: string, to: string) {
  return http.get<unknown, JsonObject>('/system/work-calendar/count', { params: { from, to } })
}

export function listImportTasks(params: { businessType?: string; page?: number; size?: number }) {
  return http.get<unknown, PageResponse<JsonObject>>('/system/import-tasks', { params })
}

export function listExportTasks(params: { businessType?: string; page?: number; size?: number }) {
  return http.get<unknown, PageResponse<JsonObject>>('/system/export-tasks', { params })
}

export function createDictType(body: { dictCode: string; dictName: string; remark?: string | null }) {
  return http.post<unknown, JsonObject>('/system/dict-types', body)
}

export function updateDictType(id: number, body: { dictName?: string; remark?: string | null }) {
  return http.put<unknown, JsonObject>(`/system/dict-types/${id}`, body)
}

export function deleteDictType(id: number) {
  return http.delete<unknown, JsonObject>(`/system/dict-types/${id}`)
}

export function createDictItem(dictTypeId: number, body: { itemLabel: string; itemValue: string; sortOrder?: number; remark?: string | null }) {
  return http.post<unknown, JsonObject>('/system/dict-items', { ...body, dictTypeId })
}

export function updateDictItem(id: number, body: { itemLabel?: string; itemValue?: string; sortOrder?: number }) {
  return http.put<unknown, JsonObject>(`/system/dict-items/${id}`, body)
}

export function deleteDictItem(id: number) {
  return http.delete<unknown, JsonObject>(`/system/dict-items/${id}`)
}
