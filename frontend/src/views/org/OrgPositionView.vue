<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPositions, createPosition, updatePosition, deletePosition } from '../../api/org'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  positionCode: '',
  positionName: '',
  remark: '',
})

async function load() {
  loading.value = true
  try {
    rows.value = await listPositions()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void load()

function openCreate() {
  editingId.value = null
  form.value = { positionCode: '', positionName: '', remark: '' }
  dialogVisible.value = true
}

function openEdit(row: JsonObject) {
  editingId.value = Number(row.id)
  form.value = {
    positionCode: String(row.positionCode ?? ''),
    positionName: String(row.positionName ?? ''),
    remark: String(row.remark ?? ''),
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.positionCode.trim() || !form.value.positionName.trim()) {
    ElMessage.warning('请填写岗位编码与岗位名称')
    return
  }
  const body = {
    positionCode: form.value.positionCode.trim(),
    positionName: form.value.positionName.trim(),
    remark: form.value.remark.trim(),
  }
  try {
    if (editingId.value != null) {
      await updatePosition(editingId.value, body)
      ElMessage.success('岗位已更新')
    } else {
      await createPosition(body)
      ElMessage.success('岗位已创建')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function handleDelete(row: JsonObject) {
  try {
    await ElMessageBox.confirm(`确认删除岗位「${row.positionName}」？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deletePosition(Number(row.id))
    ElMessage.success('岗位已删除')
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
        <h2 class="oa-page__title">岗位管理</h2>
        <p class="muted">岗位列表。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增岗位</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="positionCode" label="岗位编码" min-width="160" />
        <el-table-column prop="positionName" label="岗位名称" min-width="160" />
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
      :title="editingId != null ? '编辑岗位' : '新增岗位'"
      width="480px"
      destroy-on-close
    >
      <el-form label-width="96px">
        <el-form-item label="岗位编码" required>
          <el-input v-model="form.positionCode" maxlength="64" placeholder="请输入岗位编码" />
        </el-form-item>
        <el-form-item label="岗位名称" required>
          <el-input v-model="form.positionName" maxlength="128" placeholder="请输入岗位名称" />
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
