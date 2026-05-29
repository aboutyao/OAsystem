<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { TrendCharts, User, Timer } from '@element-plus/icons-vue'

interface SmartApprover {
  userId: number
  userName: string
  totalScore: number
  avgResponseHours: number
  currentWorkload: number
  totalApprovals: number
}

const props = defineProps<{
  roleCode: string
  businessType?: string
  amount?: number
}>()

const emit = defineEmits<{
  select: [approver: SmartApprover]
}>()

const loading = ref(false)
const approvers = ref<SmartApprover[]>([])

async function loadRecommendations() {
  loading.value = true
  try {
    // 调用智能推荐API
    const response = await fetch(`/api/workflow/smart-approvals/recommend?roleCode=${props.roleCode}&businessType=${props.businessType || ''}&amount=${props.amount || 0}`)
    const data = await response.json()
    approvers.value = data.data || []
  } catch (e) {
    console.error('Failed to load recommendations:', e)
  } finally {
    loading.value = false
  }
}

function selectApprover(approver: SmartApprover) {
  emit('select', approver)
}

function getScoreColor(score: number): string {
  if (score >= 0.8) return '#67C23A'
  if (score >= 0.6) return '#E6A23C'
  return '#F56C6C'
}

function getScoreLabel(score: number): string {
  if (score >= 0.8) return '强烈推荐'
  if (score >= 0.6) return '推荐'
  return '可选'
}

onMounted(loadRecommendations)
</script>

<template>
  <div class="smart-approval-panel" v-loading="loading">
    <div class="panel-header">
      <el-icon><TrendCharts /></el-icon>
      <span>智能推荐审批人</span>
    </div>

    <div v-if="approvers.length === 0 && !loading" class="empty-hint">
      暂无推荐
    </div>

    <div v-else class="approver-list">
      <div
        v-for="(approver, index) in approvers"
        :key="approver.userId"
        class="approver-item"
        :class="{ 'is-top': index === 0 }"
        @click="selectApprover(approver)"
      >
        <div class="approver-rank">
          <span class="rank-number">{{ index + 1 }}</span>
        </div>

        <div class="approver-info">
          <div class="approver-name">{{ approver.userName }}</div>
          <div class="approver-metrics">
            <span class="metric">
              <el-icon><Timer /></el-icon>
              平均 {{ approver.avgResponseHours.toFixed(1) }}h
            </span>
            <span class="metric">
              <el-icon><User /></el-icon>
              待办 {{ approver.currentWorkload }}
            </span>
          </div>
        </div>

        <div class="approver-score">
          <div class="score-bar" :style="{ width: `${approver.totalScore * 100}%`, backgroundColor: getScoreColor(approver.totalScore) }"></div>
          <span class="score-label" :style="{ color: getScoreColor(approver.totalScore) }">{{ getScoreLabel(approver.totalScore) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.smart-approval-panel {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 12px;
  color: var(--el-text-color-primary);
}

.approver-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.approver-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.approver-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.approver-item.is-top {
  border: 2px solid var(--el-color-success);
}

.approver-rank {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--el-color-primary-light-9);
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-number {
  font-weight: bold;
  color: var(--el-color-primary);
}

.approver-info {
  flex: 1;
}

.approver-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.approver-metrics {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.metric {
  display: flex;
  align-items: center;
  gap: 4px;
}

.approver-score {
  width: 100px;
}

.score-bar {
  height: 6px;
  border-radius: 3px;
  margin-bottom: 4px;
}

.score-label {
  font-size: 12px;
  font-weight: 500;
}

.empty-hint {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 20px;
}
</style>
