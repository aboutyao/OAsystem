<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cancelExpense, getExpense, submitExpense, withdrawExpense } from '../../../api/oa-expenses'
import { instanceDetail, instanceTimeline } from '../../../api/workflow'
import type { JsonObject } from '../../../api/types'
import { useOaActions } from '../../../composables/useOaActions'
import { formatDisplayDate, formatDisplayDateTime, statusLabel } from '../oa-shared'
import OaApprovalProgress from '../../../components/OaApprovalProgress.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const row = ref<JsonObject | null>(null)
const items = ref<JsonObject[]>([])
const timeline = ref<JsonObject[]>([])
const timelineLoading = ref(false)
const wfInstance = ref<JsonObject | null>(null)

const id = computed(() => Number(route.params.id))
const status = computed(() => (row.value ? String(row.value.status ?? '') : ''))

onMounted(async () => {
  try {
    const data = await getExpense(id.value)
    const { items: it, ...rest } = data as JsonObject & { items?: JsonObject[] }
    row.value = rest
    items.value = Array.isArray(it) ? it : []
    await loadTimeline()
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
    await loadTimeline()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  }
}

async function loadTimeline() {
  const wfId = row.value?.wfInstanceId
  if (!wfId) return
  timelineLoading.value = true
  try {
    const [timelineData, instanceData] = await Promise.all([
      instanceTimeline(Number(wfId)),
      instanceDetail(Number(wfId)),
    ])
    timeline.value = timelineData
    wfInstance.value = instanceData
  } catch {
    timeline.value = []
    wfInstance.value = null
  } finally {
    timelineLoading.value = false
  }
}

const { onSubmit, onWithdraw, onCancel } = useOaActions(reload)

function goEdit() {
  router.push(`/oa/expenses/${id.value}/edit`)
}

function handleSubmit() { onSubmit(() => submitExpense(id.value)) }
function handleWithdraw() { onWithdraw(() => withdrawExpense(id.value)) }
function handleCancel() { onCancel(() => cancelExpense(id.value)) }
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
        <el-button v-if="status === 'DRAFT'" type="success" @click="handleSubmit">提交审批</el-button>
        <el-button v-if="status === 'APPROVING'" @click="handleWithdraw">撤回</el-button>
        <el-button v-if="status === 'DRAFT' || status === 'APPROVING'" type="danger" plain @click="handleCancel">作废</el-button>
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

      <h3 class="oa-section-title">明细</h3>
      <el-table :data="items" size="small">
        <el-table-column prop="feeType" label="费用类型" />
        <el-table-column label="费用日期">
          <template #default="{ row: r }">{{ formatDisplayDate(r.feeDate) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120" />
        <el-table-column prop="description" label="说明" />
      </el-table>
    </el-card>

    <el-divider v-if="row" />

    <el-card v-if="row" shadow="never" v-loading="timelineLoading">
      <template #header>
        <div class="card-header">
          <span>审批记录</span>
          <el-button text type="primary" size="small" @click="loadTimeline" :loading="timelineLoading">刷新</el-button>
        </div>
      </template>
      <OaApprovalProgress
        :timeline="timeline.map((item: JsonObject) => ({
          action: String(item.action ?? ''),
          operatorName: String(item.operatorName ?? '系统'),
          nodeName: item.nodeName ? String(item.nodeName) : undefined,
          operatedAt: formatDisplayDateTime(item.operatedAt),
          comment: item.comment ? String(item.comment) : undefined,
        }))"
        :current-node-name="wfInstance?.currentNodeName ? String(wfInstance.currentNodeName) : undefined"
        :sla-deadline="wfInstance?.slaDeadline ? String(wfInstance.slaDeadline) : undefined"
        :sla-breached="wfInstance?.slaBreached as boolean | number | undefined"
      />
    </el-card>
  </div>
</template>

