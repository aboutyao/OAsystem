import { http } from './http'
import type { JsonObject } from './types'

export interface SearchResult {
  users: JsonObject[]
  leaves: JsonObject[]
  files: JsonObject[]
  expenses: JsonObject[]
  purchases: JsonObject[]
  contracts: JsonObject[]
  seals: JsonObject[]
  notices: JsonObject[]
  workflows: JsonObject[]
  totals: Record<string, number>
}

export function globalSearch(q: string, limit = 5) {
  return http.get<unknown, SearchResult>('/search', { params: { q, limit } })
}
