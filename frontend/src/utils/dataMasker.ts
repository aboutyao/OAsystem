/**
 * 敏感数据脱敏工具
 * 前端展示时使用
 */

/**
 * 手机号脱敏
 * 138****5678
 */
export function maskPhone(phone: string | null | undefined): string {
  if (!phone || phone.length < 7) return phone || ''
  return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
}

/**
 * 身份证脱敏
 * 110***********1234
 */
export function maskIdCard(idCard: string | null | undefined): string {
  if (!idCard || idCard.length < 8) return idCard || ''
  return idCard.substring(0, 3) + '*'.repeat(idCard.length - 7) + idCard.substring(idCard.length - 4)
}

/**
 * 银行卡脱敏
 * 6222 **** **** 1234
 */
export function maskBankCard(bankCard: string | null | undefined): string {
  if (!bankCard || bankCard.length < 8) return bankCard || ''
  return bankCard.substring(0, 4) + ' **** **** ' + bankCard.substring(bankCard.length - 4)
}

/**
 * 邮箱脱敏
 * t***@example.com
 */
export function maskEmail(email: string | null | undefined): string {
  if (!email || !email.includes('@')) return email || ''
  const atIndex = email.indexOf('@')
  if (atIndex <= 1) return email
  return email.substring(0, 1) + '***' + email.substring(atIndex)
}

/**
 * 姓名脱敏
 * 张*、张*明
 */
export function maskName(name: string | null | undefined): string {
  if (!name) return ''
  if (name.length === 1) return '*'
  if (name.length === 2) return name.substring(0, 1) + '*'
  return name.substring(0, 1) + '*'.repeat(name.length - 2) + name.substring(name.length - 1)
}

/**
 * 地址脱敏
 * 北京市****小区
 */
export function maskAddress(address: string | null | undefined): string {
  if (!address || address.length <= 6) return address || ''
  return address.substring(0, 6) + '****'
}

/**
 * 通用脱敏
 * 保留前3后4
 */
export function maskGeneral(text: string | null | undefined): string {
  if (!text || text.length <= 7) return text || ''
  return text.substring(0, 3) + '****' + text.substring(text.length - 4)
}

/**
 * Vue 指令：自动脱敏
 * 使用方式：v-mask="'phone'" 或 v-mask:phone="value"
 */
export const vMask = {
  mounted(el: HTMLElement, binding: { value: string; arg?: string }) {
    const value = el.textContent || ''
    const type = binding.arg || binding.value
    let masked = value

    switch (type) {
      case 'phone':
        masked = maskPhone(value)
        break
      case 'idcard':
        masked = maskIdCard(value)
        break
      case 'bankcard':
        masked = maskBankCard(value)
        break
      case 'email':
        masked = maskEmail(value)
        break
      case 'name':
        masked = maskName(value)
        break
      case 'address':
        masked = maskAddress(value)
        break
      default:
        masked = maskGeneral(value)
    }

    el.textContent = masked
  },
}

/**
 * 组合式函数：脱敏显示
 */
export function useMaskedDisplay(value: () => string, type: string) {
  const masked = computed(() => {
    const val = value()
    switch (type) {
      case 'phone': return maskPhone(val)
      case 'idcard': return maskIdCard(val)
      case 'bankcard': return maskBankCard(val)
      case 'email': return maskEmail(val)
      case 'name': return maskName(val)
      case 'address': return maskAddress(val)
      default: return maskGeneral(val)
    }
  })
  return { masked }
}

// 导入 computed
import { computed } from 'vue'
