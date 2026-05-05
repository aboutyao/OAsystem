<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPurchases, acceptPurchase } from '../../../api/oa-purchases'
import type { JsonObject } from '../../../api/types'
import { formatDisplayDateTime, statusLabel } from '../oa-shared'

const router = useRouter()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const arrivedRows = computed(() => {
  return rows.value.filter((r) => String(r.arrivalStatus ?? '') === 'ARRIVED')
})

function acceptanceTagType(status: string): '' | 'success' | 'warning' | 'info' {
  if (status === 'PASSED') return 'success'
  if (status === 'PENDING') return 'warning'
  return 'info'
}

function acceptanceLabel(v: unknown): string {
  const s = String(v ?? '')
  if (s === 'PENDING') return '待验收'
  if (s === 'PASSED') return '已通过'
  if (s === 'REJECTED') return '不通过'
  return s || '—'
}

async function load() {
  loading.value = true
  try {
    const res = await listPurchases(page.value, size.value)
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

async function handleAccept(row: JsonObject) {
  try {
    await ElMessageBox.confirm('确认此采购单验收通过？', '验收确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await acceptPurchase(Number(row.id))
    ElMessage.success('验收已通过')
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
        <h2 class="oa-page__title">采购验收</h2>
        <p class="muted">已到货的采购单，进行验收确认。</p>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="arrivedRows" stripe>
        <el-table-column prop="id" label="编号" width="88" />
        <el-table-column prop="purchaseNo" label="单号" min-width="140" />
        <el-table-column prop="purchaseType" label="类型" width="120" />
        <el-table-column prop="totalAmount" label="金额" width="120" />
        <el-table-column label="到货状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="success">已到货</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="验收状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="acceptanceTagType(String(row.acceptanceStatus ?? ''))">
              {{ acceptanceLabel(row.acceptanceStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ statusLabel(String(row.status ?? '')) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160">
          <template #default="{ row }">{{ formatDisplayDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">查看</el-button>
            <el-button
              v-if="row.acceptanceStatus !== 'PASSED'"
              link
              type="success"
              @click="handleAccept(row)"
            >验收</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && arrivedRows.length === 0" description="暂无待验收的采购单" />

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
