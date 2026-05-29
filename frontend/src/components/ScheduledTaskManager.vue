<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, VideoPlay, VideoPause, Refresh } from '@element-plus/icons-vue'

interface ScheduledTask {
  id: number
  name: string
  description: string
  cronExpression: string
  taskType: string
  status: string
  lastRunAt: string | null
  nextRunAt: string | null
  runCount: number
}

const loading = ref(false)
const tasks = ref<ScheduledTask[]>([])
const showAddDialog = ref(false)

const newTask = ref({
  name: '',
  description: '',
  cronExpression: '0 0 * * *',
  taskType: 'DATA_SYNC',
})

const taskTypes = [
  { value: 'DATA_SYNC', label: '数据同步' },
  { value: 'REPORT_GENERATE', label: '报表生成' },
  { value: 'CLEANUP', label: '数据清理' },
  { value: 'NOTIFICATION', label: '通知发送' },
]

async function loadTasks() {
  loading.value = true
  try {
    const response = await fetch('/api/scheduler/tasks')
    const data = await response.json()
    tasks.value = data.data || []
  } catch (e) {
    console.error('Failed to load tasks:', e)
  } finally {
    loading.value = false
  }
}

async function addTask() {
  if (!newTask.value.name) {
    ElMessage.warning('请填写任务名称')
    return
  }

  try {
    await fetch('/api/scheduler/tasks', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newTask.value),
    })
    ElMessage.success('添加成功')
    showAddDialog.value = false
    newTask.value = { name: '', description: '', cronExpression: '0 0 * * *', taskType: 'DATA_SYNC' }
    loadTasks()
  } catch (e) {
    ElMessage.error('添加失败')
  }
}

async function toggleTask(task: ScheduledTask) {
  const enable = task.status !== 'ACTIVE'
  try {
    await fetch(`/api/scheduler/tasks/${task.id}/toggle?enable=${enable}`, { method: 'POST' })
    ElMessage.success(enable ? '已启用' : '已暂停')
    loadTasks()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function executeTask(task: ScheduledTask) {
  try {
    await ElMessageBox.confirm(`确定要立即执行任务 "${task.name}" 吗？`, '确认执行', { type: 'warning' })
    await fetch(`/api/scheduler/tasks/${task.id}/execute`, { method: 'POST' })
    ElMessage.success('执行成功')
    loadTasks()
  } catch (e) {
    // 取消
  }
}

function formatDate(dateStr: string | null): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(loadTasks)
</script>

<template>
  <div class="scheduled-task-manager">
    <div class="manager-header">
      <h3>定时任务管理</h3>
      <el-button type="primary" :icon="Plus" @click="showAddDialog = true">添加任务</el-button>
    </div>

    <el-table v-loading="loading" :data="tasks" stripe>
      <el-table-column prop="name" label="任务名称" />
      <el-table-column prop="taskType" label="类型" width="100" />
      <el-table-column prop="cronExpression" label="Cron表达式" width="120" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ row.status === 'ACTIVE' ? '运行中' : '已暂停' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastRunAt" label="上次执行">
        <template #default="{ row }">{{ formatDate(row.lastRunAt) }}</template>
      </el-table-column>
      <el-table-column prop="nextRunAt" label="下次执行">
        <template #default="{ row }">{{ formatDate(row.nextRunAt) }}</template>
      </el-table-column>
      <el-table-column prop="runCount" label="执行次数" width="80" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            link
            :icon="row.status === 'ACTIVE' ? VideoPause : VideoPlay"
            @click="toggleTask(row)"
          >{{ row.status === 'ACTIVE' ? '暂停' : '启用' }}</el-button>
          <el-button type="primary" link :icon="Refresh" @click="executeTask(row)">执行</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showAddDialog" title="添加定时任务" width="500px">
      <el-form label-width="100px">
        <el-form-item label="任务名称" required>
          <el-input v-model="newTask.name" placeholder="任务名称" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="newTask.description" placeholder="任务描述" />
        </el-form-item>
        <el-form-item label="Cron表达式">
          <el-input v-model="newTask.cronExpression" placeholder="0 0 * * *" />
        </el-form-item>
        <el-form-item label="任务类型">
          <el-select v-model="newTask.taskType" style="width: 100%">
            <el-option v-for="tt in taskTypes" :key="tt.value" :label="tt.label" :value="tt.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="addTask">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.scheduled-task-manager {
  padding: 16px;
}

.manager-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.manager-header h3 {
  margin: 0;
}
</style>
