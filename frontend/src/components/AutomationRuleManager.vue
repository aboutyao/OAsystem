<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Plus, Delete, View, Warning, UploadFilled } from '@element-plus/icons-vue'

interface AutomationRule {
  id: number
  name: string
  triggerType: string
  conditionExpr: string
  actionExpr: string
  status: string
  createdAt: string
}

const loading = ref(false)
const rules = ref<AutomationRule[]>([])
const showAddDialog = ref(false)

const newRule = ref({
  name: '',
  triggerType: 'CONTRACT_EXPIRING',
  conditionExpr: '',
  actionExpr: 'NOTIFY',
})

const triggerTypes = [
  { value: 'CONTRACT_EXPIRING', label: '合同即将到期' },
  { value: 'BUDGET_EXCEEDED', label: '预算接近超支' },
  { value: 'LEAVE_BALANCE_LOW', label: '假期余额不足' },
  { value: 'SCHEDULED', label: '定时触发' },
]

const actionTypes = [
  { value: 'NOTIFY', label: '发送通知' },
  { value: 'CREATE_TASK', label: '创建任务' },
  { value: 'UPDATE_STATUS', label: '更新状态' },
]

async function loadRules() {
  loading.value = true
  try {
    const response = await fetch('/api/automation/rules')
    const data = await response.json()
    rules.value = data.data || []
  } catch (e) {
    console.error('Failed to load rules:', e)
  } finally {
    loading.value = false
  }
}

async function addRule() {
  if (!newRule.value.name) {
    ElMessage.warning('请填写规则名称')
    return
  }

  try {
    await fetch('/api/automation/rules', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newRule.value),
    })
    ElMessage.success('添加成功')
    showAddDialog.value = false
    newRule.value = { name: '', triggerType: 'CONTRACT_EXPIRING', conditionExpr: '', actionExpr: 'NOTIFY' }
    loadRules()
  } catch (e) {
    ElMessage.error('添加失败')
  }
}

async function deleteRule(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除此规则吗？', '确认删除', { type: 'warning' })
    await fetch(`/api/automation/rules/${id}`, { method: 'DELETE' })
    ElMessage.success('删除成功')
    loadRules()
  } catch (e) {
    // 取消
  }
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(loadRules)
</script>

<template>
  <div class="automation-rule-manager">
    <div class="manager-header">
      <h3>自动化规则</h3>
      <el-button type="primary" :icon="Plus" @click="showAddDialog = true">添加规则</el-button>
    </div>

    <el-table v-loading="loading" :data="rules" stripe>
      <el-table-column prop="name" label="规则名称" />
      <el-table-column prop="triggerType" label="触发条件" />
      <el-table-column prop="actionExpr" label="执行动作" />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="danger" link :icon="Delete" @click="deleteRule(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showAddDialog" title="添加自动化规则" width="500px">
      <el-form label-width="100px">
        <el-form-item label="规则名称" required>
          <el-input v-model="newRule.name" placeholder="规则名称" />
        </el-form-item>
        <el-form-item label="触发条件">
          <el-select v-model="newRule.triggerType" style="width: 100%">
            <el-option v-for="tt in triggerTypes" :key="tt.value" :label="tt.label" :value="tt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行动作">
          <el-select v-model="newRule.actionExpr" style="width: 100%">
            <el-option v-for="at in actionTypes" :key="at.value" :label="at.label" :value="at.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="addRule">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.automation-rule-manager {
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
