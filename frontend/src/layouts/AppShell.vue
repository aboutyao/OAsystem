<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getDashboardSummary, type DashboardSummary } from '../api/dashboard'
import { globalSearch, type SearchResult } from '../api/search'
import { useNotificationSSE } from '../composables/useNotificationSSE'
import CommandPalette from '../components/CommandPalette.vue'
import NotificationCenter from '../components/NotificationCenter.vue'
import SmartCalendar from '../components/SmartCalendar.vue'
import { useKeyboardShortcuts } from '../composables/useKeyboardShortcuts'
import ShortcutHelp from '../components/ShortcutHelp.vue'
import OnboardingOverlay from '../components/OnboardingOverlay.vue'
import NaturalLanguageQuery from '../components/NaturalLanguageQuery.vue'

useKeyboardShortcuts()

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapsed = ref(false)
const dashSummary = ref<DashboardSummary | null>(null)
const mobileMenuOpen = ref(false)
const isDark = ref(localStorage.getItem('theme') === 'dark')
const commandPaletteRef = ref<InstanceType<typeof CommandPalette>>()
const notificationCenterRef = ref<InstanceType<typeof NotificationCenter>>()
const smartCalendarRef = ref<InstanceType<typeof SmartCalendar>>()

// Real-time notification via SSE
const { connected: sseConnected, connect: connectSSE } = useNotificationSSE(undefined, { autoConnect: false })

// Global search state
const searchQuery = ref('')
const searchResults = ref<SearchResult | null>(null)
const searchPopoverVisible = ref(false)
const searchLoading = ref(false)
let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null

function onSearchInput(val: string) {
  if (searchDebounceTimer) clearTimeout(searchDebounceTimer)
  if (!val || val.trim().length === 0) {
    searchResults.value = null
    searchLoading.value = false
    return
  }
  searchLoading.value = true
  searchDebounceTimer = setTimeout(async () => {
    try {
      searchResults.value = await globalSearch(val.trim(), 5)
    } catch {
      searchResults.value = null
    } finally {
      searchLoading.value = false
    }
  }, 300)
}

function navigateSearchResult(type: string, item: Record<string, unknown>) {
  searchPopoverVisible.value = false
  searchQuery.value = ''
  searchResults.value = null
  switch (type) {
    case 'user':
      router.push('/org/users')
      break
    case 'leave':
      router.push(`/oa/leaves/${item.id}`)
      break
    case 'file':
      router.push(`/files/${item.id}`)
      break
    case 'expense':
      router.push(`/oa/expenses/${item.id}`)
      break
    case 'purchase':
      router.push(`/oa/purchases/${item.id}`)
      break
    case 'contract':
      router.push(`/contracts/${item.id}`)
      break
    case 'seal':
      router.push(`/oa/seals/${item.id}`)
      break
    case 'notice':
      router.push('/notices')
      break
    case 'workflow':
      router.push('/applications')
      break
  }
}

function onSearchClear() {
  searchResults.value = null
  searchLoading.value = false
}

function hasAnyResults(): boolean {
  if (!searchResults.value) return false
  return (
    (searchResults.value.users?.length ?? 0) > 0 ||
    (searchResults.value.leaves?.length ?? 0) > 0 ||
    (searchResults.value.files?.length ?? 0) > 0 ||
    (searchResults.value.expenses?.length ?? 0) > 0 ||
    (searchResults.value.purchases?.length ?? 0) > 0 ||
    (searchResults.value.contracts?.length ?? 0) > 0 ||
    (searchResults.value.seals?.length ?? 0) > 0 ||
    (searchResults.value.notices?.length ?? 0) > 0 ||
    (searchResults.value.workflows?.length ?? 0) > 0
  )
}

// Close search popover on route change
watch(() => route.path, () => {
  searchPopoverVisible.value = false
})

const title = computed(() => route.meta.title ?? '企业 OA')
const asideWidth = computed(() => (isCollapsed.value ? '64px' : '240px'))

const notificationCount = computed(() => {
  if (!dashSummary.value) return 0
  return (dashSummary.value.todoCount ?? 0) + (dashSummary.value.messageCount ?? 0) + (dashSummary.value.ccCount ?? 0)
})

// 路由变化时刷新未读计数
watch(() => route.path, () => {
  loadNotificationCount()
})

// 每30秒自动刷新未读计数
let refreshInterval: ReturnType<typeof setInterval> | null = null
onMounted(() => {
  refreshInterval = setInterval(loadNotificationCount, 30000)
})
onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval)
})

