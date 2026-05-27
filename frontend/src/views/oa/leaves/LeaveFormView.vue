<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { calculateLeaveDuration, createLeave, getLeave, updateLeave } from '../../../api/oa-leaves'
import { computeLeaveSpan } from '../oa-shared'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const isDirty = ref(false)

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

const form = reactive({
  leaveType: '事假',
  startAt: '',
  endAt: '',
  durationHours: 0,
  durationDays: 0,
  reason: '',
  handoverNote: '',
})

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
        <p class="muted">提交前可先保存为草稿。</p>
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
