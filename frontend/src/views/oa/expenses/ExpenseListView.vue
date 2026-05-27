<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listExpenses } from '../../../api/oa-expenses'
import type { JsonObject } from '../../../api/types'
import { usePaginatedList } from '../../../composables/usePaginatedList'
import { formatDisplayDateTime, statusLabel } from '../oa-shared'

const router = useRouter()
const { loading, rows, total, page, size, load, handleSizeChange } = usePaginatedList<JsonObject>(listExpenses)

onMounted(load)

function goCreate() {
  router.push('/oa/expenses/create')
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
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">报销</h2>
        <p class="muted">本人报销单；合计金额须与明细一致。</p>
      </div>
      <el-button type="primary" @click="goCreate">新建报销</el-button>
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

      <el-table v-else v-loading="loading" :data="rows" stripe>
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
          <template #default="{ row }">{{ formatDisplayDateTime(row.updatedAt) }}</template>
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
