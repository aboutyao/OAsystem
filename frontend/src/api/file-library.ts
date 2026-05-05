import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function listLibraryFolders() {
  return http.get<unknown, JsonObject[]>('/file-library/folders')
}

export function createLibraryFolder(body: { parentId?: number | null; folderName: string }) {
  return http.post<unknown, JsonObject>('/file-library/folders', body)
}

export function updateLibraryFolder(id: number, body: { folderName: string }) {
  return http.put<unknown, JsonObject>(`/file-library/folders/${id}`, body)
}

export function listLibraryFiles(page = 1, size = 20, folderId?: number, keyword?: string) {
  return http.get<unknown, PageResponse<JsonObject>>('/file-library/files', {
    params: {
      page,
      size,
      ...(folderId != null ? { folderId } : {}),
      ...(keyword ? { keyword } : {}),
    },
  })
}

export function createLibraryFile(body: {
  folderId?: number | null
  fileName: string
  mimeType?: string | null
  fileSize?: number | null
}) {
  return http.post<unknown, JsonObject>('/file-library/files', body)
}

export function getLibraryFile(id: number) {
  return http.get<unknown, JsonObject>(`/file-library/files/${id}`)
}

export function addLibraryFileVersion(id: number, body: { fileName: string; mimeType?: string | null; fileSize?: number | null }) {
  return http.post<unknown, JsonObject>(`/file-library/files/${id}/versions`, body)
}

export function moveLibraryFile(id: number, body: { folderId: number }) {
  return http.post<unknown, JsonObject>(`/file-library/files/${id}/move`, body)
}

export function deleteLibraryFile(id: number) {
  return http.delete<unknown, JsonObject>(`/file-library/files/${id}`)
}

export function listLibraryDownloadLogs(id: number) {
  return http.get<unknown, JsonObject[]>(`/file-library/files/${id}/download-logs`)
}

export function listRecycleBinFiles(page = 1, size = 20, keyword?: string) {
  return http.get<unknown, PageResponse<JsonObject>>('/file-library/recycle-bin', {
    params: {
      page,
      size,
      ...(keyword ? { keyword } : {}),
    },
  })
}

export function restoreFile(id: number) {
  return http.post<unknown, JsonObject>(`/file-library/recycle-bin/${id}/restore`)
}

export function deleteFilePermanently(id: number) {
  return http.delete<unknown, JsonObject>(`/file-library/recycle-bin/${id}`)
}
