import { http } from './http'
import type { JsonObject } from './types'
import { createCrudApi } from './crud'

const crud = createCrudApi('oa/seal-applies')

export const listSeals = crud.list
export const getSeal = crud.get
export const createSeal = crud.create
export const updateSeal = crud.update
export const submitSeal = crud.submit
export const withdrawSeal = crud.withdraw
export const cancelSeal = crud.cancel

export function returnSeal(id: number) {
  return http.post<unknown, JsonObject>(`/oa/seal-applies/${id}/return`)
}

export function uploadSealAttachment(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<unknown, { url: string; name: string; size: number }>('/file-upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
