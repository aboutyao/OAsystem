import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function listTemplates(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/workflow/templates', { params: { page, size } })
}

export function templateDetail(id: number) {
  return http.get<unknown, JsonObject>(`/workflow/templates/${id}`)
}

export function todoTasks(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/workflow/tasks/todo', { params: { page, size } })
}

export function doneTasks(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/workflow/tasks/done', { params: { page, size } })
}

export function startedByMe(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/workflow/instances/started-by-me', { params: { page, size } })
}

export function ccToMe(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/workflow/instances/cc-to-me', { params: { page, size } })
}

export function markCcRead(ccId: number) {
  return http.post<unknown, JsonObject>(`/workflow/cc/${ccId}/read`)
}

export function addCc(body: { wfInstanceId: number; receiverIds: number[]; reason?: string | null }) {
  return http.post<unknown, JsonObject>('/workflow/cc', body)
}

export function instanceDetail(wfInstanceId: number) {
  return http.get<unknown, JsonObject>(`/workflow/instances/${wfInstanceId}`)
}

export function instanceTimeline(wfInstanceId: number) {
  return http.get<unknown, JsonObject[]>(`/workflow/instances/${wfInstanceId}/timeline`)
}

export function instanceDiagram(wfInstanceId: number) {
  return http.get<unknown, JsonObject>(`/workflow/instances/${wfInstanceId}/diagram`)
}

export function approveTask(taskId: number, body?: { comment?: string | null; attachmentIds?: number[] | null }) {
  return http.post<unknown, JsonObject>(`/workflow/tasks/${taskId}/approve`, body ?? {})
}

export function getApprovalContext(taskId: number) {
  return http.get<unknown, JsonObject>(`/workflow/tasks/${taskId}/context`)
}

export function rejectTask(taskId: number, body?: { comment?: string | null; rejectTo?: string | null }) {
  return http.post<unknown, JsonObject>(`/workflow/tasks/${taskId}/reject`, body ?? {})
}

export function transferTask(taskId: number, body: { targetUserId: number; comment?: string | null }) {
  return http.post<unknown, JsonObject>(`/workflow/tasks/${taskId}/transfer`, body)
}

export function addSignTask(
  taskId: number,
  body: { assigneeUserId: number; mode?: 'SEQUENTIAL' | 'PARALLEL'; comment?: string | null }
) {
  return http.post<unknown, JsonObject>(`/workflow/tasks/${taskId}/add-sign`, body)
}

export function withdrawInstance(wfInstanceId: number) {
  return http.post<unknown, JsonObject>(`/workflow/instances/${wfInstanceId}/withdraw`)
}

export function terminateInstance(wfInstanceId: number) {
  return http.post<unknown, JsonObject>(`/workflow/instances/${wfInstanceId}/terminate`)
}

export function startInstance(body: {
  businessType: string
  businessId: number
  title: string
  variables?: Record<string, unknown>
}) {
  return http.post<unknown, JsonObject>('/workflow/instances', body)
}

export function listMyDelegations(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/workflow/delegations/me', { params: { page, size } })
}

export function listAllDelegations(page = 1, size = 20, status?: string | null) {
  return http.get<unknown, PageResponse<JsonObject>>('/workflow/delegations', {
    params: { page, size, status: status || undefined },
  })
}

export function createDelegation(body: {
  delegateeId: number
  startAt: string
  endAt: string
  businessScope?: string | null
  reason?: string | null
}) {
  return http.post<unknown, JsonObject>('/workflow/delegations', body)
}

export function cancelDelegation(id: number) {
  return http.delete<unknown, JsonObject>(`/workflow/delegations/${id}`)
}

export function listExceptions(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/workflow/exceptions', { params: { page, size } })
}

export function createTemplate(body: { templateCode: string; templateName: string; businessType: string; description?: string | null }) {
  return http.post<unknown, JsonObject>('/workflow/templates', body)
}

export function updateTemplate(id: number, body: { templateName?: string; description?: string | null; status?: string }) {
  return http.put<unknown, JsonObject>(`/workflow/templates/${id}`, body)
}

export function createVersion(templateId: number, body?: { changeReason?: string | null }) {
  return http.post<unknown, JsonObject>(`/workflow/templates/${templateId}/versions`, body ?? {})
}

export function publishVersion(versionId: number) {
  return http.post<unknown, JsonObject>(`/workflow/versions/${versionId}/publish`)
}

export function listCommentTemplates() {
  return http.get<unknown, JsonObject[]>('/workflow/comment-templates')
}

export function createCommentTemplate(content: string) {
  return http.post<unknown, JsonObject>('/workflow/comment-templates', { content })
}

export function deleteCommentTemplate(id: number) {
  return http.delete<unknown, void>(`/workflow/comment-templates/${id}`)
}

export function batchApprove(taskIds: number[], comment?: string) {
  return http.post<unknown, JsonObject>('/workflow/tasks/batch-approve', { taskIds, comment })
}
