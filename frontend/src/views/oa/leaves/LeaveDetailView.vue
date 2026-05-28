<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cancelLeave, getLeave, submitLeave, withdrawLeave } from '../../../api/oa-leaves'
import { instanceDetail, instanceTimeline } from '../../../api/workflow'
import type { JsonObject } from '../../../api/types'
import { useOaActions } from '../../../composables/useOaActions'
import { formatDisplayDateTime, statusLabel } from '../oa-shared'
import OaApprovalProgress from '../../../components/OaApprovalProgress.vue'

const LEAVE_TYPE_LABELS: Record<string, string> = {
  ANNUAL: '年假',
  SICK: '病假',
  PERSONAL: '事假',
  MATERNITY: '产假',
  PATERNITY: '陪产假',
  MARRIAGE: '婚假',
  BEREAVEMENT: '丧假',
}

function leaveTypeLabel(code: unknown): string {
  const key = String(code ?? '')
  return LEAVE_TYPE_LABELS[key] ?? key
}

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const row = ref<JsonObject | null>(null)
const timeline = ref<JsonObject[]>([])
const timelineLoading = ref(false)
const wfInstance = ref<JsonObject | null>(null)

const id = computed(() => Number(route.params.id))
const status = computed(() => (row.value ? String(row.value.status ?? '') : ''))

onMounted(async () => {
  try {
    row.value = await getLeave(id.value)
    await loadTimeline()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/oa/leaves')
  } finally {
    loading.value = false
  }
})

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

async function reload() {
  try {
    row.value = await getLeave(id.value)
    await loadTimeline()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  }
}

const { onSubmit, onWithdraw, onCancel } = useOaActions(reload)

function goEdit() {
  router.push(`/oa/leaves/${id.value}/edit`)
}

function handleSubmit() { onSubmit(() => submitLeave(id.value)) }
function handleWithdraw() { onWithdraw(() => withdrawLeave(id.value)) }
function handleCancel() { onCancel(() => cancelLeave(id.value)) }

function actionIcon(action: string): string {
  switch (action) {
    case 'SUBMIT': return 'Promotion'
    case 'APPROVE': return 'Select'
    case 'REJECT': return 'CircleClose'
    case 'WITHDRAW': return 'Back'
    case 'CANCEL': return 'Delete'
    case 'TRANSFER': return 'Switch'
    case 'ADD_SIGN': return 'Plus'
    default: return 'Document'
  }
}

function actionColor(action: string): string {
  switch (action) {
    case 'APPROVE': return '#22c55e'
    case 'REJECT': return '#ef4444'
    case 'WITHDRAW':
    case 'CANCEL': return '#f59e0b'
    case 'SUBMIT': return '#3b82f6'
    default: return '#6b7280'
  }
}

const approvalSteps = computed(() => {
  const nodeNames: string[] = []
  timeline.value.forEach((item) => {
    const name = String(item.nodeName ?? '')
    if (name && !nodeNames.includes(name)) {
      nodeNames.push(name)
    }
  })
  if (nodeNames.length === 0) return []

  const currentNode = row.value ? String(row.value.currentNodeName ?? '') : ''
  const currentIdx = currentNode ? nodeNames.indexOf(currentNode) : -1
  const finalStatus = status.value

  return nodeNames.map((name, idx) => {
    let stepStatus: string
    if (finalStatus === 'REJECTED' && idx === currentIdx) {
      stepStatus = 'error'
    } else if (finalStatus === 'WITHDRAWN' || finalStatus === 'CANCELLED') {
      stepStatus = idx <= currentIdx ? 'finish' : 'wait'
    } else if (currentIdx === -1 && (finalStatus === 'APPROVED')) {
      stepStatus = 'finish'
    } else if (idx < currentIdx) {
      stepStatus = 'finish'
    } else if (idx === currentIdx) {
      stepStatus = finalStatus === 'APPROVED' ? 'finish' : 'process'
    } else {
      stepStatus = 'wait'
    }
    return { title: name, status: stepStatus }
  })
})

const activeStep = computed(() => {
  const idx = approvalSteps.value.findIndex((s) => s.status === 'process')
  return idx >= 0 ? idx : approvalSteps.value.length
})
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head" v-if="row">
      <div>
        <h2 class="oa-page__title">请假详情 #{{ row.id }}</h2>
        <p class="muted">
          <el-tag effect="light" :type="status === 'APPROVED' ? 'success' : status === 'REJECTED' ? 'danger' : status === 'APPROVING' ? 'warning' : 'info'">
            {{ statusLabel(status) }}
          </el-tag>
        </p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="router.push('/oa/leaves')">
          <el-icon><ArrowLeft /></el-icon>返回列表
        </el-button>
        <el-button @click="window.print()">
          <el-icon><Printer /></el-icon>打印
        </el-button>
        <el-button v-if="status === 'DRAFT'" type="primary" @click="goEdit">编辑</el-button>
        <el-button v-if="status === 'DRAFT'" type="success" @click="handleSubmit">提交审批</el-button>
        <el-button v-if="status === 'APPROVING'" @click="handleWithdraw">撤回</el-button>
        <el-button v-if="status === 'DRAFT' || status === 'APPROVING'" type="danger" plain @click="handleCancel">作废</el-button>
      </div>
    </div>

    <el-card v-if="row" shadow="never">
      <h3 class="oa-section-title">基本信息</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="请假类型">{{ leaveTypeLabel(row.leaveType) }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag effect="light" :type="status === 'APPROVED' ? 'success' : status === 'REJECTED' ? 'danger' : status === 'APPROVING' ? 'warning' : 'info'" size="small">
            {{ statusLabel(status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatDisplayDateTime(row.startAt) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ formatDisplayDateTime(row.endAt) }}</el-descriptions-item>
        <el-descriptions-item label="时长(小时)">{{ row.durationHours ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="时长(天)">{{ row.durationDays ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="事由" :span="2">{{ row.reason ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="交接说明" :span="2">{{ row.handoverNote ?? '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-divider v-if="row" />

    <el-card v-if="row" shadow="never" class="oa-timeline-card" v-loading="timelineLoading">
      <template #header>
        <div class="card-header">
          <span>审批记录</span>
          <el-button text type="primary" size="small" @click="loadTimeline" :loading="timelineLoading">刷新</el-button>
        </div>
      </template>

      <el-steps
        v-if="approvalSteps.length > 0"
        :active="activeStep"
        finish-status="success"
        simple
        style="margin-bottom: 24px"
      >
        <el-step
          v-for="step in approvalSteps"
          :key="step.title"
          :title="step.title"
          :status="step.status"
        />
      </el-steps>

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
