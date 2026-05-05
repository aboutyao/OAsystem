<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listExpenses, markPaidExpense } from '../../../api/oa-expenses'
import type { JsonObject } from '../../../api/types'
import { formatDisplayDateTime, statusLabel } from '../oa-shared'

const router = useRouter()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const approvedRows = computed(() => {
  return rows.value.filter((r) => String(r.status ?? '') === 'APPROVED')
})

async function load() {
  loading.value = true
  try {
    const res = await listExpenses(page.value, size.value)
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
  router.push(`/oa/expenses/${Number(row.id)}`)
}

function paymentStatusTag(status: string): '' | 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'PAID': return 'success'
    case 'UNPAID': return 'warning'
    default: return 'info'
  }
}

async function handleMarkPaid(row: JsonObject) {
  try {
    await ElMessageBox.confirm('确认将此报销单标记为已付款？', '确认付款', { type: 'warning' })
  } catch {
    return
  }
  try {
    await markPaidExpense(Number(row.id))
    ElMessage.success('已确认付款')
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
        <h2 class="oa-page__title">报销财务审核</h2>
        <p class="muted">已审批通过的报销单，等待财务确认付款。</p>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="approvedRows" stripe>
        <el-table-column prop="id" label="编号" width="88" />
        <el-table-column prop="expenseNo" label="单号" min-width="140" />
        <el-table-column prop="expenseType" label="类型" width="120" />
        <el-table-column prop="totalAmount" label="金额" width="120" />
        <el-table-column label="付款状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="paymentStatusTag(String(row.paymentStatus ?? ''))">
              {{ row.paymentStatus === 'PAID' ? '已付款' : '未付款' }}
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
              v-if="row.paymentStatus !== 'PAID'"
              link
              type="success"
              @click="handleMarkPaid(row)"
            >确认付款</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && approvedRows.length === 0" description="暂无待财务审核的报销单" />

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
