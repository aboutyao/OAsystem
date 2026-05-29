import { http } from './http'
import type { JsonObject } from './types'

export interface Budget {
  id: number
  deptId: number
  budgetType: string
  year: number
  month?: number
  quarter?: number
  category: string
  budgetAmount: number
  usedAmount: number
  remainingAmount: number
  usagePercent: number
  warningThreshold: number
  isOverBudget: boolean
  isNearLimit: boolean
}

export interface BudgetWarning {
  budgetId: number
  deptName: string
  category: string
  budgetAmount: number
  usedAmount: number
  usagePercent?: number
  overspend?: number
  alertType: 'NEAR_LIMIT' | 'OVER_BUDGET'
  message: string
}

export function listBudgets(params?: { deptId?: number; category?: string; year?: number }) {
  return http.get<unknown, Budget[]>('/budgets', { params })
}

export function createBudget(data: {
  deptId: number
  budgetType: string
  year: number
  month?: number
  quarter?: number
  category: string
  budgetAmount: number
  warningThreshold?: number
}) {
  return http.post<unknown, JsonObject>('/budgets', data)
}

export function getBudgetWarnings() {
  return http.get<unknown, BudgetWarning[]>('/budgets/warnings')
}
