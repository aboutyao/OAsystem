<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteFilePermanently, listRecycleBinFiles, restoreFile } from '../../api/file-library'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    const res = await listRecycleBinFiles(page.value, size.value, keyword.value || undefined)
    rows.value = res.items
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

void load()

function onSearch() {
  page.value = 1
  void load()
}

function onReset() {
  keyword.value = ''
  page.value = 1
  void load()
}

async function onRestore(row: JsonObject) {
  try {
    await ElMessageBox.confirm(`确认恢复文件「${row.fileName}」？`, '恢复文件', { type: 'info' })
  } catch {
    return
  }
  await restoreFile(Number(row.id))
  ElMessage.success('文件已恢复')
  await load()
}

async function onDeletePermanently(row: JsonObject) {
  try {
    await ElMessageBox.confirm(
      `确认永久删除文件「${row.fileName}」？此操作不可撤销！`,
      '永久删除',
      { type: 'error', confirmButtonText: '永久删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  await deleteFilePermanently(Number(row.id))
  ElMessage.success('已永久删除')
  await load()
}

function formatSize(val: unknown): string {
  const n = Number(val)
  if (!n || n <= 0) return '-'
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(1) + ' MB'
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">回收站</h2>
        <p class="muted">已删除的文件将暂时保留在回收站中，可在一定期限内恢复。</p>
      </div>
    </div>

    <el-card shadow="never" class="oa-page__filters">
      <el-form inline @submit.prevent="onSearch">
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="文件名" clearable style="width: 200px" @change="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="72" />
        <el-table-column prop="fileName" label="文件名" min-width="220" />
        <el-table-column prop="folderName" label="原文件夹" width="160" />
        <el-table-column label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="uploadUserName" label="上传人" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="warning">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="删除时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onRestore(row)">恢复</el-button>
            <el-button link type="danger" @click="onDeletePermanently(row)">永久删除</el-button>
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
  </div>
</template>

<style scoped>
.oa-page__filters {
  margin-bottom: 12px;
}
</style>
