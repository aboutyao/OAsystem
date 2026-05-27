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