// Breadcrumb from menu hierarchy
const breadcrumbs = computed(() => {
  const items: { title: string; path: string }[] = [{ title: '工作台', path: '/dashboard' }]
  const currentPath = route.path
  const allMenus = authStore.menus

  // Find the menu item whose routePath matches the current route
  const currentMenu = allMenus.find((m) => m.routePath === currentPath)
  if (currentMenu) {
    // Walk up the parent chain and prepend parent items
    const trail: { title: string; path: string }[] = []
    let menu = currentMenu
    while (menu.parentId != null) {
      const parent = allMenus.find((m) => m.id === menu.parentId)
      if (parent) {
        trail.unshift({ title: parent.menuName, path: parent.routePath || '' })
        menu = parent
      } else {
        break
      }
    }
    items.push(...trail)
    // Add the current page (skip if it's already the dashboard)
    if (currentPath !== '/dashboard') {
      items.push({ title: currentMenu.menuName, path: currentPath })
    }
  } else {
    // Fallback: use route meta title
    const metaTitle = route.meta.title as string | undefined
    if (metaTitle && currentPath !== '/dashboard') {
      items.push({ title: metaTitle, path: currentPath })
    }
  }

  return items
})

onMounted(async () => {
  // Apply saved theme on load
  if (isDark.value) document.documentElement.dataset.theme = 'dark'

  if (authStore.token && !authStore.user) {
    await authStore.loadCurrentUser()
  } else if (authStore.token && authStore.menus.length === 0) {
    await authStore.loadMenus()
  }
  loadNotificationCount()
  // Connect to SSE after user is loaded
  if (authStore.token) {
    connectSSE()
  }
})

async function loadNotificationCount() {
  try {
    dashSummary.value = await getDashboardSummary()
  } catch {
    /* silent */
  }
}

async function logout() {
  await authStore.signOut()
  router.push('/login')
}

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

function goProfile() {
  router.push('/account/profile')
}

function goChangePassword() {
  router.push('/account/change-password')
}

function goNotifications() {
  router.push('/messages')
}

