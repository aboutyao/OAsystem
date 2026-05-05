<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAsset, listAssetRecords, updateAsset } from '../../api/assets'
import { getDeptTree } from '../../api/org'
import type { JsonObject } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { formatDisplayDate, formatDisplayDateTime } from '../oa/oa-shared'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const asset = ref<JsonObject | null>(null)
const records = ref<JsonObject[]>([])

const STATUS_LABEL: Record<string, string> = {
  IDLE: '闲置',
  IN_USE: '在用',
  REPAIRING: '维修中',
  SCRAPPED: '已报废',
}

const RECORD_LABEL: Record<string, string> = {
  RECEIVE: '领用',
  RETURN: '归还',
  REPAIR: '送修',
  SCRAP: '报废',
  TRANSFER: '转移',
}

const canManage = computed(() => {
  const p = authStore.user?.permissions ?? []
  return p.includes('*') || p.includes('org:create')
})

const deptTree = ref<Record<string, unknown>[]>([])
const editVisible = ref(false)
const editForm = ref<{
  assetName: string
  assetCategory: string
  model: string
  purchaseDate: string
  purchaseAmount: number | undefined
  deptId: number | undefined
  remark: string
}>({
  assetName: '',
  assetCategory: '',
  model: '',
  purchaseDate: '',
  purchaseAmount: undefined,
  deptId: undefined,
  remark: '',
})

async function load() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) return
  loading.value = true
  try {
    const [a, r] = await Promise.all([getAsset(id), listAssetRecords(id)])
    asset.value = a
    records.value = r
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    router.push('/assets')
  } finally {
    loading.value = false
  }
}

void load()
watch(() => route.params.id, () => void load())

async function loadDeptTree() {
  try {
    deptTree.value = await getDeptTree()
  } catch { /* ignore */ }
}

function openEdit() {
  if (!asset.value) return
  void loadDeptTree()
  editForm.value = {
    assetName: String(asset.value.assetName ?? ''),
    assetCategory: String(asset.value.assetCategory ?? ''),
    model: String(asset.value.model ?? ''),
    purchaseDate: asset.value.purchaseDate ? String(asset.value.purchaseDate).slice(0, 10) : '',
    purchaseAmount:
      asset.value.purchaseAmount != null ? Number(asset.value.purchaseAmount) : undefined,
    deptId: asset.value.deptId != null ? Number(asset.value.deptId) : undefined,
    remark: String(asset.value.remark ?? ''),
  }
  editVisible.value = true
}

async function submitEdit() {
  if (!asset.value) return
  if (!editForm.value.assetName.trim()) {
    ElMessage.warning('请填写资产名称')
    return
  }
  try {
    await updateAsset(Number(asset.value.id), {
      assetName: editForm.value.assetName.trim(),
      assetCategory: editForm.value.assetCategory || null,
      model: editForm.value.model || null,
      purchaseDate: editForm.value.purchaseDate || null,
      purchaseAmount: editForm.value.purchaseAmount ?? null,
      deptId: editForm.value.deptId ?? null,
      remark: editForm.value.remark || null,
    })
    ElMessage.success('已保存')
    editVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

function statusLabel(s: unknown) {
  const code = String(s ?? '')
  return STATUS_LABEL[code] ?? code
}

function recordLabel(s: unknown) {
  const code = String(s ?? '')
  return RECORD_LABEL[code] ?? code
}
</script>

<template>
  <div v-loading="loading" class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">资产详情</h2>
        <p class="muted">基础信息与流转记录。</p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="router.push('/assets')">返回</el-button>
        <el-button v-if="canManage && asset" type="primary" @click="openEdit">编辑</el-button>
      </div>
    </div>

    <el-card v-if="asset" shadow="never">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="编号">{{ asset.assetNo }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ asset.assetName }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ asset.assetCategory ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ asset.model ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="购入日期">{{ formatDisplayDate(asset.purchaseDate) }}</el-descriptions-item>
        <el-descriptions-item label="购入金额">{{ asset.purchaseAmount ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="责任人">{{ asset.responsibleUserName ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="所属部门">{{ asset.deptName ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small">{{ statusLabel(asset.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDisplayDateTime(asset.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ asset.remark ?? '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="asset" shadow="never" class="oa-page__records" id="records">
      <template #header><span>流转记录</span></template>
      <el-empty v-if="records.length === 0" description="暂无记录" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="r in records"
          :key="Number(r.id)"
          :timestamp="formatDisplayDateTime(r.operatedAt)"
        >
          <div>
            <strong>{{ recordLabel(r.recordType) }}</strong>
            <span v-if="r.fromUserName"> · 原责任人：{{ r.fromUserName }}</span>
            <span v-if="r.toUserName"> · 新责任人：{{ r.toUserName }}</span>
            <span v-if="r.operatedByName"> · 操作人：{{ r.operatedByName }}</span>
          </div>
          <div v-if="r.reason" class="muted" style="font-size: 13px">{{ r.reason }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑资产" width="520px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="名称" required>
          <el-input v-model="editForm.assetName" maxlength="255" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="editForm.assetCategory" maxlength="64" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="editForm.model" maxlength="128" />
        </el-form-item>
        <el-form-item label="购入日期">
          <el-date-picker v-model="editForm.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="购入金额">
          <el-input-number v-model="editForm.purchaseAmount" :min="0" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="所属部门">
          <el-tree-select v-model="editForm.deptId" :data="deptTree" :props="{ label: 'name', value: 'id', children: 'children' }" filterable placeholder="选择部门" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.oa-page__records {
  margin-top: 16px;
}
</style>
