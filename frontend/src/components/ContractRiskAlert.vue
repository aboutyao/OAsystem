<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning, CircleCheck, InfoFilled } from '@element-plus/icons-vue'

interface Risk {
  type: string
  typeName: string
  severity: string
  description: string
}

const props = defineProps<{
  contractId: number
}>()

const loading = ref(false)
const risks = ref<Risk[]>([])
const report = ref<any>(null)

async function loadRisks() {
  loading.value = true
  try {
    const [risksRes, reportRes] = await Promise.all([
      fetch(`/api/contracts/${props.contractId}/risks`),
      fetch('/api/contracts/risks/report'),
    ])
    const risksData = await risksRes.json()
    const reportData = await reportRes.json()

    risks.value = risksData.data || []
    report.value = reportData.data
  } catch (e) {
    console.error('Failed to load risks:', e)
  } finally {
    loading.value = false
  }
}

function getSeverityType(severity: string): 'danger' | 'warning' | 'info' {
  switch (severity) {
    case 'HIGH': return 'danger'
    case 'MEDIUM': return 'warning'
    default: return 'info'
  }
}

function getSeverityLabel(severity: string): string {
  switch (severity) {
    case 'HIGH': return '高风险'
    case 'MEDIUM': return '中风险'
    default: return '低风险'
  }
}

onMounted(loadRisks)
</script>

<template>
  <div class="contract-risk-alert" v-loading="loading">
    <!-- 风险报告 -->
    <div v-if="report" class="risk-report">
      <div class="report-item">
        <span class="report-label">已过期</span>
        <span class="report-value danger">{{ report.expiredCount }}</span>
      </div>
      <div class="report-item">
        <span class="report-label">即将到期</span>
        <span class="report-value warning">{{ report.expiringCount }}</span>
      </div>
      <div class="report-item">
        <span class="report-label">大额合同</span>
        <span class="report-value info">{{ report.largeAmountCount }}</span>
      </div>
    </div>

    <!-- 风险列表 -->
    <div v-if="risks.length > 0" class="risks-list">
      <div v-for="(risk, index) in risks" :key="index" class="risk-item">
        <el-icon class="risk-icon" :color="risk.severity === 'HIGH' ? '#F56C6C' : '#E6A23C'">
          <Warning />
        </el-icon>
        <div class="risk-content">
          <div class="risk-header">
            <span class="risk-type">{{ risk.typeName }}</span>
            <el-tag :type="getSeverityType(risk.severity)" size="small">
              {{ getSeverityLabel(risk.severity) }}
            </el-tag>
          </div>
          <div class="risk-desc">{{ risk.description }}</div>
        </div>
      </div>
    </div>

    <div v-if="risks.length === 0 && !loading" class="no-risk">
      <el-icon :size="24" color="#67C23A"><CircleCheck /></el-icon>
      <span>未发现风险</span>
    </div>
  </div>
</template>

<style scoped>
.contract-risk-alert {
  padding: 16px;
}

.risk-report {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.report-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.report-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.report-value {
  font-size: 20px;
  font-weight: bold;
}

.report-value.danger { color: #F56C6C; }
.report-value.warning { color: #E6A23C; }
.report-value.info { color: #909399; }

.risks-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.risk-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.risk-icon {
  margin-top: 2px;
}

.risk-content {
  flex: 1;
}

.risk-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.risk-type {
  font-weight: 500;
}

.risk-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.no-risk {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  color: #67C23A;
}
</style>
