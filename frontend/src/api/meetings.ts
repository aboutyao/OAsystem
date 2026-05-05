import { http } from './http'
import type { JsonObject, PageResponse } from './types'

export function listMeetingRooms(page = 1, size = 20) {
  return http.get<unknown, PageResponse<JsonObject>>('/meetings/rooms', { params: { page, size } })
}

export function createMeetingRoom(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/meetings/rooms', body)
}

export function updateMeetingRoom(id: number, body: Record<string, unknown>) {
  return http.put<unknown, JsonObject>(`/meetings/rooms/${id}`, body)
}

export function listMeetingBookings(page = 1, size = 20, roomId?: number) {
  return http.get<unknown, PageResponse<JsonObject>>('/meetings/bookings', {
    params: { page, size, ...(roomId != null ? { roomId } : {}) },
  })
}

export function createMeetingBooking(body: Record<string, unknown>) {
  return http.post<unknown, JsonObject>('/meetings/bookings', body)
}

export function cancelMeetingBooking(id: number, body?: { cancelReason?: string | null }) {
  return http.post<unknown, JsonObject>(`/meetings/bookings/${id}/cancel`, body ?? {})
}

export function meetingRoomAvailability(roomId: number) {
  return http.get<unknown, JsonObject[]>('/meetings/rooms/' + roomId + '/availability')
}
