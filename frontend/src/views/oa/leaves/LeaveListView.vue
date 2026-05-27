<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listLeaves } from '../../../api/oa-leaves'
import type { JsonObject } from '../../../api/types'
import { usePaginatedList } from '../../../composables/usePaginatedList'
import { formatRelativeTime, OA_STATUS_LABEL, statusLabel } from '../oa-shared'

const router = useRouter()
const { loading, rows, total, page, size, load, handleSizeChange } = usePaginatedList<JsonObject>(listLeaves)

const statusFilter = ref('')
const keyword = ref('')

const statusOptions = [
  { value: '', label: '全部' },
  { value: 'DRAFT', label: '草稿' },
  { value: 'APPROVING', label: '审批中' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' },
  { value: 'WITHDRAWN', label: '已撤回' },
  { value: 'CANCELLED', label: '已作废' },
]

const filteredRows = computed(() => {
  let list = rows.value
  if (statusFilter.value) {
    list = list.filter((r) => String(r.status ?? '') === statusFilter.value)
  }
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(
      (r) =>
        String(r.leaveType ?? '').toLowerCase().includes(kw) ||
        String(r.reason ?? '').toLowerCase().includes(kw) ||
        String(r.id).includes(kw),
    )
  }
  return list
})

onMounted(load)

function goCreate() {
  router.push('/oa/leaves/create')
}

function goDetail(row: JsonObject) {
  router.push(`/oa/leaves/${Number(row.id)}`)
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
        <h2 class="oa-page__title">请假</h2>
        <p class="muted">本人请假列表；草稿可编辑并提交审批。</p>
      </div>
      <el-button type="primary" @click="goCreate">
        <el-icon><Plus /></el-icon>新建请假
      </el-button>
    </div>

    <el-card shadow="never">
      <div class="oa-filter-bar">
        <el-radio-group v-model="statusFilter" size="small">
          <el-radio-button v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </el-radio-button>
        </el-radio-group>
        <el-input
          v-model="keyword"
          placeholder="搜索类型、事由、编号..."
          clearable
          style="width: 240px"
          size="small"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <div v-if="rows.length" class="oa-filter-bar" style="margin-bottom: 0">
        <div style="display: flex; gap: 8px; flex-wrap: wrap">
          <el-tag effect="plain" size="small">全部 {{ rows.length }}</el-tag>
          <el-tag
            v-for="(count, status) in statusCounts"
            :key="status"
            :type="statusTagType(String(status))"
            effect="light"
            size="small"
          >
            {{ statusLabel(String(status)) }} {{ count }}
          </el-tag>
        </div>
      </div>

      <template v-if="!loading && filteredRows.length === 0">
        <el-result icon="info" title="暂无数据" sub-title="目前没有请假记录，您可以点击下方按钮新建请假">
          <template #extra>
            <el-button type="primary" @click="goCreate">
              <el-icon><Plus /></el-icon>新建请假
            </el-button>
          </template>
        </el-result>
      </template>

      <el-table v-else v-loading="loading" :data="filteredRows" stripe @row-click="(r: JsonObject) => goDetail(r)">
        <el-table-column prop="id" label="编号" width="88" />
        <el-table-column prop="leaveType" label="类型" width="100" />
        <el-table-column label="开始时间" min-width="160">
          <template #default="{ row }">{{ formatRelativeTime(row.startAt) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="160">
          <template #default="{ row }">{{ formatRelativeTime(row.endAt) }}</template>
        </el-table-column>
        <el-table-column label="时长" width="100">
          <template #default="{ row }">{{ row.durationDays ?? '—' }} 天</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag effect="light" size="small" :type="statusTagType(String(row.status ?? ''))">
              {{ statusLabel(String(row.status ?? '')) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" @click.stop="">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="goDetail(row)">
              <el-icon><View /></el-icon>查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="oa-page__pager">
        <el-pagination
          layout="total, sizes, prev, pager, next"
          :total="total"
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          @current-change="load"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>
