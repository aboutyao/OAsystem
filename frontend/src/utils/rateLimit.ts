/**
 * 请求频率限制工具
 */

interface RateLimitConfig {
  /** 时间窗口（毫秒） */
  windowMs: number
  /** 最大请求数 */
  maxRequests: number
}

interface RateLimitEntry {
  count: number
  resetTime: number
}

const defaultConfig: RateLimitConfig = {
  windowMs: 60000, // 1 分钟
  maxRequests: 100, // 最大 100 次请求
}

const rateLimitMap = new Map<string, RateLimitEntry>()

/**
 * 检查是否超过频率限制
 */
export function checkRateLimit(key: string, config: Partial<RateLimitConfig> = {}): boolean {
  const { windowMs, maxRequests } = { ...defaultConfig, ...config }
  const now = Date.now()

  const entry = rateLimitMap.get(key)

  if (!entry || now > entry.resetTime) {
    // 新的时间窗口
    rateLimitMap.set(key, { count: 1, resetTime: now + windowMs })
    return true
  }

  if (entry.count >= maxRequests) {
    // 超过限制
    return false
  }

  // 增加计数
  entry.count++
  return true
}

/**
 * 获取剩余请求数
 */
export function getRemainingRequests(key: string, config: Partial<RateLimitConfig> = {}): number {
  const { maxRequests } = { ...defaultConfig, ...config }
  const now = Date.now()

  const entry = rateLimitMap.get(key)

  if (!entry || now > entry.resetTime) {
    return maxRequests
  }

  return Math.max(0, maxRequests - entry.count)
}

/**
 * 重置频率限制
 */
export function resetRateLimit(key: string) {
  rateLimitMap.delete(key)
}

/**
 * 清理过期的频率限制记录
 */
export function cleanupRateLimits() {
  const now = Date.now()
  for (const [key, entry] of rateLimitMap.entries()) {
    if (now > entry.resetTime) {
      rateLimitMap.delete(key)
    }
  }
}

// 每分钟清理一次过期记录
setInterval(cleanupRateLimits, 60000)

/**
 * 防抖函数
 */
export function debounce<T extends (...args: unknown[]) => unknown>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timer: ReturnType<typeof setTimeout> | null = null

  return (...args: Parameters<T>) => {
    if (timer) {
      clearTimeout(timer)
    }
    timer = setTimeout(() => {
      fn(...args)
      timer = null
    }, delay)
  }
}

/**
 * 节流函数
 */
export function throttle<T extends (...args: unknown[]) => unknown>(
  fn: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle = false

  return (...args: Parameters<T>) => {
    if (!inThrottle) {
      fn(...args)
      inThrottle = true
      setTimeout(() => {
        inThrottle = false
      }, limit)
    }
  }
}
