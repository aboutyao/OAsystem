<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listContracts, submitContract, withdrawContract } from '../../api/contracts'
import type { JsonObject } from '../../api/types'
import { formatRelativeTime, statusLabel } from '../oa/oa-shared'
import type { TableInstance } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

async function load() {
  loading.value = true
  try {
    const res = await listContracts(page.value, size.value)
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void load()

function handleSizeChange() {
  page.value = 1
  load()
}

function goCreate() {
  router.push('/contracts/create')
}

function goDetail(row: JsonObject) {
  router.push(`/contracts/${Number(row.id)}`)
}

function statusTagType(status: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'APPROVED':
    case 'SIGNED': return 'success'
    case 'APPROVING': return 'warning'
    case 'REJECTED':
    case 'TERMINATED': return 'danger'
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
      `确认批量提交 ${draftItems.length} 条合同草稿？`,
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
        await submitContract(Number(row.id))
        success++
      } catch {
        failed++
      }
    }
    if (failed === 0) {
      ElMessage.success(`已批量提交 ${success} 条合同`)
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
      `确认批量撤回 ${approvingItems.length} 条审批中的合同？`,
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
        await withdrawContract(Number(row.id))
        success++
      } catch {
        failed++
      }
    }
    if (failed === 0) {
      ElMessage.success(`已批量撤回 ${success} 条合同`)
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
        <h2 class="oa-page__title">合同</h2>
        <p class="muted">本人合同；草稿可编辑并提交审批。</p>
      </div>
      <el-button type="primary" @click="goCreate">新建合同</el-button>
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
        <el-result icon="info" title="暂无数据" sub-title="目前没有合同记录，您可以点击下方按钮新建合同">
          <template #extra>
            <el-button type="primary" @click="goCreate">
              <el-icon><Plus /></el-icon>新建合同
            </el-button>
          </template>
        </el-result>
      </template>

      <el-table v-else ref="tableRef" v-loading="loading" :data="rows" stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="编号" width="88" />
        <el-table-column prop="contractNo" label="合同编号" min-width="140" />
        <el-table-column prop="contractName" label="名称" min-width="160" />
        <el-table-column prop="contractType" label="类型" width="100" />
        <el-table-column prop="counterparty" label="相对方" width="140" />
        <el-table-column prop="amount" label="金额" width="120" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag effect="light" size="small" :type="statusTagType(String(row.status ?? ''))">
              {{ statusLabel(String(row.status ?? '')) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结束日" width="120">
          <template #default="{ row }">{{ formatRelativeTime(row.endDate) }}</template>
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
