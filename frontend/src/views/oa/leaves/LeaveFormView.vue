<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { calculateLeaveDuration, createLeave, getLeave, updateLeave } from '../../../api/oa-leaves'
import { getMyLeaveBalance, type LeaveBalanceItem } from '../../../api/dashboard'
import { computeLeaveSpan } from '../oa-shared'
import { useAutoSave } from '../../../composables/useAutoSave'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const isDirty = ref(false)
const balances = ref<LeaveBalanceItem[]>([])

const LEAVE_TYPE_MAP: Record<string, string> = {
  '年假': 'ANNUAL',
  '病假': 'SICK',
  '事假': 'PERSONAL',
  '调休': 'PERSONAL',
}

const form = reactive({
  leaveType: '事假',
  startAt: '',
  endAt: '',
  durationHours: 0,
  durationDays: 0,
  reason: '',
  handoverNote: '',
})

const currentBalance = computed(() => {
  const code = LEAVE_TYPE_MAP[form.leaveType] || form.leaveType
  return balances.value.find(b => b.leaveType === code)
})

// Auto-save draft
const { lastSaved, clear: clearDraft } = useAutoSave(form, 'leave-draft', { interval: 30000 })

async function loadBalances() {
  try {
    balances.value = await getMyLeaveBalance()
  } catch {
    balances.value = []
  }
}

onMounted(loadBalances)

const formRules = {
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startAt: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endAt: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  reason: [{ required: true, message: '请填写请假原因', trigger: 'blur' }],
}

function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (isDirty.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

onBeforeRouteLeave((to, from, next) => {
  if (!isDirty.value) return next()
  ElMessageBox.confirm('有未保存的修改，确定离开吗？', '提示', {
    confirmButtonText: '确定离开',
    cancelButtonText: '留下',
    type: 'warning',
  }).then(() => next()).catch(() => next(false))
})

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

const isEdit = computed(() => route.name === 'leave-edit')
const id = computed(() => (isEdit.value ? Number(route.params.id) : 0))

async function syncDuration() {
  if (!form.startAt || !form.endAt) return
  try {
    const res = await calculateLeaveDuration(form.startAt, form.endAt)
    form.durationHours = Number(res.durationHours ?? 0)
    form.durationDays = Number(res.durationDays ?? 0)
  } catch {
    const c = computeLeaveSpan(form.startAt, form.endAt)
    form.durationHours = c.durationHours
    form.durationDays = c.durationDays
  }
}

const durationDisplay = computed(() => {
  if (!form.startAt || !form.endAt) return form.durationDays || ''
  const a = Date.parse(form.startAt)
  const b = Date.parse(form.endAt)
  if (Number.isNaN(a) || Number.isNaN(b) || b <= a) return form.durationDays || ''
  const calendarMs = b - a
  const calendarDays = Math.round((calendarMs / 86400000) * 100) / 100
  const workingDays = form.durationDays
  if (calendarDays === workingDays) return `${workingDays} 天`
  return `${workingDays} 天（${calendarDays} 个自然日）`
})

watch(
  () => [form.startAt, form.endAt],
  () => {
    void syncDuration()
  },
)

onMounted(async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    const row = await getLeave(id.value)
    if (String(row.status) !== 'DRAFT') {
      ElMessage.warning('仅草稿可编辑')
      router.replace(`/oa/leaves/${id.value}`)
      return
    }
    form.leaveType = String(row.leaveType ?? '')
    form.startAt = normalizeDt(row.startAt)
    form.endAt = normalizeDt(row.endAt)
    form.reason = String(row.reason ?? '')
    form.handoverNote = String(row.handoverNote ?? '')
    await syncDuration()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/oa/leaves')
  } finally {
    loading.value = false
  }
})

function normalizeDt(v: unknown): string {
  if (v == null) return ''
  if (typeof v === 'string') return v.includes('T') ? v.slice(0, 19) : v
  if (typeof v === 'number') {
    const d = new Date(v)
    const p = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
  }
  if (Array.isArray(v) && v.length >= 3) {
    const y = Number(v[0])
    const m = Number(v[1])
    const day = Number(v[2])
    const h = v.length > 3 ? Number(v[3]) : 0
    const min = v.length > 4 ? Number(v[4]) : 0
    const s = v.length > 5 ? Number(v[5]) : 0
    const p = (n: number) => String(n).padStart(2, '0')
    return `${y}-${p(m)}-${p(day)}T${p(h)}:${p(min)}:${p(s)}`
  }
  return ''
}

