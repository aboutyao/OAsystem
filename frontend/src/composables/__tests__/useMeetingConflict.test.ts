import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useMeetingConflict } from '../useMeetingConflict'

vi.mock('../../api/http', () => ({
  http: {
    get: vi.fn(),
  },
}))

import { http } from '../../api/http'

describe('useMeetingConflict', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should detect no conflict when no bookings overlap', async () => {
    vi.mocked(http.get).mockResolvedValue([])
    const { conflict, checkConflict } = useMeetingConflict(() => 1)

    await checkConflict('2026-06-01T10:00:00', '2026-06-01T11:00:00')

    expect(conflict.value.hasConflict).toBe(false)
    expect(conflict.value.conflictingBookings).toHaveLength(0)
  })

  it('should detect conflict when bookings overlap', async () => {
    vi.mocked(http.get).mockResolvedValue([
      { title: '已有会议', startAt: '2026-06-01T09:30:00', endAt: '2026-06-01T10:30:00', organizerName: '张三' },
    ])
    const { conflict, checkConflict } = useMeetingConflict(() => 1)

    await checkConflict('2026-06-01T10:00:00', '2026-06-01T11:00:00')

    expect(conflict.value.hasConflict).toBe(true)
    expect(conflict.value.conflictingBookings).toHaveLength(1)
    expect(conflict.value.conflictingBookings[0].title).toBe('已有会议')
  })

  it('should not check when roomId is null', async () => {
    const { conflict, checkConflict } = useMeetingConflict(() => null)

    await checkConflict('2026-06-01T10:00:00', '2026-06-01T11:00:00')

    expect(conflict.value.hasConflict).toBe(false)
    expect(http.get).not.toHaveBeenCalled()
  })

  it('should handle API errors gracefully', async () => {
    vi.mocked(http.get).mockRejectedValue(new Error('network'))
    const { conflict, checkConflict } = useMeetingConflict(() => 1)

    await checkConflict('2026-06-01T10:00:00', '2026-06-01T11:00:00')

    expect(conflict.value.hasConflict).toBe(false)
  })
})
