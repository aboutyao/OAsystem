import { computed, ref, onMounted } from 'vue'

const ONBOARDING_KEY = 'oa_onboarding_completed'
const STEPS = [
  {
    target: '.app-shell__brand',
    title: '欢迎使用企业 OA 系统',
    content: '这是导航菜单，点击可以展开/收起侧边栏。',
    placement: 'right' as const,
  },
  {
    target: '.app-shell__search-input',
    title: '全局搜索',
    content: '搜索人员、请假、合同等。也可以按 Ctrl+K 打开命令面板。',
    placement: 'bottom' as const,
  },
  {
    target: '.app-shell__header-action',
    title: '快捷入口',
    content: '命令面板、智能日历、通知中心，触手可及。',
    placement: 'bottom' as const,
  },
]

export function useOnboarding() {
  const isActive = ref(false)
  const currentStep = ref(0)
  const completed = ref(localStorage.getItem(ONBOARDING_KEY) === 'true')

  function start() {
    if (completed.value) return
    currentStep.value = 0
    isActive.value = true
  }

  function next() {
    if (currentStep.value < STEPS.length - 1) {
      currentStep.value++
    } else {
      complete()
    }
  }

  function prev() {
    if (currentStep.value > 0) {
      currentStep.value--
    }
  }

  function complete() {
    isActive.value = false
    completed.value = true
    localStorage.setItem(ONBOARDING_KEY, 'true')
  }

  function skip() {
    complete()
  }

  function reset() {
    localStorage.removeItem(ONBOARDING_KEY)
    completed.value = false
  }

  const step = computed(() => STEPS[currentStep.value] || null)
  const totalSteps = STEPS.length
  const isFirst = computed(() => currentStep.value === 0)
  const isLast = computed(() => currentStep.value === totalSteps - 1)

  onMounted(() => {
    if (!completed.value) {
      setTimeout(() => start(), 1000)
    }
  })

  return {
    isActive,
    currentStep,
    totalSteps,
    step,
    isFirst,
    isLast,
    completed,
    start,
    next,
    prev,
    complete,
    skip,
    reset,
  }
}
