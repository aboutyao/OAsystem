<script setup lang="ts">
import { reactive, ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { contractSummary } from '../../api/reports'
import type { JsonObject } from '../../api/types'
import { Document, Money, Clock } from '@element-plus/icons-vue'

const loading = ref(false)
const data = ref<JsonObject | null>(null)
const filter = reactive({ from: '', to: '' })

const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const COLORS = ['#4f46e5', '#10b981', '#f59e0b', '#ef4444', '#6366f1', '#8b5cf6', '#ec4899', '#14b8a6']

const CONTRACT_TYPE_LABELS: Record<string, string> = {
  SALES: '销售合同',
  PURCHASE: '采购合同',
  SERVICE: '服务合同',
  LEASE: '租赁合同',
  OTHER: '其他',
}

function contractTypeLabel(code: unknown): string {
  const key = String(code ?? '')
  return CONTRACT_TYPE_LABELS[key] ?? key
}

function initChart() {
  if (!chartRef.value || !data.value) return
  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(chartRef.value)

  const byType = (data.value.byType as JsonObject[]) || []
  const pieData = byType.map((item) => ({
    name: contractTypeLabel(item.contractType),
    value: item.count as number,
  }))

  chartInstance.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 份 ({d}%)',
    },
    legend: {
      bottom: 0,
      left: 'center',
    },
    color: COLORS,
    series: [
      {
        name: '合同类型',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: true,
          formatter: '{b}\n{c} 份',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold',
          },
        },
        data: pieData,
      },
    ],
  })
}

function handleResize() {
  chartInstance?.resize()
}

async function load() {
  loading.value = true
  try {
    data.value = await contractSummary({
      from: filter.from || undefined,
      to: filter.to || undefined,
    })
    await nextTick()
    initChart()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})

void load()
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">合同统计</h2>
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
        <div class="report-stat-card">
          <div class="report-stat-card__icon" style="background: var(--oa-primary-lighter); color: var(--oa-primary)">
            <el-icon><Document /></el-icon>
          </div>
          <div class="report-stat-card__value">{{ data.contractCount }}</div>
          <div class="report-stat-card__label">合同数</div>
        </div>
        <div class="report-stat-card">
          <div class="report-stat-card__icon" style="background: #d1fae5; color: #10b981">
            <el-icon><Money /></el-icon>
          </div>
          <div class="report-stat-card__value">&yen;{{ data.totalAmount }}</div>
          <div class="report-stat-card__label">合同总金额</div>
        </div>
        <div class="report-stat-card">
          <div class="report-stat-card__icon" style="background: #fee2e2; color: #ef4444">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="report-stat-card__value">{{ data.expiringSoon30Days }}</div>
          <div class="report-stat-card__label">30 天内到期</div>
        </div>
      </div>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>按合同类型分布</template>
        <div ref="chartRef" style="width: 100%; height: 320px"></div>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>详细数据</template>
        <el-table :data="data.byType as JsonObject[]" stripe>
          <el-table-column label="类型" min-width="160">
            <template #default="{ row }">{{ contractTypeLabel(row.contractType) }}</template>
          </el-table-column>
          <el-table-column prop="count" label="数量" width="120" />
          <el-table-column prop="amount" label="总金额" width="160" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.report-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.report-stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s;
}

.report-stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.report-stat-card__icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-bottom: 12px;
}

.report-stat-card__value {
  font-size: 28px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.2;
}

.report-stat-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
</style>
