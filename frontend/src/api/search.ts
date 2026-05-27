import { http } from './http'
import type { JsonObject } from './types'

export interface SearchResult {
  users: JsonObject[]
  leaves: JsonObject[]
  files: JsonObject[]
}

export function globalSearch(q: string, limit = 5) {
  return http.get<unknown, SearchResult>('/search', { params: { q, limit } })
}
