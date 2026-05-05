import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function listFormTemplates(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/forms/templates', { params: { page, size } })
}

export function formTemplateDetail(id: number) {
  return http.get<unknown, JsonObject>(`/forms/templates/${id}`)
}

export function createFormTemplate(body: {
  templateCode: string
  templateName: string
  businessType: string
  description?: string | null
}) {
  return http.post<unknown, JsonObject>('/forms/templates', body)
}

export function createFormVersion(
  templateId: number,
  body: { fieldsJson: string; layoutJson?: string | null; changeReason?: string | null }
) {
  return http.post<unknown, JsonObject>(`/forms/templates/${templateId}/versions`, body)
}

export function publishFormVersion(versionId: number) {
  return http.post<unknown, JsonObject>(`/forms/versions/${versionId}/publish`)
}

export function getFormVersion(id: number) {
  return http.get<unknown, JsonObject>(`/forms/versions/${id}`)
}

export function listFormFieldRules(params: { page?: number; size?: number; templateId?: number }) {
  return http.get<unknown, PageResponse<JsonObject>>('/forms/field-rules', { params })
}

export function upsertFormFieldRule(
  templateId: number,
  body: { fieldCode: string; ruleType: string; ruleExpression: string; description?: string | null }
) {
  return http.post<unknown, JsonObject>(`/forms/templates/${templateId}/field-rules`, body)
}

export function updateFormTemplate(id: number, body: { templateName?: string; description?: string }) {
  return http.put<unknown, JsonObject>(`/forms/templates/${id}`, body)
}

export function deleteFormTemplate(id: number) {
  return http.delete<unknown, JsonObject>(`/forms/templates/${id}`)
}
