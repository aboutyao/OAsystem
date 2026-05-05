<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPurchases, confirmPurchaseArrival } from '../../../api/oa-purchases'
import type { JsonObject } from '../../../api/types'
import { formatDisplayDateTime, statusLabel } from '../oa-shared'

const router = useRouter()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

function arrivalTagType(status: string): '' | 'success' | 'warning' | 'info' {
  if (status === 'ARRIVED') return 'success'
  if (status === 'NOT_ARRIVED') return 'warning'
  return 'info'
}

function arrivalLabel(v: unknown): string {
  const s = String(v ?? '')
  if (s === 'NOT_ARRIVED') return '未到货'
  if (s === 'ARRIVED') return '已到货'
  return s || '—'
}

async function load() {
  loading.value = true
  try {
    const res = await listPurchases(page.value, size.value, { status: 'APPROVED' })
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
  router.push(`/oa/purchases/${Number(row.id)}`)
}

async function handleConfirmArrival(row: JsonObject) {
  try {
    await ElMessageBox.confirm('确认此采购单已到货？', '登记到货', { type: 'warning' })
  } catch {
    return
  }
  try {
    await confirmPurchaseArrival(Number(row.id))
    ElMessage.success('已登记到货')
    load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">到货登记</h2>
        <p class="muted">已审批通过的采购单，确认到货状态。</p>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="编号" width="88" />
        <el-table-column prop="purchaseNo" label="单号" min-width="140" />
        <el-table-column prop="purchaseType" label="类型" width="120" />
        <el-table-column prop="totalAmount" label="金额" width="120" />
        <el-table-column label="到货状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="arrivalTagType(String(row.arrivalStatus ?? ''))">
              {{ arrivalLabel(row.arrivalStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="success">{{ statusLabel(String(row.status ?? '')) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160">
          <template #default="{ row }">{{ formatDisplayDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">查看</el-button>
            <el-button
              v-if="row.arrivalStatus !== 'ARRIVED'"
              link
              type="success"
              @click="handleConfirmArrival(row)"
            >登记到货</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && rows.length === 0" description="暂无待到货登记的采购单" />

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
