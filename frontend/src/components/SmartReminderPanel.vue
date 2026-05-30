<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Plus, Delete, View, Warning, UploadFilled, Clock } from '@element-plus/icons-vue'

interface ReminderAnalysis {
  taskId: number
  assigneeId: number
  bestReminderTime: string
  recommendMethod: string
  pattern: {
    mostActiveHour: number
    avgResponseHours: number
    pendingCount: number
  }
}

const props = defineProps<{
  taskId: number
}>()

const loading = ref(false)
const analysis = ref<ReminderAnalysis | null>(null)

async function analyzeReminder() {
  loading.value = true
  try {
    const response = await fetch(`/api/workflow/smart-reminder/analyze/${props.taskId}`)
    const data = await response.json()
    analysis.value = data.data
  } catch (e) {
    console.error('Failed to analyze reminder:', e)
  } finally {
    loading.value = false
  }
}

async function sendReminder() {
  try {
    await fetch(`/api/workflow/smart-reminder/send/${props.taskId}`, { method: 'POST' })
    ElMessage.success('催办已发送')
  } catch (e) {
    ElMessage.error('催办失败')
  }
}

function getMethodIcon(method: string) {
  switch (method) {
    case '即时消息': return Document
    case '邮件': return Document
    default: return Document
  }
}

onMounted(analyzeReminder)
</script>

<template>
  <div class="smart-reminder-panel" v-loading="loading">
    <div class="panel-header">
      <el-icon><Warning /></el-icon>
      <span>智能催办</span>
    </div>

    <div v-if="analysis" class="analysis-content">
      <div class="analysis-item">
        <el-icon><Clock /></el-icon>
        <div class="item-info">
          <div class="item-label">最佳催办时间</div>
          <div class="item-value">{{ analysis.bestReminderTime }}</div>
        </div>
      </div>

      <div class="analysis-item">
        <el-icon><Document /></el-icon>
        <div class="item-info">
          <div class="item-label">推荐催办方式</div>
          <div class="item-value">{{ analysis.recommendMethod }}</div>
        </div>
      </div>

      <div class="analysis-item">
        <el-icon><Clock /></el-icon>
        <div class="item-info">
          <div class="item-label">平均响应时间</div>
          <div class="item-value">{{ analysis.pattern.avgResponseHours?.toFixed(1) || '-' }} 小时</div>
        </div>
      </div>

      <div class="analysis-item">
        <el-icon><Warning /></el-icon>
        <div class="item-info">
          <div class="item-label">当前待办数</div>
          <div class="item-value">{{ analysis.pattern.pendingCount }}</div>
        </div>
      </div>

      <el-button type="primary" @click="sendReminder" style="width: 100%; margin-top: 12px">
        立即催办
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.smart-reminder-panel {
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 12px;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 16px;
}

.analysis-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.analysis-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
}

.item-info {
  flex: 1;
}

.item-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 2px;
}

.item-value {
  font-weight: 500;
}
</style>
