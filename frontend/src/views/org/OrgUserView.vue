<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createUser,
  disableUser,
  enableUser,
  getDeptTree,
  importUsers,
  listPositions,
  listRanks,
  listUsers,
  resignUser,
  updateUser,
  exportUsers,
} from '../../api/org'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')

// Filter state
const filterDeptId = ref<number | undefined>(undefined)
const filterEmployeeStatus = ref('')
const filterAccountStatus = ref('')

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

// Import state
const importDialogVisible = ref(false)
const importFile = ref<File | undefined>()
const importing = ref(false)
const importResult = ref<{ created: number; skipped: number; errors: string[] } | null>(null)

// Dropdown data
const deptTree = ref<JsonObject[]>([])
const positions = ref<JsonObject[]>([])
const ranks = ref<JsonObject[]>([])

const form = reactive({
  username: '',
  employeeNo: '',
  realName: '',
  mobile: '',
  email: '',
  mainDeptId: undefined as number | undefined,
  positionId: undefined as number | undefined,
  rankId: undefined as number | undefined,
  managerUserId: undefined as number | undefined,
  password: '',
})

async function load() {
  loading.value = true
  try {
    const res = await listUsers(
      page.value,
      size.value,
      keyword.value || undefined,
      filterDeptId.value,
      filterEmployeeStatus.value || undefined,
      filterAccountStatus.value || undefined,
    )
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  page.value = 1
  void load()
}

async function loadDropdowns() {
  const [d, p, r] = await Promise.all([getDeptTree(), listPositions(), listRanks()])
  deptTree.value = d
  positions.value = Array.isArray(p) ? p : (p as any).items ?? []
  ranks.value = Array.isArray(r) ? r : (r as any).items ?? []
}

void load()
void loadDropdowns()

function openCreate() {
  editingId.value = null
  form.username = ''
  form.employeeNo = ''
  form.realName = ''
  form.mobile = ''
  form.email = ''
  form.mainDeptId = undefined
  form.positionId = undefined
  form.rankId = undefined
  form.managerUserId = undefined
  form.password = ''
  dialogVisible.value = true
}

function openEdit(row: JsonObject) {
  editingId.value = Number(row.id)
  form.username = String(row.username ?? '')
  form.employeeNo = String(row.employeeNo ?? '')
  form.realName = String(row.realName ?? '')
  form.mobile = String(row.mobile ?? '')
  form.email = String(row.email ?? '')
  form.mainDeptId = row.mainDeptId == null ? undefined : Number(row.mainDeptId)
  form.positionId = row.positionId == null ? undefined : Number(row.positionId)
  form.rankId = row.rankId == null ? undefined : Number(row.rankId)
  form.managerUserId = row.managerUserId == null ? undefined : Number(row.managerUserId)
  form.password = ''
  dialogVisible.value = true
}

async function submit() {
  if (!form.employeeNo.trim() || !form.realName.trim() || form.mainDeptId == null) {
    ElMessage.warning('请填写工号、姓名、主部门')
    return
  }
  try {
    if (editingId.value == null) {
      if (!form.username.trim()) {
        ElMessage.warning('请填写用户名')
        return
      }
      await createUser({
        username: form.username.trim(),
        employeeNo: form.employeeNo.trim(),
        realName: form.realName.trim(),
        mobile: form.mobile || null,
        email: form.email || null,
        mainDeptId: form.mainDeptId,
        positionId: form.positionId ?? null,
        rankId: form.rankId ?? null,
        managerUserId: form.managerUserId ?? null,
        password: form.password || null,
      })
      ElMessage.success('用户已创建')
    } else {
      await updateUser(editingId.value, {
        employeeNo: form.employeeNo.trim(),
        realName: form.realName.trim(),
        mobile: form.mobile || null,
        email: form.email || null,
        mainDeptId: form.mainDeptId,
        positionId: form.positionId ?? null,
        rankId: form.rankId ?? null,
        managerUserId: form.managerUserId ?? null,
      })
      ElMessage.success('用户已更新')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function onEnable(row: JsonObject) {
  await enableUser(Number(row.id))
  ElMessage.success('已启用')
  await load()
}

async function onDisable(row: JsonObject) {
  await disableUser(Number(row.id))
  ElMessage.success('已停用')
  await load()
}

async function onResign(row: JsonObject) {
  try {
    await ElMessageBox.confirm('确认将该用户设为离职？', '离职', { type: 'warning' })
  } catch {
    return
  }
  await resignUser(Number(row.id))
  ElMessage.success('已离职')
  await load()
}

function statusTag(status: string) {
  switch (status) {
    case 'ACTIVE': return { type: 'success' as const, label: '在职' }
    case 'RESIGNED': return { type: 'danger' as const, label: '离职' }
    case 'DISABLED': return { type: 'info' as const, label: '停用' }
    default: return { type: 'info' as const, label: status }
  }
}

function openImportDialog() {
  importFile.value = undefined
  importResult.value = null
  importDialogVisible.value = true
}

function handleImportFileChange(file: File | undefined) {
  importFile.value = file
}

async function submitImport() {
  if (!importFile.value) {
    ElMessage.warning('请选择 CSV 文件')
    return
  }
  importing.value = true
  importResult.value = null
  try {
    const result = await importUsers(importFile.value)
    importResult.value = result
    ElMessage.success(`导入完成：成功 ${result.created} 条，跳过 ${result.skipped} 条`)
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导入失败')
  } finally {
    importing.value = false
  }
}

function handleExport() {
  window.open(exportUsers(), '_blank')
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">用户管理</h2>
        <p class="muted">用户列表、编辑、启停、离职。</p>
      </div>
      <div class="oa-page__actions">
        <el-input v-model="keyword" placeholder="用户名/姓名/工号" clearable style="width: 220px" @change="load" />
        <el-button type="primary" @click="openCreate">新增用户</el-button>
        <el-button @click="openImportDialog">导入用户</el-button>
        <el-button @click="handleExport">导出</el-button>
      </div>
    </div>

    <el-card shadow="never" style="margin-bottom: 16px">
      <el-form :inline="true" label-width="80px">
        <el-form-item label="部门">
          <el-tree-select
            v-model="filterDeptId"
            :data="deptTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="选择部门"
            style="width: 200px"
            @change="onFilterChange"
          />
        </el-form-item>
        <el-form-item label="员工状态">
          <el-select v-model="filterEmployeeStatus" clearable placeholder="全部" style="width: 140px" @change="onFilterChange">
            <el-option label="在职" value="ACTIVE" />
            <el-option label="离职" value="RESIGNED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号状态">
          <el-select v-model="filterAccountStatus" clearable placeholder="全部" style="width: 140px" @change="onFilterChange">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="employeeNo" label="工号" width="120" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="mainDeptName" label="主部门" width="160" />
        <el-table-column label="员工状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.employeeStatus).type">
              {{ statusTag(row.employeeStatus).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.accountStatus === 'ACTIVE' ? 'success' : 'info'">
              {{ row.accountStatus === 'ACTIVE' ? '正常' : row.accountStatus === 'DISABLED' ? '停用' : row.accountStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="String(row.accountStatus) === 'DISABLED'" link type="success" @click="onEnable(row)">启用</el-button>
            <el-button v-else link type="warning" @click="onDisable(row)">停用</el-button>
            <el-button link type="danger" @click="onResign(row)">离职</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId == null ? '新增用户' : '编辑用户'" width="560px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item v-if="editingId == null" label="用户名" required>
          <el-input v-model="form.username" maxlength="64" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="工号" required>
          <el-input v-model="form.employeeNo" maxlength="64" placeholder="如 EMP001" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.realName" maxlength="64" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.mobile" maxlength="32" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" maxlength="128" />
        </el-form-item>
        <el-form-item label="主部门" required>
          <el-tree-select
            v-model="form.mainDeptId"
            :data="deptTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            placeholder="选择部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="form.positionId" placeholder="选择岗位" clearable filterable style="width: 100%">
            <el-option v-for="p in positions" :key="p.id" :label="p.positionName" :value="Number(p.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="职级">
          <el-select v-model="form.rankId" placeholder="选择职级" clearable filterable style="width: 100%">
            <el-option v-for="r in ranks" :key="r.id" :label="r.rankName" :value="Number(r.id)" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editingId == null" label="初始密码">
          <el-input v-model="form.password" show-password maxlength="128" placeholder="留空则使用默认密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Import Dialog -->
    <el-dialog v-model="importDialogVisible" title="导入用户" width="500px" destroy-on-close>
      <div style="margin-bottom: 16px">
        <p class="muted" style="margin-bottom: 8px">请上传 CSV 文件，格式：</p>
        <p class="muted" style="font-family: monospace; background: var(--el-fill-color-lighter); padding: 8px; border-radius: 4px">
          username,employeeNo,realName,mainDeptId
        </p>
        <p class="muted" style="margin-top: 8px; font-size: 13px">
          mainDeptId 为系统中已存在的部门 ID，可从导出文件获取。
        </p>
      </div>
      <el-upload
        drag
        :auto-upload="false"
        :show-file-list="true"
        :limit="1"
        accept=".csv"
        @change="handleImportFileChange"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 CSV 文件拖到此处，或<em>点击选择</em></div>
      </el-upload>

      <div v-if="importResult" style="margin-top: 16px">
        <el-alert
          :title="`导入完成：成功 ${importResult.created} 条，跳过 ${importResult.skipped} 条`"
          :type="importResult.skipped > 0 ? 'warning' : 'success'"
          show-icon
          :closable="false"
        />
        <div v-if="importResult.errors.length > 0" style="margin-top: 8px">
          <p style="font-weight: 600; margin-bottom: 4px">错误详情：</p>
          <div style="max-height: 200px; overflow-y: auto; background: var(--el-fill-color-lighter); padding: 8px; border-radius: 4px; font-size: 13px">
            <p v-for="(err, idx) in importResult.errors" :key="idx" style="color: var(--el-color-danger); margin-bottom: 2px">{{ err }}</p>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importFile" @click="submitImport">
          {{ importing ? '导入中...' : '导入' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
