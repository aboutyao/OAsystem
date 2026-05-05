<script setup lang="ts">
import { formatDisplayDateTime } from '../views/oa/oa-shared'
import type { WfTimelineItem } from '../api/types'

defineProps<{
  items: WfTimelineItem[]
}>()

function actionType(action: string): 'success' | 'danger' | 'warning' | 'info' | 'primary' {
  switch (action) {
    case 'APPROVE': return 'success'
    case 'REJECT': return 'danger'
    case 'SUBMIT': return 'warning'
    case 'WITHDRAW': return 'info'
    case 'CANCEL': return 'info'
    case 'ADD_SIGN': return 'primary'
    default: return 'primary'
  }
}

function actionLabel(action: string): string {
  switch (action) {
    case 'APPROVE': return '通过'
    case 'REJECT': return '驳回'
    case 'SUBMIT': return '提交'
    case 'WITHDRAW': return '撤回'
    case 'CANCEL': return '作废'
    case 'ADD_SIGN': return '加签'
    default: return action
  }
}
</script>

<template>
  <div v-if="items.length === 0" class="oa-timeline-empty">
    <el-empty description="暂无审批记录" :image-size="80" />
  </div>
  <el-timeline v-else>
    <el-timeline-item
      v-for="(item, idx) in items"
      :key="idx"
      :timestamp="formatDisplayDateTime(item.operatedAt)"
      :type="actionType(item.action)"
    >
      <div class="oa-timeline-item__header">
        <el-tag size="small" :type="actionType(item.action)">{{ actionLabel(item.action) }}</el-tag>
        <span class="oa-timeline-item__operator">{{ item.operatorName }}</span>
        <span v-if="item.nodeName" class="oa-timeline-item__node">· 节点 {{ item.nodeName }}</span>
      </div>
      <div v-if="item.comment" class="oa-timeline-item__comment muted">{{ item.comment }}</div>
    </el-timeline-item>
  </el-timeline>
</template>

<style scoped>
.oa-timeline-empty {
  padding: 20px 0;
}
.oa-timeline-item__header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.oa-timeline-item__operator {
  font-weight: 500;
}
.oa-timeline-item__node {
  color: var(--oa-text-secondary);
}
.oa-timeline-item__comment {
  margin-top: 4px;
  font-size: 13px;
}
.muted {
  color: var(--oa-text-secondary);
}
</style>
