<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  acceptPurchase,
  cancelPurchase,
  confirmPurchaseArrival,
  getPurchase,
  submitPurchase,
  withdrawPurchase,
} from '../../../api/oa-purchases'
import type { JsonObject } from '../../../api/types'
import { formatDisplayDateTime, statusLabel } from '../oa-shared'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const row = ref<JsonObject | null>(null)
const items = ref<JsonObject[]>([])

const id = computed(() => Number(route.params.id))
const status = computed(() => (row.value ? String(row.value.status ?? '') : ''))
const arrival = computed(() => (row.value ? String(row.value.arrivalStatus ?? '') : ''))
const acceptance = computed(() => (row.value ? String(row.value.acceptanceStatus ?? '') : ''))

onMounted(async () => {
  try {
    await reload()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/oa/purchases')
  } finally {
    loading.value = false
  }
})

async function reload() {
  try {
    const data = await getPurchase(id.value)
    const { items: it, ...rest } = data as JsonObject & { items?: JsonObject[] }
    row.value = rest
    items.value = Array.isArray(it) ? it : []
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  }
}

function goEdit() {
  router.push(`/oa/purchases/${id.value}/edit`)
}

async function onSubmit() {
  try {
    await ElMessageBox.confirm('确认提交采购申请？', '提交', { type: 'warning' })
    await submitPurchase(id.value)
    ElMessage.success('已提交')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '提交失败')
  }
}

async function onWithdraw() {
  try {
    await ElMessageBox.confirm('确认撤回？', '撤回', { type: 'warning' })
    await withdrawPurchase(id.value)
    ElMessage.success('已撤回')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '撤回失败')
  }
}

async function onCancel() {
  try {
    await ElMessageBox.confirm('确认作废？', '作废', { type: 'warning' })
    await cancelPurchase(id.value)
    ElMessage.success('已作废')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onArrival() {
  try {
    await ElMessageBox.confirm('确认已到货？', '到货确认', { type: 'warning' })
    await confirmPurchaseArrival(id.value)
    ElMessage.success('已确认到货')
    await reload()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onAccept() {
  try {
    await ElMessageBox.confirm('确认验收通过？', '验收', { type: 'warning' })
    await acceptPurchase(id.value)
    ElMessage.success('验收完成')
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
        <h2 class="oa-page__title">采购详情 {{ row.purchaseNo }}</h2>
        <p class="muted"><el-tag type="info">{{ statusLabel(status) }}</el-tag></p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="router.push('/oa/purchases')">返回列表</el-button>
        <el-button v-if="status === 'DRAFT'" type="primary" @click="goEdit">编辑</el-button>
        <el-button v-if="status === 'DRAFT'" type="success" @click="onSubmit">提交审批</el-button>
        <el-button v-if="status === 'APPROVING'" @click="onWithdraw">撤回</el-button>
        <el-button v-if="status === 'DRAFT' || status === 'APPROVING'" type="danger" plain @click="onCancel">作废</el-button>
        <el-button v-if="status === 'APPROVED' && arrival !== 'ARRIVED'" type="warning" @click="onArrival">确认到货</el-button>
        <el-button
          v-if="status === 'APPROVED' && arrival === 'ARRIVED' && acceptance !== 'PASSED'"
          type="success"
          @click="onAccept"
        >
          验收通过
        </el-button>
      </div>
    </div>

    <el-card v-if="row" shadow="never">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="采购类型">{{ row.purchaseType }}</el-descriptions-item>
        <el-descriptions-item label="合计">{{ row.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ row.supplierName ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="预算科目">{{ row.budgetSubject ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="事由" :span="2">{{ row.reason ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="到货">{{ arrival }}</el-descriptions-item>
        <el-descriptions-item label="验收">{{ acceptance }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDisplayDateTime(row.updatedAt) }}</el-descriptions-item>
      </el-descriptions>

      <h3 class="oa-subtitle">明细</h3>
      <el-table :data="items" size="small">
        <el-table-column prop="itemName" label="品名" />
        <el-table-column prop="specification" label="规格" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="unitPrice" label="单价" width="100" />
        <el-table-column prop="amount" label="金额" width="100" />
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
