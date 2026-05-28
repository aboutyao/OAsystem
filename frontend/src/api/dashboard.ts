import { http } from './http'

export interface DashboardSummary {
  todoCount: number
  messageCount: number
  startedCount: number
  ccCount: number
  exceptionCount: number
}

export interface DashboardTodo {
  taskId: number
  title: string
  nodeName?: string
  status: string
  wfInstanceId?: number
  businessType?: string
  businessId?: number
  createdAt?: string
}

export interface DashboardStarted {
  wfInstanceId: number
  title: string
  businessType: string
  businessId: number
  status: string
  currentNodeName?: string | null
  startedAt?: string
}

export interface DashboardCcItem {
  ccId: number
  wfInstanceId: number
  title: string
  businessType: string
  businessId: number
  readAt?: string | null
  createdAt?: string
}

export interface DashboardNotice {
  id: number
  title: string
  category?: string
  publishAt?: string
  topFlag?: number
  createdByName?: string
}

export interface QuickAction {
  label: string
  path: string
  requirePermission?: string
}

export function getDashboardSummary() {
  return http.get<unknown, DashboardSummary>('/dashboard/summary')
}

export function getDashboardTodos(limit = 10) {
  return http.get<unknown, DashboardTodo[]>('/dashboard/todos', { params: { limit } })
}

export function getDashboardStarted(limit = 10) {
  return http.get<unknown, DashboardStarted[]>('/dashboard/started', { params: { limit } })
}

export function getDashboardCcToMe(limit = 10) {
  return http.get<unknown, DashboardCcItem[]>('/dashboard/cc-to-me', { params: { limit } })
}

export function getDashboardNotices(limit = 10) {
  return http.get<unknown, DashboardNotice[]>('/dashboard/notices', { params: { limit } })
}

export function getDashboardQuickActions() {
  return http.get<unknown, QuickAction[]>('/dashboard/quick-actions')
}

export interface LeaveBalanceItem {
  leaveType: string
  typeName: string
  daysPerYear: number
  totalDays: number
  usedDays: number
  pendingDays: number
  remainingDays: number
}

export function getMyLeaveBalance() {
  return http.get<unknown, LeaveBalanceItem[]>('/leave-balance/my')
}

export interface DashboardInsight {
  briefing: string
  approvalVelocity: {
    avgHours: number
    teamAvgHours: number
    fasterThanTeam: boolean
    speedRatio: number
  }
  upcomingDeadlines: Array<{
    type: string
    title: string
    deadline: string
    urgency: string
  }>
  topActions: Array<{
    path: string
    label: string
    count: number
  }>
}

export function getDashboardInsights() {
  return http.get<unknown, DashboardInsight>('/dashboard/insights')
}

export function trackQuickAction(path: string) {
  return http.get<unknown, void>('/dashboard/track-action', { params: { path } })
}
