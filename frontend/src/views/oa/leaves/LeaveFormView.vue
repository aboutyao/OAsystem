<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { calculateLeaveDuration, createLeave, getLeave, updateLeave } from '../../../api/oa-leaves'
import { computeLeaveSpan } from '../oa-shared'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)

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
  if (!form.startAt || !form.endAt) {
    ElMessage.warning('请填写开始与结束时间')
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
      <el-form label-width="100px" style="max-width: 640px">
        <el-form-item label="请假类型" required>
          <el-select v-model="form.leaveType" style="width: 100%">
            <el-option label="年假" value="年假" />
            <el-option label="病假" value="病假" />
            <el-option label="事假" value="事假" />
            <el-option label="调休" value="调休" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker v-model="form.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker v-model="form.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时长(小时)">
          <el-input v-model.number="form.durationHours" disabled />
        </el-form-item>
        <el-form-item label="时长(天)">
          <el-input v-model.number="form.durationDays" disabled />
        </el-form-item>
        <el-form-item label="事由">
          <el-input v-model="form.reason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="交接说明">
          <el-input v-model="form.handoverNote" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
