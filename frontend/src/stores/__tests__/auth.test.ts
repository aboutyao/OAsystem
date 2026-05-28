import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth'

vi.mock('../../api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  me: vi.fn(),
  authMenus: vi.fn().mockResolvedValue([]),
  verifyTwoFactor: vi.fn(),
}))

vi.mock('../../api/http', () => ({
  http: {
    get: vi.fn().mockResolvedValue({ expired: false }),
    post: vi.fn(),
  },
}))

import { login, logout as apiLogout, me, authMenus } from '../../api/auth'

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('isAuthenticated', () => {
    it('returns false when no token', () => {
      const store = useAuthStore()
      expect(store.isAuthenticated).toBe(false)
    })

    it('returns true when token exists in localStorage', () => {
      localStorage.setItem('oa_access_token', 'token-123')
      const store = useAuthStore()
      expect(store.isAuthenticated).toBe(true)
    })
  })

  describe('isSuperUser', () => {
    it('returns false when user has no permissions', () => {
      const store = useAuthStore()
      store.user = { id: 1, username: 'test', realName: 'Test', mainDeptId: 1, mainDeptName: 'Dept', roles: [], permissions: [] }
      expect(store.isSuperUser).toBe(false)
    })

    it('returns true when user has * permission', () => {
      const store = useAuthStore()
      store.user = { id: 1, username: 'admin', realName: 'Admin', mainDeptId: 1, mainDeptName: 'Dept', roles: ['SUPER_ADMIN'], permissions: ['*'] }
      expect(store.isSuperUser).toBe(true)
    })
  })

  describe('signIn', () => {
    it('stores token and user on successful login', async () => {
      vi.mocked(login).mockResolvedValue({
        accessToken: 'jwt-token',
        expiresIn: 7200,
        user: { id: 1, username: 'admin', realName: 'Admin', mainDeptId: 1, mainDeptName: 'Dept', roles: ['SUPER_ADMIN'], permissions: ['*'] },
      })
      vi.mocked(authMenus).mockResolvedValue([])

      const store = useAuthStore()
      const result = await store.signIn('admin', 'password')

      expect(result.accessToken).toBe('jwt-token')
      expect(store.token).toBe('jwt-token')
      expect(store.isAuthenticated).toBe(true)
      expect(localStorage.getItem('oa_access_token')).toBe('jwt-token')
    })

    it('does not store token when 2FA is required', async () => {
      vi.mocked(login).mockResolvedValue({
        accessToken: '',
        expiresIn: 0,
        user: {} as any,
        requires2FA: true,
      })

      const store = useAuthStore()
      const result = await store.signIn('admin', 'password')

      expect(result.requires2FA).toBe(true)
      expect(store.token).toBeNull()
      expect(store.isAuthenticated).toBe(false)
    })

    it('sets passwordExpired when password has expired', async () => {
      vi.mocked(login).mockResolvedValue({
        accessToken: 'jwt-token',
        expiresIn: 7200,
        user: { id: 1, username: 'admin', realName: 'Admin', mainDeptId: 1, mainDeptName: 'Dept', roles: ['SUPER_ADMIN'], permissions: ['*'] },
        passwordExpired: true,
      })
      vi.mocked(authMenus).mockResolvedValue([])

      const store = useAuthStore()
      await store.signIn('admin', 'password')

      expect(store.passwordExpired).toBe(true)
    })
  })

  describe('signOut', () => {
    it('clears all state and localStorage', async () => {
      localStorage.setItem('oa_access_token', 'token-123')
      localStorage.setItem('oa_user', 'some-user')
      vi.mocked(apiLogout).mockResolvedValue(undefined)

      const store = useAuthStore()
      store.token = 'token-123'
      store.user = { id: 1, username: 'admin', realName: 'Admin', mainDeptId: 1, mainDeptName: 'Dept', roles: [], permissions: [] }

      await store.signOut()

      expect(store.token).toBeNull()
      expect(store.user).toBeNull()
      expect(store.isAuthenticated).toBe(false)
      expect(localStorage.getItem('oa_access_token')).toBeNull()
      expect(localStorage.getItem('oa_user')).toBeNull()
    })

    it('clears state even when backend logout fails', async () => {
      vi.mocked(apiLogout).mockRejectedValue(new Error('network'))

      const store = useAuthStore()
      store.token = 'token-123'

      await store.signOut()

      expect(store.token).toBeNull()
      expect(store.isAuthenticated).toBe(false)
    })
  })

  describe('isRouteAllowed', () => {
    it('allows /dashboard for all users', () => {
      const store = useAuthStore()
      expect(store.isRouteAllowed('/dashboard')).toBe(true)
    })

    it('allows all routes for super user', () => {
      const store = useAuthStore()
      store.user = { id: 1, username: 'admin', realName: 'Admin', mainDeptId: 1, mainDeptName: 'Dept', roles: ['SUPER_ADMIN'], permissions: ['*'] }
      expect(store.isRouteAllowed('/anything')).toBe(true)
    })

    it('denies routes not in navItems for regular user', () => {
      const store = useAuthStore()
      store.user = { id: 1, username: 'user', realName: 'User', mainDeptId: 1, mainDeptName: 'Dept', roles: ['USER'], permissions: [] }
      store.menus = [{ id: 1, parentId: null, menuCode: 'leaves', menuName: '请假', routePath: '/leaves', sortOrder: 1 }]
      expect(store.isRouteAllowed('/admin/users')).toBe(false)
    })

    it('allows routes matching navItems prefix', () => {
      const store = useAuthStore()
      store.user = { id: 1, username: 'user', realName: 'User', mainDeptId: 1, mainDeptName: 'Dept', roles: ['USER'], permissions: [] }
      store.menus = [{ id: 1, parentId: null, menuCode: 'leaves', menuName: '请假', routePath: '/leaves', sortOrder: 1 }]
      expect(store.isRouteAllowed('/leaves/123')).toBe(true)
    })
  })
})
