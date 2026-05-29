import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { JsonObject, PageResponse } from '../api/types'

/**
 * 统一分页列表 composable
 * @param fetchFn 分页查询函数
 * @param defaultSize 默认每页条数
 */
export function usePaginatedList<T = JsonObject>(
  fetchFn: (page: number, size: number) => Promise<PageResponse<T>>,
  defaultSize = 20,
) {
  const loading = ref(false)
  const rows = ref<T[]>([])
  const total = ref(0)
  const page = ref(1)
  const size = ref(defaultSize)
  const error = ref<string | null>(null)

  async function load() {
    loading.value = true
    error.value = null
    try {
      const res = await fetchFn(page.value, size.value)
      rows.value = res.items
      total.value = Number(res.total)
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : '加载失败'
      ElMessage.error(error.value)
    } finally {
      loading.value = false
    }
  }

  function handleSizeChange() {
    page.value = 1
    load()
  }

  function handleCurrentChange() {
    load()
  }

  function refresh() {
    load()
  }

  function goToPage(p: number) {
    page.value = p
    load()
  }

  return {
    loading,
    rows,
    total,
    page,
    size,
    error,
    load,
    refresh,
    handleSizeChange,
    handleCurrentChange,
    goToPage,
  }
}
