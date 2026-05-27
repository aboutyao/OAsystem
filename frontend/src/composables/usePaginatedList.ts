import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { JsonObject, PageResponse } from '../api/types'

export function usePaginatedList<T = JsonObject>(
  fetchFn: (page: number, size: number) => Promise<PageResponse<T>>,
  defaultSize = 20,
) {
  const loading = ref(false)
  const rows = ref<T[]>([])
  const total = ref(0)
  const page = ref(1)
  const size = ref(defaultSize)

  async function load() {
    loading.value = true
    try {
      const res = await fetchFn(page.value, size.value)
      rows.value = res.items
      total.value = Number(res.total)
    } catch (e: unknown) {
      ElMessage.error(e instanceof Error ? e.message : '加载失败')
    } finally {
      loading.value = false
    }
  }

  function handleSizeChange() {
    page.value = 1
    load()
  }

  return { loading, rows, total, page, size, load, handleSizeChange }
}
