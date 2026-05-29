<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { TrendCharts, DataAnalysis } from '@element-plus/icons-vue'

const loading = ref(false)
const prediction = ref<any>(null)
const trend = ref<any[]>([])

async function loadPrediction() {
  loading.value = true
  try {
    const currentYear = new Date().getFullYear()
    const currentQuarter = Math.ceil((new Date().getMonth() + 1) / 3)
    const nextQuarter = currentQuarter < 4 ? currentQuarter + 1 : 1
    const nextYear = currentQuarter < 4 ? currentYear : currentYear + 1

    const [predRes, trendRes] = await Promise.all([
      fetch(`/api/reports/cost-prediction/quarterly?year=${nextYear}&quarter=${nextQuarter}`),
      fetch(`/api/reports/cost-prediction/trend?year=${currentYear}&months=12`),
    ])

    const predData = await predRes.json()
    const trendData = await trendRes.json()

    prediction.value = predData.data
    trend.value = trendData.data || []
  } catch (e) {
    console.error('Failed to load prediction:', e)
  } finally {
    loading.value = false
  }
}

function formatAmount(amount: number): string {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
  }).format(amount)
}

onMounted(loadPrediction)
</script>

<template>
  <div class="cost-prediction-chart" v-loading="loading">
    <div class="chart-header">
      <el-icon><TrendCharts /></el-icon>
      <span>成本预测</span>
    </div>

    <!-- 预测概览 -->
    <div v-if="prediction" class="prediction-overview">
      <div class="prediction-card">
        <div class="card-label">历史季度均值</div>
        <div class="card-value">{{ formatAmount(prediction.historicalAvgQuarterly) }}</div>
      </div>
      <div class="prediction-card highlight">
        <div class="card-label">下季度预测</div>
        <div class="card-value">{{ formatAmount(prediction.predictedQuarterly) }}</div>
      </div>
      <div class="prediction-card">
        <div class="card-label">预测置信度</div>
        <div class="card-value">{{ prediction.confidenceLevel }}</div>
      </div>
    </div>

    <!-- 趋势图 -->
    <div v-if="trend.length > 0" class="trend-chart">
      <h4>成本趋势</h4>
      <div class="chart-bars">
        <div v-for="item in trend" :key="item.month" class="chart-bar-item">
          <div class="bar-label">{{ item.month }}</div>
          <div class="bar-track">
            <div class="bar-fill purchase" :style="{ width: `${(item.purchaseAmount / 100000) * 100}%` }"></div>
            <div class="bar-fill expense" :style="{ width: `${(item.expenseAmount / 100000) * 100}%` }"></div>
          </div>
          <div class="bar-value">{{ formatAmount(item.totalAmount) }}</div>
        </div>
      </div>
      <div class="chart-legend">
        <span class="legend-item"><span class="legend-color purchase"></span>采购</span>
        <span class="legend-item"><span class="legend-color expense"></span>报销</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cost-prediction-chart {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.chart-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 16px;
}

.prediction-overview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.prediction-card {
  padding: 12px;
  background: white;
  border-radius: 8px;
  text-align: center;
}

.prediction-card.highlight {
  background: var(--el-color-primary-light-9);
}

.card-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.card-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.trend-chart h4 {
  font-size: 14px;
  margin-bottom: 12px;
}

.chart-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chart-bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bar-label {
  width: 40px;
  font-size: 12px;
  text-align: right;
  color: var(--el-text-color-secondary);
}

.bar-track {
  flex: 1;
  height: 20px;
  background: white;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
}

.bar-fill {
  height: 100%;
  transition: width 0.3s ease;
}

.bar-fill.purchase {
  background: var(--el-color-primary);
}

.bar-fill.expense {
  background: var(--el-color-warning);
}

.bar-value {
  width: 80px;
  font-size: 12px;
  text-align: right;
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 12px;
  font-size: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-color.purchase { background: var(--el-color-primary); }
.legend-color.expense { background: var(--el-color-warning); }
</style>
