<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ElTree } from 'element-plus'
import {
  assignRoleButtons,
  assignRoleDataScopes,
  assignRoleMenus,
  createRole,
  deleteRole,
  getMenuTree,
  getRole,
  listButtons,
  listRoles,
  updateRole,
} from '../../api/permission'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  roleCode: '',
  roleName: '',
  roleType: 'BUSINESS',
  status: 'ENABLED',
  sortOrder: 0,
  remark: '',
})

const detail = ref<JsonObject | null>(null)
const roleIdForAssign = ref<number | null>(null)
const assignText = reactive({
  dataScopesJson: '',
})

// Menu tree
const menuTree = ref<JsonObject[]>([])
const menuTreeRef = ref<InstanceType<typeof ElTree> | null>(null)
const menuTreeProps = { children: 'children', label: 'menuName' }

// Button assignment
const allButtons = ref<JsonObject[]>([])
const selectedButtonIds = ref<number[]>([])

async function loadMenuTree() {
  try {
    menuTree.value = await getMenuTree()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载菜单树失败')
  }
}

async function loadAllButtons() {
  try {
    const res = await listButtons(1, 500)
    allButtons.value = res.items
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载按钮列表失败')
  }
}

onMounted(() => {
  loadMenuTree()
  loadAllButtons()
})

async function load() {
  loading.value = true
  try {
    const res = await listRoles(page.value, size.value)
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void load()

function openCreate() {
  editingId.value = null
  form.roleCode = ''
  form.roleName = ''
  form.roleType = 'BUSINESS'
  form.status = 'ENABLED'
  form.sortOrder = 0
  form.remark = ''
  dialogVisible.value = true
}

function openEdit(row: JsonObject) {
  editingId.value = Number(row.id)
  form.roleCode = String(row.roleCode ?? '')
  form.roleName = String(row.roleName ?? '')
  form.roleType = String(row.roleType ?? 'BUSINESS')
  form.status = String(row.status ?? 'ENABLED')
  form.sortOrder = Number(row.sortOrder ?? 0)
  form.remark = String(row.remark ?? '')
  dialogVisible.value = true
}

async function submit() {
  if (!form.roleCode.trim() || !form.roleName.trim()) {
    ElMessage.warning('请填写角色编码与名称')
    return
  }
  const body = {
    roleCode: form.roleCode.trim(),
    roleName: form.roleName.trim(),
    roleType: form.roleType,
    status: form.status,
    sortOrder: form.sortOrder,
    remark: form.remark || null,
  }
  try {
    if (editingId.value == null) {
      await createRole(body)
      ElMessage.success('角色已创建')
    } else {
      await updateRole(editingId.value, body)
      ElMessage.success('角色已更新')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function onDelete(row: JsonObject) {
  try {
    await ElMessageBox.confirm('确认删除该角色？', '删除角色', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteRole(Number(row.id))
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function onDetail(row: JsonObject) {
  try {
    detail.value = await getRole(Number(row.id))
    roleIdForAssign.value = Number(row.id)
    assignText.dataScopesJson = JSON.stringify((detail.value.dataScopes as unknown[]) ?? [], null, 2)

    // Set checked menu tree nodes
    const menuIds = ((detail.value.menuIds as unknown[]) ?? []).map(Number)
    await nextTick()
    if (menuTreeRef.value) {
      menuTreeRef.value.setCheckedKeys(menuIds, false)
    }

    // Set selected button IDs
    selectedButtonIds.value = ((detail.value.buttonIds as unknown[]) ?? []).map(Number)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载详情失败')
  }
}

async function onAssignMenus() {
  if (roleIdForAssign.value == null || !menuTreeRef.value) return
  const checkedKeys = menuTreeRef.value.getCheckedKeys(false) as number[]
  const halfCheckedKeys = menuTreeRef.value.getHalfCheckedKeys() as number[]
  const allMenuIds = [...checkedKeys, ...halfCheckedKeys].map(Number)
  try {
    await assignRoleMenus(roleIdForAssign.value, allMenuIds)
    ElMessage.success('菜单已分配')
    await onDetail({ id: roleIdForAssign.value })
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '分配菜单失败')
  }
}

async function onAssignButtons() {
  if (roleIdForAssign.value == null) return
  try {
    await assignRoleButtons(roleIdForAssign.value, selectedButtonIds.value)
    ElMessage.success('按钮已分配')
    await onDetail({ id: roleIdForAssign.value })
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '分配按钮失败')
  }
}

async function onAssignScopes() {
  if (roleIdForAssign.value == null) return
  let items: Record<string, unknown>[] = []
  try {
    items = JSON.parse(assignText.dataScopesJson || '[]')
  } catch {
    ElMessage.warning('数据权限 JSON 格式错误')
    return
  }
  try {
    await assignRoleDataScopes(roleIdForAssign.value, items)
    ElMessage.success('数据权限已分配')
    await onDetail({ id: roleIdForAssign.value })
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '分配数据权限失败')
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">角色管理</h2>
        <p class="muted">角色增删改与菜单/按钮/数据权限分配。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增角色</el-button>
    </div>

    <el-row :gutter="12">
      <el-col :span="14">
        <el-card shadow="never">
          <el-table v-loading="loading" :data="rows" stripe>
            <el-table-column prop="id" label="#" width="72" />
            <el-table-column prop="roleCode" label="编码" width="140" />
            <el-table-column prop="roleName" label="名称" min-width="140" />
            <el-table-column prop="roleType" label="类型" width="100" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button link type="primary" @click="onDetail(row)">详情</el-button>
                <el-button link @click="openEdit(row)">编辑</el-button>
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
      </el-col>

      <el-col :span="10">
        <el-card shadow="never">
          <template #header>权限分配</template>
          <template v-if="detail">
            <p class="muted">当前角色：{{ detail.roleName }} (ID={{ detail.id }})</p>
            <el-form label-width="88px">
              <el-form-item label="菜单权限">
                <el-tree
                  ref="menuTreeRef"
                  :data="menuTree"
                  :props="menuTreeProps"
                  show-checkbox
                  node-key="id"
                  default-expand-all
                  style="width: 100%; max-height: 280px; overflow-y: auto; border: 1px solid #dcdfe6; border-radius: 4px; padding: 8px"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="onAssignMenus">分配菜单</el-button>
              </el-form-item>
              <el-form-item label="按钮权限">
                <el-select
                  v-model="selectedButtonIds"
                  multiple
                  filterable
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="选择按钮权限"
                  style="width: 100%"
                >
                  <el-option
                    v-for="btn in allButtons"
                    :key="btn.id"
                    :label="`${btn.buttonName} (${btn.buttonCode})`"
                    :value="btn.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="onAssignButtons">分配按钮</el-button>
              </el-form-item>
              <el-form-item label="数据权限">
                <el-input v-model="assignText.dataScopesJson" type="textarea" :rows="8" />
              </el-form-item>
              <el-form-item>
                <el-button @click="onAssignScopes">分配数据权限</el-button>
              </el-form-item>
            </el-form>
          </template>
          <el-empty v-else description="请选择左侧角色查看/分配权限" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="editingId == null ? '新增角色' : '编辑角色'" width="520px" destroy-on-close>
      <el-form label-width="88px">
        <el-form-item label="编码" required>
          <el-input v-model="form.roleCode" maxlength="64" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.roleName" maxlength="128" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.roleType" style="width: 100%">
            <el-option label="系统" value="SYSTEM" />
            <el-option label="业务" value="BUSINESS" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
