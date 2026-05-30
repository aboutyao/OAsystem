<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Plus, Delete, View, Warning, UploadFilled, Select } from '@element-plus/icons-vue'

interface Task {
  id: number
  title: string
  nodeName: string
}

const props = defineProps<{
  visible: boolean
  tasks: Task[]
  action: 'APPROVE' | 'REJECT'
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const comment = ref('')
const loading = ref(false)

async function handleSubmit() {
  if (props.tasks.length === 0) {
    ElMessage.warning('请选择要审批的任务')
    return
  }

  const actionText = props.action === 'APPROVE' ? '通过' : '驳回'
  try {
    await ElMessageBox.confirm(
      `确定要${actionText} ${props.tasks.length} 条任务吗？`,
      `批量${actionText}`,
      { type: props.action === 'APPROVE' ? 'success' : 'warning' }
    )
  } catch {
    return
  }

  loading.value = true
  try {
    const taskIds = props.tasks.map(t => t.id)
    const response = await fetch('/api/workflow/batch-approve', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        taskIds,
        action: props.action,
        comment: comment.value,
      }),
    })
    const result = await response.json()

    if (result.data?.successCount > 0) {
      ElMessage.success(`成功${actionText} ${result.data.successCount} 条任务`)
      emit('success')
      emit('update:visible', false)
    }
    if (result.data?.failCount > 0) {
      ElMessage.warning(`${result.data.failCount} 条任务${actionText}失败`)
    }
  } catch (e) {
    ElMessage.error('批量操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    :title="action === 'APPROVE' ? '批量通过' : '批量驳回'"
    width="500px"
  >
    <div class="batch-content">
      <div class="task-summary">
        <p>已选择 <strong>{{ tasks.length }}</strong> 条任务</p>
        <div class="task-list">
          <div v-for="task in tasks.slice(0, 5)" :key="task.id" class="task-item">
            <span class="task-title">{{ task.title }}</span>
            <span class="task-node">{{ task.nodeName }}</span>
          </div>
          <div v-if="tasks.length > 5" class="task-more">
            还有 {{ tasks.length - 5 }} 条...
          </div>
        </div>
      </div>

      <el-input
        v-model="comment"
        type="textarea"
        :rows="3"
        :placeholder="action === 'APPROVE' ? '审批意见（选填）' : '驳回原因（必填）'"
      />
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button
        :type="action === 'APPROVE' ? 'success' : 'danger'"
        :icon="action === 'APPROVE' ? Select : Delete"
        :loading="loading"
        @click="handleSubmit"
      >
        {{ action === 'APPROVE' ? '确认通过' : '确认驳回' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.batch-content {
  margin-bottom: 16px;
}

.task-summary p {
  margin-bottom: 12px;
  color: var(--el-text-color-regular);
}

.task-list {
  background: var(--el-fill-color-lighter);
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.task-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.task-item:last-child {
  border-bottom: none;
}

.task-title {
  font-weight: 500;
}

.task-node {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.task-more {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding-top: 8px;
  font-size: 13px;
}
</style>
