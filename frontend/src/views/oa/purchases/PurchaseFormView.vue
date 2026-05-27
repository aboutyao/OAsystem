<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPurchase, getPurchase, updatePurchase } from '../../../api/oa-purchases'
import type { JsonObject } from '../../../api/types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)

const isEdit = computed(() => route.name === 'purchase-edit')
const id = computed(() => (isEdit.value ? Number(route.params.id) : 0))

const form = reactive({
  purchaseType: '固定资产',
  supplierName: '',
  budgetSubject: '',
  reason: '',
})

type ItemRow = {
  itemName: string
  specification: string
  quantity: number
  unit: string
  unitPrice: number
  amount: number
  sortOrder: number
}

const items = ref<ItemRow[]>([
  { itemName: '', specification: '', quantity: 1, unit: '件', unitPrice: 0, amount: 0, sortOrder: 0 },
])

const sumAmount = computed(() => items.value.reduce((s, it) => s + Number(it.amount || 0), 0))

watch(
  items,
  () => {
    for (const it of items.value) {
      const q = Number(it.quantity) || 0
      const p = Number(it.unitPrice) || 0
      it.amount = Math.round(q * p * 100) / 100
    }
  },
  { deep: true },
)

function addItem() {
  items.value.push({
    itemName: '',
    specification: '',
    quantity: 1,
    unit: '件',
    unitPrice: 0,
    amount: 0,
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
    const data = await getPurchase(id.value)
    if (String(data.status) !== 'DRAFT') {
      ElMessage.warning('仅草稿可编辑')
      router.replace(`/oa/purchases/${id.value}`)
      return
    }
    form.purchaseType = String(data.purchaseType ?? '')
    form.supplierName = String(data.supplierName ?? '')
    form.budgetSubject = String(data.budgetSubject ?? '')
    form.reason = String(data.reason ?? '')
    const raw = (data as { items?: JsonObject[] }).items
    if (Array.isArray(raw) && raw.length) {
      items.value = raw.map((r, idx) => ({
        itemName: String(r.itemName ?? ''),
        specification: String(r.specification ?? ''),
        quantity: Number(r.quantity ?? 0),
        unit: String(r.unit ?? ''),
        unitPrice: Number(r.unitPrice ?? 0),
        amount: Number(r.amount ?? 0),
        sortOrder: Number(r.sortOrder ?? idx),
      }))
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/oa/purchases')
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
    if (!it.itemName) {
      ElMessage.warning('请填写每行品名')
      return
    }
  }
  const totalAmount = sumAmount.value
  const body = {
    purchaseType: form.purchaseType,
    totalAmount,
    supplierName: form.supplierName || null,
    budgetSubject: form.budgetSubject || null,
    reason: form.reason || null,
    items: items.value.map((it, idx) => ({
      itemName: it.itemName,
      specification: it.specification || null,
      quantity: it.quantity,
      unit: it.unit || null,
      unitPrice: it.unitPrice,
      amount: it.amount,
      sortOrder: idx,
    })),
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updatePurchase(id.value, body)
      ElMessage.success('已保存')
      router.push(`/oa/purchases/${id.value}`)
    } else {
      const created = await createPurchase(body)
      ElMessage.success('已创建')
      router.push(`/oa/purchases/${Number(created.id)}`)
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
        <h2 class="oa-page__title">{{ isEdit ? '编辑采购' : '新建采购' }}</h2>
        <p class="muted">合计须与明细金额之和一致（由明细自动汇总）。</p>
      </div>
      <el-button @click="router.push(isEdit ? `/oa/purchases/${id}` : '/oa/purchases')">取消</el-button>
    </div>

    <el-card shadow="never">
      <el-form label-width="100px" style="max-width: 900px">
        <el-form-item label="采购类型" required>
          <el-select v-model="form.purchaseType" style="width: 100%">
            <el-option label="固定资产" value="固定资产" />
            <el-option label="低值易耗" value="低值易耗" />
            <el-option label="服务采购" value="服务采购" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="合计金额">
          <el-input :model-value="String(sumAmount)" disabled />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="form.supplierName" />
        </el-form-item>
        <el-form-item label="预算科目">
          <el-input v-model="form.budgetSubject" />
        </el-form-item>
        <el-form-item label="事由">
          <el-input v-model="form.reason" type="textarea" :rows="2" />
        </el-form-item>

        <el-form-item label="采购明细">
          <el-table :data="items" border size="small">
            <el-table-column label="品名" min-width="120">
              <template #default="{ row }">
                <el-input v-model="row.itemName" />
              </template>
            </el-table-column>
            <el-table-column label="规格" width="120">
              <template #default="{ row }">
                <el-input v-model="row.specification" />
              </template>
            </el-table-column>
            <el-table-column label="数量" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="0" :step="0.01" :controls="false" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="单位" width="88">
              <template #default="{ row }">
                <el-input v-model="row.unit" />
              </template>
            </el-table-column>
            <el-table-column label="单价" width="120">
              <template #default="{ row }">
                <el-input-number v-model="row.unitPrice" :min="0" :step="0.01" :controls="false" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="金额" width="100">
              <template #default="{ row }">
                <span>{{ row.amount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="" width="88">
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

