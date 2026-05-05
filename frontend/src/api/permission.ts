import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function listRoles(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/permission/roles', { params: { page, size } })
}

export function getRole(id: number) {
  return http.get<unknown, JsonObject>(`/permission/roles/${id}`)
}

export function createRole(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/permission/roles', body)
}

export function updateRole(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/permission/roles/${id}`, body)
}

export function deleteRole(id: number) {
  return http.delete<unknown, JsonObject>(`/permission/roles/${id}`)
}

export function assignRoleMenus(id: number, menuIds: number[]) {
  return http.post<unknown, JsonObject>(`/permission/roles/${id}/menus`, { menuIds })
}

export function assignRoleButtons(id: number, buttonIds: number[]) {
  return http.post<unknown, JsonObject>(`/permission/roles/${id}/buttons`, { buttonIds })
}

export function assignRoleDataScopes(id: number, items: Record<string, unknown>[]) {
  return http.post<unknown, JsonObject>(`/permission/roles/${id}/data-scopes`, { items })
}

export function getMenuTree() {
  return http.get<unknown, JsonObject[]>('/permission/menus/tree')
}

export function createMenu(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/permission/menus', body)
}

export function updateMenu(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/permission/menus/${id}`, body)
}

export function listButtons(page = 1, size = 20, menuId?: number) {
  return http.get<unknown, PageResponse<JsonObject>>('/permission/buttons', {
    params: { page, size, ...(menuId != null ? { menuId } : {}) },
  })
}

export function createButton(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/permission/buttons', body)
}

export function previewUserPermission(userId: number) {
  return http.get<unknown, JsonObject>(`/permission/users/${userId}/preview`)
}

export function listTempAuths(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/permission/temp-auths', { params: { page, size } })
}

export function createTempAuth(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/permission/temp-auths', body)
}

export function revokeTempAuth(id: number) {
  return http.patch<unknown, JsonObject>(`/permission/temp-auths/${id}/revoke`)
}

export function listFieldPermissions(page = 1, size = 20, roleId?: number, businessType?: string) {
  return http.get<unknown, PageResponse<JsonObject>>('/permission/field-permissions', {
    params: {
      page,
      size,
      ...(roleId != null ? { roleId } : {}),
      ...(businessType ? { businessType } : {}),
    },
  })
}

export function createFieldPermission(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/permission/field-permissions', body)
}

export function updateFieldPermission(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/permission/field-permissions/${id}`, body)
}

export function deleteFieldPermission(id: number) {
  return http.delete<unknown, JsonObject>(`/permission/field-permissions/${id}`)
}
