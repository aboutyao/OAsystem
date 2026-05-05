<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowEfficiency } from '../../api/reports'
import type { JsonObject } from '../../api/types'

const loading = ref(false)
const data = ref<JsonObject | null>(null)
const filter = reactive({ from: '', to: '' })

async function load() {
  loading.value = true
  try {
    data.value = await workflowEfficiency({
      from: filter.from || undefined,
      to: filter.to || undefined,
    })
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
        <h2 class="oa-page__title">流程效率</h2>
        <p class="muted">流程实例数量、状态分布、平均审批时长（小时）。</p>
      </div>
    </div>

    <el-card shadow="never" style="margin-bottom: 12px">
      <el-form inline>
        <el-form-item label="起始">
          <el-date-picker v-model="filter.from" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="结束">
          <el-date-picker v-model="filter.to" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <template v-if="data">
      <div class="report-cards">
        <el-card shadow="never">
          <div class="metric">
            <div class="metric__label">实例总数</div>
            <div class="metric__value">{{ data.totalInstances }}</div>
          </div>
        </el-card>
        <el-card shadow="never">
          <div class="metric">
            <div class="metric__label">审批中</div>
            <div class="metric__value">{{ data.approving }}</div>
          </div>
        </el-card>
        <el-card shadow="never">
          <div class="metric">
            <div class="metric__label">已通过</div>
            <div class="metric__value">{{ data.approved }}</div>
          </div>
        </el-card>
        <el-card shadow="never">
          <div class="metric">
            <div class="metric__label">已驳回</div>
            <div class="metric__value">{{ data.rejected }}</div>
          </div>
        </el-card>
        <el-card shadow="never">
          <div class="metric">
            <div class="metric__label">平均审批时长（小时）</div>
            <div class="metric__value">{{ data.avgHours }}</div>
          </div>
        </el-card>
      </div>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>按业务类型分布</template>
        <el-table :data="data.byBusinessType as JsonObject[]" stripe>
          <el-table-column prop="businessType" label="业务" min-width="180" />
          <el-table-column prop="count" label="数量" width="120" />
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
