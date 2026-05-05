<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getDashboardSummary, type DashboardSummary } from '../api/dashboard'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapsed = ref(false)
const dashSummary = ref<DashboardSummary | null>(null)

const title = computed(() => route.meta.title ?? '企业 OA')
const asideWidth = computed(() => (isCollapsed.value ? '64px' : '240px'))

const notificationCount = computed(() => {
  if (!dashSummary.value) return 0
  return (dashSummary.value.todoCount ?? 0) + (dashSummary.value.messageCount ?? 0) + (dashSummary.value.ccCount ?? 0)
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
  if (authStore.token && !authStore.user) {
    await authStore.loadCurrentUser()
  } else if (authStore.token && authStore.menus.length === 0) {
    await authStore.loadMenus()
  }
  loadNotificationCount()
})

async function loadNotificationCount() {
  try {
    dashSummary.value = await getDashboardSummary()
  } catch {
    /* silent */
  }
}

function logout() {
  authStore.signOut()
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
</script>

<template>
  <el-container class="app-shell">
    <el-aside class="app-shell__aside" :width="asideWidth" :class="{ 'is-collapsed': isCollapsed }">
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
                {{ bc.title }}
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>
        </div>
        <div class="app-shell__user">
          <el-badge :value="notificationCount" :hidden="notificationCount === 0" :max="99" class="app-shell__notification" @click="goNotifications">
            <el-icon :size="20"><Bell /></el-icon>
          </el-badge>
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
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-shell__header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.app-shell__breadcrumb {
  margin-top: 2px;
  font-size: 12px;
}

.app-shell__breadcrumb .el-breadcrumb__item:last-child .el-breadcrumb__inner {
  color: var(--oa-text-muted);
  font-weight: 400;
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

.app-shell__user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--oa-text-primary);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
