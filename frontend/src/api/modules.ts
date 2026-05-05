import { http } from './http'

export interface ModuleStatus {
  status: string
  modules: string[]
}

export function getModules() {
  return http.get<unknown, ModuleStatus>('/modules')
}
