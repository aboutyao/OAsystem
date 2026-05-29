<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

interface NodeAnalysis {
  nodeName: string
  avgDurationHours: number
  count: number
  maxDurationHours: number
}

interface ApproverEfficiency {
  userId: number
  userName: string
  avgResponseHours: number
  totalApprovals: number
  pendingCount: number
}

interface Bottleneck {
  type: string
  name: string
  detail: string
  severity: string
}

const loading = ref(false)
const nodeAnalysis = ref<NodeAnalysis[]>([])
const approverRanking = ref<ApproverEfficiency[]>([])
const bottlenecks = ref<Bottleneck[]>([])

async function loadAnalytics() {
  loading.value = true
  try {
    const response = await fetch('/api/workflow/analytics/bottlenecks')
    const data = await response.json()
    nodeAnalysis.value = data.data?.nodeAnalysis || []
    approverRanking.value = data.data?.approverRanking || []
    bottlenecks.value = data.data?.bottlenecks || []
  } catch (e) {
    console.error('Failed to load analytics:', e)
  } finally {
    loading.value = false
  }
}

const topSlowNodes = computed(() => {
  return [...nodeAnalysis.value]
    .sort((a, b) => b.avgDurationHours - a.avgDurationHours)
    .slice(0, 5)
})

const topSlowApprovers = computed(() => {
  return [...approverRanking.value]
    .sort((a, b) => b.avgResponseHours - a.avgResponseHours)
    .slice(0, 5)
})

function getDurationColor(hours: number): string {
  if (hours > 48) return '#F56C6C'
  if (hours > 24) return '#E6A23C'
  return '#67C23A'
}

onMounted(loadAnalytics)
</script>

<template>
  <div class="bottleneck-chart" v-loading="loading">
    <div class="chart-section">
      <h4>节点耗时排名（慢 → 快）</h4>
      <div class="bar-chart">
        <div v-for="node in topSlowNodes" :key="node.nodeName" class="bar-item">
          <div class="bar-label">{{ node.nodeName }}</div>
          <div class="bar-track">
            <div
              class="bar-fill"
              :style="{
                width: `${Math.min((node.avgDurationHours / 72) * 100, 100)}%`,
                backgroundColor: getDurationColor(node.avgDurationHours)
              }"
            ></div>
          </div>
          <div class="bar-value">{{ node.avgDurationHours.toFixed(1) }}h</div>
        </div>
      </div>
    </div>

    <div class="chart-section">
      <h4>审批人效率排名（慢 → 快）</h4>
      <div class="bar-chart">
        <div v-for="approver in topSlowApprovers" :key="approver.userId" class="bar-item">
          <div class="bar-label">{{ approver.userName }}</div>
          <div class="bar-track">
            <div
              class="bar-fill"
              :style="{
                width: `${Math.min((approver.avgResponseHours / 72) * 100, 100)}%`,
                backgroundColor: getDurationColor(approver.avgResponseHours)
              }"
            ></div>
          </div>
          <div class="bar-value">{{ approver.avgResponseHours.toFixed(1) }}h</div>
        </div>
      </div>
    </div>

    <div v-if="bottlenecks.length > 0" class="bottleneck-list">
      <h4>识别到的瓶颈</h4>
      <div v-for="(bottleneck, index) in bottlenecks" :key="index" class="bottleneck-item">
        <el-tag :type="bottleneck.severity === 'HIGH' ? 'danger' : 'warning'" size="small">
          {{ bottleneck.type }}
        </el-tag>
        <span class="bottleneck-name">{{ bottleneck.name }}</span>
        <span class="bottleneck-detail">{{ bottleneck.detail }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bottleneck-chart {
  padding: 16px;
}

.chart-section {
  margin-bottom: 24px;
}

.chart-section h4 {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
  color: var(--el-text-color-primary);
}

.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bar-label {
  width: 120px;
  font-size: 13px;
  text-align: right;
  color: var(--el-text-color-regular);
}

.bar-track {
  flex: 1;
  height: 20px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.bar-value {
  width: 60px;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.bottleneck-list h4 {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
}

.bottleneck-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  margin-bottom: 6px;
}

.bottleneck-name {
  font-weight: 500;
}

.bottleneck-detail {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
