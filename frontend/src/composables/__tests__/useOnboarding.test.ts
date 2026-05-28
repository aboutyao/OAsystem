import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useOnboarding } from '../useOnboarding'

describe('useOnboarding', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('should not show onboarding when already completed', () => {
    localStorage.setItem('oa_onboarding_completed', 'true')
    const { isActive, completed } = useOnboarding()
    expect(completed.value).toBe(true)
  })

  it('should start onboarding for new users', () => {
    const { start, isActive, step } = useOnboarding()
    start()
    expect(isActive.value).toBe(true)
    expect(step.value?.title).toBe('欢迎使用企业 OA 系统')
  })

  it('should advance through steps', () => {
    const { start, next, currentStep, totalSteps } = useOnboarding()
    start()
    expect(currentStep.value).toBe(0)

    next()
    expect(currentStep.value).toBe(1)

    next()
    expect(currentStep.value).toBe(2)
  })

  it('should complete and persist', () => {
    const { start, next, complete, isActive, completed } = useOnboarding()
    start()
    complete()
    expect(isActive.value).toBe(false)
    expect(completed.value).toBe(true)
    expect(localStorage.getItem('oa_onboarding_completed')).toBe('true')
  })

  it('should go back to previous step', () => {
    const { start, next, prev, currentStep } = useOnboarding()
    start()
    next()
    expect(currentStep.value).toBe(1)
    prev()
    expect(currentStep.value).toBe(0)
  })

  it('should skip entire onboarding', () => {
    const { start, skip, isActive, completed } = useOnboarding()
    start()
    skip()
    expect(isActive.value).toBe(false)
    expect(completed.value).toBe(true)
  })

  it('should reset onboarding', () => {
    localStorage.setItem('oa_onboarding_completed', 'true')
    const { reset, completed } = useOnboarding()
    reset()
    expect(completed.value).toBe(false)
    expect(localStorage.getItem('oa_onboarding_completed')).toBeNull()
  })
})
