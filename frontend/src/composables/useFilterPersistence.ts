import { ref, watch } from 'vue'

const STORAGE_PREFIX = 'oa_filter_'

/**
 * 列表筛选器持久化
 * 保存筛选条件到 localStorage，刷新后恢复
 */
export function useFilterPersistence<T extends Record<string, unknown>>(key: string, defaults: T) {
  const storageKey = STORAGE_PREFIX + key
  const filter = ref<T>(restore() || { ...defaults })

  function restore(): T | null {
    try {
      const raw = localStorage.getItem(storageKey)
      if (!raw) return null
      return JSON.parse(raw) as T
    } catch {
      return null
    }
  }

  function save() {
    try {
      localStorage.setItem(storageKey, JSON.stringify(filter.value))
    } catch {
      // ignore
    }
  }

  function reset() {
    filter.value = { ...defaults }
    save()
  }

  function clear() {
    localStorage.removeItem(storageKey)
    filter.value = { ...defaults }
  }

  // Auto-save on change
  watch(filter, save, { deep: true })

  return { filter, reset, clear }
}
