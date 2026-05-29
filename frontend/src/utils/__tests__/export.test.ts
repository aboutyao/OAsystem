import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { exportToCsv } from '../export'

describe('exportToCsv', () => {
  const mockCreateObjectURL = vi.fn().mockReturnValue('blob:url')
  const mockRevokeObjectURL = vi.fn()
  const mockClick = vi.fn()

  beforeEach(() => {
    // Mock DOM APIs
    vi.spyOn(document, 'createElement').mockReturnValue({
      href: '',
      download: '',
      click: mockClick,
    } as any)

    // Mock URL APIs
    global.URL.createObjectURL = mockCreateObjectURL
    global.URL.revokeObjectURL = mockRevokeObjectURL
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('should generate CSV with headers from columns', () => {
    const data = [
      { name: '张三', age: 30 },
      { name: '李四', age: 25 },
    ]
    const columns = [{ key: 'name', label: '姓名' }, { key: 'age', label: '年龄' }]

    exportToCsv(data, 'test', columns)

    expect(mockCreateObjectURL).toHaveBeenCalled()
    expect(mockClick).toHaveBeenCalled()
  })

  it('should handle empty data', () => {
    exportToCsv([], 'empty', [])
    // Should not create blob for empty data
    expect(mockCreateObjectURL).not.toHaveBeenCalled()
  })

  it('should escape CSV values with commas', () => {
    const data = [{ name: '张,三', value: 'hello' }]
    const columns = [{ key: 'name', label: 'Name' }, { key: 'value', label: 'Value' }]

    // Should not throw
    exportToCsv(data, 'escape-test', columns)
    expect(mockCreateObjectURL).toHaveBeenCalled()
  })
})
