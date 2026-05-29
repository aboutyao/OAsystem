<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElProgress } from 'element-plus'
import { TrendCharts, Warning, CircleCheck } from '@element-plus/icons-vue'

interface DepartmentHealth {
  deptId: number
  deptName: string
  totalScore: number
  healthLevel: string
  approvalScore: number
  budgetScore: number
  satisfactionScore: number
  productivityScore: number
  suggestions: string[]
}

const loading = ref(false)
const departments = ref<DepartmentHealth[]>([])

async function loadHealthData() {
  loading.value = true
  try {
    const response = await fetch('/api/analytics/department-health/ranking')
    const data = await response.json()
    departments.value = data.data || []
  } catch (e) {
    console.error('Failed to load health data:', e)
  } finally {
    loading.value = false
  }
}

function getHealthColor(score: number): string {
  if (score >= 80) return '#67C23A'
  if (score >= 60) return '#E6A23C'
  return '#F56C6C'
}

function getHealthIcon(score: number) {
  if (score >= 80) return CircleCheck
  return Warning
}

onMounted(loadHealthData)
</script>

<template>
  <div class="department-health-dashboard">
    <div class="dashboard-header">
      <h3>部门健康度</h3>
    </div>

    <div v-loading="loading" class="health-list">
      <div v-if="departments.length === 0 && !loading" class="empty-hint">
        暂无数据
      </div>

      <div v-for="(dept, index) in departments" :key="dept.deptId" class="health-card">
        <div class="card-header">
          <div class="rank">#{{ index + 1 }}</div>
          <div class="dept-name">{{ dept.deptName }}</div>
          <div class="score" :style="{ color: getHealthColor(dept.totalScore) }">
            {{ dept.totalScore }}
          </div>
        </div>

        <div class="score-bars">
          <div class="score-item">
            <span class="score-label">审批效率</span>
            <el-progress :percentage="dept.approvalScore" :color="getHealthColor(dept.approvalScore)" :stroke-width="6" />
          </div>
          <div class="score-item">
            <span class="score-label">预算使用</span>
            <el-progress :percentage="dept.budgetScore" :color="getHealthColor(dept.budgetScore)" :stroke-width="6" />
          </div>
          <div class="score-item">
            <span class="score-label">员工满意度</span>
            <el-progress :percentage="dept.satisfactionScore" :color="getHealthColor(dept.satisfactionScore)" :stroke-width="6" />
          </div>
          <div class="score-item">
            <span class="score-label">产出效率</span>
            <el-progress :percentage="dept.productivityScore" :color="getHealthColor(dept.productivityScore)" :stroke-width="6" />
          </div>
        </div>

        <div v-if="dept.suggestions.length > 0" class="suggestions">
          <div v-for="(suggestion, i) in dept.suggestions" :key="i" class="suggestion-item">
            <el-icon><Warning /></el-icon>
            <span>{{ suggestion }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.department-health-dashboard {
  padding: 16px;
}

.dashboard-header {
  margin-bottom: 16px;
}

.dashboard-header h3 {
  margin: 0;
}

.health-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.health-card {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.rank {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--el-color-primary-light-9);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: var(--el-color-primary);
}

.dept-name {
  flex: 1;
  font-weight: 500;
}

.score {
  font-size: 24px;
  font-weight: bold;
}

.score-bars {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.score-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.score-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.suggestions {
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.empty-hint {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 20px;
}
</style>
