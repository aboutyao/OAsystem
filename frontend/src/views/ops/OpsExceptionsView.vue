<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listExceptions } from '../../api/ops'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const filters = reactive({
  severity: '',
})

function severityType(severity: string): 'success' | 'info' | 'warning' | 'danger' {
  switch (severity) {
    case 'LOW':
      return 'info'
    case 'MEDIUM':
      return 'warning'
    case 'HIGH':
      return 'danger'
    default:
      return 'danger'
  }
}

async function load() {
  loading.value = true
  try {
    const res = await listExceptions({
      page: page.value,
      size: size.value,
      severity: filters.severity || undefined,
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
  filters.severity = ''
  page.value = 1
  void load()
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">异常中心</h2>
        <p class="muted">查看系统异常记录，按严重度筛选。</p>
      </div>
    </div>

    <el-card shadow="never" class="oa-page__filters">
      <el-form inline @submit.prevent="onSearch">
        <el-form-item label="严重度">
          <el-select v-model="filters.severity" clearable placeholder="全部" style="width: 140px">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
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
        <el-table-column prop="severity" label="严重度" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="severityType(String(row.severity))">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="异常消息" min-width="260" show-overflow-tooltip />
        <el-table-column prop="exceptionClass" label="异常类型" width="220" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" width="160" />
        <el-table-column prop="requestUri" label="请求" min-width="200" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.occurredAt) }}</template>
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
