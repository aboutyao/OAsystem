import zhCN from './zh-CN'

type Messages = typeof zhCN
type NestedKeyOf<T> = {
  [K in keyof T]: T[K] extends object ? `${K & string}.${NestedKeyOf<T[K]>}` : K & string
}[keyof T]

const messages: Record<string, Messages> = {
  'zh-CN': zhCN,
}

let currentLocale = 'zh-CN'

/**
 * 设置当前语言
 */
export function setLocale(locale: string) {
  if (messages[locale]) {
    currentLocale = locale
    localStorage.setItem('oa_locale', locale)
  }
}

/**
 * 获取当前语言
 */
export function getLocale() {
  return currentLocale
}

/**
 * 翻译函数
 * @param key 点分隔的翻译键，如 'common.loading'
 * @param params 插值参数
 */
export function t(key: string, params?: Record<string, string | number>): string {
  const keys = key.split('.')
  let value: unknown = messages[currentLocale]

  for (const k of keys) {
    if (value && typeof value === 'object') {
      value = (value as Record<string, unknown>)[k]
    } else {
      return key
    }
  }

  if (typeof value !== 'string') {
    return key
  }

  if (params) {
    return Object.entries(params).reduce(
      (result, [k, v]) => result.replace(new RegExp(`\\{${k}\\}`, 'g'), String(v)),
      value,
    )
  }

  return value
}

// 初始化语言
const savedLocale = localStorage.getItem('oa_locale')
if (savedLocale && messages[savedLocale]) {
  currentLocale = savedLocale
}
