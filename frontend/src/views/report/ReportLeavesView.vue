<script setup lang="ts">
import { reactive, ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { leaveSummary } from '../../api/reports'
import type { JsonObject } from '../../api/types'
import { Document, Select, Calendar } from '@element-plus/icons-vue'

const loading = ref(false)
const data = ref<JsonObject | null>(null)
const filter = reactive({ from: '', to: '' })

const chartRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const COLORS = ['#4f46e5', '#10b981', '#f59e0b', '#ef4444', '#6366f1', '#8b5cf6', '#ec4899', '#14b8a6']

const LEAVE_TYPE_LABELS: Record<string, string> = {
  ANNUAL: '年假',
  SICK: '病假',
  PERSONAL: '事假',
  MATERNITY: '产假',
  PATERNITY: '陪产假',
  MARRIAGE: '婚假',
  BEREAVEMENT: '丧假',
}

function leaveTypeLabel(code: unknown): string {
  const key = String(code ?? '')
  return LEAVE_TYPE_LABELS[key] ?? key
}

function initChart() {
  if (!chartRef.value || !data.value) return
  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(chartRef.value)

  const byLeaveType = (data.value.byLeaveType as JsonObject[]) || []
  const pieData = byLeaveType.map((item) => ({
    name: leaveTypeLabel(item.leaveType),
    value: item.count as number,
  }))

  chartInstance.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 次 ({d}%)',
    },
    legend: {
      bottom: 0,
      left: 'center',
    },
    color: COLORS,
    series: [
      {
        name: '请假类型',
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
          formatter: '{b}\n{c} 次',
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
    data.value = await leaveSummary({
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
        <h2 class="oa-page__title">请假统计</h2>
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
          <div class="report-stat-card__label">请假总数</div>
        </div>
        <div class="report-stat-card">
          <div class="report-stat-card__icon" style="background: #d1fae5; color: #10b981">
            <el-icon><Select /></el-icon>
          </div>
          <div class="report-stat-card__value">{{ data.approvedCount }}</div>
          <div class="report-stat-card__label">已批准</div>
        </div>
        <div class="report-stat-card">
          <div class="report-stat-card__icon" style="background: #fef3c7; color: #f59e0b">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="report-stat-card__value">{{ data.totalDays }}</div>
          <div class="report-stat-card__label">已批准总天数</div>
        </div>
      </div>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>按请假类型分布</template>
        <div ref="chartRef" style="width: 100%; height: 320px"></div>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>详细数据</template>
        <el-table :data="data.byLeaveType as JsonObject[]" stripe>
          <el-table-column label="类型" min-width="160">
            <template #default="{ row }">{{ leaveTypeLabel(row.leaveType) }}</template>
          </el-table-column>
          <el-table-column prop="count" label="次数" width="120" />
          <el-table-column prop="totalDays" label="总天数" width="120" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

