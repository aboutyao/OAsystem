<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listJobLogs } from '../../api/ops'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const filters = reactive({
  jobCode: '',
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
    const res = await listJobLogs({
      page: page.value,
      size: size.value,
      jobCode: filters.jobCode || undefined,
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
  filters.jobCode = ''
  filters.status = ''
  page.value = 1
  void load()
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">任务日志</h2>
        <p class="muted">查看定时任务的执行记录与状态。</p>
      </div>
    </div>

    <el-card shadow="never" class="oa-page__filters">
      <el-form inline @submit.prevent="onSearch">
        <el-form-item label="任务编码">
          <el-input v-model="filters.jobCode" placeholder="任务编码" clearable style="width: 180px" />
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
        <el-table-column prop="jobCode" label="任务编码" width="160" />
        <el-table-column prop="jobName" label="任务名" min-width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(String(row.status))">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" min-width="200" show-overflow-tooltip />
        <el-table-column label="执行时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.executedAt) }}</template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column prop="successCount" label="成功" width="80" />
        <el-table-column prop="failCount" label="失败" width="80" />
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
