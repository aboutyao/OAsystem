<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAsset,
  listAssets,
  receiveAsset,
  repairAsset,
  returnAsset,
  scrapAsset,
} from '../../api/assets'
import { listUsers, getDeptTree } from '../../api/org'
import type { JsonObject } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { formatDisplayDate } from '../oa/oa-shared'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const filters = reactive({
  status: '',
  assetCategory: '',
  keyword: '',
})

const canManage = computed(() => {
  const p = authStore.user?.permissions ?? []
  return p.includes('*') || p.includes('org:create')
})

const STATUS_LABEL: Record<string, string> = {
  IDLE: '闲置',
  IN_USE: '在用',
  REPAIRING: '维修中',
  SCRAPPED: '已报废',
}

const STATUS_TAG: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
  IDLE: 'info',
  IN_USE: 'success',
  REPAIRING: 'warning',
  SCRAPPED: 'danger',
}

function statusLabel(s: unknown) {
  const code = String(s ?? '')
  return STATUS_LABEL[code] ?? code
}

function statusTag(s: unknown) {
  const code = String(s ?? '')
  return STATUS_TAG[code] ?? 'info'
}

const dialogVisible = ref(false)
const form = reactive({
  assetNo: '',
  assetName: '',
  assetCategory: '',
  model: '',
  purchaseDate: '',
  purchaseAmount: undefined as number | undefined,
  responsibleUserId: undefined as number | undefined,
  deptId: undefined as number | undefined,
  remark: '',
})

async function load() {
  loading.value = true
  try {
    const res = await listAssets({
      page: page.value,
      size: size.value,
      status: filters.status || undefined,
      assetCategory: filters.assetCategory || undefined,
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

const userOptions = ref<{ id: number; realName: string }[]>([])
const deptTree = ref<Record<string, unknown>[]>([])

async function loadDropdownData() {
  try {
    const [userRes, tree] = await Promise.all([listUsers(1, 500), getDeptTree()])
    userOptions.value = userRes.items.map((u: Record<string, unknown>) => ({ id: Number(u.id), realName: String(u.realName ?? '') }))
    deptTree.value = tree
  } catch { /* ignore */ }
}

function openCreate() {
  form.assetNo = ''
  form.assetName = ''
  form.assetCategory = ''
  form.model = ''
  form.purchaseDate = ''
  form.purchaseAmount = undefined
  form.responsibleUserId = undefined
  form.deptId = undefined
  form.remark = ''
  dialogVisible.value = true
  void loadDropdownData()
}

async function submitCreate() {
  if (!form.assetNo.trim() || !form.assetName.trim()) {
    ElMessage.warning('请填写资产编号与名称')
    return
  }
  try {
    await createAsset({
      assetNo: form.assetNo.trim(),
      assetName: form.assetName.trim(),
      assetCategory: form.assetCategory || null,
      model: form.model || null,
      purchaseDate: form.purchaseDate || null,
      purchaseAmount: form.purchaseAmount ?? null,
      responsibleUserId: form.responsibleUserId ?? null,
      deptId: form.deptId ?? null,
      remark: form.remark || null,
    })
    ElMessage.success('已新增')
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '新增失败')
  }
}

async function promptReason(title: string, placeholder: string) {
  try {
    const { value } = await ElMessageBox.prompt(placeholder, title, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'textarea',
    })
    return value ?? ''
  } catch {
    return null
  }
}

async function onReceive(row: JsonObject) {
  try {
    await ElMessageBox.confirm(`确认领用「${row.assetName}」？将归入您名下。`, '领用资产', { type: 'warning' })
  } catch {
    return
  }
  try {
    await receiveAsset(Number(row.id), {})
    ElMessage.success('已领用')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '领用失败')
  }
}

async function onReturn(row: JsonObject) {
  const reason = await promptReason('归还资产', '归还原因（可选）')
  if (reason === null) return
  try {
    await returnAsset(Number(row.id), { reason: reason || null })
    ElMessage.success('已归还')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '归还失败')
  }
}

async function onRepair(row: JsonObject) {
  const reason = await promptReason('送修资产', '送修原因（可选）')
  if (reason === null) return
  try {
    await repairAsset(Number(row.id), { reason: reason || null })
    ElMessage.success('已送修')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '送修失败')
  }
}

async function onScrap(row: JsonObject) {
  const reason = await promptReason('报废资产', '报废原因（可选）')
  if (reason === null) return
  try {
    await scrapAsset(Number(row.id), { reason: reason || null })
    ElMessage.success('已报废')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '报废失败')
  }
}

function goDetail(row: JsonObject) {
  router.push(`/assets/${Number(row.id)}`)
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
        <h2 class="oa-page__title">固定资产</h2>
        <p class="muted">资产档案与流转记录；非超管仅看到自己作为责任人的资产。</p>
      </div>
      <div class="oa-page__actions">
        <el-button v-if="canManage" type="primary" @click="openCreate">新增资产</el-button>
      </div>
    </div>

    <el-card shadow="never" class="oa-page__filters">
      <el-form inline @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="闲置" value="IDLE" />
            <el-option label="在用" value="IN_USE" />
            <el-option label="维修中" value="REPAIRING" />
            <el-option label="已报废" value="SCRAPPED" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="filters.assetCategory" clearable placeholder="如 电脑" style="width: 160px" />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="filters.keyword" clearable placeholder="编号或名称" style="width: 200px" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="assetNo" label="编号" width="160" />
        <el-table-column prop="assetName" label="名称" min-width="160" />
        <el-table-column prop="assetCategory" label="分类" width="120" />
        <el-table-column prop="model" label="型号" width="140" />
        <el-table-column label="购入日期" width="120">
          <template #default="{ row }">{{ formatDisplayDate(row.purchaseDate) }}</template>
        </el-table-column>
        <el-table-column prop="responsibleUserName" label="责任人" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">查看</el-button>
            <template v-if="canManage">
              <el-button v-if="String(row.status) === 'IDLE'" link type="success" @click="onReceive(row)">领用</el-button>
              <el-button v-if="['IN_USE','REPAIRING'].includes(String(row.status))" link type="primary" @click="onReturn(row)">归还</el-button>
              <el-button v-if="['IDLE','IN_USE'].includes(String(row.status))" link type="warning" @click="onRepair(row)">送修</el-button>
              <el-button v-if="String(row.status) !== 'SCRAPPED'" link type="danger" @click="onScrap(row)">报废</el-button>
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

    <el-dialog v-model="dialogVisible" title="新增资产" width="560px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="编号" required>
          <el-input v-model="form.assetNo" maxlength="64" placeholder="如 ZC202604280001" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.assetName" maxlength="255" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.assetCategory" maxlength="64" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="form.model" maxlength="128" />
        </el-form-item>
        <el-form-item label="购入日期">
          <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="购入金额">
          <el-input-number v-model="form.purchaseAmount" :min="0" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="责任人">
          <el-select v-model="form.responsibleUserId" filterable placeholder="选择责任人" style="width: 100%">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属部门">
          <el-tree-select v-model="form.deptId" :data="deptTree" :props="{ label: 'name', value: 'id', children: 'children' }" filterable placeholder="选择部门" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.oa-page__filters {
  margin-bottom: 12px;
}
</style>
