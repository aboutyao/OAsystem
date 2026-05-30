<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Plus, Delete, View, Warning, UploadFilled } from '@element-plus/icons-vue'
import { getBudgetWarnings, type BudgetWarning } from '../api/budgets'

const warnings = ref<BudgetWarning[]>([])
const loading = ref(false)
const dismissed = ref(false)

async function loadWarnings() {
  loading.value = true
  try {
    warnings.value = await getBudgetWarnings()
  } catch (e) {
    console.error('Failed to load budget warnings:', e)
  } finally {
    loading.value = false
  }
}

function dismiss() {
  dismissed.value = true
}

function formatAmount(amount: number): string {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
  }).format(amount)
}

onMounted(() => loadWarnings())
</script>

<template>
  <div v-if="warnings.length > 0 && !dismissed" class="budget-warning-banner">
    <div class="warning-icon">
      <el-icon :size="20"><Warning /></el-icon>
    </div>
    <div class="warning-content">
      <div class="warning-title">预算预警</div>
      <div class="warning-list">
        <div v-for="warning in warnings.slice(0, 3)" :key="warning.budgetId" class="warning-item">
          <el-tag
            :type="warning.alertType === 'OVER_BUDGET' ? 'danger' : 'warning'"
            size="small"
            effect="light"
          >
            {{ warning.alertType === 'OVER_BUDGET' ? '超支' : '预警' }}
          </el-tag>
          <span class="warning-message">{{ warning.message }}</span>
        </div>
        <div v-if="warnings.length > 3" class="warning-more">
          还有 {{ warnings.length - 3 }} 条预警...
        </div>
      </div>
    </div>
    <el-button text type="primary" @click="dismiss">忽略</el-button>
  </div>
</template>

<style scoped>
.budget-warning-banner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 20px;
  background: linear-gradient(135deg, #fff7e6, #fff1cc);
  border: 1px solid #ffd666;
  border-radius: var(--oa-radius-md);
  margin-bottom: 16px;
}

.warning-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #faad14;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.warning-content {
  flex: 1;
  min-width: 0;
}

.warning-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--oa-text-primary);
  margin-bottom: 8px;
}

.warning-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.warning-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.warning-message {
  color: var(--oa-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.warning-more {
  font-size: 12px;
  color: var(--oa-text-muted);
  margin-top: 4px;
}
</style>
