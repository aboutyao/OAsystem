import { http } from './http'

export interface CurrentUser {
  id: number
  username: string
  realName: string
  mainDeptId: number
  mainDeptName: string
  roles: string[]
  permissions: string[]
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  expiresIn: number
  user: CurrentUser
}

export function login(payload: LoginRequest) {
  return http.post<unknown, LoginResponse>('/auth/login', payload)
}

export function me() {
  return http.get<unknown, CurrentUser>('/auth/me')
}

export interface AuthMenuItem {
  id: number
  parentId: number | null
  menuCode: string
  menuName: string
  routePath: string | null
  sortOrder: number
}

export function authMenus() {
  return http.get<unknown, AuthMenuItem[]>('/auth/menus')
}

export function logout() {
  return http.post<unknown, void>('/auth/logout')
}

export function changePassword(oldPassword: string, newPassword: string) {
  return http.post<unknown, { changed: boolean }>('/auth/change-password', { oldPassword, newPassword })
}
