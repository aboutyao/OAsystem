<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listPurchases } from '../../../api/oa-purchases'
import type { JsonObject } from '../../../api/types'
import { usePaginatedList } from '../../../composables/usePaginatedList'
import { formatDisplayDateTime, statusLabel } from '../oa-shared'

const router = useRouter()
const { loading, rows, total, page, size, load, handleSizeChange } = usePaginatedList<JsonObject>(listPurchases)

function arrivalLabel(v: unknown) {
  const s = String(v ?? '')
  if (s === 'NOT_ARRIVED') return '未到货'
  if (s === 'ARRIVED') return '已到货'
  return s || '—'
}

function acceptanceLabel(v: unknown) {
  const s = String(v ?? '')
  if (s === 'PENDING') return '待验收'
  if (s === 'PASSED') return '已通过'
  return s || '—'
}

onMounted(load)

function goCreate() {
  router.push('/oa/purchases/create')
}

function goDetail(row: JsonObject) {
  router.push(`/oa/purchases/${Number(row.id)}`)
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">采购申请</h2>
        <p class="muted">审批通过后可进行到货确认与验收。</p>
      </div>
      <el-button type="primary" @click="goCreate">新建采购</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="编号" width="88" />
        <el-table-column prop="purchaseNo" label="单号" min-width="140" />
        <el-table-column prop="purchaseType" label="类型" width="120" />
        <el-table-column prop="totalAmount" label="金额" width="120" />
        <el-table-column label="到货" width="100">
          <template #default="{ row }">{{ arrivalLabel(row.arrivalStatus) }}</template>
        </el-table-column>
        <el-table-column label="验收" width="100">
          <template #default="{ row }">{{ acceptanceLabel(row.acceptanceStatus) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ statusLabel(String(row.status ?? '')) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160">
          <template #default="{ row }">{{ formatDisplayDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">查看</el-button>
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
