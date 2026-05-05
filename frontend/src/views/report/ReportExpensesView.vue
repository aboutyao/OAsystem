<script setup lang="ts">
import { reactive, ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { expenseSummary } from '../../api/reports'
import type { JsonObject } from '../../api/types'
import { Document, Money, CreditCard } from '@element-plus/icons-vue'

const loading = ref(false)
const data = ref<JsonObject | null>(null)
const filter = reactive({ from: '', to: '' })

const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const COLORS = ['#4f46e5', '#10b981', '#f59e0b', '#ef4444', '#6366f1', '#8b5cf6', '#ec4899', '#14b8a6']

const EXPENSE_CATEGORY_LABELS: Record<string, string> = {
  TRAVEL: '差旅',
  MEAL: '餐饮',
  OFFICE: '办公用品',
  TRANSPORT: '交通',
  OTHER: '其他',
}

function expenseCategoryLabel(code: unknown): string {
  const key = String(code ?? '')
  return EXPENSE_CATEGORY_LABELS[key] ?? key
}

function initChart() {
  if (!chartRef.value || !data.value) return
  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(chartRef.value)

  const byCategory = (data.value.byCategory as JsonObject[]) || []
  const categories = byCategory.map((item) => expenseCategoryLabel(item.category))
  const amounts = byCategory.map((item) => (item.totalAmount ?? item.amount ?? 0) as number)

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
      formatter: (params: any) => {
        const p = params[0]
        return `${p.name}<br/>金额: ¥${p.value.toLocaleString()}`
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '8%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: categories,
      axisLabel: {
        rotate: categories.length > 4 ? 30 : 0,
      },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (val: number) => `¥${val.toLocaleString()}`,
      },
    },
    color: COLORS,
    series: [
      {
        name: '金额',
        type: 'bar',
        barWidth: '50%',
        data: amounts,
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.2)',
          },
        },
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
    data.value = await expenseSummary({
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
        <h2 class="oa-page__title">报销统计</h2>
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
          <div class="report-stat-card__value">{{ data.totalCount }}</div>
          <div class="report-stat-card__label">报销总数</div>
        </div>
        <div class="report-stat-card">
          <div class="report-stat-card__icon" style="background: #d1fae5; color: #10b981">
            <el-icon><Money /></el-icon>
          </div>
          <div class="report-stat-card__value">&yen;{{ data.totalAmount }}</div>
          <div class="report-stat-card__label">已批准金额</div>
        </div>
        <div class="report-stat-card">
          <div class="report-stat-card__icon" style="background: #fef3c7; color: #f59e0b">
            <el-icon><CreditCard /></el-icon>
          </div>
          <div class="report-stat-card__value">&yen;{{ data.paidAmount }}</div>
          <div class="report-stat-card__label">已支付金额</div>
        </div>
      </div>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>按费用类别分布</template>
        <div ref="chartRef" style="width: 100%; height: 320px"></div>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>详细数据</template>
        <el-table :data="data.byCategory as JsonObject[]" stripe>
          <el-table-column label="费用类别" min-width="160">
            <template #default="{ row }">{{ expenseCategoryLabel(row.category) }}</template>
          </el-table-column>
          <el-table-column prop="count" label="笔数" width="120" />
          <el-table-column prop="amount" label="金额" width="160" />
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
