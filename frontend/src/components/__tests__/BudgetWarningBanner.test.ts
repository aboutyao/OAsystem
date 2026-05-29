import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import BudgetWarningBanner from '../BudgetWarningBanner.vue'

// Mock the API
vi.mock('../../api/budgets', () => ({
  getBudgetWarnings: vi.fn().mockResolvedValue([]),
}))

describe('BudgetWarningBanner', () => {
  it('should render correctly', () => {
    const wrapper = mount(BudgetWarningBanner)
    expect(wrapper.exists()).toBe(true)
  })
})
