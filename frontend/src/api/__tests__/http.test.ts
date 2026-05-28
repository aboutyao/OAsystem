import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { http } from '../http'

describe('HTTP interceptors', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('request interceptor', () => {
    it('attaches Bearer token from localStorage', () => {
      localStorage.setItem('oa_access_token', 'test-token-123')
      const config = { headers: {} as Record<string, string>, method: 'get' }
      const interceptor = (http.interceptors.request as any).handlers?.[0]
      if (interceptor?.fulfilled) {
        const result = interceptor.fulfilled(config)
        expect(result.headers.Authorization).toBe('Bearer test-token-123')
      }
    })

    it('does not attach Authorization header when no token', () => {
      const config = { headers: {} as Record<string, string>, method: 'get' }
      const interceptor = (http.interceptors.request as any).handlers?.[0]
      if (interceptor?.fulfilled) {
        const result = interceptor.fulfilled(config)
        expect(result.headers.Authorization).toBeUndefined()
      }
    })

    it('adds idempotency key for POST requests', () => {
      const config = { headers: {} as Record<string, string>, method: 'post' }
      const interceptor = (http.interceptors.request as any).handlers?.[0]
      if (interceptor?.fulfilled) {
        const result = interceptor.fulfilled(config)
        expect(result.headers['X-Idempotency-Key']).toBeDefined()
        expect(result.headers['X-Idempotency-Key']).toMatch(
          /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/
        )
      }
    })

    it('does not add idempotency key for GET requests', () => {
      const config = { headers: {} as Record<string, string>, method: 'get' }
      const interceptor = (http.interceptors.request as any).handlers?.[0]
      if (interceptor?.fulfilled) {
        const result = interceptor.fulfilled(config)
        expect(result.headers['X-Idempotency-Key']).toBeUndefined()
      }
    })
  })

  describe('response interceptor', () => {
    it('unwraps SUCCESS response body', () => {
      const response = {
        data: { code: 'SUCCESS', data: { id: 1 }, message: 'ok', requestId: 'r1', timestamp: 't1' },
      }
      const interceptor = (http.interceptors.response as any).handlers?.[0]
      if (interceptor?.fulfilled) {
        const result = interceptor.fulfilled(response)
        expect(result).toEqual({ id: 1 })
      }
    })

    it('rejects non-SUCCESS response code', async () => {
      const response = {
        data: { code: 'NOT_FOUND', data: null, message: 'not found', requestId: 'r1', timestamp: 't1' },
      }
      const interceptor = (http.interceptors.response as any).handlers?.[0]
      if (interceptor?.fulfilled) {
        await expect(interceptor.fulfilled(response)).rejects.toThrow('not found')
      }
    })
  })
})
