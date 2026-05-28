<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { assetSummary } from '../../api/reports'
import type { JsonObject } from '../../api/types'
import ReportChart from '../../components/ReportChart.vue'

const loading = ref(false)
const data = ref<JsonObject | null>(null)

async function load() {
  loading.value = true
  try {
    data.value = await assetSummary()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

void load()

const statusPieOption = computed(() => {
  if (!data.value) return {}
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: [
        { value: data.value.idleCount, name: '闲置', itemStyle: { color: '#909399' } },
        { value: data.value.inUseCount, name: '在用', itemStyle: { color: '#67C23A' } },
        { value: data.value.repairingCount, name: '维修中', itemStyle: { color: '#E6A23C' } },
        { value: data.value.scrappedCount, name: '已报废', itemStyle: { color: '#F56C6C' } },
      ],
    }],
  }
})

const categoryBarOption = computed(() => {
  if (!data.value?.byCategory) return {}
  const cats = data.value.byCategory as JsonObject[]
  return {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: cats.map(c => String(c.category)), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '数量' },
    series: [{
      type: 'bar',
      data: cats.map(c => c.count),
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
        <h2 class="oa-page__title">资产统计</h2>
      </div>
      <div class="oa-page__actions">
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <template v-if="data">
      <div class="report-cards">
        <el-card shadow="never"><div class="metric"><div class="metric__label">总数</div><div class="metric__value">{{ data.assetCount }}</div></div></el-card>
        <el-card shadow="never"><div class="metric"><div class="metric__label">闲置</div><div class="metric__value">{{ data.idleCount }}</div></div></el-card>
        <el-card shadow="never"><div class="metric"><div class="metric__label">在用</div><div class="metric__value">{{ data.inUseCount }}</div></div></el-card>
        <el-card shadow="never"><div class="metric"><div class="metric__label">维修中</div><div class="metric__value">{{ data.repairingCount }}</div></div></el-card>
        <el-card shadow="never"><div class="metric"><div class="metric__label">已报废</div><div class="metric__value">{{ data.scrappedCount }}</div></div></el-card>
        <el-card shadow="never"><div class="metric"><div class="metric__label">采购总价</div><div class="metric__value">¥{{ data.totalPurchasePrice }}</div></div></el-card>
      </div>

      <div class="report-charts">
        <el-card shadow="never">
          <template #header>资产状态分布</template>
          <ReportChart :option="statusPieOption" height="280px" />
        </el-card>
        <el-card shadow="never">
          <template #header>按类别分布</template>
          <ReportChart :option="categoryBarOption" height="280px" />
        </el-card>
      </div>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>明细数据</template>
        <el-table :data="data.byCategory as JsonObject[]" stripe>
          <el-table-column prop="category" label="类别" min-width="160" />
          <el-table-column prop="count" label="数量" width="120" />
          <el-table-column prop="price" label="采购总价" width="160" />
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

