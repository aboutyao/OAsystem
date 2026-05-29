import { http } from './http'
import type { JsonObject } from './types'
import { createCrudApi } from './crud'

const crud = createCrudApi('oa/leaves')

export const listLeaves = crud.list
export const getLeave = crud.get
export const createLeave = crud.create
export const updateLeave = crud.update
export const submitLeave = crud.submit
export const withdrawLeave = crud.withdraw
export const cancelLeave = crud.cancel

export function calculateLeaveDuration(startAt: string, endAt: string) {
  return http.get<unknown, JsonObject>('/oa/leaves/calculate-duration', {
    params: { startAt, endAt },
  })
}

export function teamLeaveCalendar(start: string, end: string) {
  return http.get<unknown, JsonObject[]>('/oa/leaves/team-calendar', { params: { start, end } })
}

export function exportLeaves(filter?: Record<string, unknown>) {
  return http.post('/oa/leaves/export', filter ?? {}, { responseType: 'blob' })
}

// 请假余额相关
export function getMyLeaveBalance() {
  return http.get<unknown, JsonObject[]>('/leave-balance/my')
}

export function getUserLeaveBalance(userId: number) {
  return http.get<unknown, JsonObject[]>(`/leave-balance/user/${userId}`)
}

export function predictLeaveBalanceExhaustion() {
  return http.get<unknown, JsonObject>('/leave-balance/predict')
}
