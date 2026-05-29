import { ref, computed } from 'vue'

/**
 * 全局 Loading 状态管理
 * 支持多个并发加载任务
 */

const loadingTasks = ref<Set<string>>(new Set())

/**
 * 全局 loading 状态
 */
export const isLoading = computed(() => loadingTasks.value.size > 0)

/**
 * 当前加载任务数
 */
export const loadingCount = computed(() => loadingTasks.value.size)

/**
 * 开始加载任务
 * @param taskKey 任务唯一标识
 */
export function startLoading(taskKey: string) {
  loadingTasks.value.add(taskKey)
}

/**
 * 结束加载任务
 * @param taskKey 任务唯一标识
 */
export function stopLoading(taskKey: string) {
  loadingTasks.value.delete(taskKey)
}

/**
 * 包装异步函数，自动管理 loading 状态
 */
export function withLoading<T extends (...args: unknown[]) => Promise<unknown>>(
  fn: T,
  taskKey?: string
): T {
  const key = taskKey || fn.name || `task_${Date.now()}`

  return ((...args: unknown[]) => {
    startLoading(key)
    return fn(...args).finally(() => stopLoading(key))
  }) as T
}

/**
 * 组合式函数：组件级 loading
 */
export function useComponentLoading(initialState = false) {
  const loading = ref(initialState)

  async function withComponentLoading<T>(fn: () => Promise<T>): Promise<T> {
    loading.value = true
    try {
      return await fn()
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    withLoading: withComponentLoading,
  }
}
