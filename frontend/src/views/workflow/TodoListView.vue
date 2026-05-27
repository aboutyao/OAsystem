<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveTask, rejectTask, todoTasks } from '../../api/workflow'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'
import type { TableInstance } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const selectedRows = ref<JsonObject[]>([])
const tableRef = ref<TableInstance>()

const dialogVisible = ref(false)
const current = ref<JsonObject | null>(null)
const comment = ref('')
const acting = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await todoTasks(page.value, size.value)
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

function taskId(row: JsonObject): number {
  const t = row.taskId ?? row.id
  return Number(t)
}

function openHandle(row: JsonObject) {
  current.value = row
  comment.value = ''
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
  current.value = null
}

async function onApprove() {
  if (!current.value) return
  acting.value = true
  try {
    await approveTask(taskId(current.value), { comment: comment.value || null, attachmentIds: null })
    ElMessage.success('已通过')
    closeDialog()
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    acting.value = false
  }
}

async function onReject() {
  if (!current.value) return
  try {
    await ElMessageBox.confirm('确认驳回该流程？', '驳回', { type: 'warning' })
  } catch {
    return
  }
  acting.value = true
  try {
    await rejectTask(taskId(current.value), { comment: comment.value || null, rejectTo: null })
    ElMessage.success('已驳回')
    closeDialog()
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    acting.value = false
  }
}

function onSelectionChange(selection: JsonObject[]) {
  selectedRows.value = selection
}

function clearSelection() {
  selectedRows.value = []
  tableRef.value?.clearSelection()
}

async function batchApprove() {
  if (selectedRows.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确认批量通过 ${selectedRows.value.length} 项待办？`,
      '批量通过',
      { type: 'warning' }
    )
  } catch {
    return
  }
  acting.value = true
  let successCount = 0
  let failCount = 0
  try {
    for (const row of selectedRows.value) {
      try {
        await approveTask(taskId(row), { comment: null, attachmentIds: null })
        successCount++
      } catch {
        failCount++
      }
    }
    if (failCount === 0) {
      ElMessage.success(`已批量通过 ${successCount} 项`)
    } else {
      ElMessage.warning(`通过 ${successCount} 项，失败 ${failCount} 项`)
    }
    clearSelection()
    await load()
  } finally {
    acting.value = false
  }
}

function goDone() {
  router.push('/todos/done')
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">我的待办</h2>
        <p class="muted">处理分配给你的审批任务。</p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="goDone">我的已办</el-button>
      </div>
    </div>

    <div v-if="selectedRows.length > 0" class="batch-toolbar">
      <span>已选择 {{ selectedRows.length }} 项</span>
      <el-button type="success" :loading="acting" @click="batchApprove">批量通过</el-button>
      <el-button @click="clearSelection">取消选择</el-button>
    </div>

    <el-card shadow="never">
      <el-table ref="tableRef" v-loading="loading" :data="rows" stripe @selection-change="onSelectionChange">
        <template #empty>
          <el-empty description="暂无待办">
            <el-button type="primary" @click="goDone">查看已办</el-button>
          </el-empty>
        </template>
        <el-table-column type="selection" width="50" />
        <el-table-column prop="taskId" label="任务号" width="100" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="nodeName" label="当前节点" width="140" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="到达时间" min-width="160">
          <template #default="{ row }">{{ formatDisplayDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openHandle(row)">办理</el-button>
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

    <el-dialog v-model="dialogVisible" title="办理待办" width="520px" destroy-on-close @closed="current = null">
      <template v-if="current">
        <p><strong>{{ current.title }}</strong></p>
        <p class="muted">节点：{{ current.nodeName }}</p>
        <el-form label-position="top">
          <el-form-item label="审批意见（可选）">
            <el-input v-model="comment" type="textarea" :rows="3" maxlength="500" show-word-limit />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="danger" :loading="acting" @click="onReject">驳回</el-button>
        <el-button type="primary" :loading="acting" @click="onApprove">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.batch-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 12px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 6px;
  font-size: 14px;
}
</style>
