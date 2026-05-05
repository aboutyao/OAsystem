<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelExpense, getExpense, submitExpense, withdrawExpense } from '../../../api/oa-expenses'
import type { JsonObject } from '../../../api/types'
import { formatDisplayDate, formatDisplayDateTime, statusLabel } from '../oa-shared'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const row = ref<JsonObject | null>(null)
const items = ref<JsonObject[]>([])

const id = computed(() => Number(route.params.id))
const status = computed(() => (row.value ? String(row.value.status ?? '') : ''))

onMounted(async () => {
  try {
    const data = await getExpense(id.value)
    const { items: it, ...rest } = data as JsonObject & { items?: JsonObject[] }
    row.value = rest
    items.value = Array.isArray(it) ? it : []
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/oa/expenses')
  } finally {
    loading.value = false
  }
})

async function reload() {
  try {
    const data = await getExpense(id.value)
    const { items: it, ...rest } = data as JsonObject & { items?: JsonObject[] }
    row.value = rest
    items.value = Array.isArray(it) ? it : []
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  }
}

function goEdit() {
  router.push(`/oa/expenses/${id.value}/edit`)
}

async function onSubmit() {
  try {
    await ElMessageBox.confirm('确认提交该报销单？', '提交', { type: 'warning' })
    await submitExpense(id.value)
    ElMessage.success('已提交')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '提交失败')
  }
}

async function onWithdraw() {
  try {
    await ElMessageBox.confirm('确认撤回？', '撤回', { type: 'warning' })
    await withdrawExpense(id.value)
    ElMessage.success('已撤回')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '撤回失败')
  }
}

async function onCancel() {
  try {
    await ElMessageBox.confirm('确认作废？', '作废', { type: 'warning' })
    await cancelExpense(id.value)
    ElMessage.success('已作废')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head" v-if="row">
      <div>
        <h2 class="oa-page__title">报销详情 {{ row.expenseNo }}</h2>
        <p class="muted"><el-tag type="info">{{ statusLabel(status) }}</el-tag></p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="router.push('/oa/expenses')">返回列表</el-button>
        <el-button v-if="status === 'DRAFT'" type="primary" @click="goEdit">编辑</el-button>
        <el-button v-if="status === 'DRAFT'" type="success" @click="onSubmit">提交审批</el-button>
        <el-button v-if="status === 'APPROVING'" @click="onWithdraw">撤回</el-button>
        <el-button v-if="status === 'DRAFT' || status === 'APPROVING'" type="danger" plain @click="onCancel">作废</el-button>
      </div>
    </div>

    <el-card v-if="row" shadow="never">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="类型">{{ row.expenseType }}</el-descriptions-item>
        <el-descriptions-item label="合计">{{ row.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="收款账户" :span="2">{{ row.payeeAccount ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="事由" :span="2">{{ row.reason ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="付款状态">{{ statusLabel(String(row.paymentStatus ?? '')) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDisplayDateTime(row.updatedAt) }}</el-descriptions-item>
      </el-descriptions>

      <h3 class="oa-subtitle">明细</h3>
      <el-table :data="items" size="small">
        <el-table-column prop="feeType" label="费用类型" />
        <el-table-column label="费用日期">
          <template #default="{ row: r }">{{ formatDisplayDate(r.feeDate) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120" />
        <el-table-column prop="description" label="说明" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.oa-subtitle {
  margin: 20px 0 12px;
  font-size: 15px;
  font-weight: 600;
}
</style>
