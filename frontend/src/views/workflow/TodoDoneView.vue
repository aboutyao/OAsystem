<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { doneTasks } from '../../api/workflow'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const router = useRouter()
const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

async function load() {
  loading.value = true
  try {
    const res = await doneTasks(page.value, size.value)
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
        <h2 class="oa-page__title">我的已办</h2>
        <p class="muted">已处理或已取消的审批任务。</p>
      </div>
      <el-button @click="router.push('/todos')">返回待办</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="taskId" label="任务号" width="100" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="nodeName" label="节点" width="140" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="完成时间" min-width="160">
          <template #default="{ row }">{{ formatDisplayDateTime(row.completedAt) }}</template>
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
