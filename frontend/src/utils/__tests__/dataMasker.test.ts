import { describe, it, expect } from 'vitest'
import {
  maskPhone,
  maskIdCard,
  maskBankCard,
  maskEmail,
  maskName,
  maskAddress,
  maskGeneral,
} from '../dataMasker'

describe('dataMasker', () => {
  describe('maskPhone', () => {
    it('should mask phone number correctly', () => {
      expect(maskPhone('13812345678')).toBe('138****5678')
    })

    it('should return empty string for null input', () => {
      expect(maskPhone(null)).toBe('')
    })

    it('should return empty string for empty input', () => {
      expect(maskPhone('')).toBe('')
    })

    it('should return original for short phone', () => {
      expect(maskPhone('123')).toBe('123')
    })
  })

  describe('maskIdCard', () => {
    it('should mask 18-digit ID card', () => {
      expect(maskIdCard('110101199001011234')).toBe('110***********1234')
    })

    it('should mask 15-digit ID card', () => {
      expect(maskIdCard('110101900101123')).toBe('110********1123')
    })

    it('should return empty string for null input', () => {
      expect(maskIdCard(null)).toBe('')
    })
  })

  describe('maskBankCard', () => {
    it('should mask bank card number', () => {
      expect(maskBankCard('6222021234567890123')).toBe('6222 **** **** 0123')
    })

    it('should return empty string for null input', () => {
      expect(maskBankCard(null)).toBe('')
    })
  })

  describe('maskEmail', () => {
    it('should mask email correctly', () => {
      expect(maskEmail('test@example.com')).toBe('t***@example.com')
    })

    it('should return empty string for null input', () => {
      expect(maskEmail(null)).toBe('')
    })

    it('should return original for invalid email', () => {
      expect(maskEmail('invalid')).toBe('invalid')
    })
  })

  describe('maskName', () => {
    it('should mask single character name', () => {
      expect(maskName('张')).toBe('*')
    })

    it('should mask two character name', () => {
      expect(maskName('张三')).toBe('张*')
    })

    it('should mask three character name', () => {
      expect(maskName('张三丰')).toBe('张*丰')
    })

    it('should return empty string for null', () => {
      expect(maskName(null)).toBe('')
    })
  })

  describe('maskAddress', () => {
    it('should mask long address', () => {
      expect(maskAddress('北京市朝阳区建国路100号')).toBe('北京市朝阳区****')
    })

    it('should return original for short address', () => {
      expect(maskAddress('北京')).toBe('北京')
    })
  })

  describe('maskGeneral', () => {
    it('should mask general text', () => {
      expect(maskGeneral('1234567890')).toBe('123****7890')
    })

    it('should return original for short text', () => {
      expect(maskGeneral('1234567')).toBe('1234567')
    })
  })
})
