<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { listOperationLogs } from '../../api/audit'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const filters = reactive({
  businessType: '',
  result: '',
  operatorId: '',
})

async function load() {
  loading.value = true
  try {
    const operatorId = filters.operatorId ? Number(filters.operatorId) : undefined
    const res = await listOperationLogs({
      page: page.value,
      size: size.value,
      businessType: filters.businessType || undefined,
      result: filters.result || undefined,
      operatorId: operatorId && !Number.isNaN(operatorId) ? operatorId : undefined,
    })
    rows.value = res.items
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

void load()

watch(() => filters.result, () => {
  page.value = 1
  void load()
})

function onSearch() {
  page.value = 1
  void load()
}

function onReset() {
  filters.businessType = ''
  filters.result = ''
  filters.operatorId = ''
  page.value = 1
  void load()
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">操作日志</h2>
        <p class="muted">关键业务操作落库，审计与排障。当前阶段由各业务模块按需写入。</p>
      </div>
    </div>

    <el-card shadow="never" class="oa-page__filters">
      <el-form inline @submit.prevent="onSearch">
        <el-form-item label="业务类型">
          <el-input v-model="filters.businessType" placeholder="EXPENSE / LEAVE 等" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="filters.result" clearable placeholder="全部" style="width: 140px">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人ID">
          <el-input v-model="filters.operatorId" placeholder="数字" clearable style="width: 140px" />
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
        <el-table-column prop="operatorId" label="操作人" width="100" />
        <el-table-column prop="operationType" label="类型" width="140" />
        <el-table-column prop="businessType" label="业务" width="120" />
        <el-table-column prop="businessId" label="业务ID" width="100" />
        <el-table-column prop="requestMethod" label="方法" width="80" />
        <el-table-column prop="requestUri" label="请求" min-width="240" show-overflow-tooltip />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="String(row.result) === 'SUCCESS' ? 'success' : 'danger'">
              {{ String(row.result) === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误" min-width="160" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP" width="140" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.operatedAt) }}</template>
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
