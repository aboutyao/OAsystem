import { describe, it, expect } from 'vitest'
import { getAttachmentSuggestions } from '../useAttachmentCheck'

describe('useAttachmentCheck', () => {
  describe('getAttachmentSuggestions', () => {
    it('should return suggestions for fixed asset purchase', () => {
      const result = getAttachmentSuggestions('固定资产', 'purchase')
      expect(result.required).toContain('报价单')
    })

    it('should return empty for unknown type', () => {
      const result = getAttachmentSuggestions('未知类型', 'purchase')
      expect(result.required).toHaveLength(0)
    })
  })
})
