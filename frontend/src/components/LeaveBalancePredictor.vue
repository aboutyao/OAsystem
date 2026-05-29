<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning, Calendar, TrendCharts } from '@element-plus/icons-vue'
import { predictLeaveBalanceExhaustion } from '../api/oa-leaves'
import type { JsonObject } from '../api/types'

interface Prediction {
  typeCode: string
  typeName: string
  remainingDays: number
  avgMonthlyUsage: number
  predictedExhaustionDate: string | null
  monthsRemaining: number
  riskLevel: 'HIGH' | 'MEDIUM' | 'LOW'
  riskMessage: string
}

const loading = ref(false)
const predictions = ref<Prediction[]>([])

async function loadPredictions() {
  loading.value = true
  try {
    const result = await predictLeaveBalanceExhaustion()
    predictions.value = (result as JsonObject).predictions as Prediction[]
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载预测数据失败')
  } finally {
    loading.value = false
  }
}

function getRiskColor(riskLevel: string): string {
  switch (riskLevel) {
    case 'HIGH': return 'var(--el-color-danger)'
    case 'MEDIUM': return 'var(--el-color-warning)'
    default: return 'var(--el-color-success)'
  }
}

function getRiskIcon(riskLevel: string) {
  switch (riskLevel) {
    case 'HIGH': return Warning
    case 'MEDIUM': return Calendar
    default: return TrendCharts
  }
}

function formatMonth(dateStr: string | null): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return `${date.getFullYear()}年${date.getMonth() + 1}月`
}

onMounted(() => loadPredictions())
</script>

<template>
  <el-card shadow="never" class="predictor-card">
    <template #header>
      <div class="predictor-header">
        <el-icon><TrendCharts /></el-icon>
        <span>假期余额预测</span>
        <el-button link type="primary" @click="loadPredictions" :loading="loading">刷新</el-button>
      </div>
    </template>

    <div v-loading="loading" class="predictor-content">
      <div v-if="predictions.length === 0 && !loading" class="empty-hint">
        暂无数据
      </div>

      <div v-for="pred in predictions" :key="pred.typeCode" class="prediction-item">
        <div class="prediction-header">
          <span class="type-name">{{ pred.typeName }}</span>
          <el-tag
            :type="pred.riskLevel === 'HIGH' ? 'danger' : pred.riskLevel === 'MEDIUM' ? 'warning' : 'success'"
            size="small"
          >
            {{ pred.riskMessage }}
          </el-tag>
        </div>

        <div class="prediction-details">
          <div class="detail-item">
            <span class="label">剩余</span>
            <span class="value">{{ pred.remainingDays }} 天</span>
          </div>
          <div class="detail-item">
            <span class="label">月均使用</span>
            <span class="value">{{ pred.avgMonthlyUsage }} 天</span>
          </div>
          <div v-if="pred.predictedExhaustionDate" class="detail-item">
            <span class="label">预计耗尽</span>
            <span class="value" :style="{ color: getRiskColor(pred.riskLevel) }">
              {{ formatMonth(pred.predictedExhaustionDate) }}
            </span>
          </div>
        </div>

        <!-- 进度条 -->
        <el-progress
          v-if="pred.remainingDays > 0"
          :percentage="Math.min(100, Math.round((pred.monthsRemaining / 12) * 100))"
          :color="getRiskColor(pred.riskLevel)"
          :stroke-width="8"
          :show-text="false"
        />
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.predictor-card {
  margin-bottom: 16px;
}

.predictor-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.predictor-header .el-button {
  margin-left: auto;
}

.predictor-content {
  min-height: 100px;
}

.empty-hint {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 20px;
}

.prediction-item {
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.prediction-item:last-child {
  border-bottom: none;
}

.prediction-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.type-name {
  font-weight: 500;
}

.prediction-details {
  display: flex;
  gap: 24px;
  margin-bottom: 8px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-item .label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.detail-item .value {
  font-size: 14px;
  font-weight: 500;
}
</style>
