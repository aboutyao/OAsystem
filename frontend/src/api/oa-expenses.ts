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
