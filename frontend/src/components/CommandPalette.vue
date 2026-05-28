<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { http } from '../api/http'

const router = useRouter()
const authStore = useAuthStore()

const visible = ref(false)
const query = ref('')
const selectedIndex = ref(0)
const loading = ref(false)
const searchResults = ref<SearchResult[]>([])

interface SearchResult {
  id: string
  type: 'navigation' | 'action' | 'search'
  title: string
  subtitle?: string
  icon: string
  path?: string
  action?: () => void
}

const NAVIGATION_ITEMS: SearchResult[] = [
  { id: 'nav-dashboard', type: 'navigation', title: '工作台', icon: 'Odometer', path: '/dashboard' },
  { id: 'nav-todos', type: 'navigation', title: '我的待办', icon: 'Document', path: '/todos' },
  { id: 'nav-applications', type: 'navigation', title: '我发起的', icon: 'SetUp', path: '/applications' },
  { id: 'nav-leaves', type: 'navigation', title: '请假管理', icon: 'Calendar', path: '/oa/leaves' },
  { id: 'nav-expenses', type: 'navigation', title: '报销管理', icon: 'Wallet', path: '/oa/expenses' },
  { id: 'nav-purchases', type: 'navigation', title: '采购管理', icon: 'ShoppingCart', path: '/oa/purchases' },
  { id: 'nav-seals', type: 'navigation', title: '用印管理', icon: 'Stamp', path: '/oa/seals' },
  { id: 'nav-contracts', type: 'navigation', title: '合同管理', icon: 'Document', path: '/contracts' },
  { id: 'nav-meetings', type: 'navigation', title: '会议室', icon: 'Calendar', path: '/meetings' },
  { id: 'nav-messages', type: 'navigation', title: '消息中心', icon: 'ChatDotRound', path: '/messages' },
  { id: 'nav-files', type: 'navigation', title: '文件库', icon: 'FolderOpened', path: '/files' },
  { id: 'nav-reports', type: 'navigation', title: '数据报表', icon: 'DataAnalysis', path: '/reports' },
  { id: 'nav-org', type: 'navigation', title: '组织管理', icon: 'UserFilled', path: '/org/departments' },
  { id: 'nav-permission', type: 'navigation', title: '权限管理', icon: 'Lock', path: '/permission/roles' },
  { id: 'nav-workflow', type: 'navigation', title: '流程模板', icon: 'SetUp', path: '/workflow/templates' },
  { id: 'nav-system', type: 'navigation', title: '系统配置', icon: 'Setting', path: '/system/configs' },
  { id: 'nav-audit', type: 'navigation', title: '审计日志', icon: 'View', path: '/audit/logs' },
]

const ACTION_ITEMS: SearchResult[] = [
  { id: 'act-leave', type: 'action', title: '发起请假', icon: 'Calendar', path: '/oa/leaves/create' },
  { id: 'act-expense', type: 'action', title: '发起报销', icon: 'Wallet', path: '/oa/expenses/create' },
  { id: 'act-seal', type: 'action', title: '发起用章', icon: 'Stamp', path: '/oa/seals/create' },
  { id: 'act-purchase', type: 'action', title: '发起采购', icon: 'ShoppingCart', path: '/oa/purchases/create' },
  { id: 'act-meeting', type: 'action', title: '预订会议室', icon: 'Calendar', path: '/meetings/booking' },
  { id: 'act-contract', type: 'action', title: '新建合同', icon: 'Document', path: '/contracts/create' },
]

const filteredItems = computed(() => {
  if (!query.value.trim()) {
    return [...NAVIGATION_ITEMS, ...ACTION_ITEMS]
  }
  const q = query.value.toLowerCase()
  const navMatches = NAVIGATION_ITEMS.filter(
    (item) => item.title.toLowerCase().includes(q) || item.path?.toLowerCase().includes(q)
  )
  const actMatches = ACTION_ITEMS.filter(
    (item) => item.title.toLowerCase().includes(q)
  )
  const searchItems: SearchResult[] = searchResults.value
  return [...navMatches, ...actMatches, ...searchItems]
})

