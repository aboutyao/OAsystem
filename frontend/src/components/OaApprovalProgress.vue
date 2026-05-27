<script setup lang="ts">
import { computed } from 'vue'

interface TimelineItem {
  action: string
  operatorName: string
  nodeName?: string
  operatedAt: string
  comment?: string
}

const props = defineProps<{
  timeline: TimelineItem[]
  currentNodeName?: string
  slaDeadline?: string
  slaBreached?: boolean | number
}>()

const slaStatus = computed(() => {
  if (props.slaBreached === 1 || props.slaBreached === true) {
    return { level: 'danger' as const, text: '已超时' }
  }
  if (!props.slaDeadline) return null
  const deadline = new Date(props.slaDeadline).getTime()
  const now = Date.now()
  const remaining = deadline - now
  if (remaining <= 0) return { level: 'danger' as const, text: '已超时' }
  if (remaining < 4 * 3600000) {
    return { level: 'warning' as const, text: `${Math.round(remaining / 3600000)}小时后超时` }
  }
  return { level: 'success' as const, text: '正常' }
})

function actionLabel(action: string): string {
  switch (action) {
    case 'SUBMIT': return '发起'
    case 'APPROVE': return '通过'
    case 'REJECT': return '驳回'
    case 'WITHDRAW': return '撤回'
    case 'CANCEL': return '作废'
    case 'TRANSFER': return '转交'
    case 'ADD_SIGN': return '加签'
    default: return action
  }
}

function actionColor(action: string): string {
  switch (action) {
    case 'APPROVE': return '#22c55e'
    case 'REJECT': return '#ef4444'
    case 'WITHDRAW':
    case 'CANCEL': return '#f59e0b'
    case 'SUBMIT': return '#3b82f6'
    default: return '#6b7280'
  }
}
</script>

<template>
  <div class="oa-approval-progress">
    <div v-if="currentNodeName" class="progress-current">
      <span class="label">当前节点：</span>
      <el-tag type="warning">{{ currentNodeName }}</el-tag>
      <el-tag v-if="slaStatus" :type="slaStatus.level" style="margin-left: 8px">
        SLA: {{ slaStatus.text }}
      </el-tag>
    </div>
    <div v-if="timeline.length > 0" class="progress-timeline">
      <div v-for="(item, idx) in timeline" :key="idx" class="timeline-item">
        <div class="timeline-line">
          <div
            class="timeline-dot"
            :class="{ 'is-active': idx === timeline.length - 1 }"
            :style="{ borderColor: actionColor(item.action) }"
          />
          <div v-if="idx < timeline.length - 1" class="timeline-connector" />
        </div>
        <div class="timeline-content">
          <div class="timeline-header">
            <span class="timeline-action" :style="{ color: actionColor(item.action) }">
              {{ actionLabel(item.action) }}
            </span>
            <span class="timeline-operator">{{ item.operatorName ?? '系统' }}</span>
            <span v-if="item.nodeName" class="timeline-node">{{ item.nodeName }}</span>
          </div>
          <div v-if="item.comment" class="timeline-comment">{{ item.comment }}</div>
          <div class="timeline-time">{{ item.operatedAt }}</div>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无审批记录" :image-size="60" />
  </div>
</template>

<style scoped>
.oa-approval-progress {
  margin: 16px 0;
}

.progress-current {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}

.progress-current .label {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.progress-timeline {
  position: relative;
  padding-left: 28px;
}

.timeline-item {
  position: relative;
  padding-bottom: 20px;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-line {
  position: absolute;
  left: -28px;
  top: 0;
  bottom: 0;
  width: 28px;
}

.timeline-dot {
  position: absolute;
  left: 8px;
  top: 4px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--el-bg-color);
  border: 2px solid var(--el-border-color);
  z-index: 1;
}

.timeline-dot.is-active {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

.timeline-connector {
  position: absolute;
  left: 13px;
  top: 16px;
  bottom: 0;
  width: 2px;
  background: var(--el-border-color-lighter);
}

.timeline-content {
  font-size: 14px;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.timeline-action {
  font-weight: 600;
}

.timeline-operator {
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.timeline-node {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  background: var(--el-fill-color);
  padding: 1px 6px;
  border-radius: 4px;
}

.timeline-comment {
  color: var(--el-text-color-regular);
  font-size: 13px;
  margin-top: 4px;
  padding: 6px 10px;
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
}

.timeline-time {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  margin-top: 4px;
}
</style>
