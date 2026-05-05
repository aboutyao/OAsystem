<script setup lang="ts">
import { ref } from 'vue'
import { listExceptions } from '../../api/workflow'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

async function load() {
  loading.value = true
  try {
    const res = await listExceptions(page.value, size.value)
    rows.value = res.items
    total.value = Number(res.total)
  } finally {
    loading.value = false
  }
}

void load()
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">流程异常</h2>
        <p class="muted">检测审批中但当前没有待办，或已结束但未记录结束时间的实例，便于人工介入。</p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="wfInstanceId" label="#" width="72" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="businessType" label="业务" width="110" />
        <el-table-column prop="starterName" label="发起人" width="120" />
        <el-table-column prop="currentNodeName" label="当前节点" min-width="120" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="reason" label="异常类型" width="180">
          <template #default="{ row }">
            <el-tag size="small" type="danger">
              {{ row.reason === 'NO_PENDING_TASK' ? '审批中无待办' : '已结束未记录结束时间' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发起时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.startedAt) }}</template>
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
