import { ref, onMounted, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { JsonObject, PageResponse } from '../api/types'

interface UseListPageOptions<T = JsonObject> {
  fetchFn: (page: number, size: number) => Promise<PageResponse<T>>
  defaultSize?: number
  errorHandler?: (e: unknown) => void
}

export function useListPage<T = JsonObject>(options: UseListPageOptions<T>) {
  const { fetchFn, defaultSize = 20, errorHandler } = options

  const loading = ref(false)
  const rows: Ref<T[]> = ref([])
  const total = ref(0)
  const page = ref(1)
  const size = ref(defaultSize)

  async function load() {
    loading.value = true
    try {
      const res = await fetchFn(page.value, size.value)
      rows.value = res.items
      total.value = Number(res.total)
    } catch (e) {
      if (errorHandler) {
        errorHandler(e)
      } else {
        ElMessage.error(e instanceof Error ? e.message : '加载失败')
      }
    } finally {
      loading.value = false
    }
  }

  function handleSizeChange() {
    page.value = 1
    load()
  }

  function handlePageChange() {
    load()
  }

  onMounted(load)

  return {
    loading,
    rows,
    total,
    page,
    size,
    load,
    handleSizeChange,
    handlePageChange,
  }
}
