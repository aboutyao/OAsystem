/**
 * XSS 防护工具
 */

/**
 * HTML 转义
 */
export function escapeHtml(text: string): string {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;',
  }
  return text.replace(/[&<>"']/g, (m) => map[m])
}

/**
 * 移除 HTML 标签
 */
export function stripHtml(html: string): string {
  return html.replace(/<[^>]*>/g, '')
}

/**
 * 清理 URL（防止 javascript: 协议攻击）
 */
export function sanitizeUrl(url: string): string {
  const trimmed = url.trim().toLowerCase()
  if (trimmed.startsWith('javascript:') || trimmed.startsWith('data:text/html')) {
    return ''
  }
  return url
}

/**
 * 清理用户输入
 */
export function sanitizeInput(input: string): string {
  // 移除潜在的 XSS 攻击代码
  return input
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    .replace(/on\w+="[^"]*"/gi, '')
    .replace(/on\w+='[^']*'/gi, '')
    .replace(/javascript:/gi, '')
}

/**
 * 安全地设置 innerHTML
 * 使用方式：v-html="safeHtml(userInput)"
 */
export function safeHtml(html: string): string {
  // 允许的安全标签
  const allowedTags = ['p', 'br', 'b', 'i', 'em', 'strong', 'a', 'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6']
  const allowedAttrs = ['href', 'target', 'class', 'id']

  // 简单的白名单过滤
  let sanitized = html

  // 移除所有不在白名单中的标签
  sanitized = sanitized.replace(/<\/?([a-z][a-z0-9]*)\b[^>]*>/gi, (match, tag) => {
    if (allowedTags.includes(tag.toLowerCase())) {
      // 只保留允许的属性
      return match.replace(/\s+(?!href|target|class|id)[a-z-]+="[^"]*"/gi, '')
    }
    return ''
  })

  // 移除事件处理器
  sanitized = sanitized.replace(/\s*on\w+="[^"]*"/gi, '')
  sanitized = sanitized.replace(/\s*on\w+='[^']*'/gi, '')

  // 清理 URL
  sanitized = sanitized.replace(/href="javascript:[^"]*"/gi, 'href="#"')

  return sanitized
}

/**
 * Vue 指令：安全的 v-html
 */
export const vSafeHtml = {
  mounted(el: HTMLElement, binding: { value: string }) {
    el.innerHTML = safeHtml(binding.value)
  },
  updated(el: HTMLElement, binding: { value: string }) {
    el.innerHTML = safeHtml(binding.value)
  },
}
