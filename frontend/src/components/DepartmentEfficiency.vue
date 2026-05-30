<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElSelect, ElOption, ElStatistic } from 'element-plus'
import { Document, Plus, Delete, View, Warning, UploadFilled, Select, Clock } from '@element-plus/icons-vue'

interface EfficiencyData {
  deptId: number
  period: string
  submissionCount: number
  completedCount: number
  completionRate: string
  avgDurationHours: string
}

const loading = ref(false)
const period = ref(30)
const efficiency = ref<EfficiencyData | null>(null)

async function loadEfficiency() {
  loading.value = true
  try {
    const response = await fetch(`/api/workflow/analytics/efficiency?period=${period.value}`)
    const data = await response.json()
    efficiency.value = data.data
  } catch (e) {
    console.error('Failed to load efficiency:', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadEfficiency)
</script>

<template>
  <div class="department-efficiency" v-loading="loading">
    <div class="efficiency-header">
      <el-icon><Document /></el-icon>
      <span>部门效率统计</span>
      <el-select v-model="period" size="small" style="width: 120px; margin-left: auto" @change="loadEfficiency">
        <el-option :value="7" label="近7天" />
        <el-option :value="30" label="近30天" />
        <el-option :value="90" label="近90天" />
      </el-select>
    </div>

    <div v-if="efficiency" class="efficiency-stats">
      <div class="stat-card">
        <el-icon :size="24" color="#409EFF"><Document /></el-icon>
        <div class="stat-info">
          <div class="stat-value">{{ efficiency.submissionCount }}</div>
          <div class="stat-label">提交总数</div>
        </div>
      </div>

      <div class="stat-card">
        <el-icon :size="24" color="#67C23A"><Select /></el-icon>
        <div class="stat-info">
          <div class="stat-value">{{ efficiency.completedCount }}</div>
          <div class="stat-label">完成总数</div>
        </div>
      </div>

      <div class="stat-card">
        <el-icon :size="24" color="#E6A23C"><Document /></el-icon>
        <div class="stat-info">
          <div class="stat-value">{{ efficiency.completionRate }}</div>
          <div class="stat-label">完成率</div>
        </div>
      </div>

      <div class="stat-card">
        <el-icon :size="24" color="#F56C6C"><Clock /></el-icon>
        <div class="stat-info">
          <div class="stat-value">{{ efficiency.avgDurationHours }}h</div>
          <div class="stat-label">平均耗时</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.department-efficiency {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.efficiency-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 16px;
}

.efficiency-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: white;
  border-radius: 8px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
