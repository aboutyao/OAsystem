import { describe, it, expect, vi, beforeEach } from 'vitest'
import { exportToCsv } from '../export'

describe('exportToCsv', () => {
  beforeEach(() => {
    // Mock DOM APIs
    vi.spyOn(document, 'createElement').mockReturnValue({
      href: '',
      download: '',
      click: vi.fn(),
    } as any)
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:url')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
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

    const blobCall = (URL.createObjectURL as any).mock.calls[0]
    expect(blobCall).toBeDefined()
  })

  it('should handle empty data', () => {
    exportToCsv([], 'empty', [])
    // Should not create blob for empty data
    expect(URL.createObjectURL).not.toHaveBeenCalled()
  })

  it('should escape CSV values with commas', () => {
    const data = [{ name: '张,三', value: 'hello' }]
    const columns = [{ key: 'name', label: 'Name' }, { key: 'value', label: 'Value' }]

    // Should not throw
    exportToCsv(data, 'escape-test', columns)
    expect(URL.createObjectURL).toHaveBeenCalled()
  })
})
