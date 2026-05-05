<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adjustSupply,
  createSupply,
  listSupplies,
  returnSupply,
  stockInSupply,
  stockOutSupply,
  updateSupply,
} from '../../api/supplies'
import type { JsonObject } from '../../api/types'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const filters = reactive({
  status: '',
  category: '',
  keyword: '',
})

const canManage = computed(() => {
  const p = authStore.user?.permissions ?? []
  return p.includes('*') || p.includes('org:create')
})

const STATUS_LABEL: Record<string, string> = {
  ENABLED: '启用',
  DISABLED: '停用',
}

function statusLabel(s: unknown) {
  const code = String(s ?? '')
  return STATUS_LABEL[code] ?? code
}

function isLowStock(row: JsonObject) {
  const stock = Number(row.stockQuantity ?? 0)
  const warn = Number(row.warningQuantity ?? 0)
  return warn > 0 && stock <= warn
}

function rowClassName({ row }: { row: JsonObject }) {
  return isLowStock(row) ? 'supply-row--low' : ''
}

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  supplyCode: '',
  supplyName: '',
  category: '',
  unit: '',
  warningQuantity: undefined as number | undefined,
  status: 'ENABLED',
  remark: '',
})

async function load() {
  loading.value = true
  try {
    const res = await listSupplies({
      page: page.value,
      size: size.value,
      status: filters.status || undefined,
      category: filters.category || undefined,
      keyword: filters.keyword || undefined,
    })
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void load()

function handleSizeChange() {
  page.value = 1
  load()
}

function openCreate() {
  editingId.value = null
  form.supplyCode = ''
  form.supplyName = ''
  form.category = ''
  form.unit = ''
  form.warningQuantity = undefined
  form.status = 'ENABLED'
  form.remark = ''
  dialogVisible.value = true
}

function openEdit(row: JsonObject) {
  editingId.value = Number(row.id)
  form.supplyCode = String(row.supplyCode ?? '')
  form.supplyName = String(row.supplyName ?? '')
  form.category = String(row.category ?? '')
  form.unit = String(row.unit ?? '')
  form.warningQuantity = row.warningQuantity != null ? Number(row.warningQuantity) : undefined
  form.status = String(row.status ?? 'ENABLED')
  form.remark = String(row.remark ?? '')
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.supplyName.trim() || !form.unit.trim()) {
    ElMessage.warning('请填写名称与单位')
    return
  }
  try {
    if (editingId.value != null) {
      await updateSupply(editingId.value, {
        supplyName: form.supplyName.trim(),
        category: form.category || null,
        unit: form.unit.trim(),
        warningQuantity: form.warningQuantity ?? null,
        status: form.status,
        remark: form.remark || null,
      })
      ElMessage.success('已保存')
    } else {
      if (!form.supplyCode.trim()) {
        ElMessage.warning('请填写编号')
        return
      }
      await createSupply({
        supplyCode: form.supplyCode.trim(),
        supplyName: form.supplyName.trim(),
        category: form.category || null,
        unit: form.unit.trim(),
        warningQuantity: form.warningQuantity ?? null,
        remark: form.remark || null,
      })
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function promptQuantity(title: string, hint: string) {
  try {
    const { value } = await ElMessageBox.prompt(`${hint}（请输入「数量,原因」，原因可省略）`, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^\d+(\.\d+)?(,.*)?$/,
      inputErrorMessage: '请输入正数；如需备注，用半角逗号分隔',
    })
    const [qStr, ...rest] = String(value ?? '').split(',')
    const quantity = Number(qStr)
    if (!Number.isFinite(quantity) || quantity <= 0) {
      return null
    }
    return { quantity, reason: rest.join(',').trim() || null }
  } catch {
    return null
  }
}

async function onStockIn(row: JsonObject) {
  const input = await promptQuantity('入库', `${row.supplyName} 入库`)
  if (!input) return
  try {
    await stockInSupply(Number(row.id), { quantity: input.quantity, reason: input.reason })
    ElMessage.success('已入库')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '入库失败')
  }
}

async function onStockOut(row: JsonObject) {
  const input = await promptQuantity('出库', `${row.supplyName} 出库（领用）`)
  if (!input) return
  try {
    await stockOutSupply(Number(row.id), { quantity: input.quantity, reason: input.reason })
    ElMessage.success('已出库')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '出库失败')
  }
}

async function onReturn(row: JsonObject) {
  const input = await promptQuantity('退回', `${row.supplyName} 退回库存`)
  if (!input) return
  try {
    await returnSupply(Number(row.id), { quantity: input.quantity, reason: input.reason })
    ElMessage.success('已退回')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '退回失败')
  }
}

