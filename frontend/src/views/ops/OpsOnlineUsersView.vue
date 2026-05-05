<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listOnlineUsers } from '../../api/ops'
import type { JsonObject } from '../../api/types'
import { formatDisplayDateTime } from '../oa/oa-shared'

const loading = ref(false)
const rows = ref<JsonObject[]>([])
const autoRefresh = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

async function load() {
  loading.value = true
  try {
    rows.value = await listOnlineUsers()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void load()

function startAutoRefresh() {
  stopAutoRefresh()
  timer = setInterval(() => {
    void load()
  }, 30000)
}

function stopAutoRefresh() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function onAutoRefreshChange(val: boolean) {
  if (val) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

onMounted(() => {
  if (autoRefresh.value) {
    startAutoRefresh()
  }
})

onBeforeUnmount(() => {
  stopAutoRefresh()
})
</script>

<template>
  <div class="oa-page">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">在线用户</h2>
        <p class="muted">当前在线用户列表，基于最近 30 分钟活跃登录估算。</p>
      </div>
      <div class="oa-page__actions">
        <span style="margin-right: 12px; font-size: 14px; color: #606266">
          自动刷新
          <el-switch v-model="autoRefresh" @change="onAutoRefreshChange" style="margin-left: 4px" />
        </span>
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="username" label="账号" width="160" />
        <el-table-column prop="realName" label="姓名" width="140" />
        <el-table-column prop="deptName" label="部门" width="160" />
        <el-table-column label="登录时间" width="180">
          <template #default="{ row }">{{ formatDisplayDateTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP 地址" min-width="160" />
        <el-table-column prop="userAgent" label="User-Agent" min-width="260" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>
