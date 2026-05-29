import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import BudgetWarningBanner from '../BudgetWarningBanner.vue'

// Mock the API
vi.mock('../../api/budgets', () => ({
  getBudgetWarnings: vi.fn(),
}))

import { getBudgetWarnings } from '../../api/budgets'

describe('BudgetWarningBanner', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should not render when no warnings', async () => {
    vi.mocked(getBudgetWarnings).mockResolvedValue([])

    const wrapper = mount(BudgetWarningBanner)
    await flushPromises()

    expect(wrapper.find('.budget-warning-banner').exists()).toBe(false)
  })

  it('should render warnings when data is available', async () => {
    const warnings = [
      {
        budgetId: 1,
        deptName: '技术部',
        category: 'PURCHASE',
        budgetAmount: 100000,
        usedAmount: 120000,
        overspend: 20000,
        alertType: 'OVER_BUDGET' as const,
        message: '技术部 的 采购 预算已超支 20000 元',
      },
    ]
    vi.mocked(getBudgetWarnings).mockResolvedValue(warnings)

    const wrapper = mount(BudgetWarningBanner)
    await flushPromises()

    expect(wrapper.find('.budget-warning-banner').exists()).toBe(true)
    expect(wrapper.find('.warning-title').text()).toBe('预算预警')
  })

  it('should hide when dismiss button is clicked', async () => {
    const warnings = [
      {
        budgetId: 1,
        deptName: '技术部',
        category: 'PURCHASE',
        budgetAmount: 100000,
        usedAmount: 80000,
        usagePercent: 80,
        alertType: 'NEAR_LIMIT' as const,
        message: '技术部 的 采购 预算使用率已达 80%',
      },
    ]
    vi.mocked(getBudgetWarnings).mockResolvedValue(warnings)

    const wrapper = mount(BudgetWarningBanner)
    await flushPromises()

    expect(wrapper.find('.budget-warning-banner').exists()).toBe(true)

    await wrapper.find('button').trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.budget-warning-banner').exists()).toBe(false)
  })
})
