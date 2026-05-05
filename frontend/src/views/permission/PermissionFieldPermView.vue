<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createFieldPermission,
  deleteFieldPermission,
  listFieldPermissions,
  listRoles,
  updateFieldPermission,
} from '../../api/permission'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const filters = reactive({
  roleId: undefined as number | undefined,
  businessType: '',
})

const roles = ref<JsonObject[]>([])

const businessTypes = [
  { label: '报销', value: 'EXPENSE' },
  { label: '请假', value: 'LEAVE' },
  { label: '用章', value: 'SEAL' },
  { label: '采购', value: 'PURCHASE' },
  { label: '合同', value: 'CONTRACT' },
  { label: '通用', value: 'GENERIC' },
]

async function loadRoles() {
  try {
    const res = await listRoles(1, 500)
    roles.value = res.items
  } catch {
    // ignore
  }
}

void loadRoles()

async function load() {
  loading.value = true
  try {
    const res = await listFieldPermissions(
      page.value,
      size.value,
      filters.roleId,
      filters.businessType || undefined,
    )
    rows.value = res.items
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

void load()

function onSearch() {
  page.value = 1
  void load()
}

function onReset() {
  filters.roleId = undefined
  filters.businessType = ''
  page.value = 1
  void load()
}

// Dialog
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const form = reactive({
  roleId: undefined as number | undefined,
  businessType: '',
  fieldCode: '',
  visible: 1,
  editable: 0,
  required: 0,
  masked: 0,
})

function openCreate() {
  editingId.value = null
  form.roleId = filters.roleId
  form.businessType = filters.businessType
  form.fieldCode = ''
  form.visible = 1
  form.editable = 0
  form.required = 0
  form.masked = 0
  dialogVisible.value = true
}

function openEdit(row: JsonObject) {
  editingId.value = Number(row.id)
  form.roleId = Number(row.roleId)
  form.businessType = String(row.businessType ?? '')
  form.fieldCode = String(row.fieldCode ?? '')
  form.visible = Number(row.visible ?? 1)
  form.editable = Number(row.editable ?? 0)
  form.required = Number(row.required ?? 0)
  form.masked = Number(row.masked ?? 0)
  dialogVisible.value = true
}

async function submit() {
  if (!form.roleId || !form.businessType.trim() || !form.fieldCode.trim()) {
    ElMessage.warning('请填写角色、业务类型和字段编码')
    return
  }
  submitting.value = true
  try {
    const body = {
      roleId: form.roleId,
      businessType: form.businessType.trim(),
      fieldCode: form.fieldCode.trim(),
      visible: form.visible,
      editable: form.editable,
      required: form.required,
      masked: form.masked,
    }
    if (editingId.value == null) {
      await createFieldPermission(body)
      ElMessage.success('字段权限已创建')
    } else {
      await updateFieldPermission(editingId.value, body)
      ElMessage.success('字段权限已更新')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    submitting.value = false
  }
}

async function onDelete(row: JsonObject) {
  try {
    await ElMessageBox.confirm('确认删除该字段权限配置？', '删除', { type: 'warning' })
  } catch {
    return
  }
  await deleteFieldPermission(Number(row.id))
  ElMessage.success('已删除')
  await load()
}

function boolText(v: unknown): string {
  return Number(v) === 1 ? '是' : '否'
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">字段权限配置</h2>
        <p class="muted">配置角色对业务表单字段的可见性和编辑权限。</p>
      </div>
      <div class="oa-page__actions">
        <el-button type="primary" @click="openCreate">新增配置</el-button>
      </div>
    </div>

    <el-card shadow="never" class="oa-page__filters">
      <el-form inline @submit.prevent="onSearch">
        <el-form-item label="角色">
          <el-select v-model="filters.roleId" clearable filterable placeholder="全部" style="width: 180px">
            <el-option v-for="r in roles" :key="r.id" :label="`${r.roleName}`" :value="Number(r.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="filters.businessType" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="o in businessTypes" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="roleName" label="角色" width="140" />
        <el-table-column prop="businessType" label="业务类型" width="120" />
        <el-table-column prop="fieldCode" label="字段编码" min-width="180" />
        <el-table-column label="可见" width="80">
          <template #default="{ row }">{{ boolText(row.visible) }}</template>
        </el-table-column>
        <el-table-column label="可编辑" width="80">
          <template #default="{ row }">{{ boolText(row.editable) }}</template>
        </el-table-column>
        <el-table-column label="必填" width="80">
          <template #default="{ row }">{{ boolText(row.required) }}</template>
        </el-table-column>
        <el-table-column label="脱敏" width="80">
          <template #default="{ row }">{{ boolText(row.masked) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
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
          @size-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId == null ? '新增字段权限' : '编辑字段权限'" width="520px" destroy-on-close>
      <el-form label-width="88px">
        <el-form-item label="角色" required>
          <el-select v-model="form.roleId" filterable style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="`${r.roleName}`" :value="Number(r.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型" required>
          <el-select v-model="form.businessType" style="width: 100%">
            <el-option v-for="o in businessTypes" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="字段编码" required>
          <el-input v-model="form.fieldCode" placeholder="如 amount, applicant" />
        </el-form-item>
        <el-form-item label="可见">
          <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="可编辑">
          <el-switch v-model="form.editable" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="必填">
          <el-switch v-model="form.required" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="脱敏">
          <el-switch v-model="form.masked" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.oa-page__filters {
  margin-bottom: 12px;
}
</style>
