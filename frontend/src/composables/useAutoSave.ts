import { ref, watch, onUnmounted } from 'vue'

const STORAGE_PREFIX = 'oa_draft_'

/**
 * 表单草稿自动保存
 * 自动保存到 localStorage，页面刷新后恢复
 */
export function useAutoSave<T extends Record<string, unknown>>(
  form: T,
  key: string,
  options?: { interval?: number; enabled?: boolean }
) {
  const storageKey = STORAGE_PREFIX + key
  const lastSaved = ref<string | null>(null)
  const enabled = ref(options?.enabled ?? true)
  let timer: ReturnType<typeof setInterval> | null = null

  function save() {
    if (!enabled.value) return
    try {
      const data = JSON.stringify(form)
      localStorage.setItem(storageKey, data)
      lastSaved.value = new Date().toLocaleTimeString('zh-CN')
    } catch {
      // ignore quota exceeded
    }
  }

  function restore(): T | null {
    try {
      const raw = localStorage.getItem(storageKey)
      if (!raw) return null
      return JSON.parse(raw) as T
    } catch {
      return null
    }
  }

  function clear() {
    localStorage.removeItem(storageKey)
    lastSaved.value = null
  }

  // Auto-save on form change
  watch(
    () => ({ ...form }),
    () => {
      if (enabled.value) save()
    },
    { deep: true }
  )

  // Periodic save
  if (options?.interval) {
    timer = setInterval(save, options.interval)
  }

  onUnmounted(() => {
    if (timer) clearInterval(timer)
  })

  return { lastSaved, save, restore, clear, enabled }
}
