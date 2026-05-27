<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import {
  getDashboardSummary,
  getDashboardTodos,
  getDashboardStarted,
  getDashboardCcToMe,
  getDashboardNotices,
  getDashboardQuickActions,
  type DashboardSummary,
  type DashboardTodo,
  type DashboardStarted,
  type DashboardCcItem,
  type DashboardNotice,
  type QuickAction,
} from '../../api/dashboard'
import { formatDisplayDateTime, statusLabel } from '../oa/oa-shared'
import { Refresh } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const summary = ref<DashboardSummary | null>(null)
const todos = ref<DashboardTodo[]>([])
const started = ref<DashboardStarted[]>([])
const ccItems = ref<DashboardCcItem[]>([])
const notices = ref<DashboardNotice[]>([])
const quickActions = ref<QuickAction[]>([])
const loading = ref(false)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const todayDate = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 星期${'日一二三四五六'[now.getDay()]}`
})

const userName = computed(() => authStore.user?.realName ?? '用户')
const deptName = computed(() => authStore.user?.mainDeptName ?? '')

const QUICK_ACTION_ICONS: Record<string, string> = {
  '/oa/leaves/create': 'Calendar',
  '/oa/expenses/create': 'Wallet',
  '/oa/seals/create': 'Stamp',
  '/oa/purchases/create': 'ShoppingCart',
  '/contracts/create': 'Document',
  '/notices/create': 'Bell',
}

async function refresh() {
  loading.value = true
  try {
    const [s, t, st, cc, n, a] = await Promise.all([
      getDashboardSummary(),
      getDashboardTodos(8),
      getDashboardStarted(8),
      getDashboardCcToMe(8),
      getDashboardNotices(8),
      getDashboardQuickActions(),
    ])
    summary.value = s
    todos.value = t
    started.value = st
    ccItems.value = cc
    notices.value = n
    quickActions.value = a
  } finally {
    loading.value = false
  }
}

onMounted(refresh)

function go(path: string) {
  router.push(path)
}

function statusType(status: string): 'success' | 'info' | 'warning' | 'danger' {
  switch (status) {
    case 'APPROVED': return 'success'
    case 'APPROVING': return 'warning'
    case 'REJECTED':
    case 'TERMINATED': return 'danger'
    default: return 'info'
  }
}

const statCards = computed(() => [
  { key: 'todo', label: '我的待办', value: summary.value?.todoCount ?? 0, color: 'blue', icon: 'Document', path: '/todos' },
  { key: 'approving', label: '审批中', value: summary.value?.startedCount ?? 0, color: 'orange', icon: 'SetUp', path: '/applications' },
  { key: 'message', label: '未读消息', value: summary.value?.messageCount ?? 0, color: 'red', icon: 'ChatDotRound', path: '/messages' },
  { key: 'cc', label: '抄送未读', value: summary.value?.ccCount ?? 0, color: 'purple', icon: 'Promotion', path: '/applications/cc' },
  { key: 'exception', label: '异常流程', value: summary.value?.exceptionCount ?? 0, color: 'gray', icon: 'Warning', path: '/workflow/exceptions' },
])
</script>

<template>
  <div class="dashboard-page" v-loading="loading">
    <!-- Welcome -->
    <section class="dashboard-welcome">
      <el-avatar :size="52" style="background: var(--oa-primary); font-size: 20px; flex-shrink: 0">
        {{ userName.charAt(0) }}
      </el-avatar>
      <div class="dashboard-welcome__info">
        <h2>{{ greeting }}，{{ userName }}</h2>
        <p>{{ todayDate }}　{{ deptName ? deptName + ' · ' : '' }}欢迎回到企业级 OA 系统</p>
      </div>
      <div class="dashboard-welcome__actions">
        <el-tag type="success" effect="light" round>系统运行正常</el-tag>
        <el-button :icon="Refresh" circle @click="refresh" :loading="loading" />
      </div>
    </section>

    <!-- Stat Cards -->
    <section class="stat-grid">
      <div
        v-for="card in statCards"
        :key="card.key"
        class="stat-card"
        :class="`stat-card--${card.color}`"
        @click="go(card.path)"
      >
        <div class="stat-card__header">
          <div class="stat-card__icon">
            <el-icon :size="20"><component :is="card.icon" /></el-icon>
          </div>
          <el-icon :size="14" style="color: var(--oa-text-muted)"><ArrowRight /></el-icon>
        </div>
        <el-statistic :value="card.value" class="stat-card__value" />
        <div class="stat-card__label">{{ card.label }}</div>
      </div>
    </section>

    <!-- Quick Actions -->
    <section v-if="quickActions.length">
      <h3 class="oa-section-title">快捷入口</h3>
      <div class="dashboard-actions">
        <div
          v-for="action in quickActions"
          :key="action.path"
          class="dashboard-action"
          @click="go(action.path)"
        >
          <div class="dashboard-action__icon">
            <el-icon :size="22"><component :is="QUICK_ACTION_ICONS[action.path] ?? 'Plus'" /></el-icon>
          </div>
          <span class="dashboard-action__label">{{ action.label }}</span>
        </div>
      </div>
    </section>

    <!-- Data Previews -->
    <div class="dashboard-row">
      <el-card shadow="never" class="dashboard-row__col">
        <template #header>
          <div class="card-header">
            <span style="font-weight: 600">待办任务</span>
            <el-button text type="primary" @click="go('/todos')">查看全部</el-button>
          </div>
        </template>
        <el-empty v-if="todos.length === 0" description="暂无待办" :image-size="80" />
        <div v-else class="dashboard-list">
          <div
            v-for="item in todos"
            :key="item.taskId"
            class="dashboard-list__item"
            @click="go(`/todos`)"
          >
            <div class="dashboard-list__main">
              <span class="dashboard-list__title">{{ item.title }}</span>
              <span class="dashboard-list__sub">{{ item.nodeName }}</span>
            </div>
            <div class="dashboard-list__right">
              <el-tag size="small" :type="statusType(item.status)">{{ statusLabel(item.status) }}</el-tag>
              <span class="dashboard-list__time">{{ formatDisplayDateTime(item.createdAt) }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="dashboard-row__col">
        <template #header>
          <div class="card-header">
            <span style="font-weight: 600">我发起的</span>
            <el-button text type="primary" @click="go('/applications')">查看全部</el-button>
          </div>
        </template>
        <el-empty v-if="started.length === 0" description="暂无发起" :image-size="80" />
        <div v-else class="dashboard-list">
          <div
            v-for="item in started"
            :key="item.wfInstanceId"
            class="dashboard-list__item"
            @click="go('/applications')"
          >
            <div class="dashboard-list__main">
              <span class="dashboard-list__title">{{ item.title }}</span>
              <span class="dashboard-list__sub">{{ item.currentNodeName ?? '—' }}</span>
            </div>
            <div class="dashboard-list__right">
              <el-tag size="small" :type="statusType(item.status)">{{ statusLabel(item.status) }}</el-tag>
              <span class="dashboard-list__time">{{ formatDisplayDateTime(item.startedAt) }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <div class="dashboard-row">
      <el-card shadow="never" class="dashboard-row__col">
        <template #header>
          <div class="card-header">
            <span style="font-weight: 600">抄送我的</span>
            <el-button text type="primary" @click="go('/applications/cc')">查看全部</el-button>
          </div>
        </template>
        <el-empty v-if="ccItems.length === 0" description="暂无抄送" :image-size="80" />
        <div v-else class="dashboard-list">
          <div
            v-for="item in ccItems"
            :key="item.ccId"
            class="dashboard-list__item"
            @click="go('/applications/cc')"
          >
            <div class="dashboard-list__main">
              <span class="dashboard-list__title">{{ item.title }}</span>
            </div>
            <el-badge :is-dot="!item.readAt" :hidden="!!item.readAt" type="warning">
              <el-tag size="small" :type="item.readAt ? 'info' : 'warning'">
                {{ item.readAt ? '已读' : '未读' }}
              </el-tag>
            </el-badge>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="dashboard-row__col">
        <template #header>
          <div class="card-header">
            <span style="font-weight: 600">最近公告</span>
            <el-button text type="primary" @click="go('/notices')">查看全部</el-button>
          </div>
        </template>
        <el-empty v-if="notices.length === 0" description="暂无公告" :image-size="80" />
        <div v-else class="dashboard-list">
          <div
            v-for="item in notices"
            :key="item.id"
            class="dashboard-list__item"
            @click="go('/notices')"
          >
            <div class="dashboard-list__main">
              <span class="dashboard-list__title">{{ item.title }}</span>
              <span class="dashboard-list__sub">{{ item.category ?? '公告' }}</span>
            </div>
            <span class="dashboard-list__time">{{ formatDisplayDateTime(item.publishAt) }}</span>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.dashboard-list {
  display: flex;
  flex-direction: column;
}

.dashboard-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--oa-border-light);
  cursor: pointer;
  transition: background 0.15s ease;
  gap: 12px;
}

.dashboard-list__item:last-child {
  border-bottom: none;
}

.dashboard-list__item:hover {
  background: var(--oa-primary-bg);
  margin: 0 -20px;
  padding: 10px 20px;
  border-radius: var(--oa-radius-sm);
}

.dashboard-list__main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.dashboard-list__title {
  font-size: 14px;
  font-weight: 500;
  color: var(--oa-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dashboard-list__sub {
  font-size: 12px;
  color: var(--oa-text-muted);
}

.dashboard-list__right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.dashboard-list__time {
  font-size: 12px;
  color: var(--oa-text-muted);
  white-space: nowrap;
}
</style>
