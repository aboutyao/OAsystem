<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listContracts } from '../../api/contracts'
import type { JsonObject } from '../../api/types'
import { formatRelativeTime, statusLabel } from '../oa/oa-shared'

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

      <el-table v-else v-loading="loading" :data="rows" stripe>
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
