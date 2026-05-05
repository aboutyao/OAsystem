<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listContracts } from '../../api/contracts'
import type { JsonObject } from '../../api/types'
import { formatDisplayDate, statusLabel } from '../oa/oa-shared'

const router = useRouter()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const keyword = ref('')

const approvedRows = computed(() => {
  let list = rows.value.filter((r) => String(r.status ?? '') === 'APPROVED')
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(
      (r) =>
        String(r.contractName ?? '').toLowerCase().includes(kw) ||
        String(r.contractNo ?? '').toLowerCase().includes(kw) ||
        String(r.counterparty ?? '').toLowerCase().includes(kw),
    )
  }
  return list
})

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

onMounted(load)

function handleSizeChange() {
  page.value = 1
  load()
}

function goDetail(row: JsonObject) {
  router.push(`/contracts/${Number(row.id)}`)
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">合同归档</h2>
        <p class="muted">已审批通过的合同归档列表。</p>
      </div>
    </div>

    <el-card shadow="never">
      <div class="oa-filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索合同名称、编号、相对方..."
          clearable
          style="width: 280px"
          size="small"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <el-table v-loading="loading" :data="approvedRows" stripe>
        <el-table-column prop="id" label="编号" width="88" />
        <el-table-column prop="contractNo" label="合同编号" min-width="140" />
        <el-table-column prop="contractName" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="contractType" label="类型" width="100" />
        <el-table-column prop="counterparty" label="相对方" width="140" show-overflow-tooltip />
        <el-table-column prop="amount" label="金额" width="120" />
        <el-table-column label="开始日" width="120">
          <template #default="{ row }">{{ formatDisplayDate(row.startDate) }}</template>
        </el-table-column>
        <el-table-column label="结束日" width="120">
          <template #default="{ row }">{{ formatDisplayDate(row.endDate) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="success">{{ statusLabel(String(row.status ?? '')) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && approvedRows.length === 0" description="暂无已归档合同" />

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
