<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { listLoginLogs } from '../../api/audit'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const filters = reactive({
  username: '',
  result: '',
})

async function load() {
  loading.value = true
  try {
    const res = await listLoginLogs({
      page: page.value,
      size: size.value,
      username: filters.username || undefined,
      result: filters.result || undefined,
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
  filters.username = ''
  filters.result = ''
  page.value = 1
  void load()
}
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">登录日志</h2>
        <p class="muted">记录所有登录尝试。失败原因仅供运维定位，不暴露给前端用户。</p>
      </div>
    </div>

    <el-card shadow="never" class="oa-page__filters">
      <el-form inline @submit.prevent="onSearch">
        <el-form-item label="账号">
          <el-input v-model="filters.username" placeholder="账号关键字" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="filters.result" clearable placeholder="全部" style="width: 140px">
            <el-option label="成功" value="SUCCESS" />
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
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="username" label="账号" width="160" />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="String(row.loginResult) === 'SUCCESS' ? 'success' : 'danger'">
              {{ String(row.loginResult) === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP" width="160" />
        <el-table-column prop="userAgent" label="UA" min-width="220" show-overflow-tooltip />
        <el-table-column prop="failReason" label="失败原因" min-width="160" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDisplayDateTime(row.loggedAt) }}</template>
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
