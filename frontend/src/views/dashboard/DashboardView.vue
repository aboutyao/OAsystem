<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { useBehaviorTracking } from '../../composables/useBehaviorTracking'
import {
  getDashboardSummary,
  getDashboardTodos,
  getDashboardStarted,
  getDashboardCcToMe,
  getDashboardNotices,
  getDashboardQuickActions,
  getMyLeaveBalance,
  getDashboardInsights,
  trackQuickAction,
  type DashboardSummary,
  type DashboardTodo,
  type DashboardStarted,
  type DashboardCcItem,
  type DashboardNotice,
  type QuickAction,
  type LeaveBalanceItem,
  type DashboardInsight,
} from '../../api/dashboard'
import { formatDisplayDateTime, statusLabel } from '../oa/oa-shared'
import { Refresh } from '@element-plus/icons-vue'
import BudgetWarningBanner from '../../components/BudgetWarningBanner.vue'

const router = useRouter()
const authStore = useAuthStore()
const { trackAction: trackBehavior } = useBehaviorTracking()

const summary = ref<DashboardSummary | null>(null)
const todos = ref<DashboardTodo[]>([])
const started = ref<DashboardStarted[]>([])
const ccItems = ref<DashboardCcItem[]>([])
const notices = ref<DashboardNotice[]>([])
const quickActions = ref<QuickAction[]>([])
const leaveBalances = ref<LeaveBalanceItem[]>([])
const insight = ref<DashboardInsight | null>(null)
const loading = ref(false)

const LEAVE_TYPE_LABELS: Record<string, string> = {
  ANNUAL: '年假',
  SICK: '病假',
  PERSONAL: '事假',
  MATERNITY: '产假',
  PATERNITY: '陪产假',
  MARRIAGE: '婚假',
  BEREAVEMENT: '丧假',
}

const pendingApprovals = computed(() => summary.value?.todoCount ?? 0)

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
    const [s, t, st, cc, n, a, lb, ins] = await Promise.all([
      getDashboardSummary(),
      getDashboardTodos(8),
      getDashboardStarted(8),
      getDashboardCcToMe(8),
      getDashboardNotices(8),
      getDashboardQuickActions(),
      getMyLeaveBalance(),
      getDashboardInsights().catch(() => null),
    ])
    summary.value = s
    todos.value = t
    started.value = st
    ccItems.value = cc
    notices.value = n
    quickActions.value = a
    leaveBalances.value = lb
    insight.value = ins
  } finally {
    loading.value = false
  }
}

onMounted(refresh)

