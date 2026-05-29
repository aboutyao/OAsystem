import type { FormItemRule } from 'element-plus'

/**
 * 通用表单校验规则
 */

/** 必填 */
export const required = (message: string): FormItemRule => ({
  required: true,
  message,
  trigger: 'blur',
})

/** 必选 */
export const requiredSelect = (message: string): FormItemRule => ({
  required: true,
  message,
  trigger: 'change',
})

/** 手机号 */
export const phone: FormItemRule = {
  pattern: /^1[3-9]\d{9}$/,
  message: '请输入正确的手机号',
  trigger: 'blur',
}

/** 邮箱 */
export const email: FormItemRule = {
  type: 'email',
  message: '请输入正确的邮箱地址',
  trigger: 'blur',
}

/** 身份证 */
export const idCard: FormItemRule = {
  pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/,
  message: '请输入正确的身份证号',
  trigger: 'blur',
}

/** 银行卡 */
export const bankCard: FormItemRule = {
  pattern: /^\d{16,19}$/,
  message: '请输入正确的银行卡号',
  trigger: 'blur',
}

/** URL */
export const url: FormItemRule = {
  type: 'url',
  message: '请输入正确的URL',
  trigger: 'blur',
}

/** 最小长度 */
export const minLength = (min: number, message?: string): FormItemRule => ({
  min,
  message: message || `至少输入 ${min} 个字符`,
  trigger: 'blur',
})

/** 最大长度 */
export const maxLength = (max: number, message?: string): FormItemRule => ({
  max,
  message: message || `最多输入 ${max} 个字符`,
  trigger: 'blur',
})

/** 字符串范围长度 */
export const rangeLength = (min: number, max: number, message?: string): FormItemRule => ({
  min,
  max,
  message: message || `长度需在 ${min} 到 ${max} 之间`,
  trigger: 'blur',
})

/** 正整数 */
export const positiveInteger: FormItemRule = {
  pattern: /^[1-9]\d*$/,
  message: '请输入正整数',
  trigger: 'blur',
}

/** 正数（含小数） */
export const positiveNumber: FormItemRule = {
  pattern: /^\d+(\.\d+)?$/,
  message: '请输入正数',
  trigger: 'blur',
}

/** 金额（最多两位小数） */
export const money: FormItemRule = {
  pattern: /^\d+(\.\d{1,2})?$/,
  message: '请输入正确的金额',
  trigger: 'blur',
}

/** 中文 */
export const chinese: FormItemRule = {
  pattern: /^[一-龥]+$/,
  message: '请输入中文',
  trigger: 'blur',
}

/** 密码（至少8位，包含字母和数字） */
export const password: FormItemRule = {
  pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,}$/,
  message: '密码至少8位，包含字母和数字',
  trigger: 'blur',
}

/** 组合校验器 */
export function composeRules(...rules: (FormItemRule | null | undefined)[]): FormItemRule[] {
  return rules.filter(Boolean) as FormItemRule[]
}
