<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listBackupRecords } from '../../api/ops'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const filters = reactive({
  backupType: '',
  status: '',
})

function statusType(status: string): 'success' | 'info' | 'warning' | 'danger' {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'RUNNING':
      return 'warning'
    case 'FAILED':
      return 'danger'
    default:
      return 'info'
  }
}

async function load() {
  loading.value = true
  try {
    const res = await listBackupRecords({
      page: page.value,
      size: size.value,
      backupType: filters.backupType || undefined,
      status: filters.status || undefined,
    })
    rows.value = res.items
    total.value = Number(res.total)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
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
  filters.backupType = ''
  filters.status = ''
  page.value = 1
  void load()
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">备份记录</h2>
        <p class="muted">查看系统备份的执行记录。</p>
      </div>
    </div>

    <el-card shadow="never" class="oa-page__filters">
      <el-form inline @submit.prevent="onSearch">
        <el-form-item label="备份类型">
          <el-input v-model="filters.backupType" placeholder="备份类型" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="运行中" value="RUNNING" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="#" width="80" />
        <el-table-column prop="backupType" label="备份类型" width="140" />
        <el-table-column prop="backupPath" label="路径" min-width="240" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(String(row.status))">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="backupSize" label="大小(B)" width="120" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column prop="failReason" label="失败原因" min-width="180" show-overflow-tooltip />
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