watch(query, async (val) => {
  selectedIndex.value = 0
  if (!val.trim() || val.length < 2) {
    searchResults.value = []
    return
  }
  loading.value = true
  try {
    const data = await http.get('/search', { params: { q: val, limit: 3 } }) as any
    const results: SearchResult[] = []
    if (data.users) {
      for (const u of data.users) {
        results.push({
          id: `search-user-${u.id}`,
          type: 'search',
          title: u.name,
          subtitle: `${u.username} · ${u.employeeNo}`,
          icon: 'User',
          path: '/org/users',
        })
      }
    }
    if (data.leaves) {
      for (const l of data.leaves) {
        results.push({
          id: `search-leave-${l.id}`,
          type: 'search',
          title: `${l.createdName} 的请假`,
          subtitle: l.reason?.substring(0, 40),
          icon: 'Calendar',
          path: `/oa/leaves/${l.id}`,
        })
      }
    }
    if (data.contracts) {
      for (const c of data.contracts) {
        results.push({
          id: `search-contract-${c.id}`,
          type: 'search',
          title: c.contractName || c.contractNo,
          subtitle: c.counterparty,
          icon: 'Document',
          path: `/contracts/${c.id}`,
        })
      }
    }
    if (data.expenses) {
      for (const e of data.expenses) {
        results.push({
          id: `search-expense-${e.id}`,
          type: 'search',
          title: `${e.createdNameSnapshot || ''} 的报销`,
          subtitle: e.expenseNo,
          icon: 'Wallet',
          path: `/oa/expenses/${e.id}`,
        })
      }
    }
    searchResults.value = results
  } catch {
    searchResults.value = []
  } finally {
    loading.value = false
  }
})

function open() {
  visible.value = true
  query.value = ''
  selectedIndex.value = 0
}

function close() {
  visible.value = false
  query.value = ''
  searchResults.value = []
}

function selectItem(item: SearchResult) {
  if (item.path) {
    router.push(item.path)
  }
  close()
}

function handleKeydown(e: KeyboardEvent) {
  if (!visible.value) return
  switch (e.key) {
    case 'ArrowDown':
      e.preventDefault()
      selectedIndex.value = Math.min(selectedIndex.value + 1, filteredItems.value.length - 1)
      break
    case 'ArrowUp':
      e.preventDefault()
      selectedIndex.value = Math.max(selectedIndex.value - 1, 0)
      break
    case 'Enter':
      e.preventDefault()
      if (filteredItems.value[selectedIndex.value]) {
        selectItem(filteredItems.value[selectedIndex.value])
      }
      break
    case 'Escape':
      close()
      break
  }
}

function handleGlobalKeydown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
    e.preventDefault()
    if (visible.value) {
      close()
    } else {
      open()
    }
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleGlobalKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleGlobalKeydown)
})

defineExpose({ open })
</script>

