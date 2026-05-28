<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveTask, batchApprove as batchApproveApi, rejectTask, todoTasks, listCommentTemplates, createCommentTemplate, deleteCommentTemplate, getApprovalContext } from '../../api/workflow'
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
const commentTemplates = ref<JsonObject[]>([])
const newTemplateContent = ref('')
const approvalContext = ref<JsonObject | null>(null)
const contextLoading = ref(false)

onMounted(async () => {
  try {
    commentTemplates.value = await listCommentTemplates()
  } catch {
    // ignore
  }
})

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
  approvalContext.value = null
  dialogVisible.value = true
  loadApprovalContext(row)
}

async function loadApprovalContext(row: JsonObject) {
  contextLoading.value = true
  try {
    approvalContext.value = await getApprovalContext(taskId(row))
  } catch {
    approvalContext.value = null
  } finally {
    contextLoading.value = false
  }
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
  try {
    const taskIds = selectedRows.value.map(row => taskId(row))
    const result = await batchApproveApi(taskIds)
    if (result.failed === 0) {
      ElMessage.success(`已批量通过 ${result.success} 项`)
    } else {
      ElMessage.warning(`通过 ${result.success} 项，失败 ${result.failed} 项`)
    }
    clearSelection()
    await load()
  } finally {
    acting.value = false
  }
}

function useTemplate(template: JsonObject) {
  comment.value = String(template.content ?? '')
}

async function saveAsTemplate() {
  if (!comment.value.trim()) {
    ElMessage.warning('请先输入审批意见')
    return
  }
  try {
    const result = await createCommentTemplate(comment.value.trim())
    commentTemplates.value.push(result)
    ElMessage.success('已保存为模板')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function removeTemplate(template: JsonObject) {
  try {
    await deleteCommentTemplate(Number(template.id))
    commentTemplates.value = commentTemplates.value.filter(t => t.id !== template.id)
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
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

    <el-dialog v-model="dialogVisible" title="办理待办" width="600px" destroy-on-close @closed="current = null">
      <template v-if="current">
        <p><strong>{{ current.title }}</strong></p>
        <p class="muted">节点：{{ current.nodeName }}</p>

        <!-- Smart Approval Context Panel -->
        <div v-if="contextLoading" class="context-panel context-panel--loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载审批上下文...</span>
        </div>
        <div v-else-if="approvalContext" class="context-panel">
          <div class="context-panel__header">
            <el-icon :size="14"><InfoFilled /></el-icon>
            <span>审批参考信息</span>
          </div>

          <!-- Requester Info -->
          <div v-if="approvalContext.requester" class="context-section">
            <div class="context-row">
              <span class="context-label">申请人</span>
              <span class="context-value">{{ (approvalContext.requester as any).realName }}</span>
              <el-tag size="small" type="info">{{ (approvalContext.requester as any).deptName }}</el-tag>
            </div>
          </div>

          <!-- Risk Flags -->
          <div v-if="(approvalContext.riskFlags as any[])?.length" class="context-section">
            <div class="context-row" v-for="flag in (approvalContext.riskFlags as any[])" :key="flag">
              <el-tag size="small" :type="flag.includes('高') ? 'danger' : 'warning'" effect="light">
                {{ flag }}
              </el-tag>
            </div>
          </div>

          <!-- History Summary -->
          <div v-if="(approvalContext.requesterHistory as any[])?.length" class="context-section">
            <div class="context-subtitle">近期审批记录</div>
            <div class="context-history">
              <div v-for="(h, i) in (approvalContext.requesterHistory as any[]).slice(0, 3)" :key="i" class="context-history__item">
                <el-tag size="small" :type="h.action === 'APPROVE' ? 'success' : h.action === 'REJECT' ? 'danger' : 'info'">
                  {{ h.action === 'APPROVE' ? '通过' : h.action === 'REJECT' ? '驳回' : h.action }}
                </el-tag>
                <span class="context-history__node">{{ h.nodeName }}</span>
                <span class="context-history__time">{{ h.operatedAt }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="commentTemplates.length > 0" class="template-tags">
          <span class="muted" style="font-size: 12px">常用意见：</span>
          <el-tag
            v-for="t in commentTemplates"
            :key="t.id"
            size="small"
            closable
            @click="useTemplate(t)"
            @close="removeTemplate(t)"
            style="cursor: pointer"
          >
            {{ t.content }}
          </el-tag>
        </div>

        <el-form label-position="top">
          <el-form-item label="审批意见（可选）">
            <el-input v-model="comment" type="textarea" :rows="3" maxlength="500" show-word-limit />
          </el-form-item>
        </el-form>
        <el-button size="small" link type="primary" @click="saveAsTemplate">存为常用意见</el-button>
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
.template-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

/* Approval Context Panel */
.context-panel {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
}

.context-panel--loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  margin-bottom: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #94a3b8;
  font-size: 13px;
}

.context-panel__header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 10px;
}

.context-section {
  margin-bottom: 8px;
}

.context-section:last-child {
  margin-bottom: 0;
}

.context-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 4px;
}

.context-label {
  color: #94a3b8;
  min-width: 60px;
}

.context-value {
  color: #334155;
  font-weight: 500;
}

.context-subtitle {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 6px;
}

.context-history {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.context-history__item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.context-history__node {
  color: #64748b;
  flex: 1;
}

.context-history__time {
  color: #94a3b8;
}
</style>
