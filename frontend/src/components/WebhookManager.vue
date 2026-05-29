<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Refresh } from '@element-plus/icons-vue'

interface Webhook {
  id: number
  name: string
  url: string
  eventType: string
  status: string
  createdAt: string
}

const loading = ref(false)
const webhooks = ref<Webhook[]>([])
const showAddDialog = ref(false)

const newWebhook = ref({
  name: '',
  url: '',
  eventType: 'WORKFLOW_CREATED',
  secret: '',
})

const eventTypes = [
  { value: 'WORKFLOW_CREATED', label: '流程创建' },
  { value: 'WORKFLOW_APPROVED', label: '流程审批通过' },
  { value: 'WORKFLOW_REJECTED', label: '流程驳回' },
  { value: 'EXPENSE_SUBMITTED', label: '报销提交' },
  { value: 'PURCHASE_SUBMITTED', label: '采购提交' },
  { value: 'CONTRACT_EXPIRING', label: '合同即将到期' },
]

async function loadWebhooks() {
  loading.value = true
  try {
    const response = await fetch('/api/webhooks')
    const data = await response.json()
    webhooks.value = data.data || []
  } catch (e) {
    console.error('Failed to load webhooks:', e)
  } finally {
    loading.value = false
  }
}

async function addWebhook() {
  if (!newWebhook.value.name || !newWebhook.value.url) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    await fetch('/api/webhooks', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newWebhook.value),
    })
    ElMessage.success('添加成功')
    showAddDialog.value = false
    newWebhook.value = { name: '', url: '', eventType: 'WORKFLOW_CREATED', secret: '' }
    loadWebhooks()
  } catch (e) {
    ElMessage.error('添加失败')
  }
}

async function deleteWebhook(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除此Webhook吗？', '确认删除', { type: 'warning' })
    await fetch(`/api/webhooks/${id}`, { method: 'DELETE' })
    ElMessage.success('删除成功')
    loadWebhooks()
  } catch (e) {
    // 取消
  }
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(loadWebhooks)
</script>

<template>
  <div class="webhook-manager">
    <div class="manager-header">
      <h3>Webhook 管理</h3>
      <el-button type="primary" :icon="Plus" @click="showAddDialog = true">添加</el-button>
    </div>

    <el-table v-loading="loading" :data="webhooks" stripe>
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="url" label="URL" show-overflow-tooltip />
      <el-table-column prop="eventType" label="事件类型" />
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
          <el-button type="danger" link :icon="Delete" @click="deleteWebhook(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showAddDialog" title="添加 Webhook" width="500px">
      <el-form label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="newWebhook.name" placeholder="Webhook名称" />
        </el-form-item>
        <el-form-item label="URL" required>
          <el-input v-model="newWebhook.url" placeholder="https://example.com/webhook" />
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="newWebhook.eventType" style="width: 100%">
            <el-option v-for="et in eventTypes" :key="et.value" :label="et.label" :value="et.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="密钥">
          <el-input v-model="newWebhook.secret" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="addWebhook">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.webhook-manager {
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
