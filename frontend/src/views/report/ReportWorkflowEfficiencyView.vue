<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { workflowEfficiency } from '../../api/reports'
import type { JsonObject } from '../../api/types'
import ReportChart from '../../components/ReportChart.vue'
import { exportToCsv } from '../../utils/export'

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

function exportReport() {
  if (!data.value?.byBusinessType) return
  exportToCsv(
    data.value.byBusinessType as Record<string, unknown>[],
    `流程效率报表_${filter.from || 'all'}_${filter.to || 'all'}`,
    [{ key: 'businessType', label: '业务类型' }, { key: 'count', label: '数量' }]
  )
}

const statusPieOption = computed(() => {
  if (!data.value) return {}
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: [
        { value: data.value.approving, name: '审批中', itemStyle: { color: '#E6A23C' } },
        { value: data.value.approved, name: '已通过', itemStyle: { color: '#67C23A' } },
        { value: data.value.rejected, name: '已驳回', itemStyle: { color: '#F56C6C' } },
      ],
    }],
  }
})

const businessBarOption = computed(() => {
  if (!data.value?.byBusinessType) return {}
  const types = data.value.byBusinessType as JsonObject[]
  return {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: types.map(t => String(t.businessType)), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '数量' },
    series: [{
      type: 'bar',
      data: types.map(t => t.count),
      itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
      barMaxWidth: 40,
    }],
    grid: { left: 50, right: 20, bottom: 60, top: 30 },
  }
})
</script>

<template>
  <div class="oa-page" v-loading="loading">
    <div class="oa-page__head">
      <div>
        <h2 class="oa-page__title">流程效率</h2>
        <p class="muted">流程实例数量、状态分布、平均审批时长（小时）。</p>
      </div>
      <div class="oa-page__actions">
        <el-button @click="exportReport" :disabled="!data">导出 CSV</el-button>
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

      <div class="report-charts">
        <el-card shadow="never">
          <template #header>流程状态分布</template>
          <ReportChart :option="statusPieOption" height="280px" />
        </el-card>
        <el-card shadow="never">
          <template #header>按业务类型分布</template>
          <ReportChart :option="businessBarOption" height="280px" />
        </el-card>
      </div>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>明细数据</template>
        <el-table :data="data.byBusinessType as JsonObject[]" stripe>
          <el-table-column prop="businessType" label="业务" min-width="180" />
          <el-table-column prop="count" label="数量" width="120" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.report-charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .report-charts {
    grid-template-columns: 1fr;
  }
}
</style>

