<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElTag } from 'element-plus'
import { Document, Plus, Delete, View, Warning, UploadFilled } from '@element-plus/icons-vue'

interface AnomalyRecord {
  type: string
  typeName: string
  description: string
  severity: string
  relatedId: number | null
}

const props = defineProps<{
  userId: number
}>()

const anomalies = ref<AnomalyRecord[]>([])
const loading = ref(false)

async function loadAnomalies() {
  loading.value = true
  try {
    const response = await fetch(`/api/workflow/anomalies?userId=${props.userId}`)
    const data = await response.json()
    anomalies.value = data.data || []
  } catch (e) {
    console.error('Failed to load anomalies:', e)
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

onMounted(loadAnomalies)
</script>

<template>
  <div class="anomaly-alert" v-loading="loading">
    <div v-if="anomalies.length > 0" class="anomaly-list">
      <div
        v-for="(anomaly, index) in anomalies"
        :key="index"
        class="anomaly-item"
      >
        <el-icon class="anomaly-icon" :color="anomaly.severity === 'HIGH' ? '#F56C6C' : '#E6A23C'">
          <Warning />
        </el-icon>
        <div class="anomaly-content">
          <div class="anomaly-header">
            <span class="anomaly-type">{{ anomaly.typeName }}</span>
            <el-tag :type="getSeverityType(anomaly.severity)" size="small">
              {{ getSeverityLabel(anomaly.severity) }}
            </el-tag>
          </div>
          <div class="anomaly-desc">{{ anomaly.description }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.anomaly-alert {
  padding: 12px;
}

.anomaly-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.anomaly-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  border-left: 3px solid var(--el-color-warning);
}

.anomaly-icon {
  margin-top: 2px;
}

.anomaly-content {
  flex: 1;
}

.anomaly-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.anomaly-type {
  font-weight: 500;
}

.anomaly-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
