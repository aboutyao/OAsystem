<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, WarningFilled } from '@element-plus/icons-vue'

interface ComplianceViolation {
  type: string
  message: string
}

interface ComplianceResult {
  passed: boolean
  violations: ComplianceViolation[]
}

const props = defineProps<{
  entityType: 'expense' | 'purchase'
  entityId: number
}>()

const result = ref<ComplianceResult | null>(null)
const loading = ref(false)

async function checkCompliance() {
  loading.value = true
  try {
    const response = await fetch(`/api/compliance/check/${props.entityType}/${props.entityId}`)
    const data = await response.json()
    result.value = data.data
  } catch (e) {
    console.error('Failed to check compliance:', e)
  } finally {
    loading.value = false
  }
}

function getViolationTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    'AMOUNT_EXCEEDED': '金额超标',
    'DUPLICATE_EXPENSE': '重复报销',
    'MONTHLY_EXCEEDED': '月度超标',
    'NO_APPROVAL': '未走审批',
    'CHECK_FAILED': '检查失败',
  }
  return labels[type] || type
}
</script>

<template>
  <div class="compliance-checker">
    <div class="checker-header">
      <span>合规检查</span>
      <el-button type="primary" size="small" :loading="loading" @click="checkCompliance">
        检查
      </el-button>
    </div>

    <div v-if="result" class="check-result" :class="{ 'is-passed': result.passed, 'is-failed': !result.passed }">
      <div class="result-icon">
        <el-icon v-if="result.passed" :size="24" color="#67C23A"><CircleCheck /></el-icon>
        <el-icon v-else :size="24" color="#F56C6C"><WarningFilled /></el-icon>
      </div>
      <div class="result-text">
        {{ result.passed ? '合规检查通过' : '存在合规问题' }}
      </div>
    </div>

    <div v-if="result && !result.passed && result.violations.length > 0" class="violations-list">
      <div v-for="(violation, index) in result.violations" :key="index" class="violation-item">
        <el-tag type="danger" size="small">{{ getViolationTypeLabel(violation.type) }}</el-tag>
        <span class="violation-message">{{ violation.message }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.compliance-checker {
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.checker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 500;
}

.check-result {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 12px;
}

.check-result.is-passed {
  background: var(--el-color-success-light-9);
}

.check-result.is-failed {
  background: var(--el-color-danger-light-9);
}

.result-text {
  font-weight: 500;
}

.violations-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.violation-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: white;
  border-radius: 6px;
}

.violation-message {
  font-size: 13px;
  color: var(--el-text-color-regular);
}
</style>
