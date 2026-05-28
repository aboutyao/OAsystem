<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listExpenses, submitExpense, withdrawExpense } from '../../../api/oa-expenses'
import type { JsonObject } from '../../../api/types'
import { usePaginatedList } from '../../../composables/usePaginatedList'
import { formatRelativeTime, statusLabel } from '../oa-shared'
import type { TableInstance } from 'element-plus'

const router = useRouter()
const { loading, rows, total, page, size, load, handleSizeChange } = usePaginatedList<JsonObject>(listExpenses)

onMounted(load)

function goCreate() {
  router.push('/oa/expenses/create')
}

async function handleExport() {
  try {
    const token = localStorage.getItem('oa_access_token')
    const baseURL = (import.meta.env.VITE_API_BASE_URL as string) || '/api'
    const res = await axios.post(`${baseURL}/oa/expenses/export`, {}, {
      responseType: 'blob',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '报销列表.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导出失败')
  }
}

function goDetail(row: JsonObject) {
  router.push(`/oa/expenses/${Number(row.id)}`)
}

function statusTagType(status: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'APPROVED': return 'success'
    case 'APPROVING': return 'warning'
    case 'REJECTED':
    case 'CANCELLED': return 'danger'
    case 'DRAFT': return 'info'
    default: return ''
  }
}

const statusCounts = computed(() => {
  const counts: Record<string, number> = {}
  rows.value.forEach((r) => {
    const s = String(r.status ?? 'UNKNOWN')
    counts[s] = (counts[s] || 0) + 1
  })
  return counts
})

// --- Batch operations ---
const selectedRows = ref<JsonObject[]>([])
const tableRef = ref<TableInstance>()
const batchActing = ref(false)

function onSelectionChange(selection: JsonObject[]) {
  selectedRows.value = selection
}

function clearSelection() {
  selectedRows.value = []
  tableRef.value?.clearSelection()
}

const selectedDraftCount = computed(() =>
  selectedRows.value.filter((r) => String(r.status ?? '') === 'DRAFT').length,
)

const selectedApprovingCount = computed(() =>
  selectedRows.value.filter((r) => String(r.status ?? '') === 'APPROVING').length,
)

async function batchSubmit() {
  const draftItems = selectedRows.value.filter((r) => String(r.status ?? '') === 'DRAFT')
  if (draftItems.length === 0) {
    ElMessage.warning('所选项中没有可提交的草稿')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认批量提交 ${draftItems.length} 条报销草稿？`,
      '批量提交',
      { type: 'warning' },
    )
  } catch {
    return
  }
  batchActing.value = true
  try {
    let success = 0
    let failed = 0
    for (const row of draftItems) {
      try {
        await submitExpense(Number(row.id))
        success++
      } catch {
        failed++
      }
    }
    if (failed === 0) {
      ElMessage.success(`已批量提交 ${success} 条报销`)
    } else {
      ElMessage.warning(`提交 ${success} 条，失败 ${failed} 条`)
    }
    clearSelection()
    await load()
  } finally {
    batchActing.value = false
  }
}

async function batchWithdraw() {
  const approvingItems = selectedRows.value.filter((r) => String(r.status ?? '') === 'APPROVING')
  if (approvingItems.length === 0) {
    ElMessage.warning('所选项中没有可撤回的审批中记录')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认批量撤回 ${approvingItems.length} 条审批中的报销？`,
      '批量撤回',
      { type: 'warning' },
    )
  } catch {
    return
  }
  batchActing.value = true
  try {
    let success = 0
    let failed = 0
    for (const row of approvingItems) {
      try {
        await withdrawExpense(Number(row.id))
        success++
      } catch {
        failed++
      }
    }
    if (failed === 0) {
      ElMessage.success(`已批量撤回 ${success} 条报销`)
    } else {
      ElMessage.warning(`撤回 ${success} 条，失败 ${failed} 条`)
    }
    clearSelection()
    await load()
  } finally {
    batchActing.value = false
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">报销</h2>
        <p class="muted">本人报销单；合计金额须与明细一致。</p>
      </div>
      <el-button type="primary" @click="goCreate">新建报销</el-button>
      <el-button @click="handleExport">
        <el-icon><Download /></el-icon>导出
      </el-button>
    </div>

    <div v-if="selectedRows.length > 0" class="batch-toolbar">
      <span>已选择 {{ selectedRows.length }} 项</span>
      <el-button v-if="selectedDraftCount > 0" type="success" :loading="batchActing" @click="batchSubmit">
        批量提交 ({{ selectedDraftCount }} 条草稿)
      </el-button>
      <el-button v-if="selectedApprovingCount > 0" type="warning" :loading="batchActing" @click="batchWithdraw">
        批量撤回 ({{ selectedApprovingCount }} 条审批中)
      </el-button>
      <el-button @click="clearSelection">取消选择</el-button>
    </div>

    <el-card shadow="never">
      <div v-if="rows.length" class="oa-filter-bar" style="margin-bottom: 0">
        <div style="display: flex; gap: 8px; flex-wrap: wrap">
          <el-tag effect="plain" size="small">全部 {{ rows.length }}</el-tag>
          <el-tag
            v-for="(count, st) in statusCounts"
            :key="st"
            :type="statusTagType(String(st))"
            effect="light"
            size="small"
          >
            {{ statusLabel(String(st)) }} {{ count }}
          </el-tag>
        </div>
      </div>

      <template v-if="!loading && rows.length === 0">
        <el-result icon="info" title="暂无数据" sub-title="目前没有报销记录，您可以点击下方按钮新建报销">
          <template #extra>
            <el-button type="primary" @click="goCreate">
              <el-icon><Plus /></el-icon>新建报销
            </el-button>
          </template>
        </el-result>
      </template>

      <el-table v-else ref="tableRef" v-loading="loading" :data="rows" stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="编号" width="88" />
        <el-table-column prop="expenseNo" label="单号" min-width="140" />
        <el-table-column prop="expenseType" label="类型" width="120" />
        <el-table-column prop="totalAmount" label="金额" width="120" />
        <el-table-column prop="paymentStatus" label="付款" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag effect="light" size="small" :type="statusTagType(String(row.status ?? ''))">
              {{ statusLabel(String(row.status ?? '')) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160">
          <template #default="{ row }">{{ formatRelativeTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">
              <el-icon><View /></el-icon>查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="oa-page__pager">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          v-model:current-page="page"
          v-model:page-size="size"
          @current-change="load"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.batch-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 12px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 6px;
  font-size: 14px;
}
</style>
