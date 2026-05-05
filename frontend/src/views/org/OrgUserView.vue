<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createUser,
  disableUser,
  enableUser,
  getDeptTree,
  listPositions,
  listRanks,
  listUsers,
  resignUser,
  updateUser,
} from '../../api/org'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

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
    const res = await listUsers(page.value, size.value, keyword.value || undefined)
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
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
      </div>
    </div>

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
  </div>
</template>
