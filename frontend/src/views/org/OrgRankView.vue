<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRanks, createRank, updateRank, deleteRank } from '../../api/org'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  rankCode: '',
  rankName: '',
  remark: '',
})

async function load() {
  loading.value = true
  try {
    rows.value = await listRanks()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void load()

function openCreate() {
  editingId.value = null
  form.value = { rankCode: '', rankName: '', remark: '' }
  dialogVisible.value = true
}

function openEdit(row: JsonObject) {
  editingId.value = Number(row.id)
  form.value = {
    rankCode: String(row.rankCode ?? ''),
    rankName: String(row.rankName ?? ''),
    remark: String(row.remark ?? ''),
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.rankCode.trim() || !form.value.rankName.trim()) {
    ElMessage.warning('请填写职级编码与职级名称')
    return
  }
  const body = {
    rankCode: form.value.rankCode.trim(),
    rankName: form.value.rankName.trim(),
    remark: form.value.remark.trim(),
  }
  try {
    if (editingId.value != null) {
      await updateRank(editingId.value, body)
      ElMessage.success('职级已更新')
    } else {
      await createRank(body)
      ElMessage.success('职级已创建')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function handleDelete(row: JsonObject) {
  try {
    await ElMessageBox.confirm(`确认删除职级「${row.rankName}」？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteRank(Number(row.id))
    ElMessage.success('职级已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '删除失败')
    }
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">职级管理</h2>
        <p class="muted">职级列表。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增职级</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="rankCode" label="职级编码" min-width="160" />
        <el-table-column prop="rankName" label="职级名称" min-width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId != null ? '编辑职级' : '新增职级'"
      width="480px"
      destroy-on-close
    >
      <el-form label-width="96px">
        <el-form-item label="职级编码" required>
          <el-input v-model="form.rankCode" maxlength="64" placeholder="请输入职级编码" />
        </el-form-item>
        <el-form-item label="职级名称" required>
          <el-input v-model="form.rankName" maxlength="128" placeholder="请输入职级名称" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="256" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