<template>
  <Teleport to="body">
    <Transition name="command-palette">
      <div v-if="visible" class="command-palette-overlay" @click.self="close">
        <div class="command-palette" @keydown="handleKeydown">
          <div class="command-palette__input-wrapper">
            <el-icon :size="18" class="command-palette__search-icon"><Search /></el-icon>
            <input
              v-model="query"
              class="command-palette__input"
              placeholder="搜索页面、功能、人员...  (⌘K)"
              autofocus
            />
            <el-icon
              :size="14"
              class="command-palette__close"
              @click="close"
            ><Close /></el-icon>
          </div>

          <div class="command-palette__results">
            <div v-if="loading" class="command-palette__loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>搜索中...</span>
            </div>

            <template v-else>
              <div v-if="filteredItems.length === 0" class="command-palette__empty">
                未找到匹配结果
              </div>

              <div v-else class="command-palette__list">
                <div
                  v-for="(item, index) in filteredItems"
                  :key="item.id"
                  class="command-palette__item"
                  :class="{ 'command-palette__item--selected': index === selectedIndex }"
                  @click="selectItem(item)"
                  @mouseenter="selectedIndex = index"
                >
                  <div class="command-palette__item-icon">
                    <el-icon :size="16"><component :is="item.icon" /></el-icon>
                  </div>
                  <div class="command-palette__item-content">
                    <span class="command-palette__item-title">{{ item.title }}</span>
                    <span v-if="item.subtitle" class="command-palette__item-subtitle">{{ item.subtitle }}</span>
                  </div>
                  <el-tag
                    v-if="item.type === 'navigation'"
                    size="small"
                    type="info"
                    effect="plain"
                  >页面</el-tag>
                  <el-tag
                    v-else-if="item.type === 'action'"
                    size="small"
                    type="success"
                    effect="plain"
                  >操作</el-tag>
                  <el-tag
                    v-else
                    size="small"
                    type="warning"
                    effect="plain"
                  >搜索</el-tag>
                </div>
              </div>
            </template>
          </div>

          <div class="command-palette__footer">
            <span><kbd>↑↓</kbd> 导航</span>
            <span><kbd>Enter</kbd> 选择</span>
            <span><kbd>Esc</kbd> 关闭</span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.command-palette-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 15vh;
  backdrop-filter: blur(4px);
}

.command-palette {
  width: 580px;
  max-width: 90vw;
  background: var(--oa-bg-white, #fff);
  border-radius: 16px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.25);
  overflow: hidden;
  border: 1px solid var(--oa-border-light, #e4e7ed);
}

.command-palette__input-wrapper {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  gap: 12px;
  border-bottom: 1px solid var(--oa-border-light, #e4e7ed);
}

.command-palette__search-icon {
  color: var(--oa-text-muted, #909399);
  flex-shrink: 0;
}

.command-palette__input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 16px;
  background: transparent;
  color: var(--oa-text-primary, #303133);
  font-family: inherit;
}

.command-palette__input::placeholder {
  color: var(--oa-text-muted, #909399);
}

.command-palette__close {
  color: var(--oa-text-muted, #909399);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.15s;
}

.command-palette__close:hover {
  background: var(--oa-bg-gray, #f5f7fa);
}

.command-palette__results {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px;
}

.command-palette__loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: var(--oa-text-muted, #909399);
}

.command-palette__empty {
  text-align: center;
  padding: 32px;
  color: var(--oa-text-muted, #909399);
  font-size: 14px;
}

.command-palette__list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.command-palette__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.1s;
}

.command-palette__item:hover,
.command-palette__item--selected {
  background: var(--oa-primary-bg, #ecf5ff);
}

.command-palette__item-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--oa-bg-gray, #f5f7fa);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--oa-primary, #409eff);
  flex-shrink: 0;
}

.command-palette__item-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.command-palette__item-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--oa-text-primary, #303133);
}

.command-palette__item-subtitle {
  font-size: 12px;
  color: var(--oa-text-muted, #909399);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.command-palette__footer {
  display: flex;
  gap: 16px;
  padding: 10px 20px;
  border-top: 1px solid var(--oa-border-light, #e4e7ed);
  font-size: 12px;
  color: var(--oa-text-muted, #909399);
}

.command-palette__footer kbd {
  display: inline-block;
  padding: 2px 6px;
  border: 1px solid var(--oa-border-light, #dcdfe6);
  border-radius: 4px;
  font-size: 11px;
  font-family: inherit;
  background: var(--oa-bg-gray, #f5f7fa);
  margin-right: 4px;
}

/* Transition */
.command-palette-enter-active,
.command-palette-leave-active {
  transition: opacity 0.15s ease;
}
.command-palette-enter-active .command-palette,
.command-palette-leave-active .command-palette {
  transition: transform 0.15s ease, opacity 0.15s ease;
}
.command-palette-enter-from,
.command-palette-leave-to {
  opacity: 0;
}
.command-palette-enter-from .command-palette {
  transform: scale(0.95) translateY(-10px);
}
.command-palette-leave-to .command-palette {
  transform: scale(0.95) translateY(-10px);
}
</style>