function go(path: string) {
  trackQuickAction(path).catch(() => {})
  trackBehavior(path)
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
  <div class="dashboard-page">
    <!-- Skeleton Loading -->
    <template v-if="loading && !summary">
      <section class="dashboard-welcome skeleton-welcome">
        <el-skeleton :rows="1" animated style="width: 100%">
          <template #template>
            <div style="display: flex; align-items: center; gap: 16px">
              <el-skeleton-item variant="circle" style="width: 52px; height: 52px" />
              <div style="flex: 1">
                <el-skeleton-item variant="h3" style="width: 200px; margin-bottom: 8px" />
                <el-skeleton-item variant="text" style="width: 300px" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </section>
      <section class="stat-grid">
        <div v-for="i in 5" :key="i" class="stat-card skeleton-card">
          <el-skeleton :rows="2" animated>
            <template #template>
              <el-skeleton-item variant="rect" style="width: 40px; height: 40px; margin-bottom: 12px" />
              <el-skeleton-item variant="h1" style="width: 60px; margin-bottom: 8px" />
              <el-skeleton-item variant="text" style="width: 80px" />
            </template>
          </el-skeleton>
        </div>
      </section>
      <div class="dashboard-row">
        <el-card v-for="i in 2" :key="i" shadow="never" class="dashboard-row__col">
          <el-skeleton :rows="4" animated />
        </el-card>
      </div>
    </template>

    <!-- Real Content -->
    <template v-else>
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

    <!-- Budget Warnings -->
    <BudgetWarningBanner />

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

    <!-- AI Insights Banner -->
    <section v-if="insight" class="insight-banner">
      <div class="insight-banner__icon">
        <el-icon :size="20"><MagicStick /></el-icon>
      </div>
      <div class="insight-banner__content">
        <div class="insight-banner__briefing">{{ insight.briefing }}</div>
        <div class="insight-banner__metrics">
          <div v-if="insight.approvalVelocity" class="insight-metric">
            <span class="insight-metric__label">审批效率</span>
            <span class="insight-metric__value" :class="insight.approvalVelocity.fasterThanTeam ? 'text-success' : 'text-warning'">
              {{ insight.approvalVelocity.fasterThanTeam ? '快于团队' : '低于平均' }}
              {{ insight.approvalVelocity.speedRatio > 0 ? ` ${insight.approvalVelocity.speedRatio}x` : '' }}
            </span>
          </div>
          <div v-if="insight.upcomingDeadlines?.length" class="insight-metric">
            <span class="insight-metric__label">即将到期</span>
            <span class="insight-metric__value text-warning">{{ insight.upcomingDeadlines.length }} 项</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Personal Leave Balance -->
    <section v-if="leaveBalances.length" class="leave-balance-section">
      <h3 class="oa-section-title">我的假期余额</h3>
      <div class="leave-balance-grid">
        <div
          v-for="item in leaveBalances"
          :key="item.leaveType"
          class="leave-balance-card"
        >
          <div class="leave-balance-card__header">
            <span class="leave-balance-card__type">{{ LEAVE_TYPE_LABELS[item.leaveType] ?? item.typeName }}</span>
            <el-tag
              v-if="item.pendingDays > 0"
              size="small"
              type="warning"
              effect="light"
              round
            >
              审批中 {{ item.pendingDays }} 天
            </el-tag>
          </div>
          <div class="leave-balance-card__body">
            <div class="leave-balance-card__remaining">
              <span class="leave-balance-card__number">{{ item.remainingDays }}</span>
              <span class="leave-balance-card__unit">天剩余</span>
            </div>
            <el-progress
              :percentage="item.totalDays > 0 ? Math.round((item.usedDays / item.totalDays) * 100) : 0"
              :stroke-width="6"
              :color="item.remainingDays <= 1 ? '#F56C6C' : '#67C23A'"
              style="flex: 1; margin-left: 16px"
            />
          </div>
          <div class="leave-balance-card__footer">
            已用 {{ item.usedDays }} 天 / 共 {{ item.totalDays }} 天
          </div>
        </div>
      </div>
    </section>

    <!-- Pending Approval Alert -->
    <section v-if="pendingApprovals > 0" class="pending-alert">
      <div class="pending-alert__icon">
        <el-icon :size="20"><Bell /></el-icon>
      </div>
      <div class="pending-alert__info">
        <span class="pending-alert__title">您有 <strong>{{ pendingApprovals }}</strong> 条待办审批</span>
        <span class="pending-alert__sub">请尽快处理，避免流程延误</span>
      </div>
      <el-button type="primary" size="small" @click="go('/todos')">去处理</el-button>
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
    </template>
  </div>
</template>

<style scoped>
.skeleton-welcome {
  padding: 24px 28px;
  background: linear-gradient(135deg, var(--oa-primary-bg), #f0f4ff);
  border-radius: var(--oa-radius-lg);
  margin-bottom: 20px;
}

.skeleton-card {
  padding: 20px;
}

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

/* Leave Balance */
.leave-balance-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.leave-balance-card {
  background: var(--oa-bg-white);
  border: 1px solid var(--oa-border-light);
  border-radius: var(--oa-radius-md);
  padding: 16px;
  transition: all var(--oa-transition);
}

.leave-balance-card:hover {
  border-color: var(--oa-primary-lighter);
  box-shadow: var(--oa-shadow-sm);
}

.leave-balance-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.leave-balance-card__type {
  font-size: 14px;
  font-weight: 600;
  color: var(--oa-text-primary);
}

.leave-balance-card__body {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.leave-balance-card__remaining {
  display: flex;
  align-items: baseline;
  gap: 4px;
  flex-shrink: 0;
}

.leave-balance-card__number {
  font-size: 28px;
  font-weight: 700;
  color: var(--oa-primary);
  line-height: 1;
}

.leave-balance-card__unit {
  font-size: 12px;
  color: var(--oa-text-muted);
}

.leave-balance-card__footer {
  font-size: 12px;
  color: var(--oa-text-muted);
}

/* Pending Approval Alert */
.pending-alert {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: linear-gradient(135deg, #fff7e6, #fff1cc);
  border: 1px solid #ffd666;
  border-radius: var(--oa-radius-md);
}

.pending-alert__icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #faad14;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pending-alert__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.pending-alert__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--oa-text-primary);
}

.pending-alert__sub {
  font-size: 12px;
  color: var(--oa-text-secondary);
}

/* AI Insight Banner */
.insight-banner {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f0f7ff, #e8f4fd);
  border: 1px solid #b3d8ff;
  border-radius: var(--oa-radius-md);
  margin-bottom: 20px;
}

.insight-banner__icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.insight-banner__content {
  flex: 1;
  min-width: 0;
}

.insight-banner__briefing {
  font-size: 14px;
  font-weight: 500;
  color: var(--oa-text-primary);
  line-height: 1.6;
  margin-bottom: 8px;
}

.insight-banner__metrics {
  display: flex;
  gap: 20px;
}

.insight-metric {
  display: flex;
  align-items: center;
  gap: 6px;
}

.insight-metric__label {
  font-size: 12px;
  color: var(--oa-text-muted);
}

.insight-metric__value {
  font-size: 13px;
  font-weight: 600;
}

.text-success { color: #67C23A; }
.text-warning { color: #E6A23C; }
</style>
