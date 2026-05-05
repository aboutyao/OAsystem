<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { userSummary } from '../../api/reports'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const data = ref<JsonObject | null>(null)

async function load() {
  loading.value = true
  try {
    data.value = await userSummary()
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
        <h2 class="oa-page__title">用户与组织</h2>
      </div>
      <div class="oa-page__actions">
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <template v-if="data">
      <div class="report-cards">
        <el-card shadow="never"><div class="metric"><div class="metric__label">用户总数</div><div class="metric__value">{{ data.totalUsers }}</div></div></el-card>
        <el-card shadow="never"><div class="metric"><div class="metric__label">启用用户</div><div class="metric__value">{{ data.activeUsers }}</div></div></el-card>
        <el-card shadow="never"><div class="metric"><div class="metric__label">部门数</div><div class="metric__value">{{ data.totalDepts }}</div></div></el-card>
      </div>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>按部门分布</template>
        <el-table :data="data.byDept as JsonObject[]" stripe>
          <el-table-column prop="deptId" label="部门ID" width="100" />
          <el-table-column prop="deptName" label="部门" min-width="180" />
          <el-table-column prop="count" label="人数" width="120" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.report-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; }
.metric__label { color: var(--el-text-color-secondary); font-size: 13px; }
.metric__value { font-size: 28px; font-weight: 600; margin-top: 4px; }
</style>
