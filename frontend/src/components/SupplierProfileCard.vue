<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTag, ElStatistic } from 'element-plus'
import { Shop, Money, Calendar, TrendCharts } from '@element-plus/icons-vue'

interface SupplierProfile {
  supplierName: string
  totalOrders: number
  totalAmount: number
  avgOrderAmount: number
  lastOrderDate: string
  monthlyOrderFrequency: number
  activityLevel: string
}

const props = defineProps<{
  supplierName: string
}>()

const loading = ref(false)
const profile = ref<SupplierProfile | null>(null)

async function loadProfile() {
  loading.value = true
  try {
    const response = await fetch(`/api/suppliers/profile?name=${encodeURIComponent(props.supplierName)}`)
    const data = await response.json()
    profile.value = data.data
  } catch (e) {
    console.error('Failed to load supplier profile:', e)
  } finally {
    loading.value = false
  }
}

function getActivityLevelType(level: string): 'success' | 'warning' | 'info' {
  switch (level) {
    case 'HIGH': return 'success'
    case 'MEDIUM': return 'warning'
    default: return 'info'
  }
}

function getActivityLevelLabel(level: string): string {
  switch (level) {
    case 'HIGH': return '活跃'
    case 'MEDIUM': return '一般'
    default: return '不活跃'
  }
}

function formatAmount(amount: number): string {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
  }).format(amount)
}

onMounted(loadProfile)
</script>

<template>
  <div class="supplier-profile-card" v-loading="loading">
    <div v-if="!profile && !loading" class="empty-hint">
      暂无供应商数据
    </div>

    <div v-else-if="profile" class="profile-content">
      <div class="profile-header">
        <el-icon :size="24"><Shop /></el-icon>
        <div class="profile-title">
          <h3>{{ profile.supplierName }}</h3>
          <el-tag :type="getActivityLevelType(profile.activityLevel)" size="small">
            {{ getActivityLevelLabel(profile.activityLevel) }}
          </el-tag>
        </div>
      </div>

      <div class="profile-stats">
        <div class="stat-item">
          <div class="stat-value">{{ profile.totalOrders }}</div>
          <div class="stat-label">总订单数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ formatAmount(profile.totalAmount) }}</div>
          <div class="stat-label">总金额</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ formatAmount(profile.avgOrderAmount) }}</div>
          <div class="stat-label">平均订单</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ profile.monthlyOrderFrequency?.toFixed(1) || '-' }}</div>
          <div class="stat-label">月均订单</div>
        </div>
      </div>

      <div class="profile-meta">
        <span class="meta-item">
          <el-icon><Calendar /></el-icon>
          最近下单：{{ profile.lastOrderDate ? new Date(profile.lastOrderDate).toLocaleDateString('zh-CN') : '-' }}
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.supplier-profile-card {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.profile-title h3 {
  margin: 0 0 4px 0;
  font-size: 16px;
}

.profile-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stat-item {
  text-align: center;
  padding: 12px;
  background: white;
  border-radius: 8px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-color-primary);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.profile-meta {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.empty-hint {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 20px;
}
</style>
