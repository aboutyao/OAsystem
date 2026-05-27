<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  archiveMessage,
  batchMarkMessagesRead,
  deleteMessage,
  listMessages,
  markMessageRead,
} from '../../api/messages'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const router = useRouter()

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const readFilter = ref<string>('')
const archiveFilter = ref<string>('NORMAL')

const selection = ref<JsonObject[]>([])

const READ_LABEL: Record<string, string> = {
  UNREAD: '未读',
  READ: '已读',
}

const TYPE_LABEL: Record<string, string> = {
  TODO: '待办',
  RESULT: '结果',
  NOTICE: '公告',
  SYSTEM: '系统',
  REMIND: '提醒',
  RISK: '风险',
  WORKFLOW: '流程',
}

async function load() {
  loading.value = true
  try {
    const res = await listMessages(
      page.value,
      size.value,
      readFilter.value || undefined,
      archiveFilter.value || undefined,
    )
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

function onSelectionChange(rowsSel: JsonObject[]) {
  selection.value = rowsSel
}

async function onMarkRead(row: JsonObject) {
  try {
    await markMessageRead(Number(row.id))
    ElMessage.success('已标记已读')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onBatchRead() {
  const ids = selection.value.map((r) => Number(r.id))
  if (ids.length === 0) {
    ElMessage.warning('请先勾选消息')
    return
  }
  try {
    await batchMarkMessagesRead(ids)
    ElMessage.success('已批量已读')
    selection.value = []
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onArchive(row: JsonObject) {
  try {
    await ElMessageBox.confirm('确定归档该消息？', '归档', { type: 'warning' })
  } catch {
    return
  }
  try {
    await archiveMessage(Number(row.id))
    ElMessage.success('已归档')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onDelete(row: JsonObject) {
  try {
    await ElMessageBox.confirm('确定删除该消息？', '删除', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteMessage(Number(row.id))
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

watch([readFilter, archiveFilter], () => {
  page.value = 1
  void load()
})

function navigateToDetail(row: JsonObject) {
  const businessType = String(row.businessType ?? '')
  const wfInstanceId = row.wfInstanceId

  if (businessType === 'WORKFLOW' && wfInstanceId) {
    router.push('/workflow/instances')
    return
  }

  const businessId = row.businessId
  if (!businessType || !businessId) {
    ElMessage.info('该消息没有关联的业务记录')
    return
  }

  const id = Number(businessId)
  const routeMap: Record<string, string> = {
    LEAVE: `/oa/leaves/${id}`,
    EXPENSE: `/oa/expenses/${id}`,
    SEAL: `/oa/seals/${id}`,
    PURCHASE: `/oa/purchases/${id}`,
    CONTRACT: `/contracts/${id}`,
  }

  const path = routeMap[businessType]
  if (path) {
    router.push(path)
  } else {
    ElMessage.info(`暂不支持跳转到 ${businessType} 详情`)
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">消息中心</h2>
        <p class="muted">仅展示当前账号接收的消息；未读统计同步到工作台「未读消息」。</p>
      </div>
      <div class="oa-page__actions">
        <el-button type="primary" :disabled="selection.length === 0" @click="onBatchRead">批量已读</el-button>
      </div>
    </div>

    <el-card shadow="never" class="oa-page__filters">
      <el-form inline @submit.prevent>
        <el-form-item label="阅读状态">
          <el-select v-model="readFilter" clearable placeholder="全部" style="width: 140px">
            <el-option label="未读" value="UNREAD" />
            <el-option label="已读" value="READ" />
          </el-select>
        </el-form-item>
        <el-form-item label="归档">
          <el-select v-model="archiveFilter" style="width: 140px">
            <el-option label="正常" value="NORMAL" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe @selection-change="onSelectionChange">
        <el-table-column
          type="selection"
          width="48"
          :selectable="(row: JsonObject) => String(row.readStatus) === 'UNREAD'"
        />
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ TYPE_LABEL[String(row.messageType ?? '')] ?? row.messageType }}</template>
        </el-table-column>
        <el-table-column label="标题" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="navigateToDetail(row)">
              {{ row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="阅读" width="88">
          <template #default="{ row }">
            <el-tag size="small" :type="String(row.readStatus) === 'UNREAD' ? 'warning' : 'info'">
              {{ READ_LABEL[String(row.readStatus ?? '')] ?? row.readStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button v-if="String(row.readStatus) === 'UNREAD'" link type="primary" @click="onMarkRead(row)">已读</el-button>
            <el-button
              v-if="String(row.archiveStatus) === 'NORMAL'"
              link
              type="warning"
              @click="onArchive(row)"
            >
              归档
            </el-button>
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
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.oa-page__filters {
  margin-bottom: 12px;
}
</style>
