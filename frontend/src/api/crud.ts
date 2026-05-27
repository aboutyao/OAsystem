import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export interface CrudApi {
  list: (page?: number, size?: number) => Promise<PageResponse<JsonObject>>
  get: (id: number) => Promise<JsonObject>
  create: (body: Record<string, unknown>) => Promise<JsonObject>
  update: (id: number, body: Record<string, unknown>) => Promise<JsonObject>
  submit: (id: number) => Promise<JsonObject>
  withdraw: (id: number) => Promise<JsonObject>
  cancel: (id: number) => Promise<JsonObject>
  remove?: (id: number) => Promise<void>
}

export function createCrudApi(basePath: string): CrudApi {
  const p = `/${basePath}`
  const byId = (id: number) => `${p}/${id}`
  return {
    list: (page = 1, size = 20) =>
      http.get<unknown, PageResponse<JsonObject>>(p, { params: { page, size } }),
    get: (id) => http.get<unknown, JsonObject>(byId(id)),
    create: (body) => http.post<unknown, JsonObject>(p, body),
    update: (id, body) => http.put<unknown, JsonObject>(byId(id), body),
    submit: (id) => http.post<unknown, JsonObject>(`${byId(id)}/submit`),
    withdraw: (id) => http.post<unknown, JsonObject>(`${byId(id)}/withdraw`),
    cancel: (id) => http.post<unknown, JsonObject>(`${byId(id)}/cancel`),
  }
}
