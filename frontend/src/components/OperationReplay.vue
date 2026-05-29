<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTimeline, ElTimelineItem, ElTag } from 'element-plus'
import { VideoPlay, Edit, Delete, Check } from '@element-plus/icons-vue'

interface ReplayFrame {
  timestamp: string
  action: string
  operatorName: string
  beforeState: string | null
  afterState: string | null
}

const props = defineProps<{
  entityType: string
  entityId: number
}>()

const loading = ref(false)
const frames = ref<ReplayFrame[]>([])

async function loadReplay() {
  loading.value = true
  try {
    const response = await fetch(`/api/audit/replay?entityType=${props.entityType}&entityId=${props.entityId}`)
    const data = await response.json()
    frames.value = data.data || []
  } catch (e) {
    console.error('Failed to load replay:', e)
  } finally {
    loading.value = false
  }
}

function getActionIcon(action: string) {
  switch (action) {
    case 'CREATE': return VideoPlay
    case 'UPDATE': return Edit
    case 'DELETE': return Delete
    case 'APPROVE': return Check
    default: return Edit
  }
}

function getActionColor(action: string): string {
  switch (action) {
    case 'CREATE': return '#67C23A'
    case 'UPDATE': return '#409EFF'
    case 'DELETE': return '#F56C6C'
    case 'APPROVE': return '#67C23A'
    case 'REJECT': return '#F56C6C'
    default: return '#909399'
  }
}

function formatTime(timestamp: string): string {
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN')
}

onMounted(loadReplay)
</script>

<template>
  <div class="operation-replay" v-loading="loading">
    <div class="replay-header">
      <el-icon><VideoPlay /></el-icon>
      <span>操作回放</span>
    </div>

    <div v-if="frames.length === 0 && !loading" class="empty-hint">
      暂无操作记录
    </div>

    <el-timeline v-else>
      <el-timeline-item
        v-for="(frame, index) in frames"
        :key="index"
        :timestamp="formatTime(frame.timestamp)"
        :hollow="index !== frames.length - 1"
        :color="getActionColor(frame.action)"
      >
        <div class="frame-content">
          <div class="frame-header">
            <el-tag :type="frame.action === 'DELETE' ? 'danger' : frame.action === 'APPROVE' ? 'success' : 'info'" size="small">
              {{ frame.action }}
            </el-tag>
            <span class="operator">{{ frame.operatorName }}</span>
          </div>

          <div v-if="frame.beforeState || frame.afterState" class="frame-changes">
            <div v-if="frame.beforeState" class="change-item old">
              <span class="change-label">之前：</span>
              <span class="change-value">{{ frame.beforeState }}</span>
            </div>
            <div v-if="frame.afterState" class="change-item new">
              <span class="change-label">之后：</span>
              <span class="change-value">{{ frame.afterState }}</span>
            </div>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<style scoped>
.operation-replay {
  padding: 16px;
}

.replay-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 16px;
}

.frame-content {
  padding: 8px 0;
}

.frame-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.operator {
  font-weight: 500;
}

.frame-changes {
  background: var(--el-fill-color-lighter);
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
}

.change-item {
  margin-bottom: 4px;
}

.change-item:last-child {
  margin-bottom: 0;
}

.change-label {
  color: var(--el-text-color-secondary);
}

.change-item.old .change-value {
  color: var(--el-color-danger);
  text-decoration: line-through;
}

.change-item.new .change-value {
  color: var(--el-color-success);
}

.empty-hint {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 20px;
}
</style>
