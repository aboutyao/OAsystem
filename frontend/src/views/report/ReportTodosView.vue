<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { todoSummary } from '../../api/reports'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const data = ref<JsonObject | null>(null)

async function load() {
  loading.value = true
  try {
    data.value = await todoSummary()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void load()
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">待办统计</h2>
        <p class="muted">系统中待处理任务总览，及每位处理人当前的待办量。</p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <template v-if="data">
      <div class="report-cards">
        <el-card shadow="never"><div class="metric"><div class="metric__label">待办</div><div class="metric__value">{{ data.pending }}</div></div></el-card>
        <el-card shadow="never"><div class="metric"><div class="metric__label">已办</div><div class="metric__value">{{ data.completed }}</div></div></el-card>
        <el-card shadow="never"><div class="metric"><div class="metric__label">超时</div><div class="metric__value">{{ data.timeout }}</div></div></el-card>
      </div>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>Top 10 待办处理人</template>
        <el-table :data="data.topAssignees as JsonObject[]" stripe>
          <el-table-column prop="assigneeId" label="用户ID" width="120" />
          <el-table-column prop="name" label="姓名" min-width="160" />
          <el-table-column prop="count" label="待办数" width="120" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.report-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}
.metric__label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.metric__value {
  font-size: 28px;
  font-weight: 600;
  margin-top: 4px;
}
</style>
