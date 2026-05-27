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
  passwordExpired?: boolean
  requires2FA?: boolean
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

// ========== Two-Factor Authentication ==========

export function verifyTwoFactor(tempToken: string, code: string) {
  return http.post<unknown, LoginResponse>('/auth/2fa/verify', { tempToken, code })
}

export function setupTwoFactor() {
  return http.post<unknown, { secret: string; qrCode: string }>('/auth/2fa/setup')
}

export function enableTwoFactor(secret: string, code: string) {
  return http.post<unknown, { enabled: boolean }>('/auth/2fa/enable', { secret, code })
}

export function disableTwoFactor(code: string) {
  return http.post<unknown, { disabled: boolean }>('/auth/2fa/disable', { code })
}
