<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTag } from 'element-plus'
import { User, Warning } from '@element-plus/icons-vue'

interface WorkloadItem {
  userId: number
  userName: string
  deptName: string
  pendingCount: number
  avgResponseHours: number
  totalCompleted: number
  workloadLevel: string
}

const loading = ref(false)
const workloadData = ref<WorkloadItem[]>([])
const viewMode = ref<'user' | 'department'>('user')

async function loadWorkload() {
  loading.value = true
  try {
    const url = viewMode.value === 'user' ? '/api/analytics/workload/ranking' : '/api/analytics/workload/department'
    const response = await fetch(url)
    const data = await response.json()
    workloadData.value = data.data || []
  } catch (e) {
    console.error('Failed to load workload:', e)
  } finally {
    loading.value = false
  }
}

function getLevelType(level: string): 'danger' | 'warning' | 'success' {
  switch (level) {
    case 'HIGH': return 'danger'
    case 'MEDIUM': return 'warning'
    default: return 'success'
  }
}

function getLevelLabel(level: string): string {
  switch (level) {
    case 'HIGH': return '过载'
    case 'MEDIUM': return '一般'
    default: return '正常'
  }
}

function formatHours(hours: number): string {
  if (hours < 1) return '<1小时'
  if (hours < 24) return Math.round(hours) + '小时'
  return Math.round(hours / 24) + '天'
}

onMounted(loadWorkload)
</script>

<template>
  <div class="workload-analysis">
    <div class="analysis-header">
      <h3>工作负荷分析</h3>
      <el-radio-group v-model="viewMode" size="small" @change="loadWorkload">
        <el-radio-button value="user">按人员</el-radio-button>
        <el-radio-button value="department">按部门</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="workload-list">
      <div v-if="workloadData.length === 0 && !loading" class="empty-hint">
        暂无数据
      </div>

      <div v-for="item in workloadData" :key="item.userId" class="workload-item" :class="{ 'is-overload': item.workloadLevel === 'HIGH' }">
        <div class="user-info">
          <el-avatar :size="36">{{ item.userName?.charAt(0) }}</el-avatar>
          <div class="user-detail">
            <div class="user-name">{{ item.userName }}</div>
            <div class="user-dept">{{ item.deptName }}</div>
          </div>
        </div>

        <div class="workload-stats">
          <div class="stat">
            <span class="stat-value">{{ item.pendingCount }}</span>
            <span class="stat-label">待办</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ formatHours(item.avgResponseHours) }}</span>
            <span class="stat-label">平均响应</span>
          </div>
          <div class="stat">
            <span class="stat-value">{{ item.totalCompleted }}</span>
            <span class="stat-label">已完成</span>
          </div>
        </div>

        <el-tag :type="getLevelType(item.workloadLevel)" size="small">
          {{ getLevelLabel(item.workloadLevel) }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<style scoped>
.workload-analysis {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.analysis-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.analysis-header h3 {
  margin: 0;
}

.workload-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.workload-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  transition: all 0.2s;
}

.workload-item.is-overload {
  border-left: 3px solid #F56C6C;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 150px;
}

.user-name {
  font-weight: 500;
}

.user-dept {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.workload-stats {
  display: flex;
  gap: 24px;
  flex: 1;
}

.stat {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.empty-hint {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 20px;
}
</style>
