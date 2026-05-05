<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listExportTasks, listImportTasks } from '../../api/system'
import { http } from '../../api/http'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const tab = ref<'IMPORT' | 'EXPORT'>('IMPORT')
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const businessType = ref('')

// --- Import dialog ---
const importDialogVisible = ref(false)
const importForm = reactive({ businessType: '', fileName: '', totalRows: 0 })
const importLoading = ref(false)

// --- Export dialog ---
const exportDialogVisible = ref(false)
const exportForm = reactive({ businessType: '' })
const exportLoading = ref(false)

async function load() {
  loading.value = true
  try {
    if (tab.value === 'IMPORT') {
      const res = await listImportTasks({
        businessType: businessType.value || undefined,
        page: page.value,
        size: size.value,
      })
      rows.value = res.items
      total.value = Number(res.total)
    } else {
      const res = await listExportTasks({
        businessType: businessType.value || undefined,
        page: page.value,
        size: size.value,
      })
      rows.value = res.items
      total.value = Number(res.total)
    }
  } finally {
    loading.value = false
  }
}

void load()

function changeTab(t: 'IMPORT' | 'EXPORT') {
  tab.value = t
  page.value = 1
  void load()
}

const STATUS_TAG: Record<string, '' | 'success' | 'info' | 'warning' | 'danger'> = {
  SUCCESS: 'success',
  RUNNING: 'warning',
  FAILED: 'danger',
  PARTIAL: 'warning',
}

// --- Import ---
function openImport() {
  importForm.businessType = ''
  importForm.fileName = ''
  importForm.totalRows = 0
  importDialogVisible.value = true
}

async function submitImport() {
  if (!importForm.businessType.trim()) {
    ElMessage.warning('请输入业务类型')
    return
  }
  importLoading.value = true
  try {
    await http.post('/imports/commit', {
      businessType: importForm.businessType,
      fileName: importForm.fileName || 'import.xlsx',
      totalRows: importForm.totalRows,
    })
    ElMessage.success('导入任务已提交')
    importDialogVisible.value = false
    tab.value = 'IMPORT'
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导入失败')
  } finally {
    importLoading.value = false
  }
}

// --- Export ---
function openExport() {
  exportForm.businessType = ''
  exportDialogVisible.value = true
}

async function submitExport() {
  if (!exportForm.businessType.trim()) {
    ElMessage.warning('请输入业务类型')
    return
  }
  exportLoading.value = true
  try {
    await http.post('/exports', {
      businessType: exportForm.businessType,
    })
    ElMessage.success('导出任务已创建')
    exportDialogVisible.value = false
    tab.value = 'EXPORT'
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导出失败')
  } finally {
    exportLoading.value = false
  }
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">导入/导出任务</h2>
        <p class="muted">查看历史的导入与导出任务记录，或发起新的导入/导出任务。</p>
      </div>
      <div class="oa-page__actions">
        <el-button type="primary" @click="openImport">发起导入</el-button>
        <el-button type="success" @click="openExport">发起导出</el-button>
      </div>
    </div>

    <el-card shadow="never" style="margin-bottom: 12px">
      <el-radio-group :model-value="tab" @change="(v: string | number | boolean | undefined) => changeTab(v as 'IMPORT' | 'EXPORT')">
        <el-radio-button value="IMPORT">导入任务</el-radio-button>
        <el-radio-button value="EXPORT">导出任务</el-radio-button>
      </el-radio-group>
      <el-input
        v-model="businessType"
        placeholder="按业务类型筛选"
        style="width: 220px; margin-left: 16px"
        clearable
        @keyup.enter="load"
      />
      <el-button style="margin-left: 8px" @click="load">查询</el-button>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="taskCode" label="任务编号" min-width="180" />
        <el-table-column prop="businessType" label="业务" width="120" />
        <el-table-column prop="fileName" label="文件" min-width="200" />
        <template v-if="tab === 'IMPORT'">
          <el-table-column prop="totalRows" label="总行数" width="100" />
          <el-table-column prop="successRows" label="成功" width="90" />
          <el-table-column prop="failedRows" label="失败" width="90" />
        </template>
        <template v-else>
          <el-table-column prop="rowCount" label="行数" width="100" />
          <el-table-column prop="downloadCount" label="下载次数" width="100" />
        </template>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[String(row.status ?? '')] ?? 'info'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submittedByName" label="提交人" width="120" />
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="完成时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.finishedAt) }}</template>
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

    <!-- Import Dialog -->
    <el-dialog v-model="importDialogVisible" title="发起导入" width="480px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="业务类型">
          <el-input v-model="importForm.businessType" placeholder="如 LEAVE, EXPENSE, CONTRACT" />
        </el-form-item>
        <el-form-item label="文件名">
          <el-input v-model="importForm.fileName" placeholder="import.xlsx（可选）" />
        </el-form-item>
        <el-form-item label="数据行数">
          <el-input-number v-model="importForm.totalRows" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="submitImport">提交</el-button>
      </template>
    </el-dialog>

    <!-- Export Dialog -->
    <el-dialog v-model="exportDialogVisible" title="发起导出" width="480px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="业务类型">
          <el-input v-model="exportForm.businessType" placeholder="如 LEAVE, EXPENSE, CONTRACT" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="exportLoading" @click="submitExport">创建导出</el-button>
      </template>
    </el-dialog>
  </div>
</template>