async function onAdjust(row: JsonObject) {
  try {
    const { value } = await ElMessageBox.prompt(`目标库存（请输入「数量,原因」）`, `${row.supplyName} 库存调整`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^\d+(\.\d+)?(,.*)?$/,
      inputErrorMessage: '请输入 >= 0 的数；如需备注，用半角逗号分隔',
      inputValue: String(row.stockQuantity ?? 0),
    })
    const [qStr, ...rest] = String(value ?? '').split(',')
    const quantity = Number(qStr)
    if (!Number.isFinite(quantity) || quantity < 0) return
    await adjustSupply(Number(row.id), { quantity, reason: rest.join(',').trim() || null })
    ElMessage.success('已调整')
    await load()
  } catch (e) {
    if (e instanceof Error && e.message) ElMessage.error(e.message)
  }
}

function goRecords(row: JsonObject) {
  router.push({ path: '/supplies/records', query: { supplyId: String(row.id) } })
}

watch([filters], () => {
  page.value = 1
  void load()
}, { deep: true })
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">办公用品</h2>
        <p class="muted">
          库存与出入库管理；库存低于预警线行以橙色高亮。
          <RouterLink to="/supplies/records">出入库记录</RouterLink>
        </p>
      </div>
      <div class="oa-page__actions">
        <el-button v-if="canManage" type="primary" @click="openCreate">新增用品</el-button>
      </div>
    </div>

    <el-card shadow="never" class="oa-page__filters">
      <el-form inline @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="filters.category" clearable placeholder="如 文具" style="width: 160px" />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="filters.keyword" clearable placeholder="编号或名称" style="width: 200px" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="rows"
        stripe
        :row-class-name="rowClassName"
      >
        <el-table-column prop="supplyCode" label="编号" width="140" />
        <el-table-column prop="supplyName" label="名称" min-width="160" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column label="库存" width="120">
          <template #default="{ row }">
            <span :class="{ 'supply-low': isLowStock(row) }">{{ Number(row.stockQuantity ?? 0) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="warningQuantity" label="预警" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="String(row.status) === 'ENABLED' ? 'success' : 'info'">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320">
          <template #default="{ row }">
            <el-button link type="primary" @click="goRecords(row)">记录</el-button>
            <template v-if="canManage">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="success" @click="onStockIn(row)">入库</el-button>
              <el-button link type="warning" @click="onStockOut(row)">出库</el-button>
              <el-button link @click="onReturn(row)">退回</el-button>
              <el-button link type="danger" @click="onAdjust(row)">调整</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
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

    <el-dialog v-model="dialogVisible" :title="editingId != null ? '编辑用品' : '新增用品'" width="520px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="编号" required>
          <el-input v-model="form.supplyCode" maxlength="64" :disabled="editingId != null" placeholder="如 BG001" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.supplyName" maxlength="255" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" maxlength="64" />
        </el-form-item>
        <el-form-item label="单位" required>
          <el-input v-model="form.unit" maxlength="32" placeholder="如 支 / 盒 / 包" />
        </el-form-item>
        <el-form-item label="预警库存">
          <el-input-number v-model="form.warningQuantity" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="editingId != null" label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style>
.supply-row--low {
  background-color: #fff7e6 !important;
}
.supply-low {
  color: #d48806;
  font-weight: 600;
}
</style>
