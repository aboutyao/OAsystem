<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElStatistic } from 'element-plus'
import { Database, Document, User, Money } from '@element-plus/icons-vue'

const loading = ref(false)
const stats = ref<Record<string, number>>({})

async function loadStats() {
  loading.value = true
  try {
    const response = await fetch('/api/data-lifecycle/statistics')
    const data = await response.json()
    stats.value = data.data || {}
  } catch (e) {
    console.error('Failed to load stats:', e)
  } finally {
    loading.value = false
  }
}

function formatNumber(num: number): string {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toLocaleString()
}

onMounted(loadStats)
</script>

<template>
  <div class="data-lifecycle-stats" v-loading="loading">
    <div class="stats-header">
      <el-icon><Database /></el-icon>
      <span>数据统计</span>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <el-icon :size="20" color="#409EFF"><User /></el-icon>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(stats.users || 0) }}</div>
          <div class="stat-label">用户数</div>
        </div>
      </div>

      <div class="stat-card">
        <el-icon :size="20" color="#67C23A"><Document /></el-icon>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(stats.leaves || 0) }}</div>
          <div class="stat-label">请假记录</div>
        </div>
      </div>

      <div class="stat-card">
        <el-icon :size="20" color="#E6A23C"><Money /></el-icon>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(stats.expenses || 0) }}</div>
          <div class="stat-label">报销记录</div>
        </div>
      </div>

      <div class="stat-card">
        <el-icon :size="20" color="#909399"><Document /></el-icon>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(stats.contracts || 0) }}</div>
          <div class="stat-label">合同数</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.data-lifecycle-stats {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.stats-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
