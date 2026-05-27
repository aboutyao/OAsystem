<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createExpense, getExpense, updateExpense } from '../../../api/oa-expenses'
import type { JsonObject } from '../../../api/types'
import { toInputDate } from '../oa-shared'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const isDirty = ref(false)

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

const isEdit = computed(() => route.name === 'expense-edit')
const id = computed(() => (isEdit.value ? Number(route.params.id) : 0))

const form = reactive({
  expenseType: '差旅费',
  totalAmount: 0,
  payeeAccount: '',
  reason: '',
})

type ItemRow = {
  feeType: string
  feeDate: string
  amount: number
  description: string
  sortOrder: number
}

const items = ref<ItemRow[]>([{ feeType: '交通', feeDate: '', amount: 0, description: '', sortOrder: 0 }])

const sumAmount = computed(() => items.value.reduce((s, it) => s + Number(it.amount || 0), 0))

watch(
  sumAmount,
  (v) => {
    form.totalAmount = Math.round(v * 100) / 100
  },
  { immediate: true },
)

watch(
  items,
  () => {
    isDirty.value = true
  },
  { deep: true },
)

function addItem() {
  items.value.push({
    feeType: '',
    feeDate: '',
    amount: 0,
    description: '',
    sortOrder: items.value.length,
  })
}

function removeItem(i: number) {
  if (items.value.length <= 1) return
  items.value.splice(i, 1)
  items.value.forEach((r, idx) => (r.sortOrder = idx))
}

onMounted(async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    const data = await getExpense(id.value)
    if (String(data.status) !== 'DRAFT') {
      ElMessage.warning('仅草稿可编辑')
      router.replace(`/oa/expenses/${id.value}`)
      return
    }
    form.expenseType = String(data.expenseType ?? '')
    form.totalAmount = Number(data.totalAmount ?? 0)
    form.payeeAccount = String(data.payeeAccount ?? '')
    form.reason = String(data.reason ?? '')
    const raw = (data as { items?: JsonObject[] }).items
    if (Array.isArray(raw) && raw.length) {
      items.value = raw.map((r, idx) => ({
        feeType: String(r.feeType ?? ''),
        feeDate: toInputDate(r.feeDate),
        amount: Number(r.amount ?? 0),
        description: String(r.description ?? ''),
        sortOrder: Number(r.sortOrder ?? idx),
      }))
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/oa/expenses')
  } finally {
    loading.value = false
  }
})

async function onSave() {
  if (!items.value.length) {
    ElMessage.warning('请至少一行明细')
    return
  }
  for (const it of items.value) {
    if (!it.feeType || !it.feeDate) {
      ElMessage.warning('请填写每行的费用类型与日期')
      return
    }
  }
  const body = {
    expenseType: form.expenseType,
    totalAmount: form.totalAmount,
    payeeAccount: form.payeeAccount || null,
    reason: form.reason || null,
    items: items.value.map((it, idx) => ({
      feeType: it.feeType,
      feeDate: it.feeDate,
      amount: it.amount,
      description: it.description || null,
      sortOrder: idx,
    })),
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateExpense(id.value, body)
      ElMessage.success('已保存')
      router.push(`/oa/expenses/${id.value}`)
    } else {
      const created = await createExpense(body)
      ElMessage.success('已创建')
      router.push(`/oa/expenses/${Number(created.id)}`)
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
        <h2 class="oa-page__title">{{ isEdit ? '编辑报销' : '新建报销' }}</h2>
        <p class="muted">合计金额与明细合计需一致（已自动按明细汇总）。</p>
      </div>
      <el-button @click="router.push(isEdit ? `/oa/expenses/${id}` : '/oa/expenses')">取消</el-button>
    </div>

    <el-card shadow="never">
      <el-form label-width="100px" style="max-width: 800px">
        <el-form-item label="报销类型" required>
          <el-select v-model="form.expenseType" style="width: 100%" @change="isDirty = true">
            <el-option label="差旅费" value="差旅费" />
            <el-option label="招待费" value="招待费" />
            <el-option label="办公费" value="办公费" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="合计金额">
          <el-input v-model.number="form.totalAmount" disabled />
        </el-form-item>
        <el-form-item label="收款账户">
          <el-input v-model="form.payeeAccount" @input="isDirty = true" />
        </el-form-item>
        <el-form-item label="事由">
          <el-input v-model="form.reason" type="textarea" :rows="2" @input="isDirty = true" />
        </el-form-item>

        <el-form-item label="费用明细">
          <el-table :data="items" border size="small">
            <el-table-column label="类型" width="140">
              <template #default="{ row }">
                <el-input v-model="row.feeType" />
              </template>
            </el-table-column>
            <el-table-column label="日期" width="160">
              <template #default="{ row }">
                <el-date-picker v-model="row.feeDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled-date="(time: Date) => time.getTime() > Date.now() + 86400000" />
              </template>
            </el-table-column>
            <el-table-column label="金额" width="120">
              <template #default="{ row }">
                <el-input-number v-model="row.amount" :min="0" :step="0.01" :controls="false" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="说明">
              <template #default="{ row }">
                <el-input v-model="row.description" />
              </template>
            </el-table-column>
            <el-table-column label="" width="100">
              <template #default="{ $index }">
                <el-button link type="danger" @click="removeItem($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button class="oa-mt" @click="addItem">添加一行</el-button>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