function toggleTheme() {
  isDark.value = !isDark.value
  document.documentElement.dataset.theme = isDark.value ? 'dark' : 'light'
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

function closeMobileMenu() {
  mobileMenuOpen.value = false
}
</script>

<template>
  <el-container class="app-shell">
    <!-- Mobile hamburger toggle -->
    <div class="app-shell__mobile-toggle" @click="mobileMenuOpen = true">
      <el-icon :size="20"><Fold /></el-icon>
    </div>
    <!-- Mobile overlay -->
    <div v-if="mobileMenuOpen" class="app-shell__mobile-overlay" @click="mobileMenuOpen = false" />
    <el-aside class="app-shell__aside" :width="asideWidth" :class="{ 'is-collapsed': isCollapsed, 'is-open': mobileMenuOpen }">
      <div class="app-shell__brand">
        <el-icon :size="24"><Monitor /></el-icon>
        <span v-show="!isCollapsed" class="app-shell__brand-text">企业 OA</span>
      </div>
      <el-scrollbar>
        <el-menu
          router
          :default-active="route.path"
          :collapse="isCollapsed"
          :collapse-transition="false"
          class="app-shell__menu"
          @select="closeMobileMenu"
        >
          <template v-for="group in authStore.navGroups" :key="group.id">
            <el-sub-menu v-if="group.children.length > 1" :index="String(group.id)">
              <template #title>
                <el-icon><component :is="group.icon" /></el-icon>
                <span>{{ group.label }}</span>
              </template>
              <el-menu-item v-for="child in group.children" :key="child.path" :index="child.path">
                {{ child.label }}
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else-if="group.children.length === 1" :index="group.children[0].path">
              <el-icon><component :is="group.icon" /></el-icon>
              <template #title>{{ group.children[0].label }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
      <div class="app-shell__collapse-btn" @click="toggleCollapse">
        <el-icon>
          <Fold v-if="!isCollapsed" />
          <Expand v-else />
        </el-icon>
      </div>
    </el-aside>

    <el-container>
      <el-header class="app-shell__header">
        <div class="app-shell__header-left">
          <div>
            <div class="app-shell__title">{{ title }}</div>
            <el-breadcrumb separator="/" class="app-shell__breadcrumb">
              <el-breadcrumb-item v-for="(bc, i) in breadcrumbs" :key="i">
                <router-link v-if="i < breadcrumbs.length - 1" :to="bc.path" class="breadcrumb-link">
                  {{ bc.title }}
                </router-link>
                <span v-else>{{ bc.title }}</span>
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>
        </div>
        <div class="app-shell__user">
          <!-- Natural Language Query -->
          <NaturalLanguageQuery />
          <!-- Command Palette trigger -->
          <div class="app-shell__header-action" @click="commandPaletteRef?.open()" title="命令面板 (⌘K)">
            <el-icon :size="18"><Grid /></el-icon>
          </div>
          <!-- Calendar trigger -->
          <div class="app-shell__header-action" @click="smartCalendarRef?.open()" title="智能日历">
            <el-icon :size="18"><Calendar /></el-icon>
          </div>
          <!-- Notification Center trigger -->
          <div class="app-shell__header-action" @click="notificationCenterRef?.open()" title="通知中心">
            <el-badge :value="notificationCount" :hidden="notificationCount === 0" :max="99" type="danger">
              <el-icon :size="18"><Bell /></el-icon>
            </el-badge>
          </div>
          <el-popover
            v-model:visible="searchPopoverVisible"
            placement="bottom-end"
            :width="420"
            trigger="click"
            :show-arrow="false"
            :offset="8"
          >
            <template #reference>
              <div class="app-shell__search-input" :class="{ 'is-active': searchPopoverVisible }">
                <el-input
                  v-model="searchQuery"
                  placeholder="搜索人员、请假、文件..."
                  clearable
                  :prefix-icon="Search"
                  @input="onSearchInput"
                  @clear="onSearchClear"
                  @focus="searchPopoverVisible = true"
                />
              </div>
            </template>
            <div class="app-shell__search-popover">
              <div v-if="searchLoading" class="app-shell__search-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>搜索中...</span>
              </div>
              <div v-else-if="!hasAnyResults() && searchQuery.trim().length > 0" class="app-shell__search-empty">
                未找到相关结果
              </div>
              <div v-else-if="searchResults">
                <!-- Users -->
                <div v-if="searchResults.users && searchResults.users.length > 0" class="app-shell__search-group">
                  <div class="app-shell__search-group-title">人员</div>
                  <div
                    v-for="item in searchResults.users"
                    :key="'u-' + item.id"
                    class="app-shell__search-item"
                    @click="navigateSearchResult('user', item)"
                  >
                    <el-avatar :size="32" style="background: var(--oa-primary); flex-shrink: 0">
                      {{ String(item.name ?? '').charAt(0) }}
                    </el-avatar>
                    <div class="app-shell__search-item-info">
                      <div class="app-shell__search-item-name">{{ item.name }}</div>
                      <div class="app-shell__search-item-meta">{{ item.username }} {{ item.employeeNo ? '· ' + item.employeeNo : '' }}</div>
                    </div>
                  </div>
                </div>
                <!-- Leaves -->
                <div v-if="searchResults.leaves && searchResults.leaves.length > 0" class="app-shell__search-group">
                  <div class="app-shell__search-group-title">请假</div>
                  <div
                    v-for="item in searchResults.leaves"
                    :key="'l-' + item.id"
                    class="app-shell__search-item"
                    @click="navigateSearchResult('leave', item)"
                  >
                    <el-icon :size="20" style="color: var(--oa-primary); flex-shrink: 0; margin: 6px 0 0 6px"><Calendar /></el-icon>
                    <div class="app-shell__search-item-info">
                      <div class="app-shell__search-item-name">{{ item.createdName }} - {{ item.leaveType }}</div>
                      <div class="app-shell__search-item-meta">{{ item.reason ? String(item.reason).substring(0, 40) : '' }}</div>
                    </div>
                  </div>
                </div>
                <!-- Files -->
                <div v-if="searchResults.files && searchResults.files.length > 0" class="app-shell__search-group">
                  <div class="app-shell__search-group-title">文件</div>
                  <div
                    v-for="item in searchResults.files"
                    :key="'f-' + item.id"
                    class="app-shell__search-item"
                    @click="navigateSearchResult('file', item)"
                  >
                    <el-icon :size="20" style="color: var(--oa-success); flex-shrink: 0; margin: 6px 0 0 6px"><Document /></el-icon>
                    <div class="app-shell__search-item-info">
                      <div class="app-shell__search-item-name">{{ item.fileName }}</div>
                      <div class="app-shell__search-item-meta">{{ item.fileExt }}</div>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="app-shell__search-empty">
                输入关键词搜索人员、请假记录、文件
              </div>
            </div>
          </el-popover>

          <el-badge :value="notificationCount" :hidden="notificationCount === 0" :max="99" class="app-shell__notification" @click="goNotifications">
            <el-icon :size="20"><Bell /></el-icon>
          </el-badge>
          <div class="app-shell__theme-toggle" @click="toggleTheme">
            <el-icon :size="20"><Sunny v-if="isDark" /><Moon v-else /></el-icon>
          </div>
          <el-dropdown trigger="click" @command="(cmd: string) => { if (cmd === 'profile') goProfile(); else if (cmd === 'password') goChangePassword(); else logout() }">
            <div class="app-shell__user-info">
              <el-avatar :size="32" style="background: var(--oa-primary); cursor: pointer">
                {{ (authStore.user?.realName ?? '管')[0] }}
              </el-avatar>
              <span class="app-shell__user-name">{{ authStore.user?.realName ?? '系统管理员' }}</span>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人信息
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon>修改密码
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="app-shell__main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>

  <!-- Smart Components -->
  <CommandPalette ref="commandPaletteRef" />
  <NotificationCenter ref="notificationCenterRef" />
  <SmartCalendar ref="smartCalendarRef" />
  <ShortcutHelp />
  <OnboardingOverlay />
</template>

<style scoped>
.app-shell__search-input {
  width: 260px;
  transition: width 0.2s ease;
}
.app-shell__search-input.is-active {
  width: 320px;
}
.app-shell__search-input :deep(.el-input__wrapper) {
  border-radius: var(--oa-radius-sm);
  box-shadow: 0 0 0 1px var(--oa-border-lighter) inset;
}
.app-shell__search-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--oa-border-base) inset;
}
.app-shell__search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--oa-primary) inset;
}
.app-shell__search-popover {
  max-height: 420px;
  overflow-y: auto;
}
.app-shell__search-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  color: var(--oa-text-secondary);
  font-size: 13px;
  justify-content: center;
}
.app-shell__search-empty {
  padding: 24px 16px;
  text-align: center;
  color: var(--oa-text-secondary);
  font-size: 13px;
}
.app-shell__search-group {
  padding: 4px 0;
}
.app-shell__search-group + .app-shell__search-group {
  border-top: 1px solid var(--oa-border-lighter);
}
.app-shell__search-group-title {
  padding: 6px 12px;
  font-size: 11px;
  font-weight: 600;
  color: var(--oa-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.app-shell__search-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background var(--oa-transition);
}
.app-shell__search-item:hover {
  background: var(--oa-bg-page);
}
.app-shell__search-item-info {
  flex: 1;
  min-width: 0;
}
.app-shell__search-item-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--oa-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.app-shell__search-item-meta {
  font-size: 12px;
  color: var(--oa-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.app-shell__header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.app-shell__breadcrumb {
  margin-top: 2px;
  font-size: 12px;
}

.breadcrumb-link {
  color: var(--oa-text-regular);
  text-decoration: none;
  transition: color 0.15s ease;
}

.breadcrumb-link:hover {
  color: var(--oa-primary);
  text-decoration: underline;
}

.app-shell__breadcrumb .el-breadcrumb__item:last-child .el-breadcrumb__inner {
  color: var(--oa-text-muted);
  font-weight: 400;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.app-shell__user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--oa-radius-sm);
  transition: background var(--oa-transition);
}

.app-shell__user-info:hover {
  background: var(--oa-bg-page);
}

.app-shell__header-action {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--oa-radius-sm);
  cursor: pointer;
  transition: background var(--oa-transition);
  color: var(--oa-text-secondary);
}

.app-shell__header-action:hover {
  background: var(--oa-bg-page);
  color: var(--oa-primary);
}

.app-shell__user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--oa-text-primary);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Theme toggle */
.app-shell__theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--oa-radius-sm);
  cursor: pointer;
  color: var(--oa-text-secondary);
  transition: all var(--oa-transition);
}
.app-shell__theme-toggle:hover {
  background: var(--oa-bg-page);
  color: var(--oa-primary);
}

/* Mobile hamburger toggle */
.app-shell__mobile-toggle {
  display: none;
}

/* Mobile overlay */
.app-shell__mobile-overlay {
  display: none;
}

@media (max-width: 768px) {
  .app-shell__mobile-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
    position: fixed;
    top: 14px;
    left: 14px;
    z-index: 1001;
    width: 36px;
    height: 36px;
    border-radius: var(--oa-radius-sm);
    background: var(--oa-bg-card);
    border: 1px solid var(--oa-border);
    cursor: pointer;
    color: var(--oa-text-primary);
    box-shadow: var(--oa-shadow-sm);
  }
  .app-shell__mobile-overlay {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 999;
    background: rgba(0, 0, 0, 0.4);
  }
  .app-shell__aside {
    position: fixed !important;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }
  .app-shell__aside.is-open {
    transform: translateX(0);
  }
  .app-shell__header {
    padding-left: 56px;
  }
}
</style>
