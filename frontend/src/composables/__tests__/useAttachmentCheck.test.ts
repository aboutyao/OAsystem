import { describe, it, expect, vi, beforeEach } from 'vitest'
import { checkAttachmentCompleteness, getAttachmentSuggestions } from '../useAttachmentCheck'

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElMessage: {
    warning: vi.fn(),
    error: vi.fn(),
    success: vi.fn(),
  },
  ElMessageBox: {
    confirm: vi.fn(),
  },
}))

describe('useAttachmentCheck', () => {
  describe('getAttachmentSuggestions', () => {
    it('should return suggestions for fixed asset purchase', () => {
      const result = getAttachmentSuggestions('固定资产', 'purchase')
      expect(result.required).toContain('报价单')
      expect(result.optional).toContain('采购申请单')
    })

    it('should return suggestions for service purchase', () => {
      const result = getAttachmentSuggestions('服务采购', 'purchase')
      expect(result.required).toContain('合同')
    })

    it('should return empty for office supplies', () => {
      const result = getAttachmentSuggestions('办公用品', 'purchase')
      expect(result.required).toHaveLength(0)
    })

    it('should return suggestions for contract seal', () => {
      const result = getAttachmentSuggestions('合同章', 'seal')
      expect(result.required).toContain('合同')
    })

    it('should return empty for unknown type', () => {
      const result = getAttachmentSuggestions('未知类型', 'purchase')
      expect(result.required).toHaveLength(0)
    })
  })

  describe('checkAttachmentCompleteness', () => {
    beforeEach(() => {
      vi.clearAllMocks()
    })

    it('should pass when no rules required', async () => {
      const attachments = [{ name: 'test.pdf', url: 'http://example.com', size: 1024 }]
      const result = await checkAttachmentCompleteness(attachments, '办公用品', 'purchase')
      expect(result).toBe(true)
    })

    it('should pass when required attachments are present', async () => {
      const attachments = [
        { name: '报价单.pdf', url: 'http://example.com', size: 1024 },
      ]
      const result = await checkAttachmentCompleteness(attachments, '固定资产', 'purchase')
      expect(result).toBe(true)
    })

    it('should pass when no rules configured', async () => {
      const attachments: Array<{ name: string; url: string; size: number }> = []
      const result = await checkAttachmentCompleteness(attachments, '未知类型', 'purchase')
      expect(result).toBe(true)
    })
  })
})