async function onSave() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  await syncDuration()
  const body = {
    leaveType: form.leaveType,
    startAt: form.startAt,
    endAt: form.endAt,
    durationHours: form.durationHours,
    durationDays: form.durationDays,
    reason: form.reason || null,
    handoverNote: form.handoverNote || null,
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateLeave(id.value, body)
      ElMessage.success('已保存')
      router.push(`/oa/leaves/${id.value}`)
    } else {
      const created = await createLeave(body)
      ElMessage.success('已创建')
      clearDraft()
      router.push(`/oa/leaves/${Number(created.id)}`)
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">{{ isEdit ? '编辑请假' : '新建请假' }}</h2>
        <p class="muted">提交前可先保存为草稿。<span v-if="lastSaved" class="auto-save-hint">自动保存于 {{ lastSaved }}</span></p>
      </div>
      <el-button @click="router.push(isEdit ? `/oa/leaves/${id}` : '/oa/leaves')">取消</el-button>
    </div>

    <el-card shadow="never">
      <el-form ref="formRef" :rules="formRules" label-width="100px" style="max-width: 640px">
        <el-form-item label="请假类型" prop="leaveType">
          <el-select v-model="form.leaveType" style="width: 100%" @change="isDirty = true">
            <el-option label="年假" value="年假" />
            <el-option label="病假" value="病假" />
            <el-option label="事假" value="事假" />
            <el-option label="调休" value="调休" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="currentBalance" label="剩余假期">
          <div class="balance-display">
            <span class="balance-number" :class="{ 'balance-low': currentBalance.remainingDays <= 1 }">
              {{ currentBalance.remainingDays }}
            </span>
            <span class="balance-unit">天剩余</span>
            <el-progress
              :percentage="currentBalance.totalDays > 0 ? Math.round((currentBalance.usedDays / currentBalance.totalDays) * 100) : 0"
              :stroke-width="4"
              :color="currentBalance.remainingDays <= 1 ? '#F56C6C' : '#67C23A'"
              style="flex: 1; margin-left: 12px"
            />
            <span class="balance-detail">已用 {{ currentBalance.usedDays }} / {{ currentBalance.totalDays }} 天</span>
          </div>
        </el-form-item>
        <el-form-item label="开始时间" prop="startAt">
          <el-date-picker
            v-model="form.startAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
            :disabled-date="(time: Date) => time.getTime() < Date.now() - 86400000"
            @change="isDirty = true"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endAt">
          <el-date-picker
            v-model="form.endAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
            :disabled-date="(time: Date) => form.startAt && time.getTime() < new Date(form.startAt).getTime() - 86400000"
            @change="isDirty = true"
          />
        </el-form-item>
        <el-form-item label="时长(小时)">
          <el-input v-model.number="form.durationHours" disabled />
        </el-form-item>
        <el-form-item label="时长(天)">
          <el-input :model-value="durationDisplay" disabled />
        </el-form-item>
        <el-form-item label="事由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" @input="isDirty = true" />
        </el-form-item>
        <el-form-item label="交接说明">
          <el-input v-model="form.handoverNote" type="textarea" :rows="2" @input="isDirty = true" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.balance-display {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.balance-number {
  font-size: 24px;
  font-weight: 700;
  color: var(--oa-primary, #409eff);
  line-height: 1;
}

.balance-low {
  color: var(--oa-danger, #f56c6c);
}

.balance-unit {
  font-size: 12px;
  color: var(--oa-text-muted, #909399);
}

.balance-detail {
  font-size: 12px;
  color: var(--oa-text-muted, #909399);
  white-space: nowrap;
}

.auto-save-hint {
  font-size: 12px;
  color: var(--oa-text-muted, #909399);
  margin-left: 8px;
}
</style>
