import { http } from './http'
import type { JsonObject } from './types'
import { createCrudApi } from './crud'

const crud = createCrudApi('oa/expenses')

export const listExpenses = crud.list
export const getExpense = crud.get
export const createExpense = crud.create
export const updateExpense = crud.update
export const submitExpense = crud.submit
export const withdrawExpense = crud.withdraw
export const cancelExpense = crud.cancel

export function markPaidExpense(id: number) {
  return http.post<unknown, JsonObject>(`/oa/expenses/${id}/mark-paid`)
}

export function exportExpenses(filter?: Record<string, unknown>) {
  return http.post('/oa/expenses/export', filter ?? {}, { responseType: 'blob' })
}

export function uploadExpenseAttachment(expenseId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<unknown, JsonObject>(`/oa/expenses/${expenseId}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function listExpenseAttachments(expenseId: number) {
  return http.get<unknown, JsonObject[]>(`/oa/expenses/${expenseId}/attachments`)
}

export function deleteExpenseAttachment(expenseId: number, attachmentId: number) {
  return http.delete(`/oa/expenses/${expenseId}/attachments/${attachmentId}`)
}
