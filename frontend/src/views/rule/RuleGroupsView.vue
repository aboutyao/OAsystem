<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createRuleGroup, deleteRuleGroup, listRuleGroups, updateRuleGroup } from '../../api/rules'
import type { JsonObject } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { formatDisplayDateTime } from '../oa/oa-shared'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const rows = ref<JsonObject[]>([])

const canManage = computed(() => {
  const p = authStore.user?.permissions ?? []
  return p.includes('*') || p.includes('rule:version:publish')
})

const STATUS_TAG: Record<string, '' | 'success' | 'info' | 'warning' | 'danger'> = {
  ENABLED: 'success',
  DISABLED: 'info',
}

async function load() {
  loading.value = true
  try {
    rows.value = await listRuleGroups()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void load()

// Dialog
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const form = reactive({
  groupCode: '',
  groupName: '',
  description: '',
  status: 'ENABLED',
})

function openCreate() {
  editingId.value = null
  form.groupCode = ''
  form.groupName = ''
  form.description = ''
  form.status = 'ENABLED'
  dialogVisible.value = true
}

function openEdit(row: JsonObject) {
  editingId.value = Number(row.id)
  form.groupCode = String(row.groupCode ?? '')
  form.groupName = String(row.groupName ?? '')
  form.description = String(row.description ?? '')
  form.status = String(row.status ?? 'ENABLED')
  dialogVisible.value = true
}

async function submit() {
  if (!form.groupCode.trim() || !form.groupName.trim()) {
    ElMessage.warning('请填写分组编码与名称')
    return
  }
  submitting.value = true
  try {
    const body = {
      groupCode: form.groupCode.trim(),
      groupName: form.groupName.trim(),
      description: form.description || undefined,
      status: form.status,
    }
    if (editingId.value == null) {
      await createRuleGroup(body)
      ElMessage.success('规则分组已创建')
    } else {
      await updateRuleGroup(editingId.value, body)
      ElMessage.success('规则分组已更新')
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
    await ElMessageBox.confirm(
      `确认删除分组「${row.groupName}」？需确保该分组下无规则。`,
      '删除分组',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteRuleGroup(Number(row.id))
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

function goToRules() {
  void router.push('/rules')
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">规则分组管理</h2>
        <p class="muted">按业务场景对规则进行分组管理。</p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="goToRules">返回规则列表</el-button>
        <el-button v-if="canManage" type="primary" @click="openCreate">新建分组</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="groupCode" label="分组编码" min-width="180" />
        <el-table-column prop="groupName" label="分组名称" min-width="180" />
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
        <el-table-column prop="ruleCount" label="规则数" width="100" align="center" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="STATUS_TAG[String(row.status ?? '')] ?? 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column v-if="canManage" label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId == null ? '新建规则分组' : '编辑规则分组'" width="480px" destroy-on-close>
      <el-form label-width="88px">
        <el-form-item label="分组编码" required>
          <el-input v-model="form.groupCode" placeholder="如 APPROVAL_ROUTING" :disabled="editingId != null" />
        </el-form-item>
        <el-form-item label="分组名称" required>
          <el-input v-model="form.groupName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
