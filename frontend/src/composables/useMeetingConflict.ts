import { ref, watch } from 'vue'
import { http } from '../api/http'

interface ConflictInfo {
  hasConflict: boolean
  conflictingBookings: Array<{
    title: string
    startAt: string
    endAt: string
    organizerName: string
  }>
}

export function useMeetingConflict(roomId: () => number | null) {
  const conflict = ref<ConflictInfo>({ hasConflict: false, conflictingBookings: [] })
  const checking = ref(false)

  async function checkConflict(startAt: string, endAt: string) {
    if (!roomId() || !startAt || !endAt) {
      conflict.value = { hasConflict: false, conflictingBookings: [] }
      return
    }
    checking.value = true
    try {
      const bookings = await http.get(`/calendar/meetings`, {
        params: { year: new Date(startAt).getFullYear(), month: new Date(startAt).getMonth() + 1 },
      }) as any[]
      const start = new Date(startAt).getTime()
      const end = new Date(endAt).getTime()
      const overlaps = bookings.filter((b: any) => {
        const bStart = new Date(b.startAt).getTime()
        const bEnd = new Date(b.endAt).getTime()
        return start < bEnd && end > bStart
      })
      conflict.value = {
        hasConflict: overlaps.length > 0,
        conflictingBookings: overlaps.map((b: any) => ({
          title: b.title,
          startAt: b.startAt,
          endAt: b.endAt,
          organizerName: b.organizerName,
        })),
      }
    } catch {
      conflict.value = { hasConflict: false, conflictingBookings: [] }
    } finally {
      checking.value = false
    }
  }

  return { conflict, checking, checkConflict }
}
