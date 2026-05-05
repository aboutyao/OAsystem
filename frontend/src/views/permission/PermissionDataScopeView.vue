<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRoles, assignRoleDataScopes } from '../../api/permission'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

// --- Edit dialog ---
const dialogVisible = ref(false)
const editingRole = ref<JsonObject | null>(null)
const scopeItems = ref<Array<{ scopeType: string; businessType: string; deptIds: number[] }>>([])

const SCOPE_TYPES = [
  { value: 'ALL', label: '全部数据' },
  { value: 'DEPT', label: '本部门及下级' },
  { value: 'DEPT_ONLY', label: '仅本部门' },
  { value: 'SELF', label: '仅本人' },
]

const BUSINESS_TYPES = [
  { value: 'LEAVE', label: '请假' },
  { value: 'EXPENSE', label: '报销' },
  { value: 'CONTRACT', label: '合同' },
  { value: 'NOTICE', label: '公告' },
  { value: 'MEETING', label: '会议' },
  { value: 'ASSET', label: '资产' },
  { value: 'SEAL', label: '印章' },
  { value: 'PURCHASE', label: '采购' },
]

const SCOPE_LABEL_MAP: Record<string, string> = Object.fromEntries(SCOPE_TYPES.map(s => [s.value, s.label]))

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

function formatScope(scopeType: unknown): string {
  return SCOPE_LABEL_MAP[String(scopeType)] ?? String(scopeType ?? '-')
}

function openEdit(row: JsonObject) {
  editingRole.value = row
  const existing = (row.dataScopes as JsonObject[] | undefined) ?? []
  scopeItems.value = existing.map(s => ({
    scopeType: String(s.scopeType ?? 'ALL'),
    businessType: String(s.businessType ?? ''),
    deptIds: (s.deptIds as number[] | undefined) ?? [],
  }))
  if (scopeItems.value.length === 0) {
    scopeItems.value.push({ scopeType: 'ALL', businessType: 'LEAVE', deptIds: [] })
  }
  dialogVisible.value = true
}

function addScopeItem() {
  scopeItems.value.push({ scopeType: 'ALL', businessType: '', deptIds: [] })
}

function removeScopeItem(idx: number) {
  scopeItems.value.splice(idx, 1)
}

async function saveScopes() {
  if (!editingRole.value) return
  const invalid = scopeItems.value.some(s => !s.businessType)
  if (invalid) {
    ElMessage.warning('请为每条规则选择业务类型')
    return
  }
  try {
    await assignRoleDataScopes(Number(editingRole.value.id), scopeItems.value)
    ElMessage.success('数据权限已更新')
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">数据权限</h2>
        <p class="muted">管理各角色的数据权限配置；点击操作列可编辑。</p>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="roleCode" label="角色编码" width="160" />
        <el-table-column prop="roleName" label="角色名称" min-width="160" />
        <el-table-column prop="roleType" label="角色类型" width="120" />
        <el-table-column label="数据权限范围" min-width="200">
          <template #default="{ row }">
            <span v-if="row.dataScopes && (row.dataScopes as unknown[]).length > 0">
              {{ (row.dataScopes as JsonObject[]).map((s: JsonObject) => formatScope(s.scopeType)).join(', ') }}
            </span>
            <span v-else class="muted">未配置</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
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

    <!-- Edit Data Scope Dialog -->
    <el-dialog v-model="dialogVisible" title="编辑数据权限" width="640px" destroy-on-close>
      <div v-if="editingRole" style="margin-bottom: 12px">
        <strong>{{ editingRole.roleName }}</strong>
        <span class="muted" style="margin-left: 8px">({{ editingRole.roleCode }})</span>
      </div>

      <div v-for="(item, idx) in scopeItems" :key="idx" class="scope-row">
        <el-select v-model="item.businessType" placeholder="业务类型" style="width: 140px">
          <el-option v-for="bt in BUSINESS_TYPES" :key="bt.value" :label="bt.label" :value="bt.value" />
        </el-select>
        <el-select v-model="item.scopeType" placeholder="权限范围" style="width: 160px; margin-left: 8px">
          <el-option v-for="st in SCOPE_TYPES" :key="st.value" :label="st.label" :value="st.value" />
        </el-select>
        <el-button link type="danger" style="margin-left: 8px" @click="removeScopeItem(idx)" :disabled="scopeItems.length <= 1">删除</el-button>
      </div>

      <el-button type="primary" plain size="small" style="margin-top: 8px" @click="addScopeItem">添加规则</el-button>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveScopes">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.scope-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
</style>
