import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authMenus, login, me, type AuthMenuItem, type CurrentUser } from '../api/auth'

const TOKEN_KEY = 'oa_access_token'

export interface NavGroup {
  id: number
  label: string
  icon: string
  children: { path: string; label: string }[]
}

const ICON_MAP: Record<string, string> = {
  dashboard: 'Odometer',
  message: 'ChatDotRound',
  workflow: 'SetUp',
  oa: 'EditPen',
  contract: 'Document',
  notice: 'Bell',
  meeting: 'Calendar',
  asset: 'Box',
  file: 'FolderOpened',
  report: 'DataAnalysis',
  system: 'Setting',
  org: 'UserFilled',
  permission: 'Lock',
  rule: 'SetUp',
  form: 'Notebook',
  audit: 'View',
  ops: 'Monitor',
}

function guessIcon(menuCode: string, routePath: string): string {
  const code = menuCode.toLowerCase()
  for (const [key, icon] of Object.entries(ICON_MAP)) {
    if (code.includes(key) || routePath.includes(key)) return icon
  }
  return 'Menu'
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<CurrentUser | null>(null)
  const menus = ref<AuthMenuItem[]>([])

  const isAuthenticated = computed(() => Boolean(token.value))

  const isSuperUser = computed(() => Boolean(user.value?.permissions?.includes('*')))

  const navItems = computed(() => {
    const items = menus.value
      .filter((m) => m.routePath)
      .map((m) => ({ path: m.routePath as string, label: m.menuName }))
    if (items.length === 0 && token.value) {
      return [{ path: '/dashboard', label: '工作台' }]
    }
    return items
  })

  const navGroups = computed<NavGroup[]>(() => {
    const all = menus.value
    if (all.length === 0 && token.value) {
      return [{ id: 0, label: '工作台', icon: 'Odometer', children: [{ path: '/dashboard', label: '工作台' }] }]
    }

    const parents = all.filter((m) => !m.routePath).sort((a, b) => a.sortOrder - b.sortOrder)
    const leaves = all.filter((m) => m.routePath)

    const groups: NavGroup[] = parents.map((p) => ({
      id: p.id,
      label: p.menuName,
      icon: guessIcon(p.menuCode, ''),
      children: leaves
        .filter((l) => l.parentId === p.id)
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .map((l) => ({ path: l.routePath as string, label: l.menuName })),
    }))

    const orphanLeaves = leaves.filter((l) => !parents.some((p) => p.id === l.parentId))
    if (orphanLeaves.length > 0) {
      groups.push({
        id: -1,
        label: '其他',
        icon: 'Menu',
        children: orphanLeaves.map((l) => ({ path: l.routePath as string, label: l.menuName })),
      })
    }

    return groups.filter((g) => g.children.length > 0)
  })

  async function loadMenus() {
    if (!token.value) {
      menus.value = []
      return
    }
    try {
      menus.value = await authMenus()
    } catch {
      menus.value = []
    }
  }

  async function signIn(username: string, password: string) {
    const result = await login({ username, password })
    token.value = result.accessToken
    user.value = result.user
    localStorage.setItem(TOKEN_KEY, result.accessToken)
    await loadMenus()
  }

  async function loadCurrentUser() {
    if (!token.value) return
    user.value = await me()
    await loadMenus()
  }

  function signOut() {
    token.value = null
    user.value = null
    menus.value = []
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem('oa_user')
  }

  function isRouteAllowed(path: string): boolean {
    if (isSuperUser.value) {
      return true
    }
    if (path === '/dashboard' || path === '/') {
      return true
    }
    const prefixes = navItems.value.map((i) => i.path).filter(Boolean)
    for (const p of prefixes) {
      if (path === p || path.startsWith(p + '/')) {
        return true
      }
    }
    return false
  }

  return {
    token,
    user,
    menus,
    navItems,
    navGroups,
    isAuthenticated,
    isSuperUser,
    isRouteAllowed,
    signIn,
    signOut,
    loadCurrentUser,
    loadMenus,
  }
})
